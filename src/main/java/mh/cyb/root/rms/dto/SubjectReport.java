package mh.cyb.root.rms.dto;

import java.util.Map;

public class SubjectReport {
    
    private String subjectName;
    private Map<String, Integer> examMarks; // examName -> marks
    private int totalObtained;
    private int totalMaximum;
    private double percentage;
    private String grade;
    
    public SubjectReport() {}
    
    public SubjectReport(String subjectName, Map<String, Integer> examMarks, int totalMaximum) {
        this.subjectName = subjectName;
        this.examMarks = examMarks;
        this.totalMaximum = totalMaximum;
        calculateTotals();
    }
    
    private void calculateTotals() {
        totalObtained = examMarks.values().stream().mapToInt(Integer::intValue).sum();
        percentage = totalMaximum > 0 ? (double) totalObtained / totalMaximum * 100 : 0;
        grade = calculateGrade(percentage);
    }
    
    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 85) return "A";
        if (percentage >= 75) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }
    
    // Getters and Setters
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    
    public Map<String, Integer> getExamMarks() { return examMarks; }
    public void setExamMarks(Map<String, Integer> examMarks) { 
        this.examMarks = examMarks;
        calculateTotals();
    }
    
    public int getTotalObtained() { return totalObtained; }
    public int getTotalMaximum() { return totalMaximum; }
    public void setTotalMaximum(int totalMaximum) { 
        this.totalMaximum = totalMaximum;
        calculateTotals();
    }
    
    public double getPercentage() { return Math.round(percentage * 100.0) / 100.0; }
    public String getGrade() { return grade; }
}
