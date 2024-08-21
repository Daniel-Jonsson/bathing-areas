package se.miun.dajo1903.dt031g.bathingsites

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

/**
 * This is the activity for adding new bathing sites to the application. It sets the view to display
 * the layout created which contains one or two fragments.
 * @author Daniel Jönsson
 * @see AppCompatActivity
 */
class NewBathingSiteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_bathing_site)
    }
}