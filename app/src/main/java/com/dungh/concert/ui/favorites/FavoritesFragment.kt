package com.dungh.concert.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dungh.concert.adapter.ConcertAdapter
import com.dungh.concert.databinding.FragmentFavoritesBinding

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FavoritesViewModel
    private lateinit var adapter: ConcertAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(FavoritesViewModel::class.java)
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)

        setupRecyclerView()
        bindViewModel()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ConcertAdapter(onClick = { concert ->
            // Reuses the generated directions from navigation graph!
            val action = FavoritesFragmentDirections.actionFavoritesToConcertDetail(concert.id ?: "")
            findNavController().navigate(action)
        })
        binding.recyclerFavorites.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
        binding.recyclerFavorites.adapter = adapter
    }

    private fun bindViewModel() {
        viewModel.favorites.observe(viewLifecycleOwner) { favorites ->
            adapter.updateData(favorites)
            binding.textEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressFavorites.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            binding.textError.text = error ?: ""
            binding.textError.visibility = if (error.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
