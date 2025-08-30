package mh.cyb.root.rms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "teacher_assignments")
public class TeacherAssignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    // Constructors
    public TeacherAssignment() {}
    
    public TeacherAssignment(Teacher teacher, Subject subject, Session session) {
        this.teacher = teacher;
        this.subject = subject;
        this.session = session;
        this.active = true;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    
    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
