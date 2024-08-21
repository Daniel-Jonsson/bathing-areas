package se.miun.dajo1903.dt031g.bathingsites

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import se.miun.dajo1903.dt031g.bathingsites.databinding.ActivityMainBinding

/**
 * The main activity is what the user meets when they first launch the application.
 * @author Daniel Jönsson
 * @see AppCompatActivity
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActionButtonClickListener()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.main_menu_setting_item -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.main_menu_download_item -> startActivity(Intent(this, DownloadActivity::class.java))
            R.id.main_menu_map_item -> {
                startActivity(Intent(this, MapsActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Function initializes the action button click listener and starts NewBathingSiteActivity when
     * the button gets clicked.
     */
    private fun initActionButtonClickListener() {
        val actionBtn = binding.addBathingsiteActionButton
        actionBtn.setOnClickListener {
            val intent = Intent(this, NewBathingSiteActivity::class.java)
            startActivity(intent)
        }
    }



}
