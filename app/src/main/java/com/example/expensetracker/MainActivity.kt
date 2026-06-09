package com.example.expensetracker

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Expense(
    val id: Long,
    val description: String,
    val amount: Double,
    val date: String,
    val category: String,
    val type: String
)

class MainActivity : AppCompatActivity() {

    private val expenses = mutableListOf<Expense>()
    private lateinit var expenseList: LinearLayout
    private lateinit var tvBalance: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var summaryList: LinearLayout

    private lateinit var tvBudgetAmount: TextView
    private lateinit var tvBudgetSpent: TextView
    private lateinit var tvBudgetRemaining: TextView
    private lateinit var budgetProgress: ProgressBar
    private lateinit var tvBudgetWarning: TextView
    private var monthlyBudget = 0.0

    // Different categories for Expense and Income
    private val expenseCategories = arrayOf("Food", "Transport", "Bills", "Shopping", "Health", "Other")
    private val incomeCategories = arrayOf("Sales", "Capital", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnIncome = findViewById<Button>(R.id.btnIncome)
        expenseList = findViewById(R.id.expenseList)
        tvBalance = findViewById(R.id.tvBalance)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvIncome = findViewById(R.id.tvIncome)
        tvExpense = findViewById(R.id.tvExpense)
        summaryList = findViewById(R.id.summaryList)

        // Find budget views
        tvBudgetAmount = findViewById(R.id.tvBudgetAmount)
        tvBudgetSpent = findViewById(R.id.tvBudgetSpent)
        tvBudgetRemaining = findViewById(R.id.tvBudgetRemaining)
        budgetProgress = findViewById(R.id.budgetProgress)
        tvBudgetWarning = findViewById(R.id.tvBudgetWarning)
        val btnSetBudget = findViewById<Button>(R.id.btnSetBudget)
        val btnClearBudget = findViewById<Button>(R.id.btnClearBudget)

        // Load budget
        monthlyBudget = loadBudget()
        updateBudgetDisplay()

        // Set budget button click
        btnSetBudget.setOnClickListener {
            showSetBudgetDialog()
        }

        // Clear budget button click
        btnClearBudget.setOnClickListener {
            showClearBudgetDialog()
        }

        loadExpenses()
        refreshList()

        btnAdd.setOnClickListener {
            showAddDialog(etAmount, etDescription, "expense")
        }

        btnIncome.setOnClickListener {
            showAddDialog(etAmount, etDescription, "income")
        }
    }

    private fun saveBudget(budget: Double) {
        val prefs = getSharedPreferences("ExpenseData", Context.MODE_PRIVATE)
        prefs.edit().putString("monthly_budget", budget.toString()).apply()
    }

    private fun loadBudget(): Double {
        val prefs = getSharedPreferences("ExpenseData", Context.MODE_PRIVATE)
        val budgetStr = prefs.getString("monthly_budget", "0")
        return budgetStr?.toDoubleOrNull() ?: 0.0
    }

