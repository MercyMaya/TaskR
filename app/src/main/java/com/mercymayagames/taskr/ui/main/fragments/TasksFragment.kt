package com.mercymayagames.taskr.ui.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.mercymayagames.taskr.R
import com.mercymayagames.taskr.data.model.Task
import com.mercymayagames.taskr.databinding.FragmentTasksBinding
import com.mercymayagames.taskr.network.ApiClient
import com.mercymayagames.taskr.ui.adapters.SectionedTaskAdapter
import com.mercymayagames.taskr.util.SharedPrefManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * TasksFragment displays tasks grouped by priority.
 * It uses SectionedTaskAdapter to show headers (High, Normal, Low) and tasks.
 */
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPrefManager: SharedPrefManager
    // Instead of the old TaskAdapter, we now use SectionedTaskAdapter.
    private lateinit var sectionedTaskAdapter: SectionedTaskAdapter
    // A mutable list to hold fetched tasks.
    private val taskList = mutableListOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPrefManager = SharedPrefManager(requireContext())

        // Initialize the SectionedTaskAdapter with the required callbacks.
        sectionedTaskAdapter = SectionedTaskAdapter(
            context = requireContext(),
            onTaskChecked = { task, isChecked ->
                updateTaskCompletion(task, isChecked)
            },
            onEditTaskRequested = { task ->
                showEditTaskDialog(task)
            },
            onDeleteTaskRequested = { task ->
                // For active tasks, deletion might not be enabled.
                // (If using a completed screen, implement deletion accordingly.)
            },
            isCompletedScreen = false
        )

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = sectionedTaskAdapter

        // Floating Action Button for adding new tasks.
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        // Fetch tasks from the server.
        fetchTasksFromServer()
    }

    private fun fetchTasksFromServer() {
        val userId = sharedPrefManager.getUserId()
        // Call the API to fetch active tasks.
        val call = ApiClient.apiInterface.fetchTasks("fetch", userId)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        val tasksJson = body.getAsJsonArray("tasks")
                        taskList.clear()
                        for (element in tasksJson) {
                            val obj = element.asJsonObject
                            val task = Task(
                                localId = 0, // For local DB usage if needed.
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
                            taskList.add(task)
                        }
                        // Update the sectioned adapter with the new list.
                        sectionedTaskAdapter.updateTasks(taskList)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            body.get("message").asString,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch tasks", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateTaskCompletion(task: Task, isCompleted: Boolean) {
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.updateTask(
            action = "update",
            userId = userId,
            taskId = task.id ?: 0,
            title = task.taskTitle,
            description = task.taskDescription,
            priority = task.priority,
            isCompleted = if (isCompleted) 1 else 0
        )
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        // Re-fetch tasks so the updated state is shown.
                        fetchTasksFromServer()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            body.get("message").asString,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error updating task", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Setup spinner with priority options.
        val priorities = listOf("High", "Normal", "Low")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val priority = spinnerPriority.selectedItem.toString()
            addTaskToServer(title, description, priority)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun addTaskToServer(title: String, description: String, priority: String) {
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.addTask("add", userId, title, description, priority)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        fetchTasksFromServer()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            body.get("message").asString,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to add task", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Pre-fill data for editing.
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

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

    private fun updateTaskOnServer(task: Task, title: String, description: String, priority: String) {
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
                        fetchTasksFromServer()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            body.get("message").asString,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error editing task", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
