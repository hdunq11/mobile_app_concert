package com.dungh.concert.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dungh.concert.databinding.ItemTicketBinding
import com.dungh.concert.model.Order

class TicketAdapter(
    private val tickets: MutableList<Order> = mutableListOf(),
    private val onClick: ((Order) -> Unit)? = null
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    fun updateData(newTickets: List<Order>) {
        tickets.clear()
        tickets.addAll(newTickets)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(tickets[position])
    }

    override fun getItemCount(): Int = tickets.size

    inner class TicketViewHolder(private val binding: ItemTicketBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {
            binding.ticketOrderId.text = order.concert_title ?: "Mã đơn: ${order.id?.take(8)}"
            binding.ticketPrice.text = String.format("%,.0f đ", order.total_price ?: 0.0)
            
            val (displayStatus, colorRes) = when (order.status) {
                "paid" -> "Đã thanh toán" to com.dungh.concert.R.color.success
                "pending" -> "Chờ thanh toán" to com.dungh.concert.R.color.warning
                "cancelled" -> "Đã hủy" to com.dungh.concert.R.color.text_muted
                else -> (order.status ?: "Không rõ") to com.dungh.concert.R.color.text_secondary
            }
            binding.ticketStatus.text = displayStatus
            binding.ticketStatus.setTextColor(
                androidx.core.content.ContextCompat.getColor(binding.root.context, colorRes)
            )
            
            binding.ticketDate.text = "Ngày đặt: " + (order.created_at?.replace("T", " ")?.take(16) ?: "Chưa xác định")

            binding.root.setOnClickListener {
                onClick?.invoke(order)
            }
        }
    }
}
