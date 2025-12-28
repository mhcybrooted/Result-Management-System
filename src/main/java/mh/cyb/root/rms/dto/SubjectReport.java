package mh.cyb.root.rms.dto;

import java.util.Map;

public class SubjectReport {

    private String subjectName;
    private Map<String, Integer> examMarks; // examName -> marks
    private int totalObtained;
    private int totalMaximum;
    private double percentage;
    private String grade;
    private double gradePoint;
    private boolean isOptional;

    public SubjectReport() {
    }

    public SubjectReport(String subjectName, Map<String, Integer> examMarks, int totalMaximum) {
        this.subjectName = subjectName;
        this.examMarks = examMarks;
        this.totalMaximum = totalMaximum;
        calculateTotals();
    }

    private void calculateTotals() {
        totalObtained = examMarks.values().stream().mapToInt(Integer::intValue).sum();
        percentage = totalMaximum > 0 ? (double) totalObtained / totalMaximum * 100 : 0;
        // grade is now set externally
    }

    // Getters and Setters
    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Map<String, Integer> getExamMarks() {
        return examMarks;
    }

    public void setExamMarks(Map<String, Integer> examMarks) {
        this.examMarks = examMarks;
        calculateTotals();
    }

    public int getTotalObtained() {
        return totalObtained;
    }

    public int getTotalMaximum() {
        return totalMaximum;
    }

    public void setTotalMaximum(int totalMaximum) {
        this.totalMaximum = totalMaximum;
        calculateTotals();
    }

    public double getPercentage() {
        return Math.round(percentage * 100.0) / 100.0;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public double getGradePoint() {
        return gradePoint;
    }

    public void setGradePoint(double gradePoint) {
        this.gradePoint = gradePoint;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public void setOptional(boolean optional) {
        isOptional = optional;
    }
}
