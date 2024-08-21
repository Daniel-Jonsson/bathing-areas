package se.miun.dajo1903.dt031g.bathingsites

import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import se.miun.dajo1903.dt031g.bathingsites.databinding.FragmentAddBathingSiteBinding
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Usage of lifecycleScope was learned from the following links:
 * https://developer.android.com/topic/libraries/architecture/coroutines
 * https://youtu.be/kXSBkAA03Tc?si=Z8HdC_6IcsRC2plZ
 *
 * To show a custom dialogfragment was learned from:
 * https://developer.android.com/guide/fragments/dialogs
 *
 * How to download an image using HttpURLConnection was learnt from:
 * https://gist.github.com/davidllorca/28bd2e368f37534f4c3f
 *
 * Room database implementation:
 * https://www.geeksforgeeks.org/room-database-with-kotlin-coroutines-in-android/
 *
 * The AddBathingSiteFragment is used to allow users to add a specific bathing site to the database
 * based on the given information. It also contains functionality to show current weather for a
 * specified address or coordinates.
 *
 * @author Daniel Jönsson
 * @see Fragment
 */
class AddBathingSiteFragment : Fragment() {

    /** Instance field variables */
    private val TAG = "AddBathingSiteFragment"
    private lateinit var binding: FragmentAddBathingSiteBinding
    private lateinit var bathingSiteRequiredFields: MutableList<EditText>
    private lateinit var appDatabase: AppDatabase
    private lateinit var dbHelper: DatabaseHelperImpl

    /**
     * Sets that the fragment has a menu.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        appDatabase = DatabaseBuilder.getInstance(requireContext())
        dbHelper = DatabaseHelperImpl(appDatabase)
    }

    /**
     * Same old, binds the view by inflating it using a provided LayoutInflater.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBathingSiteBinding.inflate(inflater, container, false)
        bathingSiteRequiredFields = mutableListOf(
            binding.name,
            binding.address,
            binding.longitude,
            binding.latitude
        )
        setCurrentDateOnField(binding.date)
        return binding.root
    }

    /**
     * inflates a menu to the view.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.add_bathing_site_menu, menu)
    }

    /**
     * When an item from the menu is selected. If the itemId matches a specific id the application
     * will perform specific actions such as emptying the form, adding bathing site, view the weather
     * and so on.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clear_input_fields -> {
                emptyForm()
                return true
            }

            R.id.add_bathing_site -> {
                if(!checkForm()) {
                    invalidateForm()
                    return true
                }
                if (binding.address.text.isNotEmpty()) {
                    if (checkAddress()) {
                        Snackbar.make(binding.root, "Address does not exist.", Snackbar.LENGTH_LONG).show()
                        return true
                    }
                }
                addBathingSiteToDB()
                return true
            }

            R.id.view_weather -> {
                val validationResult = validWeatherForm()
                if (validationResult.containsKey(true)) {
                    showWeatherData(validationResult.values.flatten())
                } else {
                    showSnackbar(getString(R.string.invalid_form_weather))
                }
                return true
            }
            R.id.add_site_menu_setting_item -> {
                startActivity(Intent(context, SettingsActivity::class.java))
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * empties all data from the form.
     */
    private fun emptyForm() {
        bathingSiteRequiredFields.forEach { field ->
            field.text.clear()
        }
        binding.description.text.clear()
        binding.rating.rating = 0F
        binding.waterTemp.text.clear()
        setCurrentDateOnField(binding.date)
    }

    /**
     * Checks if the form is valid before adding bathing site.
     * @return A boolean value determining if the form is valid or not.
     */
    private fun checkForm(): Boolean {
        val nameNotEmpty = binding.name.text.isNotEmpty()
        val addressNotEmpty = binding.address.text.isNotEmpty()
        val longitudeNotEmpty = binding.longitude.text.isNotEmpty()
        val latitudeNotEmpty = binding.latitude.text.isNotEmpty()

        val addressOrCoordinatesEntered = addressNotEmpty || (latitudeNotEmpty && longitudeNotEmpty)
        return nameNotEmpty && addressOrCoordinatesEntered
    }

    /**
     * Checks if the address exists using Geocoder in order to get a location based on a name provided.
     * @return true if the address does not exist, else false.
     */
    private fun checkAddress(): Boolean {
        return Geocoder(requireContext()).getFromLocationName(binding.address.text.toString(), 1)
            .isNullOrEmpty()
    }

