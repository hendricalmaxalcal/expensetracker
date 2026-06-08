package com.example.expensetracker

import android.os.Bundle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

// Data model
data class Expense(
    val title: String,
    val amount: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ExpenseTrackerTheme {
                Scaffold { innerPadding ->
                    ExpenseTrackerHome(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseTrackerHome(modifier: Modifier = Modifier) {

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    // FIX: use mutableStateListOf for proper recomposition
    val expenses = remember { mutableStateListOf<Expense>() }

    // Total calculation
    val totalExpense = expenses.sumOf {
        it.amount.toDoubleOrNull() ?: 0.0
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {

        Text(
            text = "Expense Tracker",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Total Expenses: TZS $totalExpense",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Expense Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (title.isNotBlank() && amount.isNotBlank()) {

                    expenses.add(
                        Expense(
                            title = title,
                            amount = amount
                        )
                    )

                    title = ""
                    amount = ""
                }
            }
        ) {
            Text("Save Expense")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Saved Expenses",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(10.dp))

        expenses.forEachIndexed { index, expense ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "• ${expense.title} - TZS ${expense.amount}"
                )

                Button(
                    onClick = {
                        expenses.removeAt(index)
                    }
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseTrackerPreview() {
    ExpenseTrackerTheme {
        ExpenseTrackerHome()
    }
}