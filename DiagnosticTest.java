
import java.util.*;

public class DiagnosticTest {

    // Mock Entities
    static class Subject {
        String name;
        int maxMarks;
        boolean optional;

        public Subject(String n, int m) {
            name = n;
            maxMarks = m;
            optional = false;
        }
    }

    static class Exam {
        String name;
        boolean active;

        public Exam(String n, boolean a) {
            name = n;
            active = a;
        }
    }

    static class Marks {
        Subject subject;
        Exam exam;
        int obtained;

        public Marks(Subject s, Exam e, int o) {
            subject = s;
            exam = e;
            obtained = o;
        }

        public Subject getSubject() {
            return subject;
        }

        public int getObtainedMarks() {
            return obtained;
        }
    }

    // Calculator with defaults matching application.properties
    static class Calculator {
        int minD = 33;

        public String getGrade(double p) {
            if (p >= 80)
                return "A+";
            if (p >= 70)
                return "A";
            if (p >= 60)
                return "A-";
            if (p >= 50)
                return "B";
            if (p >= 40)
                return "C";
            if (p >= 33)
                return "D";
            return "F";
        }
    }

    public static void main(String[] args) {
        System.out.println("Running Diagnostic for: 39 Marks Scenario");
        Calculator calc = new Calculator();

        // Setup: One Subject, 39 Marks
        List<Marks> marksList = new ArrayList<>();
        Subject sub = new Subject("TestSubject", 100);
        Exam exam = new Exam("TestExam", true);
        marksList.add(new Marks(sub, exam, 39));

        // Logic
        boolean isFail = false;
        double totalObtained = 0;
        double totalMax = 0;

        System.out.println("Processing Marks:");
        for (Marks m : marksList) {
            double p = (m.obtained * 100.0) / m.subject.maxMarks;
            String g = calc.getGrade(p);
            System.out.println("  - " + m.subject.name + ": " + m.obtained + "/" + m.subject.maxMarks + " = " + p
                    + "% -> Grade " + g);

            totalObtained += m.obtained;
            totalMax += m.subject.maxMarks;

            if ("F".equals(g) && !m.subject.optional) {
                isFail = true;
                System.out.println("    [!] Fail Triggered by " + m.subject.name);
            }
        }

        double overallP = (totalObtained * 100.0) / totalMax;
        String overallG = calc.getGrade(overallP);
        String status = isFail ? "FAIL" : "PASS";

        System.out.println("\n--- RESULT ---");
        System.out.println("Total: " + totalObtained + "/" + totalMax + " (" + overallP + "%)");
        System.out.println("Overall Grade: " + overallG);
        System.out.println("Status: " + status);

        if ("D".equals(overallG) && "FAIL".equals(status)) {
            System.out.println("REPRODUCED!");
        } else {
            System.out.println("NOT REPRODUCED. Status is " + status);
        }
    }
}
