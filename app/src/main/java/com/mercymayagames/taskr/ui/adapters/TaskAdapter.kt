package com.mercymayagames.taskr.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.mercymayagames.taskr.R
import com.mercymayagames.taskr.data.model.Task

/**
 * In the following adapter, we display a list of tasks with:
 * - A checkbox to mark them completed
 * - An expandable area for the description
 * - An edit icon that allows the user to update the task
 */
class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val context: Context,
    /**
     * In the following lines, we pass callbacks to handle
     * completed checkbox toggles and edit requests.
     */
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onEditTaskRequested: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Track which item is expanded
    private var expandedPosition: Int = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        // Populate basic fields
        holder.tvTitle.text = task.taskTitle
        holder.tvPriority.text = "Priority: ${task.priority}"
        holder.cbCompleted.isChecked = task.isCompleted

        // Expand/collapse logic
        val isExpanded = (position == expandedPosition)
        holder.llExpandedArea.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.tvDescription.text = task.taskDescription

        // When the user checks/unchecks the checkbox, call our callback
        holder.cbCompleted.setOnClickListener {
            onTaskChecked(task, holder.cbCompleted.isChecked)
        }

        // Clicking the item toggles expanded state
        holder.itemView.setOnClickListener {
            expandedPosition = if (isExpanded) RecyclerView.NO_POSITION else position
            notifyDataSetChanged()
        }

        // The edit icon is visible in the expanded area
        holder.ivEditTask.setOnClickListener {
            // Trigger the edit callback
            onEditTaskRequested(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)

        /**
         * In the following lines, we store references to the expanded area
         * (description & edit icon).
         */
        val llExpandedArea: LinearLayout = itemView.findViewById(R.id.llExpandedArea)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val ivEditTask: ImageView = itemView.findViewById(R.id.ivEditTask)
    }
}
