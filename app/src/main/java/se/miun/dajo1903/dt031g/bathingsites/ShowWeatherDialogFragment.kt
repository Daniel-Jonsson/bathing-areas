package se.miun.dajo1903.dt031g.bathingsites


import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import se.miun.dajo1903.dt031g.bathingsites.databinding.FragmentShowWeatherDialogBinding

/**
 * The ShowWeatherDialog is responsible for displaying the fetched weather information the
 * newInstance function creates a new instance of the class and accepts two parameters desc which is
 * the weather description and icon which is the current icon for the weather fetched.
 *
 * Creating dialog-fragment was learned from: https://developer.android.com/reference/androidx/fragment/app/DialogFragment
 *
 * @author Daniel Jönsson
 * @see DialogFragment
 */

class ShowWeatherDialogFragment : DialogFragment() {

    private lateinit var weatherDesc: String
    private lateinit var weatherIcon: Bitmap
    private lateinit var binding: FragmentShowWeatherDialogBinding

    companion object {
        private const val WEATHER_DESC = "Weather description"
        private const val WEATHER_ICON = "Weather icon"

        fun newInstance(desc: String, icon: Bitmap) = ShowWeatherDialogFragment().apply {
            arguments = Bundle().apply {
                putString(WEATHER_DESC, desc)
                putParcelable(WEATHER_ICON, icon)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShowWeatherDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.weatherDescription.text = arguments?.getString(WEATHER_DESC)
        binding.weatherIcon.setImageBitmap(arguments?.getParcelable(WEATHER_ICON))
        binding.dialogPositiveButton.setOnClickListener {
            this.dismiss()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            weatherDesc = it.getString(WEATHER_DESC)!!
            weatherIcon = it.getParcelable(WEATHER_ICON)!!
        }
    }
}