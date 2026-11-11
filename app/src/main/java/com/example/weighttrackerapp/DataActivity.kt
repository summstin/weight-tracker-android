package com.example.weighttrackerapp

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class DataActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var tableLayout: TableLayout
    private lateinit var addButton: Button
    private lateinit var goalButton: Button
    private lateinit var goalText: TextView

    private val SMS_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        db = DatabaseHelper(this)

        tableLayout = findViewById(R.id.tableLayout)
        addButton = findViewById(R.id.addtodaysweightbutton)
        goalButton = findViewById(R.id.goalweightbutton)
        goalText = findViewById(R.id.goalView)

        loadTable()
        showGoalIfExists()

        addButton.setOnClickListener { showAddDialog() }
        goalButton.setOnClickListener { showGoalDialog() }
    }

    // load table
    private fun loadTable() {

        if (tableLayout.childCount > 1) {
            tableLayout.removeViews(1, tableLayout.childCount - 1)
        }

        val cursor = db.getAllWeights()

        while (cursor.moveToNext()) {

            val id = cursor.getInt(0)
            val date = cursor.getString(1)
            val weight = cursor.getString(2)

            val row = TableRow(this)

            val dateView = TextView(this)
            dateView.text = date

            val weightView = TextView(this)
            weightView.text = weight

            val deleteButton = Button(this).apply { text = "Delete" }
            val editButton = Button(this).apply { text = "Edit" }

            deleteButton.setOnClickListener {
                db.deleteWeight(id)
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                loadTable()
            }

            editButton.setOnClickListener {
                showEditDialog(id, date, weight)
            }

            row.addView(dateView)
            row.addView(weightView)
            row.addView(editButton)
            row.addView(deleteButton)

            tableLayout.addView(row)
        }

        cursor.close()
    }

    // add weight
    private fun showAddDialog() {

        val dialog = LinearLayout(this)
        dialog.orientation = LinearLayout.VERTICAL

        val dateInput = EditText(this)
        dateInput.hint = "Enter Date"

        val weightInput = EditText(this)
        weightInput.hint = "Enter Weight"

        dialog.addView(dateInput)
        dialog.addView(weightInput)

        AlertDialog.Builder(this)
            .setTitle("Add Weight Entry")
            .setView(dialog)
            .setPositiveButton("Add") { _, _ ->

                val date = dateInput.text.toString()
                val weight = weightInput.text.toString()

                if (date.isNotEmpty() && weight.isNotEmpty()) {

                    db.insertWeight(date, weight)
                    loadTable()

                    checkGoal(weight)

                    Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // edit weight
    private fun showEditDialog(id: Int, currentDate: String, currentWeight: String) {

        val dialog = LinearLayout(this)
        dialog.orientation = LinearLayout.VERTICAL

        val dateInput = EditText(this)
        dateInput.setText(currentDate)

        val weightInput = EditText(this)
        weightInput.setText(currentWeight)

        dialog.addView(dateInput)
        dialog.addView(weightInput)

        AlertDialog.Builder(this)
            .setTitle("Edit Weight")
            .setView(dialog)
            .setPositiveButton("Update") { _, _ ->

                val newDate = dateInput.text.toString()
                val newWeight = weightInput.text.toString()

                if (newDate.isNotEmpty() && newWeight.isNotEmpty()) {

                    db.updateWeight(id, newDate, newWeight)
                    loadTable()

                    checkGoal(newWeight)

                    Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // goal weight
    private fun showGoalDialog() {

        val input = EditText(this)
        input.hint = "Enter goal weight"

        AlertDialog.Builder(this)
            .setTitle("Set Goal Weight")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->

                val goal = input.text.toString()

                if (goal.isNotEmpty()) {

                    val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("goal_weight", goal).apply()

                    goalText.text = "Goal Weight: $goal"

                    Toast.makeText(this, "Goal Saved", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGoalIfExists() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val goal = prefs.getString("goal_weight", null)

        if (goal != null) {
            goalText.text = "Goal Weight: $goal"
        }
    }

    // goal check and sms
    private fun checkGoal(currentWeight: String) {

        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val goal = prefs.getString("goal_weight", null)

        if (goal != null && currentWeight == goal) {
            sendSms()
        }
    }

    private fun sendSms() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_CODE
            )
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(
                "YOUR_PHONE_NUMBER",
                null,
                "Goal weight reached!",
                null,
                null
            )

            Toast.makeText(this, "SMS Sent!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "SMS failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}