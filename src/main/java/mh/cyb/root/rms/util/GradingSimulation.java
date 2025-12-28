
import java.util.*;

public class GradingSimulation {

    // --- Mock Classes ---
    static class Subject {
        String name;
        int maxMarks;
        boolean optional;

        public Subject(String name, int maxMarks, boolean optional) {
            this.name = name;
            this.maxMarks = maxMarks;
            this.optional = optional;
        }
    }

    static class Marks {
        Subject subject;
        int obtainedMarks;

        public Marks(Subject s, int m) {
            this.subject = s;
            this.obtainedMarks = m;
        }

        public Subject getSubject() {
            return subject;
        }

        public int getObtainedMarks() {
            return obtainedMarks;
        }
    }

    static class GradeCalculator {
        // Properties from application.properties
        int minD = 33;

        // Simplified Map for Grades
        public String calculateGrade(double percentage) {
            if (percentage >= 80)
                return "A+";
            if (percentage >= 70)
                return "A";
            if (percentage >= 60)
                return "A-";
            if (percentage >= 50)
                return "B";
            if (percentage >= 40)
                return "C";
            if (percentage >= 33)
                return "D";
            return "F";
        }

        public double calculateGP(double percentage) {
            if (percentage >= 80)
                return 5.0;
            if (percentage >= 70)
                return 4.0;
            if (percentage >= 60)
                return 3.5;
            if (percentage >= 50)
                return 3.0;
            if (percentage >= 40)
                return 2.0;
            if (percentage >= 33)
                return 1.0;
            return 0.0;
        }
    }

    public static void main(String[] args) {
        System.out.println("Running Grading Simulation...");
        GradeCalculator calc = new GradeCalculator();

        // Scenario:
        // Subject 1: Math (High Score) -> 68/100
        // Subject 2: English (Fail) -> 0/100 (Absent/Not Entered)

        List<Marks> marksList = new ArrayList<>();
        marksList.add(new Marks(new Subject("Math", 100, false), 68));
        marksList.add(new Marks(new Subject("English", 100, false), 0));

        // --- Logic from ResultBuilder.java ---
        double totalGP = 0.0;
        int compulsoryCount = 0;
        boolean isFail = false;
        double totalObtained = 0;
        double totalMax = 0;

        for (Marks m : marksList) {
            int obtained = m.getObtainedMarks();
            int max = m.getSubject().maxMarks;

            double percentage = (obtained * 100.0) / max;
            String grade = calc.calculateGrade(percentage);
            double gp = calc.calculateGP(percentage);

            System.out.println("Subject: " + m.getSubject().name);
            System.out.println("  Marks: " + obtained + "/" + max + " (" + percentage + "%)");
            System.out.println("  Grade: " + grade + " | GP: " + gp);

            if (!m.getSubject().optional) {
                totalGP += gp;
                compulsoryCount++;
                totalObtained += obtained;
                totalMax += max;

                if ("F".equals(grade)) {
                    isFail = true;
                    System.out.println("  -> FAIL CONDITION TRIGGERED");
                }
            }
        }

        // Overall Result Calculation
        double overallPercentage = (totalObtained * 100.0) / totalMax;
        String overallGrade = calc.calculateGrade(overallPercentage);

        System.out.println("\n--- Overall Calculation ---");
        System.out.println("Total Obtained: " + totalObtained + "/" + totalMax);
        System.out.println("Overall Percentage: " + overallPercentage + "%");
        System.out.println("Raw Overall Grade (based on %): " + overallGrade);

        String finalStatus = "PASS";
        if (isFail) {
            finalStatus = "FAIL";
        }

        System.out.println("Final Result Status: " + finalStatus);

        if ("D".equals(overallGrade) && "FAIL".equals(finalStatus)) {
            System.out.println("\nCONCLUSION: REPRODUCED 'Grade D but Fail'");
            System.out.println("The student has an average percentage entitling them to Grade D,");
            System.out.println("but failed a specific compulsory subject, causing overall FAIL.");
        }
    }
}
