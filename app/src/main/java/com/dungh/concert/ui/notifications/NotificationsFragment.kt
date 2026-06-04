package com.dungh.concert.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dungh.concert.databinding.FragmentNotificationsBinding
import com.dungh.concert.network.RetrofitClient
import com.dungh.concert.network.SessionManager
import com.dungh.concert.ui.auth.LoginActivity
import com.dungh.concert.ui.info.InfoContentActivity
import coil.load
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sessionManager = SessionManager(requireContext())
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        val user = sessionManager.fetchUser()
        if (user != null) {
            bindUser(user.full_name, user.email, user.avatar_url)
            loadStats()
            binding.btnEditProfile.setOnClickListener { showEditProfileDialog() }
        } else {
            binding.textUserName.text = "Xin chào, Bạn!"
            binding.textUserEmail.text = "Chưa đăng nhập"
            binding.btnEditProfile.isEnabled = false
        }

        binding.btnPaymentMethods.setOnClickListener {
            startActivity(
                android.content.Intent(requireContext(), InfoContentActivity::class.java).apply {
                    putExtra(InfoContentActivity.EXTRA_TYPE, InfoContentActivity.TYPE_PAYMENT)
                }
            )
        }

        binding.btnTermsPolicy.setOnClickListener {
            startActivity(
                android.content.Intent(requireContext(), InfoContentActivity::class.java).apply {
                    putExtra(InfoContentActivity.EXTRA_TYPE, InfoContentActivity.TYPE_TERMS)
                }
            )
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.logout()
            Toast.makeText(requireContext(), "Đăng xuất thành công!", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            activity?.finish()
        }

        return binding.root
    }

    private fun bindUser(name: String?, email: String?, avatarUrl: String?) {
        binding.textUserName.text = "Xin chào, ${name ?: "Bạn"}!"
        binding.textUserEmail.text = email ?: "Chưa cập nhật email"

        if (!avatarUrl.isNullOrBlank()) {
            binding.imageUserAvatar.load(avatarUrl) {
                crossfade(true)
                placeholder(com.dungh.concert.R.drawable.ic_user)
                error(com.dungh.concert.R.drawable.ic_user)
            }
            binding.imageUserAvatar.imageTintList = null
        }
    }

    private fun showEditProfileDialog() {
        val user = sessionManager.fetchUser() ?: return
        val input = EditText(requireContext()).apply {
            setText(user.full_name ?: "")
            hint = "Họ và tên"
            setPadding(48, 32, 48, 16)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Chỉnh sửa hồ sơ")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "Họ tên không được để trống", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveProfile(newName)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun saveProfile(fullName: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.concertApi.updateProfile(
                    mapOf("full_name" to fullName)
                )
                if (response.isSuccessful && response.body() != null) {
                    val updated = response.body()!!
                    sessionManager.saveUser(updated)
                    bindUser(updated.full_name, updated.email, updated.avatar_url)
                    Toast.makeText(requireContext(), "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Cập nhật thất bại. Lỗi: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Lỗi kết nối: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val ordersResponse = RetrofitClient.concertApi.getMyOrders()
                if (ordersResponse.isSuccessful) {
                    val orders = ordersResponse.body()
                    binding.textStatTickets.text = (orders?.size ?: 0).toString()
                }

                val favResponse = RetrofitClient.concertApi.getFavorites()
                if (favResponse.isSuccessful) {
                    val favorites = favResponse.body()
                    binding.textStatFavorites.text = (favorites?.size ?: 0).toString()
                }
            } catch (_: Exception) {
                // Ignore silent stats error
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isLoggedIn()) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.concertApi.getCurrentUser()
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!
                        sessionManager.saveUser(user)
                        bindUser(user.full_name, user.email, user.avatar_url)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
