package se.miun.dajo1903.dt031g.bathingsites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import se.miun.dajo1903.dt031g.bathingsites.databinding.FragmentBathingSitesBinding

/**
 * The BathingSitesFragment is responsible for displaying the custom view BathingSitesView
 *
 * @author Daniel Jönsson
 * @see Fragment
 */
class BathingSitesFragment : Fragment() {
    private lateinit var binding: FragmentBathingSitesBinding

    /**
     * Inflates the layout for the fragment, initializes a click listener for listening on click
     * on the custom view.
     *
     * @param inflater The LayoutInflater used to inflate the views.
     * @param container The parent view that the fragment should be attached to
     * @param savedInstanceState Determines if this fragment is being re-constructed or not.
     * @return The root view for the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBathingSitesBinding.inflate(inflater, container, false)
        initBathingSitesViewClickListener()
        return binding.root
    }

    /**
     * Updates the amount of bathing-sites once the fragment resumes. For example after a user
     * downloaded a bunch of bathing sites and returns to the main screen this is called and updates
     * the amount of sites.
     */
    override fun onResume() {
        lifecycleScope.launch {
            binding.bathingsitesView.setBathingSitesAmount()
        }
        super.onResume()
    }

    /**
     * Initializes a click listener and starts an activity upon click.
     */
    private fun initBathingSitesViewClickListener() {
        val bathingSitesView = binding.bathingsitesView
        bathingSitesView.setOnClickListener {
            startActivity(Intent(context, BathingSitesActivity::class.java))
        }
    }
}