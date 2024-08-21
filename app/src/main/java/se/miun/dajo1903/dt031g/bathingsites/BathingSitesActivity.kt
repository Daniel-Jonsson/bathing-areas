package se.miun.dajo1903.dt031g.bathingsites

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import se.miun.dajo1903.dt031g.bathingsites.databinding.ActivityBathingSitesBinding

/**
 * Click listener on recycler view concept was learnt from:
 * https://www.geeksforgeeks.org/how-to-apply-onclicklistener-to-recyclerview-items-in-android/
 *
 * The BathingSitesActivity is used to display all the downloaded bathing sites. Utilizes recycler
 * view in order to display a list of sites based on a specified layout created.
 *
 * @author Daniel Jönsson
 * @see AppCompatActivity
 */
class BathingSitesActivity : AppCompatActivity() {

    /** Instance field variables */
    private lateinit var bathingSites: List<BathingSite>
    private lateinit var bathingSiteRV: RecyclerView
    private lateinit var bathingSitesAdapter: BathingSiteRecyclerAdapter
    private lateinit var binding: ActivityBathingSitesBinding

    /**
     * The usual stuff, sets the view and binds it. Sets a layoutManager for the recyclerview and
     * calls setupRecyclerView in order to display all the bathing sites.
     * @param savedInstanceState If the activity is being re-constructed and some data needs to be
     * re-introduced.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_bathing_sites)
        binding = ActivityBathingSitesBinding.inflate(layoutInflater)
        bathingSiteRV = binding.bathingSitesRV
        bathingSiteRV.layoutManager = LinearLayoutManager(this)
        setContentView(binding.root)
        setupRecyclerView()
    }

    /**
     * When an item is clicked, if the itemId match with the home button the activity finishes and
     * user gets taken to the previous activity.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    /**
     * Checks if the amount of bathingsites is 0 it displays a specific textview saying that no
     * sites are stored.
     */
    private fun checkRecyclerView() {
        if (bathingSitesAdapter.itemCount == 0) {
            binding.noBathingSitesAdded.visibility = View.VISIBLE
        }
    }

    /**
     * Sets up the recyclerview on a background thread, gets all the bathingsites and sorts them by
     * name. Sets the bathingSitesAdapter by instantiating BathingSiteRecyclerAdapter with a provided
     * list of bathingSites. Sets this adapter to the recycler view. Also sets an click listener in
     * order to display clicked bathing site data.
     */
    private fun setupRecyclerView() {
        lifecycleScope.launch {
            bathingSites = DatabaseBuilder.getInstance(this@BathingSitesActivity)
                .BathingSiteDao().getBathingSites()
                .sortedBy { it.name }
            bathingSitesAdapter = BathingSiteRecyclerAdapter(bathingSites)
            bathingSiteRV.adapter = bathingSitesAdapter
            checkRecyclerView()
            bathingSitesAdapter.setOnClickListener(object : BathingSiteRecyclerAdapter.OnClickListener {
                override fun onClick(bathingSite: BathingSite) {
                    showBathingSiteData(bathingSite)
                }
            })
        }
    }

    /**
     * Used to display the bathing site data using AlertDialog.
     */
    private fun showBathingSiteData(bathingSite: BathingSite) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(
            """
                Name: ${bathingSite.name}
                Description: ${bathingSite.desc}
                Address: ${bathingSite.address}
                Longitude: ${bathingSite.longitude}
                Latitude: ${bathingSite.latitude}
                Water temperature: ${bathingSite.waterTemp}
                Date for water temp: ${bathingSite.dateForTemp}
                Rating: ${bathingSite.grade}
            """.trimIndent()
        )
        dialogBuilder.setPositiveButton(getString(android.R.string.ok)) {dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.show()
    }
}