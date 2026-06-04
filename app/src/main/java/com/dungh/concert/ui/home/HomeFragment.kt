package com.dungh.concert.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.dungh.concert.R
import com.dungh.concert.adapter.ConcertAdapter
import com.dungh.concert.databinding.FragmentHomeBinding
import com.dungh.concert.network.SessionManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: ConcertAdapter
    private lateinit var recommendAdapter: ConcertAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        sessionManager = SessionManager(requireContext())
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupWelcomeHeader()
        setupSpinners()
        setupDateFilter()
        setupFilters()
        setupRecyclerViews()
        setupHeaderActions()
        bindViewModel()

        return binding.root
    }

    private fun setupWelcomeHeader() {
        val user = sessionManager.fetchUser()
        binding.textWelcome.text = if (user != null) {
            "Xin chào, ${user.full_name ?: "Bạn"}!"
        } else {
            "Xin chào!"
        }
    }

    private fun setupHeaderActions() {
        binding.btnNotification.setOnClickListener {
            findNavController().navigate(R.id.navigation_notifications)
        }
    }

    private fun setupSpinners() {
        val cities = arrayOf(
            "Tất cả", "Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Cần Thơ", "Hải Phòng"
        )
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            cities
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerCity.adapter = spinnerAdapter
        binding.spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDateFilter() {
        binding.btnPickDate.setOnClickListener {
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày diễn")
                .build()
                .also { picker ->
                    picker.addOnPositiveButtonClickListener { selection ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val dateStr = sdf.format(Date(selection))
                        binding.textSelectedDate.text = dateStr
                        binding.btnClearDate.visibility = View.VISIBLE
                        viewModel.setDateFilter(dateStr)
                    }
                    picker.show(parentFragmentManager, "date_picker")
                }
        }

        binding.btnClearDate.setOnClickListener {
            binding.textSelectedDate.text = "Tất cả ngày"
            binding.btnClearDate.visibility = View.GONE
            viewModel.setDateFilter(null)
        }
    }

    private fun setupFilters() {
        binding.editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFilters()
                true
            } else {
                false
            }
        }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }
        })

        binding.chipGroupGenres.setOnCheckedStateChangeListener { _, _ ->
            updateChipStyles()
            applyFilters()
        }
        updateChipStyles()
    }

    private fun updateChipStyles() {
        val ctx = requireContext()
        for (i in 0 until binding.chipGroupGenres.childCount) {
            val chip = binding.chipGroupGenres.getChildAt(i) as? Chip ?: continue
            if (chip.isChecked) {
                chip.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                chip.chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.primary)
            } else {
                chip.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                chip.chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.bg_surface_alt)
            }
        }
    }

    private fun applyFilters() {
        val search = binding.editSearch.text.toString().trim()
        val city = binding.spinnerCity.selectedItem.toString()
        val selectedChipId = binding.chipGroupGenres.checkedChipId
        val genre = if (selectedChipId != View.NO_ID) {
            groupToGenreText(selectedChipId)
        } else {
            null
        }
        viewModel.setFilters(search, genre, city)
    }

    private fun groupToGenreText(chipId: Int): String? {
        val chip = binding.chipGroupGenres.findViewById<Chip>(chipId) ?: return null
        return chip.text.toString()
    }

    private fun setupRecyclerViews() {
        adapter = ConcertAdapter(onClick = { concert ->
            val action = HomeFragmentDirections.actionHomeToConcertDetail(concert.id ?: "")
            findNavController().navigate(action)
        })
        binding.recyclerConcerts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerConcerts.adapter = adapter
        binding.recyclerConcerts.isNestedScrollingEnabled = false

        recommendAdapter = ConcertAdapter(onClick = { concert ->
            val action = HomeFragmentDirections.actionHomeToConcertDetail(concert.id ?: "")
            findNavController().navigate(action)
        })
        binding.recyclerRecommend.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerRecommend.adapter = recommendAdapter
        binding.recyclerRecommend.isNestedScrollingEnabled = false
    }

    private fun bindViewModel() {
        viewModel.concerts.observe(viewLifecycleOwner) { concerts ->
            adapter.updateData(concerts)
            binding.textEmpty.visibility = if (concerts.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.recommendedConcerts.observe(viewLifecycleOwner) { recommendations ->
            recommendAdapter.updateData(recommendations)
            val hasItems = recommendations.isNotEmpty()
            binding.titleRecommend.visibility = if (hasItems) View.VISIBLE else View.GONE
            binding.recyclerRecommend.visibility = if (hasItems) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            binding.textError.text = error ?: ""
            binding.textError.visibility = if (error.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
