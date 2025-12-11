package mh.cyb.root.rms.config;

import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.repository.*;
import mh.cyb.root.rms.service.AdminUserService;
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

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private MarksRepository marksRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialize sample data if database is empty
        if (sessionRepository.count() == 0) {
            initializeData();
        }

        // Create default admin user if none exists
        if (adminUserRepository.count() == 0) {
            createDefaultAdmin();
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
        mh.cyb.root.rms.entity.Class class10 = new mh.cyb.root.rms.entity.Class("Class 10",
                "Grade 10 - Secondary level");
        mh.cyb.root.rms.entity.Class class11 = new mh.cyb.root.rms.entity.Class("Class 11",
                "Grade 11 - Higher Secondary");
        mh.cyb.root.rms.entity.Class class12 = new mh.cyb.root.rms.entity.Class("Class 12",
                "Grade 12 - Higher Secondary");

        classRepository.save(class9);
        classRepository.save(class10);
        classRepository.save(class11);
        classRepository.save(class12);

        // Create sample students for active session
        Student student1 = new Student("John Doe", "101", "Class 10", session2024);
        Student student2 = new Student("Jane Smith", "102", "Class 10", session2024);
        Student student3 = new Student("Mike Johnson", "103", "Class 10", session2024); // To verify Fail Logic
        Student student4 = new Student("Sarah Wilson", "201", "Class 9", session2024);
        Student student5 = new Student("David Brown", "202", "Class 9", session2024);
        Student student6 = new Student("Emily Optional-Fail", "104", "Class 10", session2024); // To verify Optional
                                                                                               // Fail

        studentRepository.save(student1);
        studentRepository.save(student2);
        studentRepository.save(student3);
        studentRepository.save(student4);
        studentRepository.save(student5);
        studentRepository.save(student6);

        // Create sample subjects
        Subject math10 = new Subject("Mathematics", class10, 100);
        math10.setOptional(false);
        Subject english10 = new Subject("English", class10, 100);
        english10.setOptional(false);
        Subject science10 = new Subject("Science", class10, 100);
        science10.setOptional(false);

        // Optional Subject
        Subject compSci10 = new Subject("Computer Science", class10, 100);
        compSci10.setOptional(true);

        Subject math9 = new Subject("Mathematics", class9, 100);
        Subject english9 = new Subject("English", class9, 100);

        subjectRepository.save(math10);
        subjectRepository.save(english10);
        subjectRepository.save(science10);
        subjectRepository.save(compSci10);
        subjectRepository.save(math9);
        subjectRepository.save(english9);

        // Create sample exams for active session
        Exam midterm = new Exam("Midterm Exam", LocalDate.of(2025, 1, 15), session2024);
        Exam finalExam = new Exam("Final Exam", LocalDate.of(2025, 3, 15), session2024);
        Exam quiz1 = new Exam("Quiz 1", LocalDate.of(2025, 2, 1), session2024);

        examRepository.save(midterm);
        examRepository.save(finalExam);
        examRepository.save(quiz1);

        // --- Marks Initialization ---

        // 1. John Doe (Excellent Performance - Golden A+ Candidate)
        marksRepository.save(new Marks(student1, math10, finalExam, 95, LocalDate.now()));
        marksRepository.save(new Marks(student1, english10, finalExam, 92, LocalDate.now()));
        marksRepository.save(new Marks(student1, science10, finalExam, 98, LocalDate.now()));
        marksRepository.save(new Marks(student1, compSci10, finalExam, 90, LocalDate.now())); // Optional A+

        // 2. Jane Smith (Average Performance)
        marksRepository.save(new Marks(student2, math10, finalExam, 65, LocalDate.now()));
        marksRepository.save(new Marks(student2, english10, finalExam, 70, LocalDate.now()));
        marksRepository.save(new Marks(student2, science10, finalExam, 60, LocalDate.now()));
        // No optional subject for Jane

        // 3. Mike Johnson (Fail Scenario - Compulsory Fail)
        marksRepository.save(new Marks(student3, math10, finalExam, 25, LocalDate.now())); // F
        marksRepository.save(new Marks(student3, english10, finalExam, 60, LocalDate.now()));
        marksRepository.save(new Marks(student3, science10, finalExam, 65, LocalDate.now()));

        // 4. Emily Optional-Fail (Optional Fail -> Pass Scenario)
        marksRepository.save(new Marks(student6, math10, finalExam, 60, LocalDate.now()));
        marksRepository.save(new Marks(student6, english10, finalExam, 65, LocalDate.now()));
        marksRepository.save(new Marks(student6, science10, finalExam, 70, LocalDate.now()));
        marksRepository.save(new Marks(student6, compSci10, finalExam, 20, LocalDate.now())); // F (Ignored)

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
        System.out.println(
                "Students: John Doe (101), Jane Smith (102), Mike Johnson (103), Sarah Wilson (201), David Brown (202), Emily (104)");
        System.out.println("Subjects: Math, English, Science (Compulsory), Comp Sci (Optional)");
        System.out.println("Marks initialized for various scenarios (Best, Avg, Fail, Opt-Fail)");
    }

    private void createDefaultAdmin() {
        // Create default admin user
        adminUserService.createAdmin("admin", "admin123");

        System.out.println("Default admin user created:");
        System.out.println("Username: admin");
        System.out.println("Password: admin123");
        System.out.println("Access admin functions at: /admin-login");
    }
}
