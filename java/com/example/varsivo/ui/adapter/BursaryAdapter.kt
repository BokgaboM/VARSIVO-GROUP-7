package com.example.varsivo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.varsivo.R
import com.example.varsivo.api.Bursary
import com.example.varsivo.databinding.ItemBursaryBinding

/**
 * RecyclerView adapter for the bursary cards. The first card is the "closing soon" one
 * (gold tint); the rest are blue, matching the wireframe.
 */
class BursaryAdapter(
    private val onBursaryClick: (Bursary) -> Unit
) : RecyclerView.Adapter<BursaryAdapter.ViewHolder>() {

    private var items: List<Bursary> = emptyList()

    fun submit(newItems: List<Bursary>) {
        items = newItems
        notifyDataSetChanged() // small lists, so a full refresh is fine
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemBursaryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position], position == 0)

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemBursaryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bursary: Bursary, isClosingSoon: Boolean) {
            val context = binding.root.context
            val textColor = ContextCompat.getColor(
                context, if (isClosingSoon) R.color.varsivo_navy else R.color.white
            )
            binding.tintOverlay.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (isClosingSoon) R.color.varsivo_card_gold_tint else R.color.varsivo_card_blue_tint
                )
            )
            binding.closingDateText.text = bursary.closingDate.ifBlank { context.getString(R.string.tbc) }
            binding.closingDateText.setTextColor(textColor)
            binding.closesLabel.text =
                context.getString(if (isClosingSoon) R.string.closing_soon else R.string.closes)
            binding.closesLabel.setTextColor(textColor)
            binding.bursaryNameText.text = bursary.name
            binding.root.setOnClickListener { onBursaryClick(bursary) }
        }
    }
}
