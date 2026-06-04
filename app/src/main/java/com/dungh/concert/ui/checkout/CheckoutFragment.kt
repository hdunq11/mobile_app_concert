package com.dungh.concert.ui.checkout

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.dungh.concert.R
import com.dungh.concert.databinding.FragmentCheckoutBinding
import com.dungh.concert.network.OrderPricing
import com.dungh.concert.network.SessionManager
import com.dungh.concert.util.DateTimeUtils
import com.google.android.material.card.MaterialCardView

class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CheckoutViewModel
    private lateinit var sessionManager: SessionManager
    private val args: CheckoutFragmentArgs by navArgs()
    private var progressDialog: AlertDialog? = null
    private var customProgressText: TextView? = null

    private var deliveryFee = 0.0f
    private var insuranceFee = 0.0f
    private var discount = 0.0f
    private var finalPrice = 0.0f
    private var isETicketSelected = true
    private var appliedVoucherCode: String? = null
    private var holdTimer: android.os.CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(CheckoutViewModel::class.java)
        sessionManager = SessionManager(requireContext())
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)

        autoFillCustomerInfo()
        setupDeliverySelector()
        setupInsuranceSelector()
        setupListeners()
        bindViewModel()

        // Configure Initial Bill Breakdown
        binding.textSeatCount.text = "${args.seatIds.size} ghế"
        binding.textTotalAmount.text = String.format("%,.0f đ", args.totalPrice)
        binding.textVoucherDiscount.text = "- 0 đ"
        binding.radioMomo.isChecked = true
        binding.textBookingFee.text = String.format("%,.0f đ", OrderPricing.BOOKING_FEE)

        recalculateTotalPrice()
        startHoldTimerIfNeeded()

        return binding.root
    }

    private fun startHoldTimerIfNeeded() {
        val iso = args.reservedUntil
        if (iso.isNullOrBlank()) return
        binding.textHoldTimer.visibility = View.VISIBLE
        val remaining = DateTimeUtils.parseIsoToMillis(iso) - System.currentTimeMillis()
        if (remaining <= 0) {
            binding.textHoldTimer.text = "Hết giờ giữ chỗ!"
            binding.btnPayment.isEnabled = false
            return
        }
        holdTimer?.cancel()
        holdTimer = object : android.os.CountDownTimer(remaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.textHoldTimer.text =
                    String.format("Giữ chỗ còn: %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.textHoldTimer.text = "Hết giờ giữ chỗ!"
                binding.btnPayment.isEnabled = false
                Toast.makeText(
                    requireContext(),
                    "Thời gian giữ ghế đã hết. Vui lòng chọn lại ghế.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    private fun autoFillCustomerInfo() {
        val user = sessionManager.fetchUser()
        if (user != null) {
            binding.editCustomerName.setText(user.full_name ?: "")
            binding.editCustomerEmail.setText(user.email ?: "")
        }
    }

    private fun setupDeliverySelector() {
        // E-Ticket click handler
        binding.cardETicket.setOnClickListener {
            if (!isETicketSelected) {
                isETicketSelected = true
                deliveryFee = 0.0f
                binding.layoutDeliveryFeeRow.visibility = View.GONE
                
                // Toggle card visual styling
                binding.cardETicket.setStrokeColor(Color.parseColor("#FF1493"))
                binding.cardETicket.strokeWidth = (1.5f * resources.displayMetrics.density).toInt()
                binding.cardPaperTicket.setStrokeColor(Color.parseColor("#2E2E3C"))
                binding.cardPaperTicket.strokeWidth = (1f * resources.displayMetrics.density).toInt()
                
                recalculateTotalPrice()
            }
        }

        // Paper Ticket click handler
        binding.cardPaperTicket.setOnClickListener {
            if (isETicketSelected) {
                isETicketSelected = false
                deliveryFee = 30000.0f
                binding.layoutDeliveryFeeRow.visibility = View.VISIBLE
                binding.textDeliveryFee.text = "30.000 đ"
                
                // Toggle card visual styling
                binding.cardPaperTicket.setStrokeColor(Color.parseColor("#FF1493"))
                binding.cardPaperTicket.strokeWidth = (1.5f * resources.displayMetrics.density).toInt()
                binding.cardETicket.setStrokeColor(Color.parseColor("#2E2E3C"))
                binding.cardETicket.strokeWidth = (1f * resources.displayMetrics.density).toInt()
                
                recalculateTotalPrice()
            }
        }
    }

    private fun setupInsuranceSelector() {
        binding.checkInsurance.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                insuranceFee = 50000.0f * args.seatIds.size
                binding.layoutInsuranceFeeRow.visibility = View.VISIBLE
                binding.textInsuranceFee.text = String.format("%,.0f đ", insuranceFee)
            } else {
                insuranceFee = 0.0f
                binding.layoutInsuranceFeeRow.visibility = View.GONE
            }
            recalculateTotalPrice()
        }
    }

    private fun recalculateTotalPrice() {
        val deliveryMethod = if (isETicketSelected) "e_ticket" else "paper"
        finalPrice = OrderPricing.previewTotal(
            seatSubtotal = args.totalPrice.toDouble(),
            seatCount = args.seatIds.size,
            deliveryMethod = deliveryMethod,
            hasInsurance = binding.checkInsurance.isChecked,
            discountAmount = discount.toDouble()
        ).toFloat()
        binding.textFinalTotal.text = String.format("%,.0f đ", finalPrice)
    }

    private fun setupListeners() {
        binding.btnApplyVoucher.setOnClickListener {
            val code = binding.editVoucher.text.toString().trim()
            if (code.isEmpty()) {
                appliedVoucherCode = null
                discount = 0f
                viewModel.clearVoucher()
                binding.textVoucherDiscount.text = "- 0 đ"
                recalculateTotalPrice()
                return@setOnClickListener
            }
            viewModel.validateVoucher(code, args.totalPrice.toDouble())
        }

        binding.btnPayment.setOnClickListener {
            if (validateInputs()) {
                val paymentMethod = when (binding.radioPaymentMethod.checkedRadioButtonId) {
                    R.id.radioCreditCard -> "credit_card"
                    R.id.radioMomo -> "momo"
                    R.id.radioBankTransfer -> "bank_transfer"
                    else -> "momo"
                }
                val deliveryMethod = if (isETicketSelected) "e_ticket" else "paper"

                viewModel.executeBookingFlow(
                    args.concertId,
                    args.seatIds,
                    deliveryMethod,
                    binding.checkInsurance.isChecked,
                    appliedVoucherCode,
                    paymentMethod
                )
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name = binding.editCustomerName.text.toString().trim()
        val phone = binding.editCustomerPhone.text.toString().trim()
        val email = binding.editCustomerEmail.text.toString().trim()
        val address = binding.editCustomerAddress.text.toString().trim()

        if (name.isEmpty()) {
            binding.editCustomerName.error = "Vui lòng nhập họ tên người nhận"
            binding.editCustomerName.requestFocus()
            return false
        }
        if (phone.isEmpty()) {
            binding.editCustomerPhone.error = "Vui lòng nhập số điện thoại"
            binding.editCustomerPhone.requestFocus()
            return false
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editCustomerEmail.error = "Vui lòng nhập Email hợp lệ"
            binding.editCustomerEmail.requestFocus()
            return false
        }
        if (!isETicketSelected && address.isEmpty()) {
            binding.editCustomerAddress.error = "Vui lòng nhập địa chỉ nhận vé giấy"
            binding.editCustomerAddress.requestFocus()
            return false
        }
        return true
    }

    private fun bindViewModel() {
        viewModel.orderResult.observe(viewLifecycleOwner) { order ->
            order?.let {
                dismissProgress()
                findNavController().navigate(
                    CheckoutFragmentDirections.actionCheckoutToConfirmation(it.id ?: "")
                )
            }
        }

        viewModel.simulatedProgressText.observe(viewLifecycleOwner) { progressText ->
            if (progressText != null) {
                showProgress(progressText)
            } else {
                dismissProgress()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnPayment.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            binding.textError.text = error ?: ""
            binding.textError.visibility = if (error.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        viewModel.voucherResult.observe(viewLifecycleOwner) { result ->
            if (result?.valid == true) {
                appliedVoucherCode = result.code
                discount = (result.discount_amount ?: 0.0).toFloat()
                binding.textVoucherDiscount.text = String.format("- %,.0f đ", discount)
                recalculateTotalPrice()
                Toast.makeText(
                    requireContext(),
                    result.description ?: "Áp dụng mã thành công!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.voucherError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                appliedVoucherCode = null
                discount = 0f
                binding.textVoucherDiscount.text = "- 0 đ"
                recalculateTotalPrice()
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProgress(message: String) {
        if (progressDialog == null) {
            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(40, 40, 40, 40)
                setBackgroundColor(Color.parseColor("#1E1E26"))
            }

            val size = 100
            val progressBar = ProgressBar(requireContext()).apply {
                indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF1493"))
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    bottomMargin = 24
                }
            }

            customProgressText = TextView(requireContext()).apply {
                text = message
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            layout.addView(progressBar)
            layout.addView(customProgressText)

            progressDialog = AlertDialog.Builder(requireContext())
                .setView(layout)
                .setCancelable(false)
                .create()
        }

        customProgressText?.text = message
        if (progressDialog?.isShowing == false) {
            progressDialog?.show()
            progressDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
        progressDialog = null
        customProgressText = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        holdTimer?.cancel()
        dismissProgress()
        _binding = null
    }
}
