package com.mercymayagames.taskr.ui.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import android.app.AlertDialog

/**
 * In the following fragment, we display tasks that are marked as completed (is_completed=1).
 * We allow the user to optionally:
 *   - "Uncheck" tasks (mark them uncompleted) to move them back to the active tasks screen.
 *   - Edit a completed task's title, description, or priority if desired.
 * There's also a "Save" button for demonstration, which you can customize or remove.
 */
class CompletedTasksFragment : Fragment() {

    /**
     * In the following lines, we bind to our fragment_completed_tasks.xml layout
     * using ViewBinding.
     */
    private var _binding: FragmentCompletedTasksBinding? = null
    private val binding get() = _binding!!

    /**
     * We maintain a mutable list of completed tasks and a TaskAdapter to display them.
     */
    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var taskAdapter: TaskAdapter
    private val completedTaskList = mutableListOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate using ViewBinding
        _binding = FragmentCompletedTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPrefManager = SharedPrefManager(requireContext())

        /**
         * In the following lines, we set up a TaskAdapter with two callbacks:
         *  - onTaskChecked: if the user unchecks (or re-checks) a completed task
         *  - onEditTaskRequested: if the user wants to edit the task from the completed screen
         */
        taskAdapter = TaskAdapter(
            tasks = completedTaskList,
            context = requireContext(),
            onTaskChecked = { task, isChecked ->
                // If the user "unchecks" a completed task, we can revert is_completed=0
                // so it returns to the active tasks screen.
                // isChecked == false means user unchecked it in the Completed list
                if (!isChecked) {
                    markTaskAsUncompleted(task)
                }
                // If isChecked is true, user effectively re-checked a completed task,
                // which doesn't usually change anything, so we can ignore it or handle no-op.
            },
            onEditTaskRequested = { task ->
                // If we want to let the user edit a completed task’s title/desc/priority
                // we can show the same edit dialog used in TasksFragment.
                showEditTaskDialog(task)
            }
        )

        binding.rvCompletedTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCompletedTasks.adapter = taskAdapter

        /**
         * "Save" button that explicitly saves tasks that remain completed.
         * In a real scenario, you might do a final sync operation here.
         */
        binding.btnSaveCompleted.setOnClickListener {
            Toast.makeText(requireContext(), "Changes saved!", Toast.LENGTH_SHORT).show()
            // e.g., re-fetch or confirm changes. This is a stub demonstration.
        }

        // Fetch the list of completed tasks from the server
        fetchCompletedTasks()
    }

    /**
     * In the following function, we call the "fetch_completed" action on the server,
     * which returns tasks with is_completed=1 and is_soft_deleted=0.
     */
    private fun fetchCompletedTasks() {
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.fetchCompletedTasks("fetch_completed", userId)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        completedTaskList.clear()
                        val tasksJson = body.getAsJsonArray("tasks")
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

    /**
     * In the following function, we revert a completed task (is_completed=1) to uncompleted (is_completed=0).
     * This will cause it to appear again in the active tasks screen, and be removed from completed tasks.
     */
    private fun markTaskAsUncompleted(task: Task) {
        val userId = sharedPrefManager.getUserId()
        val call = ApiClient.apiInterface.updateTask(
            action = "update",
            userId = userId,
            taskId = task.id ?: 0,
            title = task.taskTitle,
            description = task.taskDescription,
            priority = task.priority,
            isCompleted = 0 // revert to uncompleted
        )
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        // Re-fetch completed tasks. The "uncompleted" one won't appear anymore.
                        fetchCompletedTasks()
                    } else {
                        Toast.makeText(requireContext(), body.get("message").asString, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error reverting task to uncompleted", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * In the following lines, we allow editing of a completed task’s title, description, or priority.
     * We reuse the same dialog_add_task.xml as the "Add" dialog, but pre-fill with the task data.
     */
    private fun showEditTaskDialog(task: Task) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            com.mercymayagames.taskr.R.layout.dialog_add_task,
            null
        )
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val etTitle = dialogView.findViewById<EditText>(com.mercymayagames.taskr.R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(com.mercymayagames.taskr.R.id.etDescription)
        val spinnerPriority = dialogView.findViewById<Spinner>(com.mercymayagames.taskr.R.id.spinnerPriority)
        val btnSave = dialogView.findViewById<Button>(com.mercymayagames.taskr.R.id.btnSave)

        // Pre-fill existing data
        etTitle.setText(task.taskTitle)
        etDescription.setText(task.taskDescription)

        // Priority spinner setup
        val priorities = listOf("High", "Normal", "Low")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter

        // Set the current priority selection
        val currentIndex = priorities.indexOf(task.priority)
        spinnerPriority.setSelection(if (currentIndex >= 0) currentIndex else 1)

        // Update the server when the user clicks "Save"
        btnSave.setOnClickListener {
            val newTitle = etTitle.text.toString().trim()
            val newDescription = etDescription.text.toString().trim()
            val newPriority = spinnerPriority.selectedItem.toString()

            updateTaskOnServer(task, newTitle, newDescription, newPriority)
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * In the following function, we call "update" to modify the title/description/priority of
     * an existing completed task. We keep isCompleted as it is (1) unless you want to revert it.
     */
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
                        // Re-fetch completed tasks to see the updated info
                        fetchCompletedTasks()
                    } else {
                        Toast.makeText(requireContext(), body.get("message").asString, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error editing completed task", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * In the following lines, we set _binding to null on destroy to avoid memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
