package com.rama.adiskide.adapters

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.rama.adiskide.R
import com.rama.adiskide.Task
import com.rama.adiskide.TaskType
import com.rama.adiskide.widgets.WdButton

class TaskAdapter(
    context: Context,
    private val tasks: MutableList<Task>,
    private val db: SQLiteDatabase
) : ArrayAdapter<Task>(context, 0, tasks) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_task, parent, false)

        val task = tasks[position]

        val label = view.findViewById<TextView>(R.id.task_label)
//        val icon = view.findViewById<ImageView>(R.id.task_icon)

        label.text = task.label
//        icon.setImageResource(if (task.type == TaskType.ROUTINE) R.drawable.icon_seedlings else R.drawable.icon_fire)

        // Complete task (remove from list only)
//        val completeTaskButton = view.findViewById<FrameLayout>(R.id.complete_task)
//        completeTaskButton.setOnClickListener {
//            tasks.remove(task)
//            notifyDataSetChanged()
//        }

        // Edit task (open edit modal)
//        val editTaskButton = view.findViewById<FrameLayout>(R.id.edit_task)
//        editTaskButton.setOnClickListener {
//            val dialogView = LayoutInflater.from(context)
//                .inflate(R.layout.dialog_task_edit, null)
//            val dialog = AlertDialog.Builder(context)
//                .setView(dialogView)
//                .create()
//
//            val labelInput = dialogView.findViewById<EditText>(R.id.label)
//            val typeGroup = dialogView.findViewById<RadioGroup>(R.id.type)
//            val difficultyGroup = dialogView.findViewById<RadioGroup>(R.id.difficulty)
//
//            val deleteButton = dialogView.findViewById<WdButton>(R.id.delete_button)
//            val editButton = dialogView.findViewById<WdButton>(R.id.add_button)
//            val cancelButton = dialogView.findViewById<WdButton>(R.id.cancel_button)
//
//            // Pre-fill current task data
//            labelInput.setText(task.label)
//            typeGroup.check(
//                if (task.type == TaskType.ROUTINE) typeGroup.getChildAt(1).id else typeGroup.getChildAt(
//                    0
//                ).id
//            )
//            difficultyGroup.check(
//                when (task.difficulty) {
//                    5 -> difficultyGroup.getChildAt(0).id
//                    10 -> difficultyGroup.getChildAt(1).id
//                    15 -> difficultyGroup.getChildAt(2).id
//                    20 -> difficultyGroup.getChildAt(3).id
//                    25 -> difficultyGroup.getChildAt(4).id
//                    else -> difficultyGroup.getChildAt(0).id
//                }
//            )
//
//            // Delete task
//            deleteButton.setOnClickListener {
//                db.delete("tasks", "id = ?", arrayOf(task.id.toString()))
//                tasks.remove(task)
//                notifyDataSetChanged()
//                dialog.dismiss()
//            }
//
//            // Save edited task
//            editButton.setOnClickListener {
//                val newLabel = labelInput.text.toString().trim()
//                val newType =
//                    if (typeGroup.checkedRadioButtonId == typeGroup.getChildAt(0).id) TaskType.BURNER else TaskType.ROUTINE
//                val newDifficulty = when (difficultyGroup.checkedRadioButtonId) {
//                    R.id.diff_5 -> 5
//                    R.id.diff_10 -> 10
//                    R.id.diff_15 -> 15
//                    R.id.diff_20 -> 20
//                    R.id.diff_25 -> 25
//                    else -> 5
//                }
//
//                // Update database
//                val values = ContentValues().apply {
//                    put("label", newLabel)
//                    put("type", newType.name)
//                    put("difficulty", newDifficulty)
//                }
//                db.update("tasks", values, "id = ?", arrayOf(task.id.toString()))
//
//                // Update local task object and UI
//                task.label = newLabel
//                task.type = newType
//                task.difficulty = newDifficulty
//                notifyDataSetChanged()
//
//                dialog.dismiss()
//            }
//
//            // Cancel
//            cancelButton.setOnClickListener {
//                dialog.dismiss()
//            }
//
//            dialog.show()
//        }

        return view
    }
}