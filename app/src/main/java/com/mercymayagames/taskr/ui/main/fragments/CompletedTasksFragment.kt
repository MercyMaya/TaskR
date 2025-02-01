package com.mercymayagames.taskr.ui.main.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.mercymayagames.taskr.data.model.Task
import com.mercymayagames.taskr.databinding.FragmentCompletedTasksBinding
import com.mercymayagames.taskr.network.ApiClient
import com.mercymayagames.taskr.ui.adapters.SectionedTaskAdapter
import com.mercymayagames.taskr.util.SharedPrefManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * In this fragment, we display completed tasks using the SectionedTaskAdapter.
 * Tasks are grouped by priority (High, Normal, Low) with headers.
 * Any update (mark uncompleted, edit, or delete) will rebuild the sections.
 */
class CompletedTasksFragment : Fragment() {

    private var _binding: FragmentCompletedTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPrefManager: SharedPrefManager
    // Use the SectionedTaskAdapter instead of the old TaskAdapter.
    private lateinit var sectionedTaskAdapter: SectionedTaskAdapter
    private val completedTaskList = mutableListOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompletedTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPrefManager = SharedPrefManager(requireContext())

        // Initialize the SectionedTaskAdapter with callbacks.
        sectionedTaskAdapter = SectionedTaskAdapter(
            context = requireContext(),
            onTaskChecked = { task, isChecked ->
                // In the completed screen, unchecking should mark the task as uncompleted.
                if (!isChecked) {
                    markTaskAsUncompleted(task)
                }
            },
            onEditTaskRequested = { task ->
                showEditTaskDialog(task)
            },
            onDeleteTaskRequested = { task ->
                showConfirmDeleteDialog(task)
            },
            isCompletedScreen = true
        )

        binding.rvCompletedTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCompletedTasks.adapter = sectionedTaskAdapter

        // "Save" button (demonstration).
        binding.btnSaveCompleted.setOnClickListener {
            Toast.makeText(requireContext(), "Changes saved!", Toast.LENGTH_SHORT).show()
        }

        // Initial load of completed tasks.
        fetchCompletedTasks()
    }

    private fun fetchCompletedTasks() {
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.fetchCompletedTasks("fetch_completed", userId)
        call.enqueue(object : Callback<JsonObject> {
            @SuppressLint("NotifyDataSetChanged")
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
                        // Update the sectioned adapter with the new list.
                        sectionedTaskAdapter.updateTasks(completedTaskList)
                    } else {
                        Toast.makeText(
                            requireContext(), body.get("message").asString, Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(), "Failed to fetch completed tasks", Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun markTaskAsUncompleted(task: Task) {
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.updateTask(
            "update",
            userId,
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
                        // Remove the task from the local list and update the adapter.
                        completedTaskList.remove(task)
                        sectionedTaskAdapter.updateTasks(completedTaskList)
                    } else {
                        Toast.makeText(
                            requireContext(), body.get("message").asString, Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(), "Error marking task uncompleted", Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogView = layoutInflater.inflate(
            com.mercymayagames.taskr.R.layout.dialog_add_task, null
        )
        val dialog = android.app.AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val etTitle = dialogView.findViewById<android.widget.EditText>(com.mercymayagames.taskr.R.id.etTitle)
        val etDescription = dialogView.findViewById<android.widget.EditText>(com.mercymayagames.taskr.R.id.etDescription)
        val spinnerPriority = dialogView.findViewById<android.widget.Spinner>(com.mercymayagames.taskr.R.id.spinnerPriority)
        val btnSave = dialogView.findViewById<android.widget.Button>(com.mercymayagames.taskr.R.id.btnSave)

        // Optional Delete button in the dialog.
        val btnDelete = android.widget.Button(requireContext()).apply {
            text = getString(com.mercymayagames.taskr.R.string.dialog_delete_button)
            setOnClickListener {
                dialog.dismiss()
                showConfirmDeleteDialog(task)
            }
        }
        (spinnerPriority.parent as ViewGroup).addView(btnDelete)

        // Pre-fill fields.
        etTitle.setText(task.taskTitle)
        etDescription.setText(task.taskDescription)
        val priorities = listOf("High", "Normal", "Low")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter
        val currentIndex = priorities.indexOf(task.priority)
        spinnerPriority.setSelection(if (currentIndex >= 0) currentIndex else 1)

        btnSave.setOnClickListener {
            val newTitle = etTitle.text.toString().trim()
            val newDescription = etDescription.text.toString().trim()
            val newPriority = spinnerPriority.selectedItem.toString()
            updateTaskOnServer(task, newTitle, newDescription, newPriority)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateTaskOnServer(
        task: Task, title: String, description: String, priority: String
    ) {
        val userId = sharedPrefManager.getUserId()
        val isCompleted = if (task.isCompleted) 1 else 0
        val call = ApiClient.apiInterface.updateTask(
            "update",
            userId,
            taskId = task.id ?: 0,
            title = title,
            description = description,
            priority = priority,
            isCompleted = isCompleted
        )
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        // Update the task in our local list.
                        val index = completedTaskList.indexOf(task)
                        if (index != -1) {
                            val updatedTask = task.copy(
                                taskTitle = title,
                                taskDescription = description,
                                priority = priority
                            )
                            completedTaskList[index] = updatedTask
                            sectionedTaskAdapter.updateTasks(completedTaskList)
                        }
                    } else {
                        Toast.makeText(
                            requireContext(), body.get("message").asString, Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error editing task", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun showConfirmDeleteDialog(task: Task) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(com.mercymayagames.taskr.R.string.dialog_delete_title))
            .setMessage(getString(com.mercymayagames.taskr.R.string.dialog_delete_message))
            .setPositiveButton(getString(com.mercymayagames.taskr.R.string.yes)) { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton(getString(com.mercymayagames.taskr.R.string.no), null)
            .show()
    }

    private fun deleteTask(task: Task) {
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.softDeleteTask("soft_delete", userId, task.id ?: 0)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        // Remove the task from our local list and update the adapter.
                        completedTaskList.remove(task)
                        sectionedTaskAdapter.updateTasks(completedTaskList)
                    } else {
                        Toast.makeText(
                            requireContext(), body.get("message").asString, Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to delete task", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