    private fun showAddDialog(etAmount: EditText, etDescription: EditText, type: String) {
        val amountText = etAmount.text.toString()
        val description = etDescription.text.toString()

        if (amountText.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Choose category list based on type
        val categoriesToShow = if (type == "expense") expenseCategories else incomeCategories
        val title = if (type == "expense") "Select Expense Category" else "Select Income Category"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(categoriesToShow) { _, which ->
                val category = categoriesToShow[which]
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                val date = sdf.format(Date())

                val expense = Expense(
                    id = System.currentTimeMillis(),
                    description = description,
                    amount = amount,
                    date = date,
                    category = category,
                    type = type
                )

                expenses.add(0, expense)
                saveExpenses()
                refreshList()

                etAmount.text.clear()
                etDescription.text.clear()
                val label = if (type == "income") "Income" else "Expense"
                Toast.makeText(this, "$label saved!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun refreshList() {
        expenseList.removeAllViews()

        val totalIncome = expenses.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpenses = expenses.filter { it.type == "expense" }.sumOf { it.amount }
        val balance = totalIncome - totalExpenses

        tvBalance.text = String.format("TZS %.2f", balance)
        tvIncome.text = String.format("+ TZS %.2f", totalIncome)
        tvExpense.text = String.format("- TZS %.2f", totalExpenses)

        if (expenses.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            for (expense in expenses) {
                addExpenseRow(expense)
            }
        }

        refreshMonthlySummary()
        updateBudgetDisplay()
    }

    private fun refreshMonthlySummary() {
        summaryList.removeAllViews()

        // Get current date info
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // Parse dates and filter transactions for current month/year
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val thisMonthTransactions = expenses.filter { expense ->
            try {
                val date = dateFormat.parse(expense.date)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        val thisMonthIncome = thisMonthTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val thisMonthExpenses = thisMonthTransactions.filter { it.type == "expense" }.sumOf { it.amount }
        val monthNet = thisMonthIncome - thisMonthExpenses

        // Always show the summary card with data
        val monthName = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())

        // Card header
        val cardHeader = TextView(this)
        cardHeader.text = "📊 $monthName Summary"
        cardHeader.textSize = 16f
        cardHeader.setTypeface(null, android.graphics.Typeface.BOLD)
        cardHeader.setTextColor(Color.parseColor("#1A1A2E"))
        cardHeader.setPadding(0, 0, 0, 16)
        summaryList.addView(cardHeader)

        if (thisMonthTransactions.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No transactions yet this month.\nAdd an expense or income above!"
            tv.textSize = 13f
            tv.setTextColor(Color.parseColor("#8A94A6"))
            tv.gravity = Gravity.CENTER
            tv.setPadding(0, 16, 0, 16)
            summaryList.addView(tv)
            return
        }

        // Income row
        val incomeRow = createSummaryRow("💰 Income", thisMonthIncome, Color.parseColor("#38A169"))
        summaryList.addView(incomeRow)

        // Expense row
        val expenseRow = createSummaryRow("💸 Expenses", thisMonthExpenses, Color.parseColor("#E53E3E"))
        summaryList.addView(expenseRow)

        // Divider
        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1)
        divider.setBackgroundColor(Color.parseColor("#E2E8F0"))
        divider.setPadding(0, 8, 0, 8)
        summaryList.addView(divider)

        // Net row
        val netColor = if (monthNet >= 0) Color.parseColor("#38A169") else Color.parseColor("#E53E3E")
        val netIcon = if (monthNet >= 0) "📈" else "📉"
        val netRow = createSummaryRow("$netIcon Net Savings", monthNet, netColor, true)
        summaryList.addView(netRow)

        // Show category breakdown only if there are expenses
        if (thisMonthExpenses > 0) {
            val categoryDivider = View(this)
            categoryDivider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1)
            categoryDivider.setBackgroundColor(Color.parseColor("#E2E8F0"))
            categoryDivider.setPadding(0, 16, 0, 12)
            summaryList.addView(categoryDivider)

            val tvBreakdown = TextView(this)
            tvBreakdown.text = "📂 Expenses by Category"
            tvBreakdown.textSize = 13f
            tvBreakdown.setTypeface(null, android.graphics.Typeface.BOLD)
            tvBreakdown.setTextColor(Color.parseColor("#1A1A2E"))
            tvBreakdown.setPadding(0, 0, 0, 12)
            summaryList.addView(tvBreakdown)

            // Group expenses by category
            val expensesOnly = thisMonthTransactions.filter { it.type == "expense" }
            val byCategory = expensesOnly.groupBy { it.category }

            for ((category, items) in byCategory) {
                val total = items.sumOf { it.amount }
                val percent = ((total / thisMonthExpenses) * 100).toInt()

                val row = LinearLayout(this)
                row.orientation = LinearLayout.VERTICAL
                row.setPadding(0, 8, 0, 8)

                // Label row
                val labelRow = LinearLayout(this)
                labelRow.orientation = LinearLayout.HORIZONTAL
                labelRow.gravity = Gravity.CENTER_VERTICAL

                val tvCat = TextView(this)
                tvCat.text = "${getCategoryIcon(category)} $category"
                tvCat.textSize = 14f
                tvCat.setTextColor(Color.parseColor("#1A1A2E"))
                tvCat.layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                val tvAmt = TextView(this)
                tvAmt.text = "TZS ${String.format("%.2f", total)} ($percent%)"
                tvAmt.textSize = 12f
                tvAmt.setTextColor(Color.parseColor("#8A94A6"))

                labelRow.addView(tvCat)
                labelRow.addView(tvAmt)

                // Progress bar
                val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
                progressBar.max = 100
                progressBar.progress = percent
                progressBar.progressDrawable.setColorFilter(
                    Color.parseColor("#1976D2"),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
                val pbParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 8)
                pbParams.setMargins(0, 6, 0, 0)
                progressBar.layoutParams = pbParams

                row.addView(labelRow)
                row.addView(progressBar)
                summaryList.addView(row)
            }
        }
    }

    private fun createSummaryRow(label: String, amount: Double, color: Int, isBold: Boolean = false): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, 8, 0, 8)

        val tvLabel = TextView(this)
        tvLabel.text = label
        tvLabel.textSize = 14f
        if (!isBold) tvLabel.setTextColor(Color.parseColor("#4A5568"))
        tvLabel.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val tvAmount = TextView(this)
        val sign = if (amount < 0) "-" else "+"
        tvAmount.text = "$sign TZS ${String.format("%.2f", kotlin.math.abs(amount))}"
        tvAmount.textSize = 14f
        tvAmount.setTextColor(color)
        if (isBold) tvAmount.setTypeface(null, android.graphics.Typeface.BOLD)

        row.addView(tvLabel)
        row.addView(tvAmount)

        return row
    }

