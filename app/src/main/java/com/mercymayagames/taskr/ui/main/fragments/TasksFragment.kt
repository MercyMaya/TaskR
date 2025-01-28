package com.mercymayagames.taskr.ui.main.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.mercymayagames.taskr.data.model.Task
import com.mercymayagames.taskr.databinding.FragmentTasksBinding
import com.mercymayagames.taskr.network.ApiClient
import com.mercymayagames.taskr.ui.adapters.TaskAdapter
import com.mercymayagames.taskr.util.SharedPrefManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * In the following fragment, we display only active tasks (is_completed=0, is_soft_deleted=0).
 * Tasks that are completed are removed from this list. We also have an edit icon
 * to update task details (title, description, priority).
 */
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /**
         * In the following lines, we inflate our fragment layout
         * using ViewBinding generated from fragment_tasks.xml
         */
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPrefManager = SharedPrefManager(requireContext())

        // Setup RecyclerView with our custom adapter
        taskAdapter = TaskAdapter(
            tasks = taskList,
            context = requireContext(),
            onTaskChecked = { task, isChecked ->
                // Mark the task as completed or active
                updateTaskCompletion(task, isChecked)
            },
            onEditTaskRequested = { task ->
                // Show dialog to edit title, description, and priority
                showEditTaskDialog(task)
            }
        )

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = taskAdapter

        // Floating Action Button for adding new tasks
        binding.fabAddTask.setOnClickListener {
            // Show a dialog or a new Activity to add a task
            showAddTaskDialog()
        }

        // Finally, fetch tasks from the server
        fetchTasksFromServer()
    }

    private fun fetchTasksFromServer() {
        /**
         * In the following lines, we call the "fetch" action on the server
         * which returns only tasks that are is_completed=0 & is_soft_deleted=0.
         */
        val userId = sharedPrefManager.getUserId()
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
                                localId = 0, // local DB usage if needed
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
                        taskAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(requireContext(), body.get("message").asString, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch tasks", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateTaskCompletion(task: Task, isCompleted: Boolean) {
        /**
         * In the following lines, we call "update" on the server
         * to set is_completed=1 if the user checks the box,
         * or 0 if they uncheck it.
         * Once completed, the task is automatically removed
         * from the "fetch" results on the next refresh.
         */
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
                        // Now that the server marks it completed,
                        // re-fetch the list (the completed task won't appear).
                        fetchTasksFromServer()
                    } else {
                        Toast.makeText(requireContext(), body.get("message").asString, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error updating task", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddTaskDialog() {
        /**
         * In the following lines, we open a dialog using 'dialog_add_task.xml'
         * to create a new task.
         */
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            com.mercymayagames.taskr.R.layout.dialog_add_task, null
        )
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val etTitle = dialogView.findViewById<EditText>(com.mercymayagames.taskr.R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(com.mercymayagames.taskr.R.id.etDescription)
        val spinnerPriority = dialogView.findViewById<Spinner>(com.mercymayagames.taskr.R.id.spinnerPriority)
        val btnSave = dialogView.findViewById<Button>(com.mercymayagames.taskr.R.id.btnSave)

        // Setup spinner with priority options
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
        /**
         * In the following lines, we call "add" to create a new task on the server
         * for the current user. Once added, we refresh the list.
         */
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.addTask("add", userId, title, description, priority)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        fetchTasksFromServer()
                    } else {
                        Toast.makeText(requireContext(), body.get("message").asString, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to add task", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEditTaskDialog(task: Task) {
        /**
         * In the following lines, we show the same 'dialog_add_task.xml'
         * but use it to edit an existing task (pre-fill data).
         */
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            com.mercymayagames.taskr.R.layout.dialog_add_task, null
        )
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Pre-fill data
        val etTitle = dialogView.findViewById<EditText>(com.mercymayagames.taskr.R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(com.mercymayagames.taskr.R.id.etDescription)
        val spinnerPriority = dialogView.findViewById<Spinner>(com.mercymayagames.taskr.R.id.spinnerPriority)
        val btnSave = dialogView.findViewById<Button>(com.mercymayagames.taskr.R.id.btnSave)

        etTitle.setText(task.taskTitle)
        etDescription.setText(task.taskDescription)

        // Priority spinner
        val priorities = listOf("High", "Normal", "Low")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter

        // Set spinner selection based on current priority
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
        /**
         * In the following lines, we call "update" with new title/description/priority.
         * If the task was previously completed, the isCompleted remains the same.
         * If it was active, we keep is_completed=0.
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
                        fetchTasksFromServer()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
