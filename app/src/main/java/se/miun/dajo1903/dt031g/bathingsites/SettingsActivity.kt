package se.miun.dajo1903.dt031g.bathingsites

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

/**
 * The SettingsActivity is responsible for displaying the settings of the app
 * @author Daniel Jönsson
 * @see AppCompatActivity
 */
class SettingsActivity : AppCompatActivity() {

    /**
     * The onCreate function is called when the activity is first being created, if it is being
     * created for the first time (not from re-creation) the supportFragmentManager begins to replace
     * the view with an instance of SettingsFragment.
     * @see SettingsFragment
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * This function is called whenever an option item is selected on the menu, in this case when
     * the user clicks the home button (back arrow) the finish() is called to indicate that the activity
     * should be closed.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * This inner class is used to create the settings preferences based on prefrences from an xml
     * file.
     * @see PreferenceFragmentCompat
     */
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    /**
     * This companion object is used to simplify the ability to get hold of needed preferences from
     * the settings activity. This is used across some of the other activities to find out the current
     * preference.
     */
    companion object {
        fun getWeatherURL(context: Context) : String {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPref.getString(context.getString(R.string.weather_setting_url_key), Util.WEATHER_URL)!!
        }

        fun getDownloadURL(context: Context) : String {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPref.getString(context.getString(R.string.download_setting_url_key), Util.DOWNLOAD_URL)!!
        }

        fun getSearchDistance(context: Context) : Double {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            val pref = sharedPref.getString(context.getString(R.string.distance_map_setting_key),
                Util.DISTANCE_DEFAULT.toString()
            )!!
            return pref.toDouble()
        }
    }
}