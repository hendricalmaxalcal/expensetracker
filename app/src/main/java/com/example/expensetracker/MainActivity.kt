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

    private val categories = arrayOf("Food", "Transport", "Bills", "Shopping", "Health", "Other")

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

        loadExpenses()
        refreshList()

        btnAdd.setOnClickListener {
            showAddDialog(etAmount, etDescription, "expense")
        }

        btnIncome.setOnClickListener {
            showAddDialog(etAmount, etDescription, "income")
        }
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

        AlertDialog.Builder(this)
            .setTitle("Select Category")
            .setItems(categories) { _, which ->
                val category = categories[which]
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
                tvCat.text = "${categoryIcon(category)} $category"
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
        tvDesc.text = "${categoryIcon(expense.category)} ${expense.description}"
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

        row.addView(leftLayout)
        row.addView(tvAmt)

        wrapper.addView(row)

        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1)
        divider.setBackgroundColor(Color.parseColor("#F0F4F8"))
        wrapper.addView(divider)

        // Handle both tap (edit) and swipe (delete)
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

                    // If horizontal movement is greater than vertical, it's a swipe
                    if (deltaX > deltaY && deltaX > 20) {
                        isSwiping = true
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val deltaX = kotlin.math.abs(event.rawX - startX)
                    val deltaY = kotlin.math.abs(event.rawY - startY)

                    // Check if it was a swipe (horizontal movement)
                    if (deltaX > SWIPE_THRESHOLD && deltaX > deltaY) {
                        // Swipe detected - delete
                        showDeleteDialog(expense)
                    } else if (!isSwiping && deltaX < 10 && deltaY < 10) {
                        // Tap detected (no significant movement) - edit
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

        // Setup category spinner
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter
        val categoryIndex = categories.indexOf(expense.category)
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
                    // Keep the original date
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

    private fun categoryIcon(category: String): String {
        return when (category) {
            "Food" -> "🍔"
            "Transport" -> "🚗"
            "Bills" -> "💡"
            "Shopping" -> "🛍️"
            "Health" -> "🏥"
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