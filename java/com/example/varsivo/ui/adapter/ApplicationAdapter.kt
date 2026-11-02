package com.example.varsivo.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.varsivo.R
import com.example.varsivo.data.ApplicationStatus
import com.example.varsivo.data.TrackedApplication
import com.example.varsivo.databinding.ItemApplicationBinding

/** Cards for the Applications tracker. Tap the status chip to move it along; the bin deletes. */
class ApplicationAdapter(
    private val onStatusClick: (TrackedApplication) -> Unit,
    private val onDeleteClick: (TrackedApplication) -> Unit
) : RecyclerView.Adapter<ApplicationAdapter.ViewHolder>() {

    private var items: List<TrackedApplication> = emptyList()

    fun submit(newItems: List<TrackedApplication>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemApplicationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemApplicationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: TrackedApplication) {
            val context = binding.root.context
            binding.titleText.text = context.getString(R.string.application_title, app.institution, app.programme)
            binding.syncText.setText(if (app.synced) R.string.synced else R.string.offline_will_sync)

            val (labelRes, bgRes, fgRes) = when (app.status) {
                ApplicationStatus.OFFER -> Triple(R.string.status_offer, R.color.varsivo_offer_bg, R.color.varsivo_navy)
                ApplicationStatus.PENDING -> Triple(R.string.status_pending, R.color.varsivo_pending_bg, R.color.white)
                ApplicationStatus.SUBMITTED -> Triple(R.string.status_submitted, R.color.varsivo_submitted_bg, R.color.varsivo_navy)
            }
            binding.statusChip.setText(labelRes)
            binding.statusChip.setTextColor(ContextCompat.getColor(context, fgRes))
            (binding.statusChip.background.mutate() as GradientDrawable)
                .setColor(ContextCompat.getColor(context, bgRes))

            binding.statusChip.setOnClickListener { onStatusClick(app) }
            binding.deleteButton.setOnClickListener { onDeleteClick(app) }
        }
    }
}