    /**
     * Gets coordinates from an provided address, if the user has not provided coordinates and only
     * provided address this is a way to still get the coordinates and display it on the map later.
     * This is done on a background thread in order to not block the main thread as this operation
     * may take time.
     * @param address The address from which a lat/long coordinate should be fetched.
     * @return A lat/long coordinate from the provided address or null.
     */
    private suspend fun getCoordinateFromAddress(address: String) : LatLng? {
        return withContext(Dispatchers.IO) {
            val coder = Geocoder(requireContext())
            try {
                val addressList = coder.getFromLocationName(address, 1) ?: return@withContext null
                val location = addressList[0]
                return@withContext LatLng(location.latitude, location.longitude)
            } catch (e: IOException) {
                Snackbar.make(binding.root, "Error geting coordinates", Snackbar.LENGTH_LONG).show()
            }
            return@withContext null
        }
    }

    /**
     * Invalidates all the required fields if they are empty.
     */
    private fun invalidateForm() {
        bathingSiteRequiredFields.forEach { field ->
            if (field.text.isEmpty()) {
                val fieldName = getFieldName(field.id)
                field.error = getString(R.string.invalid_field, fieldName)
            }
        }
    }

    /**
     * Gets the name of a field by getting the resource name based on the provided id.
     * @param fieldId The id to the field edittext.
     * @return a String containing the field name with first character uppercased.
     */
    private fun getFieldName(fieldId: Int) : String =
            resources.getResourceName(fieldId)
            .substringAfterLast("/")
            .replaceFirstChar { firstChar -> firstChar.uppercaseChar() }


