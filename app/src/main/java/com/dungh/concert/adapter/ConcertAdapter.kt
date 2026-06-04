package com.dungh.concert.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dungh.concert.R
import com.dungh.concert.databinding.ItemConcertBinding
import com.dungh.concert.model.Concert
import java.text.SimpleDateFormat
import java.util.Locale

class ConcertAdapter(
    private val concerts: MutableList<Concert> = mutableListOf(),
    private val onClick: (Concert) -> Unit
) : RecyclerView.Adapter<ConcertAdapter.ConcertViewHolder>() {

    fun updateData(newConcerts: List<Concert>) {
        concerts.clear()
        concerts.addAll(newConcerts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConcertViewHolder {
        val binding = ItemConcertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConcertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConcertViewHolder, position: Int) {
        holder.bind(concerts[position])
    }

    override fun getItemCount(): Int = concerts.size

    inner class ConcertViewHolder(private val binding: ItemConcertBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(concert: Concert) {
            val ctx = binding.root.context
            binding.concertTitle.text = concert.title ?: ctx.getString(R.string.unknown_event)

            val venueName = concert.venue?.name
            val city = concert.venue?.city
            binding.concertVenue.text = when {
                !venueName.isNullOrBlank() && !city.isNullOrBlank() -> "$venueName · $city"
                !venueName.isNullOrBlank() -> venueName
                else -> ctx.getString(R.string.unknown_venue)
            }

            val artists = concert.concert_artists?.mapNotNull { it.artist?.name }?.joinToString(", ")
            binding.concertArtists.text = if (artists.isNullOrBlank()) {
                ctx.getString(R.string.no_artist_info)
            } else {
                artists
            }

            bindDateBadge(concert.start_time)

            binding.concertBanner.load(concert.banner_url) {
                crossfade(true)
                placeholder(R.drawable.event)
                error(R.drawable.event)
            }

            binding.root.setOnClickListener { onClick(concert) }
        }

        private fun bindDateBadge(startTime: String?) {
            if (startTime.isNullOrBlank()) {
                binding.dateBadge.visibility = View.GONE
                return
            }
            binding.dateBadge.visibility = View.VISIBLE
            try {
                val datePart = startTime.split("T")[0]
                val parts = datePart.split("-")
                if (parts.size == 3) {
                    binding.concertDateDay.text = parts[2].toInt().toString()
                    val month = SimpleDateFormat("MMM", Locale.ENGLISH)
                        .format(SimpleDateFormat("MM", Locale.US).parse(parts[1])!!)
                        .uppercase(Locale.ENGLISH)
                    binding.concertDateMonth.text = month
                }
            } catch (_: Exception) {
                binding.dateBadge.visibility = View.GONE
            }
        }
    }
}
