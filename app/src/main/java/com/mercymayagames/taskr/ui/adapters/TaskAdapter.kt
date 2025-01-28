package com.mercymayagames.taskr.ui.adapters

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mercymayagames.taskr.R
import com.mercymayagames.taskr.data.model.Task

/**
 * In the following adapter, we display a list of tasks with:
 * - A checkbox to mark them completed/incomplete
 * - An expandable area with the description
 * - An edit icon for modifying the task
 * - A delete icon if this is the completed screen (where tasks can be deleted)
 * - A stroke outline color set by the task's priority (High=Red, Normal=Yellow, Low=Green)
 */
class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val context: Context,
    // Callback for when a task is checked/unchecked
    private val onTaskChecked: (Task, Boolean) -> Unit,
    // Callback for when the user taps the edit icon
    private val onEditTaskRequested: (Task) -> Unit,
    // Callback for when user taps the delete icon
    private val onDeleteTaskRequested: (Task) -> Unit,
    // Flag to determine if we're in the "Completed" screen or not
    private val isCompletedScreen: Boolean
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

        // Show/hide the delete icon if this is the Completed screen
        holder.ivDeleteTask.visibility = if (isCompletedScreen) View.VISIBLE else View.GONE

        // When the user checks/unchecks the checkbox, call the callback
        holder.cbCompleted.setOnClickListener {
            onTaskChecked(task, holder.cbCompleted.isChecked)
        }

        // Clicking the item toggles expanded state
        holder.itemView.setOnClickListener {
            expandedPosition = if (isExpanded) RecyclerView.NO_POSITION else position
            notifyDataSetChanged()
        }

        // The edit icon callback
        holder.ivEditTask.setOnClickListener {
            onEditTaskRequested(task)
        }

        // The delete icon callback (only visible if isCompletedScreen = true)
        holder.ivDeleteTask.setOnClickListener {
            onDeleteTaskRequested(task)
        }

        // Dynamically set stroke color based on priority
        // We assume itemView has background=bg_task_item.xml, which is a ShapeDrawable
        val background = holder.itemView.background
        if (background is GradientDrawable) {
            val colorRes = when (task.priority) {
                "High" -> R.color.priorityHighOutline
                "Normal" -> R.color.priorityNormalOutline
                "Low" -> R.color.priorityLowOutline
                else -> android.R.color.transparent
            }
            val strokeColor = ContextCompat.getColor(context, colorRes)
            // 2dp stroke width, adjust to taste
            background.setStroke(2, strokeColor)
        }
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        val llExpandedArea: LinearLayout = itemView.findViewById(R.id.llExpandedArea)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val ivEditTask: ImageView = itemView.findViewById(R.id.ivEditTask)
        val ivDeleteTask: ImageView = itemView.findViewById(R.id.ivDeleteTask)
    }
}
