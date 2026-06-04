package com.dungh.concert.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dungh.concert.databinding.ActivityRegisterBinding
import com.dungh.concert.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(applicationContext)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.btnRegister.setOnClickListener {
            performRegister()
        }

        binding.btnGoLogin.setOnClickListener {
            finish()
        }
    }

    private fun performRegister() {
        val name = binding.editName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString().trim()
        val passwordConfirm = binding.editPasswordConfirm.text.toString().trim()

        if (name.isEmpty()) {
            binding.editName.error = "Vui lòng nhập Họ và Tên"
            return
        }
        if (email.isEmpty()) {
            binding.editEmail.error = "Vui lòng nhập Email"
            return
        }
        if (password.isEmpty()) {
            binding.editPassword.error = "Vui lòng nhập Mật khẩu"
            return
        }
        if (passwordConfirm.isEmpty()) {
            binding.editPasswordConfirm.error = "Vui lòng xác nhận Mật khẩu"
            return
        }
        if (password != passwordConfirm) {
            binding.editPasswordConfirm.error = "Mật khẩu xác nhận không khớp"
            return
        }

        binding.progressRegister.visibility = View.VISIBLE
        binding.textError.visibility = View.GONE
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.concertApi.register(
                    mapOf(
                        "email" to email,
                        "password" to password,
                        "password_confirm" to passwordConfirm,
                        "full_name" to name
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Đăng ký thành công! Hãy đăng nhập.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    if (errorBody.contains("email") && errorBody.contains("already exists")) {
                        showError("Email này đã được sử dụng.")
                    } else {
                        showError("Đăng ký thất bại. Lỗi: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                showError("Lỗi kết nối mạng: ${e.localizedMessage ?: "Không xác định"}")
            } finally {
                binding.progressRegister.visibility = View.GONE
                binding.btnRegister.isEnabled = true
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }
}
