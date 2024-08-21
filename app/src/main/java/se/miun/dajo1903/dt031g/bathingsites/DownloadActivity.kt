package se.miun.dajo1903.dt031g.bathingsites

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import java.io.File
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.miun.dajo1903.dt031g.bathingsites.databinding.ActivityDownloadBinding
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * The download activity is used to download bathing sites from a specified URL provided in the
 * settings. It implements the DownloadListener to be able to override its single method
 * DownloadListener::onDownloadStart to be notified about a download that started.
 * @author Daniel Jönsson
 * @see AppCompatActivity
 * @see DownloadListener
 */
class DownloadActivity : AppCompatActivity(), DownloadListener {
    /** Instance field variables */
    private val TAG = "DownloadActivity"
    private lateinit var binding: ActivityDownloadBinding
    private lateinit var downloadingView: ConstraintLayout
    private lateinit var appDatabase: AppDatabase
    private lateinit var dbHelper: DatabaseHelperImpl
    private lateinit var webURL: String
    private var downloadProgress: Int = 0

    /**
     * Inflates and binds the view, sets the content view, gets the webURL from the setting
     * activity while also initializing the appDatabase and dbHelper.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        downloadingView = binding.downloadingView
        webURL = SettingsActivity.getDownloadURL(this)
        initDownloadView()
        appDatabase = DatabaseBuilder.getInstance(this)
        dbHelper = DatabaseHelperImpl(appDatabase)
        setWebViewTouchListener(binding.downloadWebView, true)
    }

    /**
     * An private enum class containing two constants determining the state of the download-progress
     * view. If VISIBLE the download has started and the progress bar should be shown and be
     * incremented. When INVISIBLE the download has either not started or just finished and the
     * progress hides itself.
     */
    private enum class STATE {
        VISIBLE,
        INVISIBLE
    }