    /**
     * Sets current date on the date field.
     */
    private fun setCurrentDateOnField(field: EditText) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        field.setText(LocalDate.now().format(formatter))
    }

    /**
     * Called to validate the form to be able to fetch weather information. If either the latitude
     * and longitude is not empty or the address is not empty the function will return a Pair
     * containing a boolean value and a list of the provided data to fetch from. If it is not valid
     * a empty map is returned.
     */
    private fun validWeatherForm() : Map<Boolean, List<EditText>> {
        return when {
            binding.longitude.text.isNotEmpty() && binding.latitude.text.isNotEmpty() -> {
                mapOf(Pair(true, listOf(binding.latitude, binding.longitude)))
            }
            binding.address.text.isNotEmpty() -> mapOf(Pair(true, listOf(binding.address)))
            else -> emptyMap()
        }
    }

    /**
     * Shows the weather data, extracts the weather data from AddBathingSiteFragment::downloadWeatherData
     * @param listData The list of address or latitude/longitude based on what the user provided.
     */
    private fun showWeatherData(listData: List<EditText>) {
        showProgressDialog()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val searchString = if (listData.size == 2) "lat=${listData[0].text}&lon=${listData[1].text}" else "q=${listData[0].text}"
                delay(500)
                val weatherData = downloadWeatherData(searchString)
                delay(500)
                if (weatherData != null) {
                    val weatherDesc = weatherData.getJSONArray("weather").getJSONObject(0).getString("description")
                    val weatherIconData = weatherData.getJSONArray("weather").getJSONObject(0).getString("icon")
                    val weatherTemp = weatherData.getJSONObject("main").getString("temp")
                    val message = getString(R.string.weather_dialog_message, weatherDesc, weatherTemp)
                    val weatherIcon = downloadWeatherIcon(weatherIconData)
                    if (weatherIcon != null) {
                        ShowWeatherDialogFragment.newInstance(message, weatherIcon).show(childFragmentManager, "weather_dialog_fragment")
                    }
                } else {
                    showSnackbar(getString(R.string.weather_data_error))
                }
            } catch (e: Exception) {
                showSnackbar(getString(R.string.weather_data_error))
                Log.e(TAG, "Exception in showWeatherData: ", e)
            } finally {
                hideProgressDialog()
            }
        }
    }

    /**
     * Used to save the bathing site to the room database. If the exception is a
     * SQLiteConstraintException there was a conflict and the coordinates already exists.
     */
    private fun addBathingSiteToDB() {
        lifecycleScope.launch {
            try {
                saveToDatabase()
                delay(1000)
                emptyForm()
                showSnackbar(getString(R.string.saved_to_database), true)
            } catch (e: Exception) {
                println(e)
                when (e) {
                    is SQLiteConstraintException -> showSnackbar(getString(R.string.duplicate_bathing_site_error))
                    else -> showSnackbar(getString(R.string.unknown_error))
                }
            }
        }
    }

    /**
     * Tries to save the bathing site to the database. If the latitude and longitude is empty the
     * application tries to get coordinates based on address and inserts that. Else it just inserts
     * a new bathing site entity.
     */
    private suspend fun saveToDatabase() {
        withContext(Dispatchers.IO) {
            val name = binding.name.text
            val desc = binding.description.text
            val address = binding.address.text
            val latitude = binding.latitude.text
            val longitude = binding.longitude.text
            val waterTemp = binding.waterTemp.text
            val tempDate = binding.date.text
            val rating = binding.rating.rating

            if (latitude.isEmpty() && longitude.isEmpty()) {
                val latLng = getCoordinateFromAddress(address.toString())
                if (latLng != null) {
                    val defaultLatitude = latLng.latitude
                    val defaultLongitude = latLng.longitude
                    dbHelper.insertOne(BathingSite(
                        name = name.toString(),
                        desc = if (desc.isNotEmpty()) desc.toString() else null,
                        address = if (address.isNotEmpty()) address.toString() else null,
                        latitude = defaultLatitude.toString(),
                        longitude = defaultLongitude.toString(),
                        waterTemp = if (waterTemp.isNotEmpty()) waterTemp.toString() else null,
                        dateForTemp = if (tempDate.isNotEmpty()) tempDate.toString() else null,
                        grade = rating.toString()
                    )
                    )
                }
            } else {
                dbHelper.insertOne(BathingSite(
                    name = name.toString(),
                    desc = if (desc.isNotEmpty()) desc.toString() else null,
                    address = if (address.isNotEmpty()) address.toString() else null,
                    latitude = latitude.toString(),
                    longitude = longitude.toString(),
                    waterTemp = if (waterTemp.isNotEmpty()) waterTemp.toString() else null,
                    dateForTemp = if (tempDate.isNotEmpty()) tempDate.toString() else null,
                    grade = rating.toString()
                )
                )
            }
        }
    }

    /**
     * "downloads" the weather data and returns a JSONObject containing the data. Opens a URL connection
     * and tries to connect. If the connection succeeds the content gets extracted using a bufferedReader
     * to read the text. This is then returned as a JSONObject.
     */
    private suspend fun downloadWeatherData(searchString: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            println("${SettingsActivity.getWeatherURL(requireContext())}?$searchString")
            val weatherConnection = URL("${SettingsActivity.getWeatherURL(requireContext())}?$searchString").openConnection() as HttpURLConnection
            try {
                weatherConnection.connect()
                if (weatherConnection.responseCode == HttpURLConnection.HTTP_OK) {
                    val content = weatherConnection.inputStream.bufferedReader().use { it.readText() }
                    return@withContext JSONObject(content)
                } else {
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in downloadWeatherData: ", e)
                return@withContext null
            } finally {
                weatherConnection.disconnect()
            }
        }
    }

    /**
     * Downloads the icon for the current weather and returns it as a Bitmap. It uses
     * BitmapFactory::decodeStream to decode an open inputStream into a bitmap.
     */
    private suspend fun downloadWeatherIcon(iconString: String) : Bitmap? {
        return withContext(Dispatchers.IO) {
            val weatherIconConnection = URL("${Util.WEATHER_ICON}$iconString.png").openConnection() as HttpURLConnection
            try {
                weatherIconConnection.connect()
                if(weatherIconConnection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = weatherIconConnection.inputStream
                    return@withContext BitmapFactory.decodeStream(inputStream)
                }
                else {
                    showSnackbar(getString(R.string.weather_icon_error))
                    return@withContext null
                }
            } catch (e: Exception) {
                showSnackbar(getString(R.string.weather_icon_error))
                return@withContext null
            } finally {
                weatherIconConnection.disconnect()
            }
        }
    }

    /**
     * Shows progress dialog when weather is fetched
     */
    private fun showProgressDialog() {
        binding.progressView.visibility = View.VISIBLE
    }

    /**
     * Hides progress dialog when weather finished fetching
     */
    private fun hideProgressDialog() {
        binding.progressView.visibility = View.INVISIBLE
    }

    /**
     * Shows snackbar based on a specified message.
     *
     * @param message The message to display
     * @param shouldUseCallback A boolean value determining if the snackbar should use a callback
     * to finish the activity once the snackbar is dismissed. Defaults to false.
     */
    private fun showSnackbar(message: String, shouldUseCallback: Boolean = false) {
        if (shouldUseCallback) {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    requireActivity().finish()
                }
            }).show()
        } else {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
        }
    }
}