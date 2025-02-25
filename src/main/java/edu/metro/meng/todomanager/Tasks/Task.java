package edu.metro.meng.todomanager.Tasks;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "Task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String title;
    private String description;
    private LocalDate dueDate;
    @Column(nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.PENDING;
}
