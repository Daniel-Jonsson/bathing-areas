package se.miun.dajo1903.dt031g.bathingsites


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter class for displaying a list of bathing sites in a RecyclerView.
 *
 * @param dataSet The list of BathingSite entities to be displayed in the RecyclerView.
 */
class BathingSiteRecyclerAdapter(private val dataSet: List<BathingSite>) : RecyclerView.Adapter<BathingSiteRecyclerAdapter.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    /**
     * ViewHolder class representing an individual item in the RecyclerView
     *
     * @param view The view for a single item in the RecyclerView
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bathingSiteName: TextView

        init {
            bathingSiteName = view.findViewById(R.id.bathingSiteName)
        }
    }

    /**
     * Hook called when the RecyclerView needs a ViewHolder to represent a given item.
     *
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The type of the new view
     * @return A new ViewHolder that holds the view of the given view type
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bathingsites_list, parent, false)
        return ViewHolder(view)
    }

    /**
     * Hook called by the RecyclerView to display data at a specified position. Also calls the
     * onClick function with the specified bathing site item.
     *
     * @param holder The ViewHolder that should be updated to represent the contents of the item at
     * the specified position.
     * @param position The position of the item within the adapters dataSet.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.bathingSiteName.text = dataSet[position].name

        holder.itemView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(item)
            }
        }
    }

    /**
     * Gets the size of the dataSet in the adapter
     *
     * @return The total number of bathing sites.
     */
    override fun getItemCount(): Int = dataSet.size

    /**
     * Sets an on click listener on the items in the RecyclerView.
     *
     * @param onClickListener The on click listener
     */
    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    /**
     * Inner interface for a callback to be invoked when a user clicks on a bathing site.
     */
    interface OnClickListener {
        /**
         * Called when a bathing site is clicked.
         * @param bathingSite The bathingSite clicked.
         */
        fun onClick(bathingSite: BathingSite)
    }
}