    /**
     * Finishes the activity once the user presses the back arrow in the action bar.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Inits the download view, checks if the webURL gotten from the settings activity is valid if
     * not then it instead uses the Util class to get the default download URL. The webViewClient
     * also overrides the WebViewClient::shouldOverrideUrlLoading and returns false, this to be able
     * to initialize the download straight from the app instead of being taken to the devices browser.
     */
    private fun initDownloadView() {
        val downloadWebView = binding.downloadWebView
        if (webURL == "") downloadWebView.loadUrl(Util.DOWNLOAD_URL) else downloadWebView.loadUrl(webURL)
        downloadWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }
        downloadWebView.setDownloadListener(this)
    }

    /**
     * Hook called once the user clicks on a download:able link, specifies the url to the clicked file.
     * Gets hold of the file name without the extension with the help of substring. The download path
     * is gotten by specifying the directory to the files system and provides the temporary child to
     * this directory.
     */
    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        url?.run {
            val fileNameWithoutExtension = url.substringAfterLast("/").substringBeforeLast(".")
            val downloadPath = File(this@DownloadActivity.filesDir, fileNameWithoutExtension)
            downloadBathingSites(downloadPath, this@run, fileNameWithoutExtension)
        }
    }

    /**
     * Starts to download the bathing sites, begins to display the download progress and launches
     * the downloading on a background thread and does the following:
     * 1. starts by incrementing the progress bar by 33%
     * 2. Downloads the file to the specified download path using DownloadActivity::downloadFile
     * 3. Increments the progress bar by another 33%
     * 4. Gets a list of bathing sites based on the file just downloaded, extracting content from it
     * and returning a list.
     * 5. Inserts the whole list to the Rooms database by utilizing the dbHelper::insertMany which
     * takes a list of bathing sites and tries to insert them while ignoring sites which have identical
     * coordinates.
     */
    fun downloadBathingSites(downloadToPath: File, url: String, fileName: String) {
        changeDownloadViewState(STATE.VISIBLE, fileName)
        lifecycleScope.launch {
            try {
                incrementProgress(33)
                downloadFile(url, downloadToPath)
                incrementProgress(33)
                val bathingSites = makeBathingSitesFromFile(downloadToPath)
                dbHelper.insertMany(bathingSites)
                incrementProgress(34)
            } catch (e: Exception) {
                Log.e(TAG, "Error while downloading bathing sites: ", e)
                makeSnackBar(getString(R.string.error_downloading, fileName))
            } finally {
                downloadToPath.delete()
                changeDownloadViewState(STATE.INVISIBLE)
            }
        }
    }

    /**
     * Downloads the file by opening a connection based on the url string, the app tries to connect
     * using URLConnection::connect, if the response status code is 200 the function calls another
     * function to download to the specified download path. If it did not succeed and the status code
     * is something other than 200 it logs it to the Logcat and makes a snackbar with a specific string.
     */
    private suspend fun downloadFile(urlString: String, downloadPath: File) {
        withContext(Dispatchers.IO){
            val url = URL(urlString)
            val httpUrlConnection = url.openConnection() as HttpURLConnection
            try {
                httpUrlConnection.connect()
                if (httpUrlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                    downloadVoiceToPath(httpUrlConnection, downloadPath)
                } else {
                    Log.i(TAG,"Http response error: ${httpUrlConnection.responseCode}")
                    makeSnackBar(getString(R.string.http_resp_error, httpUrlConnection.responseCode))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error in downloadFile()", e)
            }
            finally {
                httpUrlConnection.disconnect()
            }
        }
    }

    /**
     * This function is called when an http response of 200 is returned from the connection. It
     * creates a BufferedInputStream out of the connections input stream. It then creates an output
     * stream to copy the input stream to the output stream which in this case is the specified
     * download path. This is done on the IO thread which is made for offloading IO tasks from the
     * main thread by utilizing a thread pool.
     *
     * @param connection The established HttpURLConnection
     * @param downloadPath The path to download file to
     */
    private suspend fun downloadVoiceToPath(connection: HttpURLConnection, downloadPath: File) {
        withContext(Dispatchers.IO) {
            val inputStream = BufferedInputStream(connection.inputStream)
            val outputStream = FileOutputStream(downloadPath)
            try {
                outputStream.use { output ->
                    inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
            }catch (e: Exception) {
                Log.e(TAG, "Error in downloadVoiceToPath()", e)
            }
        }
    }

    /**
     * function responsible for making a list of bathing sites based on a file path to the recently
     * downloaded file. Extracts the content from each line and adds it to the next line.
     *
     * @param filePath The filepath in which the file lies.
     */
    private suspend fun makeBathingSitesFromFile(filePath: File) : List<BathingSite> {
        val bathingSites: MutableList<BathingSite> = mutableListOf()
        return withContext(Dispatchers.IO) {
            val inputStream = FileInputStream(filePath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line = reader.readLine()
            inputStream.use { _ ->
                while (line != null) {
                    val bathingSite = extractContent(line)
                    bathingSites.add(bathingSite)
                    line = reader.readLine()
                }
                return@withContext bathingSites
            }
        }
    }

    /**
     * Extracts the content from a single line from the read file and turns it into a BathingSite
     * entity and returns it.
     *
     * @param line The line containing bathing site information.
     */
    private suspend fun extractContent(line: String) : BathingSite =
        withContext(Dispatchers.IO) {
            val dataList = line.split(",")
            val re = Regex('"'.toString())
            val long = re.replace(dataList[0], "")
            val lat = re.replace(dataList[1], "")
            val name = re.replace(dataList[2], "")
            val address = dataList
            .drop(3)
            .filter {
                it.isNotBlank()
            }
            .joinToString(",")
            .substringBefore("\"")
            .removePrefix(" ")
            .removeSuffix(" ")
            .substringBeforeLast(',')
            return@withContext BathingSite(
                name = name,
                desc = "No description provided.",
                address = address,
                latitude = lat,
                longitude = long,
                waterTemp = "No water tempature provided",
                dateForTemp = "",
                grade = "No rating provided."
            )
        }

    /**
     * Makes a snackbar based on a message.
     *
     * @param msg The message to display
     */
    private fun makeSnackBar(msg: String) {
        Snackbar.make(binding.downloadWebView, msg, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Increments the download progress bar with a specified integer.
     *
     * @param amount The amount in which to increment the download progress.
     */
    private suspend fun incrementProgress(amount: Int) {
        withContext(Dispatchers.Main) {
            downloadProgress += amount
            val downloadProgress = downloadingView.getViewById(R.id.downloadText) as TextView
            val progressBar = downloadingView.getViewById(R.id.downloadProgressbar) as ProgressBar
            downloadProgress.text = getString(R.string.current_progress, this@DownloadActivity.downloadProgress, "%")
            progressBar.progress += amount
            delay(500)
        }
    }

    /**
     * Changes the download progressbar state based on the provided state.
     *
     * @param state The state in which to show or hide the bar
     * @param fileName The filename without extension to display when downloading.
     */
    private fun changeDownloadViewState(state: STATE = STATE.INVISIBLE, fileName: String = "") {
        val downloadText = downloadingView.getViewById(R.id.downloadingText) as TextView
        val downloadProgress = downloadingView.getViewById(R.id.downloadText) as TextView
        val progressBar = downloadingView.getViewById(R.id.downloadProgressbar) as ProgressBar
        when (state) {
            STATE.VISIBLE -> {
                setWebViewTouchListener(binding.downloadWebView, false)
                downloadingView.visibility = View.VISIBLE
                downloadText.text = getString(R.string.downloading_file, fileName)
            }
            else -> {
                downloadText.text = ""
                downloadProgress.text = ""
                this.downloadProgress = 0
                progressBar.progress = 0
                downloadingView.visibility = View.INVISIBLE
                setWebViewTouchListener(binding.downloadWebView, true)
            }
        }
    }

    /**
     * Used to disable the webview while a file is currently being downloaded. This way prevents the
     * user to click multiple files at the same time.
     */
    private fun setWebViewTouchListener(webView: WebView, enableTouch: Boolean) {
        webView.setOnTouchListener { _, _ ->
            webView.performClick()
            return@setOnTouchListener !enableTouch
        }
    }
}