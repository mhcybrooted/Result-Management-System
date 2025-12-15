
import java.util.*;

public class RollNumberCollisionTest {

    // --- Mock Database / Repository ---
    static class Student {
        Long id;
        String name;
        String rollNumber;
        String className;
        boolean sessionActive;

        public Student(Long id, String name, String roll, String cls) {
            this.id = id;
            this.name = name;
            this.rollNumber = roll;
            this.className = cls;
            this.sessionActive = true;
        }
    }

    static class Marks {
        Student student;
        String subject;
        int score;

        public Marks(Student s, String sub, int sc) {
            student = s;
            subject = sub;
            score = sc;
        }
    }

    // List to act as DB tables
    static List<Student> studentsDB = new ArrayList<>();
    static List<Marks> marksDB = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Running Roll Number Collision Test...");

        // Setup: Two students with SAME Roll Number "101" in active session
        Student s1 = new Student(1L, "Alice (Class 6)", "101", "Class 6");
        Student s2 = new Student(2L, "Bob (Class 7)", "101", "Class 7");

        studentsDB.add(s1);
        studentsDB.add(s2);

        // Alice passes her subjects
        marksDB.add(new Marks(s1, "Math", 80));
        marksDB.add(new Marks(s1, "Science", 75));

        // Bob FAILS his subject
        marksDB.add(new Marks(s2, "History", 10)); // Fail!

        // --- SIMULATE REPOSITORY QUERY ---
        // Query: SELECT m FROM Marks m WHERE m.student.rollNumber = :rollNumber AND ...
        String searchRoll = "101";
        System.out.println("Searching for Roll Number: " + searchRoll);

        List<Marks> resultMarks = new ArrayList<>();
        for (Marks m : marksDB) {
            if (m.student.rollNumber.equals(searchRoll) && m.student.sessionActive) {
                resultMarks.add(m);
            }
        }

        // --- SIMULATE EXAM SERVICE LOGIC ---
        // It likely picks the first student it finds for the header info
        Student foundStudent = studentsDB.stream()
                .filter(s -> s.rollNumber.equals(searchRoll))
                .findFirst().orElse(null);

        if (foundStudent == null) {
            System.out.println("No student found.");
            return;
        }

        System.out.println("Service identified student: " + foundStudent.name);
        System.out.println("But fetched marks for:");

        boolean failCondition = false;
        for (Marks m : resultMarks) {
            System.out.println(" - " + m.subject + ": " + m.score + " (Student ID: " + m.student.id + ")");
            if (m.score < 33) {
                failCondition = true;
                System.out.println("   [!] FAIL TRIGGERED!");
            }
        }

        if (foundStudent.id == 1L && failCondition) {
            System.out.println("\nCONCLUSION: COLLISION REPRODUCED!");
            System.out.println(
                    "Alice (who passed) is shown as FAILED because the system pulled in Bob's failing marks due to duplicate Roll Number '101'.");
        } else {
            System.out.println("Conclusion: No collision detected.");
        }
    }
}
