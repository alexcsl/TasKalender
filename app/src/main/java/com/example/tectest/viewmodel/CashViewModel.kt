package com.example.tectest.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tectest.data.model.Transaction
import com.example.tectest.data.model.TransactionCategory
import com.example.tectest.data.model.TransactionType
import com.example.tectest.data.repository.TransactionRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

// --- UI States ---
sealed class TransactionsUIState {
    object Loading : TransactionsUIState()
    // Data class will hold transactions grouped by day for the RecyclerView
    data class Success(
        val transactionsByDay: Map<Date, List<Transaction>>, // Key: Start of day, Value: List of transactions for that day
        val monthlyTotalIncome: Double,
        val monthlyTotalExpenses: Double
    ) : TransactionsUIState()
    data class Error(val message: String) : TransactionsUIState()
}

sealed class CategoriesUIState {
    object Loading : CategoriesUIState()
    data class Success(val categories: List<TransactionCategory>) : CategoriesUIState()
    data class Error(val message: String) : CategoriesUIState()
}

// --- Data for Chart ---
data class ChartData(
    val incomeTotal: Float,
    val expenseTotal: Float
)

class CashViewModel : ViewModel() {

    private val transactionRepository = TransactionRepository()

    // --- LiveData for UI ---
    private val _transactionsState = MutableLiveData<TransactionsUIState>()
    val transactionsState: LiveData<TransactionsUIState> = _transactionsState

    private val _categoriesState = MutableLiveData<CategoriesUIState>()
    val categoriesState: LiveData<CategoriesUIState> = _categoriesState

    private val _chartData = MutableLiveData<ChartData?>()
    val chartData: LiveData<ChartData?> = _chartData

    private val _actionFeedback = MutableLiveData<Result<String>?>()
    val actionFeedback: LiveData<Result<String>?> = _actionFeedback

    private val _selectedMonthYear = MutableLiveData<Pair<Int, Int>>().apply {
        val cal = Calendar.getInstance()
        value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
    val selectedMonthYear: LiveData<Pair<Int, Int>> = _selectedMonthYear



    init {
        loadDataForSelectedMonth()
        loadTransactionCategories()
    }

    fun changeMonth(year: Int, month: Int) {
        _selectedMonthYear.value = Pair(year, month)
        loadDataForSelectedMonth()
    }

    fun refreshData() {
        loadDataForSelectedMonth()
        loadTransactionCategories()
    }

    private fun loadDataForSelectedMonth() {
        _transactionsState.value = TransactionsUIState.Loading
        val currentSelection = _selectedMonthYear.value
        if (currentSelection == null) {
            _transactionsState.value = TransactionsUIState.Error("Month and year not selected.")
            return
        }
        val year = currentSelection.first
        val month = currentSelection.second

        viewModelScope.launch {
            val result = transactionRepository.getTransactionsForMonth(year, month)
            result.onSuccess { transactions ->
                val groupedByDay = transactions.groupBy {
                    // Normalize transaction date to the start of its day for grouping
                    val cal = Calendar.getInstance()
                    cal.time = it.date.toDate()
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                    cal.time
                }.toSortedMap(compareByDescending { it }) // sort descending


                var totalIncome = 0.0
                var totalExpenses = 0.0
                transactions.forEach { txn ->
                    if (txn.type == TransactionType.INCOME.name) {
                        totalIncome += txn.amount
                    } else if (txn.type == TransactionType.EXPENSE.name) {
                        totalExpenses += txn.amount
                    }
                }
                _transactionsState.postValue(TransactionsUIState.Success(groupedByDay, totalIncome, totalExpenses))
                _chartData.postValue(ChartData(totalIncome.toFloat(), totalExpenses.toFloat()))
                Log.d("CashViewModel", "Loaded ${transactions.size} transactions for $year-$month. Income: $totalIncome, Expense: $totalExpenses")

            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Failed to load transactions"
                _transactionsState.postValue(TransactionsUIState.Error(errorMsg))
                Log.e("CashViewModel", "Error loading transactions: $errorMsg", exception)
            }
        }
    }

    fun loadTransactionCategories() {
        _categoriesState.value = CategoriesUIState.Loading
        viewModelScope.launch {
            val result = transactionRepository.getTransactionCategories()
            result.onSuccess { categories ->
                _categoriesState.postValue(CategoriesUIState.Success(categories))
            }.onFailure { exception ->
                _categoriesState.postValue(CategoriesUIState.Error(exception.localizedMessage ?: "Failed to load categories"))
            }
        }
    }

    fun addTransaction(title: String, amount: Double, type: TransactionType, date: Date, category: String) {
        if (title.isBlank() || amount <= 0 || category.isBlank()) {
            _actionFeedback.postValue(Result.failure(IllegalArgumentException("All fields are required and amount must be positive.")))
            return
        }
        val transaction = Transaction(
            title = title,
            amount = amount,
            type = type.name,
            date = Timestamp(date),
            category = category
        )
        viewModelScope.launch {
            val result = transactionRepository.addTransaction(transaction)
            result.onSuccess {
                _actionFeedback.postValue(Result.success("Transaction added successfully!"))
                loadDataForSelectedMonth()
            }.onFailure { exception ->
                _actionFeedback.postValue(Result.failure(exception))
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            val result = transactionRepository.deleteTransaction(transactionId)
            result.onSuccess {
                _actionFeedback.postValue(Result.success("Transaction deleted."))
                loadDataForSelectedMonth()
            }.onFailure { exception ->
                _actionFeedback.postValue(Result.failure(exception))
            }
        }
    }

    fun addTransactionCategory(categoryName: String) {
        if (categoryName.isBlank()){
            _actionFeedback.postValue(Result.failure(IllegalArgumentException("Category name cannot be empty.")))
            return
        }
        viewModelScope.launch {
            val result = transactionRepository.addTransactionCategory(categoryName)
            result.onSuccess {
                _actionFeedback.postValue(Result.success("Category '$categoryName' added!"))
                loadTransactionCategories()
            }.onFailure { exception ->
                if (exception.message?.contains("already exists", ignoreCase = true) == true) {
                    _actionFeedback.postValue(Result.failure(IllegalStateException("Category '$categoryName' already exists.")))
                } else {
                    _actionFeedback.postValue(Result.failure(exception))
                }
            }
        }
    }

    fun clearActionFeedback() {
        _actionFeedback.value = null
    }
}