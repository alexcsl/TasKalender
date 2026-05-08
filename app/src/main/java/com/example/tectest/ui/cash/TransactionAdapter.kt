package com.example.tectest.ui.cash.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tectest.R
import com.example.tectest.data.model.Transaction
import com.example.tectest.data.model.TransactionType
import com.example.tectest.databinding.ItemTransactionBinding
import com.example.tectest.databinding.ItemTransactionDateHeaderBinding
import java.text.NumberFormat
import java.util.Locale
import android.widget.TextView
import android.widget.Toast

sealed class TransactionListItem {
    data class HeaderItem(val dateString: String, val uniqueId: String = dateString) : TransactionListItem()
    data class TransactionData(val transaction: Transaction) : TransactionListItem()

    val id: String
        get() = when (this) {
            is HeaderItem -> this.uniqueId
            is TransactionData -> this.transaction.id
        }
}

class TransactionListItemDiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {
    override fun areItemsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
        return oldItem == newItem
    }
}

class TransactionAdapter(
    private val onTransactionClicked: (Transaction) -> Unit
) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(TransactionListItemDiffCallback()) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TRANSACTION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionListItem.HeaderItem -> VIEW_TYPE_HEADER
            is TransactionListItem.TransactionData -> VIEW_TYPE_TRANSACTION
            null -> throw IllegalStateException("Null item in ListAdapter")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
//                 val binding = ItemTransactionDateHeaderBinding.inflate(inflater, parent, false)
//                 HeaderViewHolder(binding)
                val textView = TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setPadding(48, 24, 24, 12) // Adjust padding (left, top, right, bottom)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                    setTextColor(ContextCompat.getColor(parent.context, R.color.app_on_background_text))
                }
                HeaderViewHolder(textView)
            }
            VIEW_TYPE_TRANSACTION -> {
                val binding = ItemTransactionBinding.inflate(inflater, parent, false)
                TransactionItemViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) // getItem() is from ListAdapter
        if (item != null) { // Add null check for safety
            when (item) {
                is TransactionListItem.HeaderItem -> (holder as HeaderViewHolder).bind(item)
                is TransactionListItem.TransactionData -> (holder as TransactionItemViewHolder).bind(item.transaction, onTransactionClicked, currencyFormatter)
            }
        }
    }

    class HeaderViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(headerItem: TransactionListItem.HeaderItem) {
            textView.text = headerItem.dateString
        }
    }

    class TransactionItemViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            transaction: Transaction,
            onTransactionClicked: (Transaction) -> Unit,
            formatter: NumberFormat
        ) {
            binding.textViewTransactionTitle.text = transaction.title
            binding.textViewTransactionCategory.text = transaction.category

            val amountFormatted = formatter.format(transaction.amount)
            if (transaction.type == TransactionType.INCOME.name) {
                binding.textViewTransactionAmount.text = "+ $amountFormatted"
                binding.textViewTransactionAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.income_color))
                binding.imageViewTransactionTypeIcon.setImageResource(R.drawable.ic_arrow_upward_24)
                binding.imageViewTransactionTypeIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.income_color))
            } else { // Expense
                binding.textViewTransactionAmount.text = "- $amountFormatted"
                binding.textViewTransactionAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.expense_color))
                binding.imageViewTransactionTypeIcon.setImageResource(R.drawable.ic_arrow_downward_24)
                binding.imageViewTransactionTypeIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.expense_color))
            }

            itemView.setOnClickListener {
                Toast.makeText(itemView.context, "Transaction: ${transaction.title}", Toast.LENGTH_SHORT).show()
                // onTransactionClicked(transaction) // Uncomment if want to enable edit from this list
            }
        }
    }
}