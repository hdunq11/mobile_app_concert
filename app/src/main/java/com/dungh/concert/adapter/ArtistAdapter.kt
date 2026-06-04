package com.dungh.concert.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dungh.concert.R
import com.dungh.concert.databinding.ItemArtistBinding
import com.dungh.concert.model.Artist

class ArtistAdapter(
    private val artists: MutableList<Artist> = mutableListOf()
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    fun updateData(newArtists: List<Artist>) {
        artists.clear()
        artists.addAll(newArtists)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArtistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        holder.bind(artists[position])
    }

    override fun getItemCount(): Int = artists.size

    inner class ArtistViewHolder(private val binding: ItemArtistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(artist: Artist) {
            binding.artistName.text = artist.name ?: "Nghệ sĩ"
            
            binding.artistImage.load(artist.image_url) {
                crossfade(true)
                placeholder(R.drawable.ic_user)
                error(R.drawable.ic_user)
            }
        }
    }
}