    private fun addExpenseRow(expense: Expense) {
        val wrapper = LinearLayout(this)
        wrapper.orientation = LinearLayout.VERTICAL
        wrapper.setPadding(0, 8, 0, 8)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL

        val leftLayout = LinearLayout(this)
        leftLayout.orientation = LinearLayout.VERTICAL
        leftLayout.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val tvDesc = TextView(this)
        tvDesc.text = "${getCategoryIcon(expense.category)} ${expense.description}"
        tvDesc.textSize = 15f
        tvDesc.setTextColor(Color.parseColor("#1A1A2E"))

        val tvMeta = TextView(this)
        tvMeta.text = "${expense.category} • ${expense.date}"
        tvMeta.textSize = 11f
        tvMeta.setTextColor(Color.parseColor("#8A94A6"))

        leftLayout.addView(tvDesc)
        leftLayout.addView(tvMeta)

        val tvAmt = TextView(this)
        val isIncome = expense.type == "income"
        tvAmt.text = String.format("%s TZS %.2f", if (isIncome) "+" else "-", expense.amount)
        tvAmt.textSize = 15f
        tvAmt.setTextColor(Color.parseColor(if (isIncome) "#38A169" else "#E53E3E"))
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD)

        // Delete Button
        val btnDelete = TextView(this)
        btnDelete.text = "  🗑️  "
        btnDelete.textSize = 16f
        btnDelete.setTextColor(Color.parseColor("#E53E3E"))
        btnDelete.setPadding(16, 8, 8, 8)
        btnDelete.setOnClickListener {
            showDeleteDialog(expense)
        }

        row.addView(leftLayout)
        row.addView(tvAmt)
        row.addView(btnDelete)

