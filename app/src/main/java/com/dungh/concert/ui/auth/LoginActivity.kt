package com.dungh.concert.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dungh.concert.MainActivity
import com.dungh.concert.databinding.ActivityLoginBinding
import com.dungh.concert.network.RetrofitClient
import com.dungh.concert.network.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Retrofit with application Context
        RetrofitClient.init(applicationContext)
        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            startMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.btnGoRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.editEmail.error = "Vui lòng nhập Email"
            return
        }
        if (password.isEmpty()) {
            binding.editPassword.error = "Vui lòng nhập Mật khẩu"
            return
        }

        binding.progressLogin.visibility = View.VISIBLE
        binding.textError.visibility = View.GONE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.concertApi.login(
                    mapOf("email" to email, "password" to password)
                )

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    val access = loginResponse.access
                    val refresh = loginResponse.refresh
                    val user = loginResponse.user

                    if (access != null && refresh != null && user != null) {
                        sessionManager.saveTokens(access, refresh)
                        sessionManager.saveUser(user)
                        
                        Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                        startMainActivity()
                    } else {
                        showError("Lỗi dữ liệu từ hệ thống.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody?.contains("Invalid credentials") == true || response.code() == 401) {
                        showError("Email hoặc Mật khẩu không chính xác.")
                    } else {
                        showError("Đăng nhập thất bại. Lỗi: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                showError("Lỗi kết nối mạng: ${e.localizedMessage ?: "Không xác định"}")
            } finally {
                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    private fun startMainActivity() {
        if (intent.getBooleanExtra("FROM_BOOKING", false)) {
            finish() // Returns back to the SeatSelectionFragment caller directly!
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
    }
}
