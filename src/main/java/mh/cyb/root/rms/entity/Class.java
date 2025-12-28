package mh.cyb.root.rms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "classes")
public class Class {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Class name is required")
    @Column(name = "class_name", nullable = false, unique = true)
    private String className;
    
    @Column(name = "description")
    private String description;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    // Constructors
    public Class() {}
    
    public Class(String className, String description) {
        this.className = className;
        this.description = description;
        this.active = true;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
