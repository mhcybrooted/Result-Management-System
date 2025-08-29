package mh.cyb.root.rms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "marks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "subject_id", "exam_id"})
})
public class Marks {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;
    
    @NotNull(message = "Obtained marks is required")
    @Min(value = 0, message = "Obtained marks cannot be negative")
    @Column(name = "obtained_marks", nullable = false)
    private Integer obtainedMarks;
    
    @Column(name = "exam_date")
    private LocalDate examDate;
    
    // Constructors
    public Marks() {}
    
    public Marks(Student student, Subject subject, Exam exam, Integer obtainedMarks, LocalDate examDate) {
        this.student = student;
        this.subject = subject;
        this.exam = exam;
        this.obtainedMarks = obtainedMarks;
        this.examDate = examDate;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    
    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }
    
    public Integer getObtainedMarks() { return obtainedMarks; }
    public void setObtainedMarks(Integer obtainedMarks) { this.obtainedMarks = obtainedMarks; }
    
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
}
