package se.miun.dajo1903.dt031g.bathingsites

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import se.miun.dajo1903.dt031g.bathingsites.databinding.BathingsitesViewBinding

/**
 * Custom view which extends the ConstraintLayout, this is used to display the amount of bathing
 * sites currently stored while also displaying an image. The @JvmOverloads annotation simplifies
 * the process of creating constructor for every "situation" and instead overloads this constructor
 * and provides default values. This will create the constructor for every "situation" for us
 * automatically.
 *
 * @author Daniel Jönsson
 * @see JvmOverloads
 */
class BathingSitesView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0) : ConstraintLayout(context, attributeSet, defStyleAttr) {

    private var binding: BathingsitesViewBinding

    /**
     * binds the view using LayoutInflater
     */
    init {
        binding = BathingsitesViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    /**
     * Gets the bathingsites amount and set the textview to display it.
     */
    suspend fun setBathingSitesAmount() {
        val appDatabase = DatabaseBuilder.getInstance(context)
        val dbHelper = DatabaseHelperImpl(appDatabase)
        val bathingSiteAmount = dbHelper.getBathingSites().size
        binding.bathingviewAmount.text = context.getString(R.string.amount_bathingsites, bathingSiteAmount)
    }
}
