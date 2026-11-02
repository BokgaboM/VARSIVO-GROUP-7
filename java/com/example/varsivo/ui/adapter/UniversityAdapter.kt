package com.example.varsivo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.varsivo.api.University
import com.example.varsivo.databinding.ItemUniversityBinding

/** One card per university with "Apply now" and "View on map" actions. */
class UniversityAdapter(
    private val onApplyClick: (University) -> Unit,
    private val onMapClick: (University) -> Unit
) : RecyclerView.Adapter<UniversityAdapter.ViewHolder>() {

    private var items: List<University> = emptyList()

    fun submit(newItems: List<University>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemUniversityBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemUniversityBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(university: University) {
            binding.universityNameText.text = university.name
            binding.countryText.text = university.country
            binding.applyButton.setOnClickListener { onApplyClick(university) }
            binding.mapIcon.setOnClickListener { onMapClick(university) }
        }
    }
}
