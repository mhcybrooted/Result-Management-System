package mh.cyb.root.rms.config;

import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private ClassRepository classRepository;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @Autowired
    private TeacherRepository teacherRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Initialize sample data if database is empty
        if (sessionRepository.count() == 0) {
            initializeData();
        }
    }
    
    private void initializeData() {
        // Create sample sessions
        Session session2024 = new Session("2024-25", LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31));
        session2024.setActive(true); // Set as active session
        Session session2025 = new Session("2025-26", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31));
        
        sessionRepository.save(session2024);
        sessionRepository.save(session2025);
        
        // Create sample classes
        mh.cyb.root.rms.entity.Class class9 = new mh.cyb.root.rms.entity.Class("Class 9", "Grade 9 - Secondary level");
        mh.cyb.root.rms.entity.Class class10 = new mh.cyb.root.rms.entity.Class("Class 10", "Grade 10 - Secondary level");
        mh.cyb.root.rms.entity.Class class11 = new mh.cyb.root.rms.entity.Class("Class 11", "Grade 11 - Higher Secondary");
        mh.cyb.root.rms.entity.Class class12 = new mh.cyb.root.rms.entity.Class("Class 12", "Grade 12 - Higher Secondary");
        
        classRepository.save(class9);
        classRepository.save(class10);
        classRepository.save(class11);
        classRepository.save(class12);
        
        // Create sample students for active session
        Student student1 = new Student("John Doe", "101", "Class 10", session2024);
        Student student2 = new Student("Jane Smith", "102", "Class 10", session2024);
        Student student3 = new Student("Mike Johnson", "103", "Class 10", session2024);
        Student student4 = new Student("Sarah Wilson", "201", "Class 9", session2024);
        Student student5 = new Student("David Brown", "202", "Class 9", session2024);
        
        studentRepository.save(student1);
        studentRepository.save(student2);
        studentRepository.save(student3);
        studentRepository.save(student4);
        studentRepository.save(student5);
        
        // Create sample subjects
        Subject math10 = new Subject("Mathematics", class10, 100);
        Subject english10 = new Subject("English", class10, 100);
        Subject science10 = new Subject("Science", class10, 100);
        Subject math9 = new Subject("Mathematics", class9, 100);
        Subject english9 = new Subject("English", class9, 100);
        
        subjectRepository.save(math10);
        subjectRepository.save(english10);
        subjectRepository.save(science10);
        subjectRepository.save(math9);
        subjectRepository.save(english9);
        
        // Create sample exams for active session
        Exam midterm = new Exam("Midterm Exam", LocalDate.of(2025, 1, 15), session2024);
        Exam finalExam = new Exam("Final Exam", LocalDate.of(2025, 3, 15), session2024);
        Exam quiz1 = new Exam("Quiz 1", LocalDate.of(2025, 2, 1), session2024);
        
        examRepository.save(midterm);
        examRepository.save(finalExam);
        examRepository.save(quiz1);
        
        // Create sample teachers
        Teacher teacher1 = new Teacher("Dr. Sarah Johnson", "sarah.johnson@school.edu", "+1-555-0101");
        Teacher teacher2 = new Teacher("Prof. Michael Chen", "michael.chen@school.edu", "+1-555-0102");
        Teacher teacher3 = new Teacher("Ms. Emily Davis", "emily.davis@school.edu", "+1-555-0103");
        Teacher teacher4 = new Teacher("Mr. Robert Wilson", "robert.wilson@school.edu", "+1-555-0104");
        
        teacherRepository.save(teacher1);
        teacherRepository.save(teacher2);
        teacherRepository.save(teacher3);
        teacherRepository.save(teacher4);
        
        System.out.println("Sample data initialized successfully!");
        System.out.println("Sessions: 2024-25 (Active), 2025-26");
        System.out.println("Classes: Class 9, Class 10, Class 11, Class 12");
        System.out.println("Students: John Doe (101), Jane Smith (102), Mike Johnson (103), Sarah Wilson (201), David Brown (202)");
        System.out.println("Subjects: Mathematics, English, Science for Class 9 and 10");
        System.out.println("Exams: Midterm Exam, Final Exam, Quiz 1 (for 2024-25 session)");
        System.out.println("Teachers: Dr. Sarah Johnson, Prof. Michael Chen, Ms. Emily Davis, Mr. Robert Wilson");
    }
}
