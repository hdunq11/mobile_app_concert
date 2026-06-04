package com.dungh.concert.ui.concert_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.dungh.concert.R
import com.dungh.concert.adapter.ArtistAdapter
import com.dungh.concert.databinding.FragmentConcertDetailBinding

class ConcertDetailFragment : Fragment() {

    private var _binding: FragmentConcertDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ConcertDetailViewModel
    private val args: ConcertDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ConcertDetailViewModel::class.java)
        _binding = FragmentConcertDetailBinding.inflate(inflater, container, false)

        // Setup Back Button on transparent toolbar
        binding.toolbarDetail.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbarDetail.navigationIcon?.setTint(android.graphics.Color.WHITE)
        binding.toolbarDetail.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.fetchConcertDetail(args.concertId)
        bindViewModel()
        setupListeners()

        return binding.root
    }

    private fun bindViewModel() {
        viewModel.concert.observe(viewLifecycleOwner) { concert ->
            concert?.let {
                binding.concertDetailTitle.text = it.title
                binding.concertDetailVenue.text = it.venue?.name ?: "Địa điểm chưa có"
                binding.concertDetailDateTime.text = it.start_time?.replace("T", " ") ?: "Ngày chưa có"
                binding.concertDetailDescription.text = it.description ?: "Chưa có mô tả"

                val artists = it.concert_artists?.mapNotNull { ca -> ca.artist?.name }?.joinToString(", ")
                binding.concertDetailArtistsContent.text = if (artists.isNullOrBlank()) {
                    "Chưa có thông tin nghệ sĩ"
                } else {
                    artists
                }

                // Bind horizontal circular artists list
                val artistList = it.concert_artists?.mapNotNull { ca -> ca.artist } ?: emptyList()
                val artistAdapter = ArtistAdapter()
                binding.recyclerArtists.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                binding.recyclerArtists.adapter = artistAdapter
                artistAdapter.updateData(artistList)

                binding.concertBannerDetail.load(it.banner_url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_home_black_24dp)
                }

                binding.btnBuyTicket.isEnabled = true
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressDetail.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            binding.textErrorDetail.text = error ?: ""
            binding.textErrorDetail.visibility = if (error.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        viewModel.isFavorite.observe(viewLifecycleOwner) { isFav ->
            binding.btnFavorite.setImageResource(
                if (isFav) {
                    android.R.drawable.btn_star_big_on
                } else {
                    android.R.drawable.btn_star_big_off
                }
            )
        }
    }

    private fun setupListeners() {
        binding.btnBuyTicket.setOnClickListener {
            val concertId = args.concertId
            val action = ConcertDetailFragmentDirections.actionConcertDetailToSeatSelection(concertId)
            findNavController().navigate(action)
        }

        binding.btnFavorite.setOnClickListener {
            viewModel.toggleFavorite(args.concertId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
