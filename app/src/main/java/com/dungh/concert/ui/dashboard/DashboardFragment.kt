package com.dungh.concert.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dungh.concert.network.RetrofitClient
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dungh.concert.adapter.TicketAdapter
import com.dungh.concert.databinding.FragmentDashboardBinding
import com.dungh.concert.ui.my_tickets.MyTicketsViewModel
import coil.load

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MyTicketsViewModel
    private lateinit var adapter: TicketAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(MyTicketsViewModel::class.java)
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        setupRecyclerView()
        bindViewModel()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = TicketAdapter(onClick = { order ->
            showTicketQrDialog(order)
        })
        binding.recyclerTickets.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTickets.adapter = adapter
    }

    private fun showTicketQrDialog(order: com.dungh.concert.model.Order) {
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            cardElevation = 16f
            radius = 48f
            setCardBackgroundColor(android.graphics.Color.parseColor("#181824"))
            strokeColor = android.graphics.Color.parseColor("#2E2E3C")
            strokeWidth = 3
        }

        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(40, 48, 40, 48)
        }

        val titleView = android.widget.TextView(requireContext()).apply {
            text = order.concert_title ?: "Mã đơn: ${order.id?.take(8)}"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        val qrFrame = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            radius = 24f
            setCardBackgroundColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(400, 400).apply {
                bottomMargin = 24
                gravity = android.view.Gravity.CENTER
            }
        }

        val qrImage = android.widget.ImageView(requireContext()).apply {
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setPadding(24, 24, 24, 24)
            val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=${order.id}"
            load(qrUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_dialog_dialer)
            }
        }
        qrFrame.addView(qrImage)

        val codeLabel = android.widget.TextView(requireContext()).apply {
            text = "MÃ SOÁT VÉ: ${order.id?.uppercase() ?: "PENDING"}"
            setTextColor(android.graphics.Color.parseColor("#00F5FF"))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }

        val priceLabel = android.widget.TextView(requireContext()).apply {
            text = "Giá vé: " + String.format("%,.0f đ", order.total_price ?: 0.0)
            setTextColor(android.graphics.Color.parseColor("#A0A0B5"))
            textSize = 13f
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }

        val closeBtn = androidx.appcompat.widget.AppCompatButton(requireContext()).apply {
            text = "ĐÓNG VÉ"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundResource(com.dungh.concert.R.drawable.bg_neon_pink_button)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                120
            )
        }

        val cancelBtn = androidx.appcompat.widget.AppCompatButton(requireContext()).apply {
            text = "HỦY ĐƠN HÀNG"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundResource(com.dungh.concert.R.drawable.bg_rounded_card)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#FF4444")
            )
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                100
            ).apply {
                bottomMargin = 16
            }
            visibility = if (order.status == "pending") View.VISIBLE else View.GONE
        }

        layout.addView(titleView)
        layout.addView(qrFrame)
        layout.addView(codeLabel)
        layout.addView(priceLabel)
        if (order.status == "pending") {
            layout.addView(cancelBtn)
        }
        layout.addView(closeBtn)

        card.addView(layout)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(card)
            .create()

        cancelBtn.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận hủy")
                .setMessage("Bạn có chắc muốn hủy đơn đặt vé này?")
                .setPositiveButton("Hủy đơn") { _, _ ->
                    cancelOrder(order.id, dialog)
                }
                .setNegativeButton("Không", null)
                .show()
        }

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun cancelOrder(orderId: String?, dialog: android.app.AlertDialog) {
        if (orderId.isNullOrBlank()) return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.concertApi.cancelOrder(orderId)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    viewModel.fetchMyOrders()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Không thể hủy đơn. Lỗi: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Lỗi: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bindViewModel() {
        viewModel.orders.observe(viewLifecycleOwner) { orders ->
            adapter.updateData(orders)
            binding.textEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressTickets.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            binding.textError.text = error ?: ""
            binding.textError.visibility = if (error.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchMyOrders()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
