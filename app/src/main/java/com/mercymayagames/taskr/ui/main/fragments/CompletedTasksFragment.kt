package com.mercymayagames.taskr.ui.main.fragments

import android.R
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.mercymayagames.taskr.data.model.Task
import com.mercymayagames.taskr.databinding.FragmentCompletedTasksBinding
import com.mercymayagames.taskr.network.ApiClient
import com.mercymayagames.taskr.ui.adapters.TaskAdapter
import com.mercymayagames.taskr.util.SharedPrefManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * In the following lines, we display completed tasks.
 * We add a delete icon for final 'soft delete'
 * and an optional "edit" flow that includes a delete button in the dialog.
 */
class CompletedTasksFragment : Fragment() {

    private var _binding: FragmentCompletedTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var taskAdapter: TaskAdapter
    private val completedTaskList = mutableListOf<Task>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCompletedTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPrefManager = SharedPrefManager(requireContext())

        // Setup RecyclerView for completed tasks
        taskAdapter = TaskAdapter(
            tasks = completedTaskList,
            context = requireContext(),
            // Checking/unchecking in completed tasks might revert them to active
            onTaskChecked = { task, isChecked ->
                if (!isChecked) {
                    // If user unchecks in the completed screen, we can mark is_completed=0
                    // so it moves back to active tasks.
                    markTaskAsUncompleted(task)
                }
            },
            onEditTaskRequested = { task ->
                showEditTaskDialog(task)
            },
            onDeleteTaskRequested = { task ->
                showConfirmDeleteDialog(task)
            },
            isCompletedScreen = true  // so we show the delete icon
        )

        binding.rvCompletedTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCompletedTasks.adapter = taskAdapter

        /**
         * "Save" button that explicitly saves tasks that remain completed.
         * In a real scenario, you might do a final sync or confirm changes.
         * Right now, it's a placeholder that simply shows a Toast message.
         */
        binding.btnSaveCompleted.setOnClickListener {
            Toast.makeText(requireContext(), "Changes saved!", Toast.LENGTH_SHORT).show()
        }

        fetchCompletedTasks()
    }

    private fun fetchCompletedTasks() {
        /**
         * In the following lines, we call the "fetch_completed" action
         * which returns tasks where is_completed=1 and is_soft_deleted=0.
         */
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.fetchCompletedTasks("fetch_completed", userId)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        val tasksJson = body.getAsJsonArray("tasks")
                        completedTaskList.clear()
                        for (element in tasksJson) {
                            val obj = element.asJsonObject
                            val task = Task(
                                localId = 0,
                                id = obj.get("id").asInt,
                                userId = obj.get("user_id").asInt,
                                taskTitle = obj.get("task_title").asString,
                                taskDescription = obj.get("task_description").asString,
                                priority = obj.get("priority").asString,
                                isCompleted = (obj.get("is_completed").asInt == 1),
                                isSoftDeleted = (obj.get("is_soft_deleted").asInt == 1),
                                createdAt = obj.get("created_at").asString,
                                updatedAt = obj.get("updated_at").asString
                            )
                            completedTaskList.add(task)
                        }
                        taskAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(requireContext(), body.get("message").asString, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch completed tasks", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun markTaskAsUncompleted(task: Task) {
        // Set is_completed=0 so the item moves back to active tasks
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.updateTask(
            action = "update",
            userId = userId,
            taskId = task.id ?: 0,
            title = task.taskTitle,
            description = task.taskDescription,
            priority = task.priority,
            isCompleted = 0
        )
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        // Re-fetch the completed list to remove the uncompleted item
                        fetchCompletedTasks()
                    } else {
                        Toast.makeText(requireContext(), body.get("message").asString, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error marking task uncompleted", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEditTaskDialog(task: Task) {
        /**
         * We reuse the same dialog_add_task.xml (or rename to dialog_edit_task if you prefer).
         * We add a "Delete" button at the bottom only if the task is from the Completed screen.
         */
        val dialogView = layoutInflater.inflate(
            com.mercymayagames.taskr.R.layout.dialog_add_task, null
        )
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val etTitle = dialogView.findViewById<android.widget.EditText>(com.mercymayagames.taskr.R.id.etTitle)
        val etDescription = dialogView.findViewById<android.widget.EditText>(com.mercymayagames.taskr.R.id.etDescription)
        val spinnerPriority = dialogView.findViewById<android.widget.Spinner>(com.mercymayagames.taskr.R.id.spinnerPriority)
        val btnSave = dialogView.findViewById<android.widget.Button>(com.mercymayagames.taskr.R.id.btnSave)

        /**
         * We'll add a separate "Delete" button to the same layout if we want,
         * or you can place it inside the dialog via code.
         * For demonstration, let's create it on the fly below the save button.
         */
        val btnDelete = android.widget.Button(requireContext()).apply {
            text = "DELETE"
            setOnClickListener {
                dialog.dismiss()
                showConfirmDeleteDialog(task)
            }
        }

        // Add the delete button to the linear layout that holds spinnerPriority & btnSave
        val parentLayout = spinnerPriority.parent as ViewGroup
        parentLayout.addView(btnDelete) // or position it how you'd like

        // Pre-fill data
        etTitle.setText(task.taskTitle)
        etDescription.setText(task.taskDescription)

        val priorities = listOf("High", "Normal", "Low")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter

        val currentIndex = priorities.indexOf(task.priority)
        spinnerPriority.setSelection(if (currentIndex >= 0) currentIndex else 1)

        // Save updates
        btnSave.setOnClickListener {
            val newTitle = etTitle.text.toString().trim()
            val newDescription = etDescription.text.toString().trim()
            val newPriority = spinnerPriority.selectedItem.toString()

            updateTaskOnServer(task, newTitle, newDescription, newPriority)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateTaskOnServer(task: Task, title: String, description: String, priority: String) {
        /**
         * We call 'update' to change the task details.
         * If it's completed, we keep isCompleted=1;
         * if we wanted, we could revert it to isCompleted=0, but that's not typical for 'Edit.'
         */
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.updateTask(
            action = "update",
            userId = userId,
            taskId = task.id ?: 0,
            title = title,
            description = description,
            priority = priority,
            isCompleted = if (task.isCompleted) 1 else 0
        )
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        fetchCompletedTasks()
                    } else {
                        Toast.makeText(requireContext(), body.get("message").asString, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error editing task", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showConfirmDeleteDialog(task: Task) {
        /**
         * "Are you sure?" confirmation before we soft-delete the completed task.
         */
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete this completed task?")
            .setPositiveButton("Yes") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteTask(task: Task) {
        /**
         * Actually perform the soft_delete call.
         * This sets is_soft_deleted=1 so it won't appear anywhere (active or completed).
         */
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.softDeleteTask("soft_delete", userId, task.id ?: 0)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        fetchCompletedTasks()
                    } else {
                        Toast.makeText(requireContext(), body.get("message").asString, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to delete task", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