        wrapper.addView(row)

        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1)
        divider.setBackgroundColor(Color.parseColor("#F0F4F8"))
        wrapper.addView(divider)

        // Keep swipe to delete as well (both work)
        var startX = 0f
        var startY = 0f
        var isSwiping = false
        val SWIPE_THRESHOLD = 150f

        wrapper.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    isSwiping = false
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaX = kotlin.math.abs(event.rawX - startX)
                    val deltaY = kotlin.math.abs(event.rawY - startY)

                    if (deltaX > deltaY && deltaX > 20) {
                        isSwiping = true
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val deltaX = kotlin.math.abs(event.rawX - startX)
                    val deltaY = kotlin.math.abs(event.rawY - startY)

                    if (deltaX > SWIPE_THRESHOLD && deltaX > deltaY) {
                        showDeleteDialog(expense)
                    } else if (!isSwiping && deltaX < 10 && deltaY < 10) {
                        showEditDialog(expense)
                    }
                    isSwiping = false
                    true
                }
                else -> false
            }
        }

        expenseList.addView(wrapper)
    }

    private fun showEditDialog(expense: Expense) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_expense, null)
        val etEditAmount = dialogView.findViewById<EditText>(R.id.etEditAmount)
        val etEditDescription = dialogView.findViewById<EditText>(R.id.etEditDescription)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)

        // Set current values
        etEditAmount.setText(expense.amount.toString())
        etEditDescription.setText(expense.description)

        // Choose category list based on type
        val categoriesToShow = if (expense.type == "expense") expenseCategories else incomeCategories

        // Setup category spinner
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriesToShow)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter
        val categoryIndex = categoriesToShow.indexOf(expense.category)
        if (categoryIndex >= 0) spinnerCategory.setSelection(categoryIndex)

        // Setup type spinner
        val types = arrayOf("expense", "income")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter
        val typeIndex = types.indexOf(expense.type)
        if (typeIndex >= 0) spinnerType.setSelection(typeIndex)

        AlertDialog.Builder(this)
            .setTitle("Edit Transaction")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newAmount = etEditAmount.text.toString().toDoubleOrNull()
                val newDescription = etEditDescription.text.toString()
                val newCategory = spinnerCategory.selectedItem.toString()
                val newType = spinnerType.selectedItem.toString()

                if (newAmount == null || newAmount <= 0) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newDescription.isEmpty()) {
                    Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Update the expense
                val index = expenses.indexOfFirst { it.id == expense.id }
                if (index >= 0) {
                    val updatedExpense = expense.copy(
                        description = newDescription,
                        amount = newAmount,
                        category = newCategory,
                        type = newType
                    )
                    expenses[index] = updatedExpense
                    saveExpenses()
                    refreshList()
                    Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Delete '${expense.description}'?")
            .setPositiveButton("Delete") { _, _ ->
                expenses.removeAll { it.id == expense.id }
                saveExpenses()
                refreshList()
                Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCategoryIcon(category: String): String {
        return when (category) {
            // Expense categories
            "Food" -> "🍔"
            "Transport" -> "🚗"
            "Bills" -> "💡"
            "Shopping" -> "🛍️"
            "Health" -> "🏥"
            // Income categories
            "Sales" -> "💰"
            "Capital" -> "📈"
            else -> "📌"
        }
    }

    private fun saveExpenses() {
        val jsonArray = JSONArray()
        for (expense in expenses) {
            val obj = JSONObject()
            obj.put("id", expense.id)
            obj.put("description", expense.description)
            obj.put("amount", expense.amount)
            obj.put("date", expense.date)
            obj.put("category", expense.category)
            obj.put("type", expense.type)
            jsonArray.put(obj)
        }
        val prefs = getSharedPreferences("ExpenseData", Context.MODE_PRIVATE)
        prefs.edit().putString("expenses", jsonArray.toString()).apply()
    }

    private fun showSetBudgetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_budget, null)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)

        if (monthlyBudget > 0) {
            etBudget.setText(monthlyBudget.toInt().toString())
        }

        AlertDialog.Builder(this)
            .setTitle("Set Monthly Budget")
            .setMessage("Enter your budget amount for this month (TZS)")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val budgetText = etBudget.text.toString()
                if (budgetText.isNotEmpty()) {
                    val budget = budgetText.toDoubleOrNull()
                    if (budget != null && budget > 0) {
                        monthlyBudget = budget
                        saveBudget(monthlyBudget)
                        updateBudgetDisplay()

                        // Reset alert flags when setting new budget
                        val prefs = getSharedPreferences("ExpenseData", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("alert_90_shown", false).apply()
                        prefs.edit().putBoolean("alert_100_shown", false).apply()

                        Toast.makeText(this, "Budget set to TZS ${String.format("%.2f", budget)}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearBudgetDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Budget")
            .setMessage("Are you sure you want to clear your monthly budget?\n\nThis will reset your budget to TZS 0.00")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Clear") { _, _ ->
                monthlyBudget = 0.0
                saveBudget(monthlyBudget)

                // Reset alert flags
                val prefs = getSharedPreferences("ExpenseData", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("alert_90_shown", false).apply()
                prefs.edit().putBoolean("alert_100_shown", false).apply()

                updateBudgetDisplay()
                Toast.makeText(this, "Budget cleared!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCurrentMonthExpenses(): Double {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        return expenses.filter { expense ->
            expense.type == "expense" && try {
                val date = dateFormat.parse(expense.date)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
                } else false
            } catch (e: Exception) { false }
        }.sumOf { it.amount }
    }

    private fun updateBudgetDisplay() {
        if (monthlyBudget == 0.0) {
            tvBudgetAmount.text = "No budget set"
            tvBudgetSpent.text = "Tap 'Set' to create a budget"
            tvBudgetRemaining.text = ""
            budgetProgress.visibility = ProgressBar.GONE
            tvBudgetWarning.visibility = View.GONE
            return
        }

        budgetProgress.visibility = ProgressBar.VISIBLE
        val currentSpent = getCurrentMonthExpenses()
        val remaining = monthlyBudget - currentSpent
        val percentage = ((currentSpent / monthlyBudget) * 100).toInt()

        tvBudgetAmount.text = String.format("📊 Budget: TZS %.2f", monthlyBudget)
        tvBudgetSpent.text = String.format("💸 Spent: TZS %.2f", currentSpent)

        if (remaining >= 0) {
            tvBudgetRemaining.text = String.format("✅ Remaining: TZS %.2f", remaining)
            tvBudgetRemaining.setTextColor(Color.parseColor("#38A169"))
        } else {
            tvBudgetRemaining.text = String.format("⚠️ Over budget by: TZS %.2f", kotlin.math.abs(remaining))
            tvBudgetRemaining.setTextColor(Color.parseColor("#E53E3E"))
        }

        budgetProgress.progress = minOf(percentage, 100)

        val prefs = getSharedPreferences("ExpenseData", Context.MODE_PRIVATE)
        val alert90Shown = prefs.getBoolean("alert_90_shown", false)
        val alert100Shown = prefs.getBoolean("alert_100_shown", false)

        if (percentage >= 100 && !alert100Shown) {
            budgetProgress.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E53E3E"))
            showBudgetAlertDialog("Budget Exceeded!",
                "You have exceeded your monthly budget!\n\n" +
                        "Budget: TZS ${String.format("%.2f", monthlyBudget)}\n" +
                        "Spent: TZS ${String.format("%.2f", currentSpent)}\n" +
                        "Overspent: TZS ${String.format("%.2f", kotlin.math.abs(remaining))}\n\n" +
                        "Consider reducing your expenses.")
            prefs.edit().putBoolean("alert_100_shown", true).apply()
        }
        else if (percentage >= 90 && !alert90Shown && percentage < 100) {
            budgetProgress.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#ED8936"))
            showBudgetAlertDialog("Budget Warning!",
                "You have used $percentage% of your monthly budget!\n\n" +
                        "Budget: TZS ${String.format("%.2f", monthlyBudget)}\n" +
                        "Spent: TZS ${String.format("%.2f", currentSpent)}\n" +
                        "Remaining: TZS ${String.format("%.2f", remaining)}\n\n" +
                        "Be careful with your spending.")
            prefs.edit().putBoolean("alert_90_shown", true).apply()
        }
        else if (percentage < 90) {
            if (alert90Shown || alert100Shown) {
                prefs.edit().putBoolean("alert_90_shown", false).apply()
                prefs.edit().putBoolean("alert_100_shown", false).apply()
            }
            budgetProgress.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#38A169"))
            tvBudgetWarning.visibility = View.GONE
        }
    }

    private fun showBudgetAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("View Budget") { _, _ ->
                Toast.makeText(this, "Check your budget summary above", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun loadExpenses() {
        val prefs = getSharedPreferences("ExpenseData", Context.MODE_PRIVATE)
        val json = prefs.getString("expenses", "[]")
        val jsonArray = JSONArray(json)
        expenses.clear()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            expenses.add(Expense(
                id = obj.getLong("id"),
                description = obj.getString("description"),
                amount = obj.getDouble("amount"),
                date = obj.getString("date"),
                category = obj.optString("category", "Other"),
                type = obj.optString("type", "expense")
            ))
        }
    }
}