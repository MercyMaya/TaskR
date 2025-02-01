package com.mercymayagames.taskr.ui.adapters

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mercymayagames.taskr.R
import com.mercymayagames.taskr.data.model.Task

// Sealed class to differentiate headers and task items.
sealed class ListItem {
    data class Header(val title: String) : ListItem()
    data class TaskItem(val task: Task) : ListItem()
}

/**
 * SectionedTaskAdapter displays tasks grouped by priority.
 * It supports two view types: headers and task items.
 */
class SectionedTaskAdapter(
    private val context: Context,
    // Callbacks remain the same as your original adapter.
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onEditTaskRequested: (Task) -> Unit,
    private val onDeleteTaskRequested: (Task) -> Unit,
    // Flag to determine if we're in the "Completed" screen or not.
    private val isCompletedScreen: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // View type constants.
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_TASK = 1

    // Mixed list of headers and task items.
    private val items = mutableListOf<ListItem>()

    // To track expanded tasks. You can use a set keyed by task id or adapter position.
    private val expandedPositions = mutableSetOf<Int>()

    /**
     * Call this method to update the tasks list.
     * It groups tasks by priority ("High", "Normal", "Low") and inserts header items.
     */
    fun updateTasks(tasks: List<Task>) {
        items.clear()
        // Group tasks by priority.
        val grouped = tasks.groupBy { it.priority }
        val priorityOrder = listOf("High", "Normal", "Low")
        for (priority in priorityOrder) {
            val groupTasks = grouped[priority]
            if (!groupTasks.isNullOrEmpty()) {
                // Add a header for this priority.
                items.add(ListItem.Header(priority))
                // Add each task under this header.
                groupTasks.forEach { task ->
                    items.add(ListItem.TaskItem(task))
                }
            }
        }
        expandedPositions.clear()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Header -> VIEW_TYPE_HEADER
            is ListItem.TaskItem -> VIEW_TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(context).inflate(R.layout.priority_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)
            TaskViewHolder(view)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }
            is ListItem.TaskItem -> {
                (holder as TaskViewHolder).bind(item.task, position)
            }
        }
    }

    // ViewHolder for header items.
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHeader: TextView = itemView.findViewById(R.id.tvHeader)
        fun bind(header: ListItem.Header) {
            tvHeader.text = header.title
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val llExpandedArea: LinearLayout = itemView.findViewById(R.id.llExpandedArea)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val ivEditTask: ImageView = itemView.findViewById(R.id.ivEditTask)
        val ivDeleteTask: ImageView = itemView.findViewById(R.id.ivDeleteTask)

        fun bind(task: Task, adapterPosition: Int) {
            // Set the task title.
            tvTitle.text = task.taskTitle

            // If task description is empty or blank, show "No description".
            tvDescription.text = if (task.taskDescription.isBlank()) "No description" else task.taskDescription

            // Set the completed checkbox state.
            cbCompleted.isChecked = task.isCompleted

            // Expand/collapse logic.
            val isExpanded = expandedPositions.contains(adapterPosition)
            llExpandedArea.visibility = if (isExpanded) View.VISIBLE else View.GONE
            if (isExpanded) {
                tvTitle.maxLines = Integer.MAX_VALUE
                tvTitle.ellipsize = null
            } else {
                tvTitle.maxLines = 1
                tvTitle.ellipsize = TextUtils.TruncateAt.END
            }

            // When the user toggles the checkbox, call the callback.
            cbCompleted.setOnClickListener {
                onTaskChecked(task, cbCompleted.isChecked)
            }

            // Toggle expanded/collapsed state when the item view is clicked.
            itemView.setOnClickListener {
                if (expandedPositions.contains(adapterPosition)) {
                    expandedPositions.remove(adapterPosition)
                } else {
                    expandedPositions.add(adapterPosition)
                }
                notifyItemChanged(adapterPosition)
            }

            // Edit icon callback.
            ivEditTask.setOnClickListener {
                onEditTaskRequested(task)
            }

            // Delete icon: visible only if in a completed screen.
            ivDeleteTask.visibility = if (isCompletedScreen) View.VISIBLE else View.GONE
            ivDeleteTask.setOnClickListener {
                onDeleteTaskRequested(task)
            }

            // (Optional) Dynamically set the stroke color based on the task's original priority.
            // This code remains in case you still want to reflect priority via outline color.
            val background = itemView.background
            if (background is GradientDrawable) {
                val colorRes = when (task.priority) {
                    "High" -> R.color.priorityHighOutline
                    "Normal" -> R.color.priorityNormalOutline
                    "Low" -> R.color.priorityLowOutline
                    else -> android.R.color.transparent
                }
                val strokeColor = ContextCompat.getColor(context, colorRes)
                background.setStroke(2, strokeColor)
            }
        }
    }

}
