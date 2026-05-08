package com.example.tectest.ui.cash

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
// import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tectest.R
import com.example.tectest.data.model.Transaction
import com.example.tectest.databinding.FragmentCashBinding
import com.example.tectest.ui.cash.adapter.TransactionAdapter
import com.example.tectest.ui.cash.adapter.TransactionListItem
import com.example.tectest.viewmodel.CashViewModel
import com.example.tectest.viewmodel.ChartData
import com.example.tectest.viewmodel.TransactionsUIState
import com.google.firebase.Timestamp
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

class CashFragment : Fragment() {

    private var _binding: FragmentCashBinding? = null
    private val binding get() = _binding!!

    private var trashIconBitmap: Bitmap? = null

    private val cashViewModel: CashViewModel by activityViewModels()
    private lateinit var transactionAdapter: TransactionAdapter
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCashBinding.inflate(inflater, container, false)
        Log.d("CashFragment", "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("CashFragment", "onViewCreated")

        trashIconBitmap = getBitmapFromDrawable(requireContext(), R.drawable.ic_delete_24)

        setupPieChart()
        setupRecyclerView()
        setupClickListeners()
        setupObservers()
    }

    private fun setupPieChart() {
        binding.pieChartCashFlow.apply {
            setNoDataText("No transaction data for this month.") // If no data
            setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.app_on_background_text)) // The text color
            setUsePercentValues(true)
            description.isEnabled = false // No description
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true // Show hole in center
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(ContextCompat.getColor(requireContext(), R.color.app_on_surface_text))
            setTransparentCircleAlpha(110) // This alpha is part of the color usually
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "" // Center text of pie chart
            setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.app_accent_yellow))
            setCenterTextSize(16f)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.isEnabled = false // No legend
            setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.app_on_primary_text))
            setEntryLabelTextSize(12f)
        }
    }

    // to get the bitmap for the trash icon
    fun getBitmapFromDrawable(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { transaction ->
            // if transaction can be edited. add here:
            // Toast.makeText(requireContext(), "Clicked: ${transaction.title}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // To swipe for delete
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, // No drag & drop
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Swipe left or right
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // Not used
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = transactionAdapter.currentList.getOrNull(position)
                    if (item is TransactionListItem.TransactionData) {
                        cashViewModel.deleteTransaction(item.transaction.id)
                    } else {
                        transactionAdapter.notifyItemChanged(position)
                    }
                }
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val paint = Paint()
                    val icon = trashIconBitmap

                    if (icon == null) { // if bitmap loading failed
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        itemView.translationX = dX // Still allow swipe if icon fails
                        itemView.alpha = 1.0f - Math.abs(dX) / itemView.width.toFloat()
                        return
                    }

                    if (dX > 0) { // Swiping Right
                        paint.color = ContextCompat.getColor(requireContext(), R.color.expense_color)
                        c.drawRect(
                            itemView.left.toFloat(), itemView.top.toFloat(),
                            itemView.left.toFloat() + dX, itemView.bottom.toFloat(), paint
                        )
                        val iconMargin = (itemView.height - icon.height) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconLeft = itemView.left + iconMargin
                        c.drawBitmap(icon, iconLeft.toFloat(), iconTop.toFloat(), paint)
                    } else if (dX < 0) { // Swiping Left
                        paint.color = ContextCompat.getColor(requireContext(), R.color.expense_color)
                        c.drawRect(
                            itemView.right.toFloat() + dX, itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )
                        val iconMargin = (itemView.height - icon.height) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconLeft = itemView.right - iconMargin - icon.width
                        c.drawBitmap(icon, iconLeft.toFloat(), iconTop.toFloat(), paint)
                    }
                    itemView.alpha = 1.0f - Math.abs(dX) / itemView.width.toFloat()
                    itemView.translationX = dX // Manually move the item view
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewTransactions)
        Log.d("CashFragment", "RecyclerView and ItemTouchHelper setup complete.")
    }

    private fun setupClickListeners() {
        binding.fabAddTransaction.setOnClickListener {
            val addTransactionSheet = AddTransactionBottomSheetDialogFragment.newInstance()
            addTransactionSheet.show(childFragmentManager, AddTransactionBottomSheetDialogFragment.TAG)
        }

        binding.buttonPrevMonth.setOnClickListener { changeMonthBy(-1) }
        binding.buttonNextMonth.setOnClickListener { changeMonthBy(1) }
    }

    private fun changeMonthBy(monthOffset: Int) {
        val currentSelection = cashViewModel.selectedMonthYear.value
        if (currentSelection != null) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentSelection.first)
                set(Calendar.MONTH, currentSelection.second - 1)
                add(Calendar.MONTH, monthOffset)
            }
            cashViewModel.changeMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        } else {
            // Handle case where currentSelection is null, e.g., default to current month
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, monthOffset)
            cashViewModel.changeMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        }
    }

    private fun setupObservers() {
        cashViewModel.selectedMonthYear.observe(viewLifecycleOwner) { (year, month) ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
            }
            binding.textViewMonthYearCash.text = monthYearFormat.format(cal.time)
        }

        cashViewModel.transactionsState.observe(viewLifecycleOwner) { state ->
            binding.progressBarCash.isVisible = state is TransactionsUIState.Loading
            binding.recyclerViewTransactions.isVisible = state is TransactionsUIState.Success
            binding.textViewNoTransactions.isVisible = false // Reset

            when (state) {
                is TransactionsUIState.Loading -> {
                    Log.d("CashFragment", "Transactions State: Loading")
                }
                is TransactionsUIState.Success -> {
                    Log.d("CashFragment", "Transactions State: Success - Income: ${state.monthlyTotalIncome}, Expense: ${state.monthlyTotalExpenses}")
                    binding.textViewTotalIncomeValue.text = "+ ${currencyFormatter.format(state.monthlyTotalIncome)}"
                    binding.textViewTotalExpenseValue.text = "- ${currencyFormatter.format(state.monthlyTotalExpenses)}"
                    val net = state.monthlyTotalIncome - state.monthlyTotalExpenses
                    binding.textViewNetBalanceValue.text = currencyFormatter.format(net)
                    binding.textViewNetBalanceValue.setTextColor(
                        ContextCompat.getColor(requireContext(), if (net >= 0) R.color.income_color else R.color.expense_color)
                    )

                    val listItems = mutableListOf<TransactionListItem>()
                    val sdfHeader = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                    // transactionsByDay sorted
                    state.transactionsByDay.toSortedMap(compareByDescending { it }) // sorted by date descending
                        .forEach { (date, transactionsOnDay) ->
                            listItems.add(TransactionListItem.HeaderItem(sdfHeader.format(date)))
                            // Sort transactions within the day by createdAt
                            transactionsOnDay.sortedByDescending { it.createdAt ?: Timestamp(0,0) }.forEach { txn ->
                                listItems.add(TransactionListItem.TransactionData(txn))
                            }
                        }
                    transactionAdapter.submitList(listItems)
                    binding.textViewNoTransactions.isVisible = listItems.isEmpty() && (state !is TransactionsUIState.Loading)
                }
                is TransactionsUIState.Error -> {
                    Log.e("CashFragment", "Transactions State: Error - ${state.message}")
                    binding.textViewNoTransactions.text = state.message
                    binding.textViewNoTransactions.isVisible = true
                    binding.recyclerViewTransactions.isVisible = false
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        cashViewModel.chartData.observe(viewLifecycleOwner) { chartData ->
            updatePieChart(chartData)
        }

        cashViewModel.actionFeedback.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            result.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            result.onFailure { exception ->
                    Toast.makeText(requireContext(), "Action Error: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }


    private fun updatePieChart(data: ChartData?) {
        if (data == null || (data.incomeTotal == 0f && data.expenseTotal == 0f)) {
            binding.pieChartCashFlow.data?.clearValues() // Clear existing data
            binding.pieChartCashFlow.clear()            // Clear the chart
            binding.pieChartCashFlow.setNoDataText("No transaction data for this month.")
            binding.pieChartCashFlow.invalidate()       // Refresh to show no data text
            binding.pieChartCashFlow.visibility = View.VISIBLE // Keep chart visible to show "no data"
            return
        }
        binding.pieChartCashFlow.visibility = View.VISIBLE

        val entries = ArrayList<PieEntry>()
        if (data.incomeTotal > 0) {
            entries.add(PieEntry(data.incomeTotal, "")) // Add label if needed
        }
        if (data.expenseTotal > 0) {
            entries.add(PieEntry(data.expenseTotal, "")) // Same here
        }

        if (entries.isEmpty()){ // Level 2.0 Validation
            binding.pieChartCashFlow.clear()
            binding.pieChartCashFlow.setNoDataText("No transaction data for this month.")
            binding.pieChartCashFlow.invalidate()
            return
        }


        val dataSet = PieDataSet(entries, "").apply {
            sliceSpace = 2f
            selectionShift = 5f
            val colors = ArrayList<Int>()
            // Order of adding colors should match order of adding entries
            if (data.incomeTotal > 0) colors.add(ContextCompat.getColor(requireContext(), R.color.income_color))
            if (data.expenseTotal > 0) colors.add(ContextCompat.getColor(requireContext(), R.color.expense_color))
            this.colors = colors
            setDrawValues(false) // Set true if want to show percentage and uncomment below if want percentage shown
//            valueLinePart1OffsetPercentage = 80f
//            valueLinePart1Length = 0.4f
//            valueLinePart2Length = 0.4f
//            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        }


        val pieData = PieData(dataSet).apply {
            // Uncomment if want to set pie data to show
//            setValueFormatter(PercentFormatter(binding.pieChartCashFlow))
//            setValueTextSize(10f)
//            setValueTextColor(ContextCompat.getColor(requireContext(), R.color.app_on_background_text)) // Text color for percentages
        }

        binding.pieChartCashFlow.data = pieData
        binding.pieChartCashFlow.description.isEnabled = false
        binding.pieChartCashFlow.legend.isEnabled = false
        binding.pieChartCashFlow.setDrawEntryLabels(false)
        binding.pieChartCashFlow.invalidate()
        binding.pieChartCashFlow.animateY(1000, Easing.EaseInOutQuad)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        if(::transactionAdapter.isInitialized) binding.recyclerViewTransactions.adapter = null // Clear adapter
        _binding = null
        Log.d("CashFragment", "onDestroyView")
    }
}