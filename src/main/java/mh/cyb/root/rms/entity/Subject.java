package mh.cyb.root.rms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "subjects")
public class Subject {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Subject name is required")
    @Column(name = "subject_name", nullable = false)
    private String subjectName;
    
    @NotBlank(message = "Class name is required")
    @Column(name = "class_name", nullable = false)
    private String className;
    
    @NotNull(message = "Max marks is required")
    @Min(value = 1, message = "Max marks must be at least 1")
    @Column(name = "max_marks", nullable = false)
    private Integer maxMarks;
    
    // Constructors
    public Subject() {}
    
    public Subject(String subjectName, String className, Integer maxMarks) {
        this.subjectName = subjectName;
        this.className = className;
        this.maxMarks = maxMarks;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }
}
