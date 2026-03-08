package com.rama.adiskide.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.rama.adiskide.R
import com.rama.adiskide.Task
import com.rama.adiskide.TaskType

class TaskAdapter(
    context: Context,
    private val tasks: List<Task>
) : ArrayAdapter<Task>(context, 0, tasks) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_task, parent, false)

        val task = tasks[position]

        val label = view.findViewById<TextView>(R.id.task_label)
        label.text = task.label

        val icon = view.findViewById<ImageView>(R.id.task_icon)

        if (task.type == TaskType.ROUTINE) {
            icon.setImageResource(R.drawable.icon_seedlings)
        } else {
            icon.setImageResource(R.drawable.icon_fire)
        }

        return view
    }
}