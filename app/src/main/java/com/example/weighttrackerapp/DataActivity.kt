package com.example.weighttrackerapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weighttrackerapp.data.WeightEntry
import com.example.weighttrackerapp.domain.InputValidator
import com.example.weighttrackerapp.domain.Outcome
import com.example.weighttrackerapp.domain.formatWeight
import com.example.weighttrackerapp.ui.WeightAdapter
import com.example.weighttrackerapp.ui.WeightViewModel
import com.example.weighttrackerapp.ui.WeightViewModelFactory
import kotlinx.coroutines.launch
import java.time.LocalDate

// Main screen. It only displays what WeightViewModel exposes and forwards button taps to it
class DataActivity : AppCompatActivity() {

    private var userId = AppConstants.NO_USER

    private val viewModel: WeightViewModel by viewModels {
        WeightViewModelFactory(userId, ServiceLocator.weightService(this))
    }

    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) toast("Without SMS permission, goal texts can't be sent")
        }

    private lateinit var weightAdapter: WeightAdapter
    private lateinit var goalText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        userId = intent.getLongExtra(AppConstants.EXTRA_USER_ID, AppConstants.NO_USER)
        if (userId == AppConstants.NO_USER) {
            // No logged-in user: this screen must not show anyone's data
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        goalText = findViewById(R.id.goalView)

        weightAdapter = WeightAdapter(
            onEdit = { entry -> showEditDialog(entry) },
            onDelete = { entry -> viewModel.deleteEntry(entry.id) }
        )
        findViewById<RecyclerView>(R.id.weightList).apply {
            layoutManager = LinearLayoutManager(this@DataActivity)
            this.adapter = weightAdapter
        }

        findViewById<Button>(R.id.addtodaysweightbutton).setOnClickListener { showAddDialog() }
        findViewById<Button>(R.id.goalweightbutton).setOnClickListener { showGoalDialog() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch { viewModel.messages.collect { toast(it) } }
            }
        }
    }

    private fun render(state: WeightViewModel.UiState) {
        weightAdapter.submitList(state.entries)
        goalText.text = state.goal?.let { "GOAL WEIGHT: ${formatWeight(it.weight)}" } ?: "GOAL WEIGHT: NOT SET"
    }

    private fun showAddDialog() {
        showEntryDialog(
            title = "Add Weight Entry",
            positiveLabel = "Add",
            initialDate = LocalDate.now().toString(),
            initialWeight = ""
        ) { date, weight -> viewModel.addEntry(date, weight) }
    }

    private fun showEditDialog(entry: WeightEntry) {
        showEntryDialog(
            title = "Edit Weight",
            positiveLabel = "Update",
            initialDate = entry.date,
            initialWeight = formatWeight(entry.weight)
        ) { date, weight -> viewModel.updateEntry(entry.id, date, weight) }
    }

    // Shared add/edit dialog, it stays open and shows an error beside the field until the input is valid
    private fun showEntryDialog(
        title: String,
        positiveLabel: String,
        initialDate: String,
        initialWeight: String,
        onSubmit: (date: String, weight: String) -> Unit
    ) {
        val dateInput = EditText(this).apply {
            hint = "Date (YYYY-MM-DD)"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(initialDate)
        }
        val weightInput = EditText(this).apply {
            hint = "Weight (lbs)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(initialWeight)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogContent(dateInput, weightInput))
            .setPositiveButton(positiveLabel, null)   // click handler set below so it doesn't auto-dismiss
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val date = dateInput.text.toString()
                val weight = weightInput.text.toString()

                val dateError = (InputValidator.parseDate(date) as? Outcome.Failure)?.message
                val weightError = (InputValidator.parseWeight(weight) as? Outcome.Failure)?.message
                dateInput.error = dateError
                weightInput.error = weightError

                if (dateError == null && weightError == null) {
                    onSubmit(date, weight)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showGoalDialog() {
        val current = viewModel.state.value.goal

        val goalInput = EditText(this).apply {
            hint = "Goal weight (lbs)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            current?.let { setText(formatWeight(it.weight)) }
        }
        val phoneInput = EditText(this).apply {
            hint = "Phone number for goal text (optional)"
            inputType = InputType.TYPE_CLASS_PHONE
            current?.phoneNumber?.let { setText(it) }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Goal Weight")
            .setView(dialogContent(goalInput, phoneInput))
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val goal = goalInput.text.toString()
                val phone = phoneInput.text.toString()

                val goalError = (InputValidator.parseWeight(goal) as? Outcome.Failure)?.message
                val phoneError = (InputValidator.parsePhone(phone) as? Outcome.Failure)?.message
                goalInput.error = goalError
                phoneInput.error = phoneError

                if (goalError == null && phoneError == null) {
                    viewModel.saveGoal(goal, phone)
                    // Only ask for SMS permission when the user actually wants goal texts
                    if (phone.isNotBlank() && !hasSmsPermission()) {
                        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                    }
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun hasSmsPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    private fun dialogContent(vararg inputs: EditText): LinearLayout {
        val padding = (DIALOG_PADDING_DP * resources.displayMetrics.density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, 0)
            inputs.forEach { addView(it) }
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private companion object {
        const val DIALOG_PADDING_DP = 24
    }
}