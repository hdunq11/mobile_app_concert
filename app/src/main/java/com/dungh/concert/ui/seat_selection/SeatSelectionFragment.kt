package com.dungh.concert.ui.seat_selection

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dungh.concert.R
import com.dungh.concert.databinding.FragmentSeatSelectionBinding
import com.dungh.concert.model.SeatMapZone
import com.dungh.concert.network.SessionManager
import com.dungh.concert.util.DateTimeUtils
import com.dungh.concert.ui.auth.LoginActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.RangeSlider

class SeatSelectionFragment : Fragment() {

    private var _binding: FragmentSeatSelectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SeatSelectionViewModel
    private lateinit var sessionManager: SessionManager
    private val args: SeatSelectionFragmentArgs by navArgs()
    private lateinit var arenaAdapter: ArenaZoneAdapter
    private var countDownTimer: android.os.CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(SeatSelectionViewModel::class.java)
        sessionManager = SessionManager(requireContext())
        _binding = FragmentSeatSelectionBinding.inflate(inflater, container, false)

        setupWelcomeHeader()
        setupArenaOverview()
        setupListeners()
        bindViewModel()
        observeReservedUntil()

        viewModel.fetchConcertDetail(args.concertId)
        viewModel.fetchSeatMap(args.concertId)

        return binding.root
    }

    private fun setupWelcomeHeader() {
        // Clear mock text, will bind dynamically from BE
        binding.textDateDay.text = "--"
        binding.textDateMonthYear.text = "Tháng --\n----"
    }

    private fun setupArenaOverview() {
        binding.recyclerArenaZones.layoutManager = GridLayoutManager(requireContext(), 2)
        arenaAdapter = ArenaZoneAdapter { zone ->
            viewModel.selectZone(zone)
        }
        binding.recyclerArenaZones.adapter = arenaAdapter
    }

    private fun setupListeners() {
        // Range Slider price filtering
        binding.sliderPriceFilter.addOnChangeListener(RangeSlider.OnChangeListener { slider, value, fromUser ->
            val values = slider.values
            if (values.size >= 2) {
                val minPrice = values[0].toDouble()
                val maxPrice = values[1].toDouble()
                viewModel.setPriceFilter(minPrice, maxPrice)
                
                binding.textSliderRangeLabel.text = String.format("%,.0fđ - %,.0fđ", minPrice, maxPrice)
                arenaAdapter.updatePriceFilter(minPrice, maxPrice)
            }
        })

        // Change Section click (Back to Arena Overview)
        binding.btnChangeSection.setOnClickListener {
            viewModel.selectZone(null)
        }

        // Hide Change Date button since concert only has one date/time
        binding.btnChangeDate.visibility = View.GONE

        // Continue Button
        binding.btnContinue.setOnClickListener {
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(requireContext(), "Bạn cần đăng nhập để đặt vé!", Toast.LENGTH_LONG).show()
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    putExtra("FROM_BOOKING", true)
                }
                startActivity(intent)
                return@setOnClickListener
            }

            viewModel.reserveSeatsAndProceed(
                concertId = args.concertId,
                onSuccess = { reservedUntil ->
                    val selected = viewModel.selectedSeats.value ?: return@reserveSeatsAndProceed
                    val action = SeatSelectionFragmentDirections.actionSeatSelectionToCheckout(
                        args.concertId,
                        selected.toTypedArray(),
                        (viewModel.totalPrice.value ?: 0.0).toFloat(),
                        reservedUntil ?: ""
                    )
                    findNavController().navigate(action)
                },
                onError = { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun bindViewModel() {
        // Dynamic Concert Info Binding
        viewModel.concert.observe(viewLifecycleOwner) { concert ->
            concert?.let {
                binding.textConcertTitle.text = it.title
                
                // Parse date start_time (e.g., 2026-06-25T19:00:00Z)
                it.start_time?.let { timeStr ->
                    try {
                        val datePart = timeStr.split("T")[0] // yyyy-MM-dd
                        val parts = datePart.split("-")
                        if (parts.size == 3) {
                            val year = parts[0]
                            val month = parts[1].toInt().toString()
                            val day = parts[2].toInt().toString()
                            binding.textDateDay.text = day
                            binding.textDateMonthYear.text = "Tháng $month\n$year"
                        }
                    } catch (e: Exception) {
                        binding.textDateDay.text = "Show"
                        binding.textDateMonthYear.text = "Chi tiết"
                    }
                }
            }
        }

        // Dynamic Event Info Binding (Update Title and Price Range from Map)
        viewModel.seatMap.observe(viewLifecycleOwner) { zones ->
            if (zones.isNullOrEmpty()) return@observe
            
            arenaAdapter.updateData(zones)
            binding.textAvailableStatus.text = "● Còn vé khả dụng"
            binding.textAvailableStatus.setTextColor(Color.parseColor("#00FF7F"))

            val prices = zones.mapNotNull { it.price }
            if (prices.isNotEmpty()) {
                val min = prices.minOrNull() ?: 0.0
                val max = prices.maxOrNull() ?: 0.0
                binding.textConcertPriceRange.text = String.format("Giá: %,.0fđ - %,.0fđ", min, max)
                
                // Configure range slider dynamically
                binding.sliderPriceFilter.valueFrom = min.toFloat()
                binding.sliderPriceFilter.valueTo = max.toFloat()
                binding.sliderPriceFilter.values = listOf(min.toFloat(), max.toFloat())
                binding.textSliderRangeLabel.text = String.format("%,.0fđ - %,.0fđ", min, max)
                arenaAdapter.updatePriceFilter(min, max)
            }
        }

        // Stepper Stage transitions (Arena Selection vs Seating Grid)
        viewModel.selectedZone.observe(viewLifecycleOwner) { zone ->
            if (zone == null) {
                binding.layoutArenaOverview.visibility = View.VISIBLE
                binding.layoutDetailedGrid.visibility = View.GONE
            } else {
                binding.layoutArenaOverview.visibility = View.GONE
                binding.layoutDetailedGrid.visibility = View.VISIBLE
                binding.textSelectedZoneTitle.text = "Khu vực: ${zone.name} - Giá: ${String.format("%,.0f", zone.price)} đ"
                renderDetailedGrid(zone)
            }
        }

        // Chosen Seats chips dynamic binding
        viewModel.selectedSeatsDetails.observe(viewLifecycleOwner) { details ->
            binding.containerSelectedChips.removeAllViews()
            if (details.isNullOrEmpty()) {
                binding.textSelectedSeatsLabel.visibility = View.GONE
                binding.scrollSelectedChips.visibility = View.GONE
            } else {
                binding.textSelectedSeatsLabel.visibility = View.VISIBLE
                binding.scrollSelectedChips.visibility = View.VISIBLE

                details.forEach { detail ->
                    val seatCard = MaterialCardView(requireContext()).apply {
                        radius = 16f
                        cardElevation = 2f
                        setCardBackgroundColor(Color.parseColor("#181824"))
                        strokeColor = Color.parseColor("#2E2E3C")
                        strokeWidth = 2
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(8, 4, 8, 4)
                        }
                    }

                    val chipLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(16, 8, 16, 8)
                    }

                    val labelText = TextView(requireContext()).apply {
                        text = "${detail.zoneName} - Hàng ${detail.row}, Ghế ${detail.number} (${String.format("%,.0f", detail.price)}đ)  "
                        setTextColor(Color.WHITE)
                        textSize = 11f
                        setTypeface(null, Typeface.BOLD)
                    }

                    val deleteBtn = TextView(requireContext()).apply {
                        text = "✕"
                        setTextColor(Color.parseColor("#FF1493"))
                        textSize = 12f
                        setTypeface(null, Typeface.BOLD)
                        setPadding(8, 0, 8, 0)
                        setOnClickListener {
                            viewModel.removeSeatById(detail.seatId)
                        }
                    }

                    chipLayout.addView(labelText)
                    chipLayout.addView(deleteBtn)
                    seatCard.addView(chipLayout)
                    binding.containerSelectedChips.addView(seatCard)
                }
            }

            // Sync total selected text and continue button status
            binding.textSelectedCount.text = "${details.size} ghế"
            binding.btnContinue.isEnabled = details.isNotEmpty()
            binding.btnContinue.alpha = if (details.isNotEmpty()) 1.0f else 0.5f
        }

        viewModel.totalPrice.observe(viewLifecycleOwner) { price ->
            binding.textTotalPrice.text = String.format("%,.0f đ", price)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressSeats.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            binding.textError.text = error ?: ""
            binding.textError.visibility = if (error.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun renderDetailedGrid(zone: SeatMapZone) {
        binding.containerSeats.removeAllViews()

        // Filter out seats if the zone price is out of the filter range
        val min = viewModel.minPriceFilter.value ?: 0.0
        val max = viewModel.maxPriceFilter.value ?: 3000000.0
        val isFilteredOut = (zone.price ?: 0.0) < min || (zone.price ?: 0.0) > max

        if (isFilteredOut) {
            val emptyMsg = TextView(requireContext()).apply {
                text = "Khu vực này hiện đã bị lọc bởi khoảng giá bạn chọn."
                setTextColor(Color.parseColor("#FF1493"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(24, 48, 24, 48)
            }
            binding.containerSeats.addView(emptyMsg)
            return
        }

        // Group seats by row
        val seatsByRow = zone.seats?.groupBy { it.row } ?: emptyMap()

        seatsByRow.forEach { (rowLabel, seats) ->
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
            }

            // Row Label
            val rowLabelView = TextView(requireContext()).apply {
                text = "$rowLabel  "
                textSize = 12f
                setTextColor(Color.parseColor("#888899"))
                minWidth = 32
            }
            rowLayout.addView(rowLabelView)

            // Render seats
            val density = resources.displayMetrics.density
            val seatSize = (38 * density).toInt()
            val seatMargin = (6 * density).toInt()

            seats.sortedBy { it.number }.forEach { seat ->
                val seatView = TextView(requireContext()).apply {
                    text = "${seat.number}"
                    textSize = 11f
                    gravity = Gravity.CENTER
                    setTypeface(null, Typeface.BOLD)

                    layoutParams = LinearLayout.LayoutParams(seatSize, seatSize).apply {
                        marginEnd = seatMargin
                    }

                    // Check if seat is selected in viewModel
                    val isSelected = viewModel.selectedSeats.value?.contains(seat.seat_id) == true
                    if (isSelected) {
                        seat.status = "selected"
                    }

                    updateSeatAppearance(this, seat, zone.color ?: "#8E2DE2")

                    setOnClickListener {
                        if (seat.status == "sold") {
                            Toast.makeText(requireContext(), "Ghế này đã bán!", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        if (seat.status == "reserved") {
                            Toast.makeText(requireContext(), "Ghế này đang được giữ chỗ!", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val currentlySelected = viewModel.selectedSeats.value?.contains(seat.seat_id) == true
                        viewModel.toggleSeat(
                            seatId = seat.seat_id ?: "",
                            zoneId = zone.zone_id ?: "",
                            zoneName = zone.name ?: "Khu vực",
                            row = rowLabel ?: "",
                            number = seat.number ?: 0,
                            price = zone.price ?: 0.0
                        )

                        if (currentlySelected) {
                            seat.status = "available"
                        } else {
                            seat.status = "selected"
                        }
                        updateSeatAppearance(this, seat, zone.color ?: "#8E2DE2")
                    }
                }
                rowLayout.addView(seatView)
            }

            binding.containerSeats.addView(rowLayout)
        }
    }

    private fun updateSeatAppearance(textView: TextView, seat: com.dungh.concert.model.SeatMapSeat, zoneColorHex: String) {
        val colorHex = try {
            Color.parseColor(zoneColorHex)
        } catch (e: Exception) {
            Color.parseColor("#8E2DE2")
        }

        when (seat.status) {
            "sold" -> {
                textView.setBackgroundResource(R.drawable.bg_rounded_card)
                textView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E2E38"))
                textView.setTextColor(Color.parseColor("#555566"))
            }
            "reserved" -> {
                textView.setBackgroundResource(R.drawable.bg_rounded_card)
                textView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF8C00"))
                textView.setTextColor(Color.WHITE)
            }
            "selected" -> {
                textView.setBackgroundResource(R.drawable.bg_neon_button)
                textView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00F5FF"))
                textView.setTextColor(Color.BLACK)
            }
            else -> {
                textView.setBackgroundResource(R.drawable.bg_rounded_card)
                textView.backgroundTintList = ColorStateList.valueOf(colorHex)
                textView.setTextColor(Color.WHITE)
            }
        }
    }

    private fun startHoldTimer(reservedUntilIso: String? = null) {
        countDownTimer?.cancel()
        val duration = if (!reservedUntilIso.isNullOrBlank()) {
            (DateTimeUtils.parseIsoToMillis(reservedUntilIso) - System.currentTimeMillis())
                .coerceAtLeast(0)
        } else {
            600_000L
        }
        if (duration == 0L) {
            binding.textTimer.text = "Hết giờ!"
            return
        }
        countDownTimer = object : android.os.CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.textTimer.text = String.format("Giữ chỗ: %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.textTimer.text = "Hết giờ!"
                Toast.makeText(
                    requireContext(),
                    "Thời gian giữ ghế đã hết. Vui lòng tải lại sơ đồ ghế!",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.selectZone(null)
                viewModel.fetchSeatMap(args.concertId)
            }
        }.start()
    }

    private fun observeReservedUntil() {
        viewModel.reservedUntilIso.observe(viewLifecycleOwner) { iso ->
            if (!iso.isNullOrBlank()) {
                startHoldTimer(iso)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }

    // --- Embedded ProVIP Recycler Adapter for Arena Zones ---
    private class ArenaZoneAdapter(
        private val onClick: (SeatMapZone) -> Unit
    ) : RecyclerView.Adapter<ArenaZoneAdapter.ViewHolder>() {
        private var list: List<SeatMapZone> = emptyList()
        private var minPrice = 0.0
        private var maxPrice = 3000000.0

        fun updateData(newList: List<SeatMapZone>) {
            this.list = newList
            notifyDataSetChanged()
        }

        fun updatePriceFilter(min: Double, max: Double) {
            this.minPrice = min
            this.maxPrice = max
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val card = MaterialCardView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16)
                }
                radius = 32f
                cardElevation = 4f
                strokeWidth = 2
            }

            val layout = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(32, 32, 32, 32)
            }
            card.addView(layout)

            return ViewHolder(card, layout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val zone = list[position]
            val zonePrice = zone.price ?: 0.0
            val isFiltered = zonePrice in minPrice..maxPrice

            val parsedColor = try {
                Color.parseColor(zone.color ?: "#8E2DE2")
            } catch (e: Exception) {
                Color.parseColor("#8E2DE2")
            }

            holder.card.apply {
                setCardBackgroundColor(ColorStateList.valueOf(if (isFiltered) Color.parseColor("#181824") else Color.parseColor("#09090F")))
                strokeColor = if (isFiltered) parsedColor else Color.parseColor("#2E2E3C")
                alpha = if (isFiltered) 1.0f else 0.25f
                isEnabled = isFiltered
                setOnClickListener {
                    if (isFiltered) onClick(zone)
                }
            }

            holder.layout.removeAllViews()

            val nameView = TextView(holder.layout.context).apply {
                text = zone.name ?: "Khu vực"
                setTextColor(Color.WHITE)
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
            }

            val priceView = TextView(holder.layout.context).apply {
                text = String.format("%,.0f đ", zonePrice)
                setTextColor(if (isFiltered) parsedColor else Color.parseColor("#606075"))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 6, 0, 6)
            }

            val seatsLeft = zone.seats?.count { it.status == "available" } ?: 0
            val countView = TextView(holder.layout.context).apply {
                text = "Còn $seatsLeft ghế trống"
                setTextColor(Color.parseColor("#A0A0B5"))
                textSize = 11f
                gravity = Gravity.CENTER
            }

            holder.layout.addView(nameView)
            holder.layout.addView(priceView)
            holder.layout.addView(countView)
        }

        override fun getItemCount(): Int = list.size

        class ViewHolder(val card: MaterialCardView, val layout: LinearLayout) : RecyclerView.ViewHolder(card)
    }
}
