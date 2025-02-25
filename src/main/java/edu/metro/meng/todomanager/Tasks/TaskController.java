package edu.metro.meng.todomanager.Tasks;

import edu.metro.meng.todomanager.Users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
public class TaskController {
    @Autowired
    private TaskRepo taskRepo;

    @GetMapping(value = {"/", "/tasks"})
    public String tasks(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            System.out.println("No user found in session");
            return "redirect:/login";
        }
        // Fetch tasks for the user
        List<Task> userTasks = taskRepo.findByUserId(user.getId());

        LocalDate today = LocalDate.now();
        userTasks.forEach(task -> {
            if (task.getDueDate() != null && task.getDueDate().isBefore(today) && task.getStatus() == TaskStatus.PENDING) {
                task.setStatus(TaskStatus.OVERDUE);
                taskRepo.save(task);
            }
        });

        model.addAttribute("task", new Task());
        model.addAttribute("tasks", userTasks);
        return "tasks";
    }

    @PostMapping("/create")
    public String createTask(@ModelAttribute("task") Task task, @AuthenticationPrincipal User user) {
        task.setUserId(user.getId());
        taskRepo.save(task);
        System.out.println("Task saved: " + task);
        return "redirect:/";
    }
    @GetMapping("/delete/{id}")
    public String deleteTask(@PathVariable Long id, Model model) {
        if (taskRepo.existsById(id)) {
            taskRepo.deleteById(id);
            return "redirect:/";
        } else {
            model.addAttribute("error", "Task not found.");
            return "redirect:/"; // Redirect back to the tasks list (or show an error page).
        }
    }
    @GetMapping("/update/{id}")
    public String updateTask(@PathVariable Long id, Model model) {
        var taskOptional = taskRepo.findById(id);
        if (taskOptional.isPresent()) {
            model.addAttribute("task", taskOptional.get());
            return "update";
        } else {
            model.addAttribute("error", "Task not found.");
            return "redirect:/"; // Redirect back to the tasks list (or another appropriate page).
        }
    }
    @PostMapping("/update")
    public String update(@ModelAttribute("task") Task task, @AuthenticationPrincipal User user){
        Task existingTask = taskRepo.findById(task.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid task ID: " + task.getId()));
        // Preserve the user ID and update other fields
        existingTask.setUserId(existingTask.getUserId());
        existingTask.setTitle(task.getTitle());
        existingTask.setDescription(task.getDescription());
        existingTask.setDueDate(task.getDueDate());
        existingTask.setStatus(task.getStatus());
        //Save updated task
        taskRepo.save(existingTask);
        System.out.println("Updated task: " + existingTask);
        return "redirect:/";
    }
    /*@PostMapping("/populate-tasks")
    public String populateTasks(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new IllegalStateException("User must be logged in to populate tasks.");
        }

        List<Task> tasks = List.of(
                new Task(null, "Buy Groceries", "Milk, Eggs, Bread", LocalDate.now().plusDays(1), user.getId(),TaskStatus.PENDING),
                new Task(null, "Watch Movie", "Watch Inception on Netflix", LocalDate.now().plusDays(2), user.getId(), TaskStatus.PENDING),
                new Task(null, "Workout", "Go to the gym for 1 hour", LocalDate.now().plusDays(3), user.getId(),TaskStatus.PENDING),
                new Task(null, "Prepare Presentation", "Slides for Monday's meeting", LocalDate.now().plusDays(4), user.getId(), TaskStatus.PENDING),
                new Task(null, "Finish Homework", "Complete math assignment", LocalDate.now().plusDays(5), user.getId(), TaskStatus.PENDING),
                new Task(null, "Read a Book", "Read 50 pages of 'Atomic Habits'", LocalDate.now().plusDays(6), user.getId(), TaskStatus.PENDING),
                new Task(null, "Call Mom", "Weekly check-in call", LocalDate.now().plusDays(7), user.getId(), TaskStatus.PENDING),
                new Task(null, "Clean Room", "Organize and dust the shelves", LocalDate.now().plusDays(8), user.getId(), TaskStatus.PENDING),
                new Task(null, "Plan Vacation", "Research destinations for summer", LocalDate.now().plusDays(9), user.getId(), TaskStatus.PENDING),
                new Task(null, "Complete Project", "Submit the ToDo Manager project", LocalDate.now().plusDays(10), user.getId(), TaskStatus.PENDING)
        );

        taskRepo.saveAll(tasks);

        System.out.println("Populated 10 tasks for user: " + user.getUsername());
        return "redirect:/tasks";
    }*/
    @PostMapping("/tasks/{id}/complete")
    public String markTaskAsCompleted(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid task ID: " + id));

        if (!task.getUserId().equals(user.getId())) {
            throw new SecurityException("Unauthorized action");
        }

        task.setStatus(TaskStatus.COMPLETED);
        taskRepo.save(task);

        System.out.println("Task marked as completed: " + task);
        return "redirect:/tasks";
    }
    @PostMapping("/tasks/{id}/uncomplete")
    public String unmarkTaskAsCompleted(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid task ID: " + id));

        if (!task.getUserId().equals(user.getId())) {
            throw new SecurityException("Unauthorized action");
        }

        task.setStatus(TaskStatus.PENDING);
        taskRepo.save(task);

        System.out.println("Task unmarked as completed: " + task);
        return "redirect:/tasks";
    }
    @GetMapping("/tasks/filter")
    public String filterTasks(
            @RequestParam(value = "status", required = false, defaultValue = "PENDING") TaskStatus status,
            Model model,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/login";
        }
        // Fetch tasks based on the current filter
        List<Task> filteredTasks = taskRepo.findByUserIdAndStatus(user.getId(), status);
        // Add the filtered tasks and current filter to the model
        model.addAttribute("tasks", filteredTasks);
        model.addAttribute("task", new Task());
        model.addAttribute("filter", status); // Pass the current filter
        return "tasks";
    }
    @PostMapping("/tasks/{id}/toggle")
    public String toggleTaskCompletion(@PathVariable Long id, @AuthenticationPrincipal User user) {
        // Fetch the task
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid task ID: " + id));

        // Ensure the task belongs to the logged-in user
        if (!task.getUserId().equals(user.getId())) {
            throw new SecurityException("Unauthorized action");
        }

        // Toggle the status
        if (task.getStatus() == TaskStatus.COMPLETED) {
            task.setStatus(TaskStatus.PENDING); // Mark as pending
        } else {
            task.setStatus(TaskStatus.COMPLETED); // Mark as completed
        }

        // Save the updated task
        taskRepo.save(task);

        System.out.println("Task status toggled: " + task);
        return "redirect:/tasks";
    }
}
