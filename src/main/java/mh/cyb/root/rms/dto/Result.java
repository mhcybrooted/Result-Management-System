package mh.cyb.root.rms.dto;

import mh.cyb.root.rms.entity.Marks;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Result {

    private String studentName;
    private String rollNumber;
    private String className;
    private List<Marks> marksList;
    private double totalObtained;
    private double totalMax;
    private double percentage;
    private String grade;
    private double gpa;
    private String result;

    // Constructors
    public Result() {
    }

    public Result(String studentName, String rollNumber, String className, List<Marks> marksList) {
        this.studentName = studentName;
        this.rollNumber = rollNumber;
        this.className = className;
        this.marksList = marksList;
        calculateTotals();
    }

    private void calculateTotals() {
        // Group marks by subject to handle optional subjects properly
        Map<String, List<Marks>> marksBySubject = marksList.stream()
                .collect(Collectors.groupingBy(m -> m.getSubject().getSubjectName()));

        double totalGP = 0.0;
        int compulsoryCount = 0;
        boolean isFail = false;
        totalObtained = 0;
        totalMax = 0;

        for (Map.Entry<String, List<Marks>> entry : marksBySubject.entrySet()) {
            List<Marks> subjectMarks = entry.getValue();

            // Calculate subject totals
            int subjectObtained = subjectMarks.stream().mapToInt(Marks::getObtainedMarks).sum();
            int subjectMax = subjectMarks.stream().mapToInt(m -> m.getSubject().getMaxMarks()).sum();

            double subjectPercentage = subjectMax > 0 ? (subjectObtained * 100.0 / subjectMax) : 0;
            String subjectGrade = calculateGrade(subjectPercentage);
            double subjectGP = calculateGradePoint(subjectPercentage);

            // Check if subject is optional (all marks in a subject should have same
            // optional flag)
            boolean isOptional = !subjectMarks.isEmpty() && subjectMarks.get(0).getSubject().isOptional();

            if (isOptional) {
                // Optional Logic: If GP >= 2.0, add (GP - 2.0)
                if (subjectGP >= 2.0) {
                    totalGP += (subjectGP - 2.0);
                }
                // Don't include optional in total for percentage display
            } else {
                // Compulsory Logic: Add full GP
                totalGP += subjectGP;
                compulsoryCount++;

                // Add to totals for percentage
                totalObtained += subjectObtained;
                totalMax += subjectMax;

                // CHECK FAIL CONDITION
                if ("F".equals(subjectGrade)) {
                    isFail = true;
                }
            }
        }

        // Calculate overall percentage (compulsory subjects only)
        percentage = totalMax > 0 ? (totalObtained / totalMax) * 100 : 0;
        grade = calculateGrade(percentage);

        // Calculate GPA
        if (compulsoryCount > 0) {
            gpa = totalGP / compulsoryCount;
        } else {
            gpa = 0.0;
        }

        // Cap at 5.00
        if (gpa > 5.00) {
            gpa = 5.00;
        }

        // Apply Fail Logic
        if (isFail) {
            gpa = 0.00;
            result = "FAIL";
        } else {
            result = "PASS";
        }

        // Round GPA to 2 decimal places
        gpa = Math.round(gpa * 100.0) / 100.0;
    }

    private String calculateGrade(double percentage) {
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

    private double calculateGradePoint(double percentage) {
        if (percentage >= 80)
            return 5.00; // A+
        if (percentage >= 70)
            return 4.00; // A
        if (percentage >= 60)
            return 3.50; // A-
        if (percentage >= 50)
            return 3.00; // B
        if (percentage >= 40)
            return 2.00; // C
        if (percentage >= 33)
            return 1.00; // D
        return 0.00; // F
    }

    // Getters and Setters
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<Marks> getMarksList() {
        return marksList;
    }

    public void setMarksList(List<Marks> marksList) {
        this.marksList = marksList;
        calculateTotals();
    }

    public double getTotalObtained() {
        return totalObtained;
    }

    public double getTotalMax() {
        return totalMax;
    }

    public double getPercentage() {
        return Math.round(percentage * 100.0) / 100.0;
    }

    public String getGrade() {
        return grade;
    }

    public double getGpa() {
        return gpa;
    }

    public String getResult() {
        return result;
    }
}
