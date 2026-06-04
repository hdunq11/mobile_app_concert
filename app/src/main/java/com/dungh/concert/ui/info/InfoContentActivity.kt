package com.dungh.concert.ui.info

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dungh.concert.databinding.ActivityInfoContentBinding

class InfoContentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TYPE = "info_type"
        const val TYPE_PAYMENT = "payment"
        const val TYPE_TERMS = "terms"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityInfoContentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        when (intent.getStringExtra(EXTRA_TYPE)) {
            TYPE_PAYMENT -> {
                title = "Phương thức thanh toán"
                binding.textInfoTitle.text = "Phương thức thanh toán liên kết"
                binding.textInfoBody.text = """
                    Ứng dụng hỗ trợ các kênh thanh toán sau (giả lập cho môi trường demo):

                    • Momo — Ví điện tử, thanh toán nhanh
                    • Visa/Mastercard — Thẻ tín dụng/ghi nợ quốc tế
                    • VNPAY — Chuyển khoản ngân hàng nội địa

                    Sau khi xác nhận đơn, hệ thống gọi API thanh toán mock và cập nhật trạng thái vé sang「Đã thanh toán」.

                    Lưu ý: Đây là luồng demo DATN, không trừ tiền thật.
                """.trimIndent()
            }
            else -> {
                title = "Điều khoản & Bảo mật"
                binding.textInfoTitle.text = "Điều khoản sử dụng & Chính sách bảo mật"
                binding.textInfoBody.text = """
                    1. Dữ liệu cá nhân (họ tên, email, số điện thoại) chỉ dùng để xử lý đơn đặt vé và liên hệ khi cần.

                    2. Mật khẩu được lưu dạng băm trên server, không lưu plain-text trên thiết bị (chỉ lưu token JWT).

                    3. Bạn có thể cập nhật hồ sơ hoặc đăng xuất bất cứ lúc nào trong tab Hồ sơ.

                    4. Mã giảm giá (DATN10, CONCERT20) do hệ thống quản lý — mỗi mã có phần trăm giảm riêng trên giá vé.

                    5. Vé điện tử được hiển thị bằng mã QR đơn hàng sau thanh toán thành công.

                    Liên hệ hỗ trợ: support@concert-datn.local
                """.trimIndent()
            }
        }

        binding.btnClose.setOnClickListener { finish() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
