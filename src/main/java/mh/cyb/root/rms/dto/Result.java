package mh.cyb.root.rms.dto;

import mh.cyb.root.rms.entity.Marks;
import java.util.List;

public class Result {
    
    private String studentName;
    private String rollNumber;
    private String className;
    private List<Marks> marksList;
    private double totalObtained;
    private double totalMax;
    private double percentage;
    private String grade;
    
    // Constructors
    public Result() {}
    
    public Result(String studentName, String rollNumber, String className, List<Marks> marksList) {
        this.studentName = studentName;
        this.rollNumber = rollNumber;
        this.className = className;
        this.marksList = marksList;
        calculateTotals();
    }
    
    private void calculateTotals() {
        totalObtained = marksList.stream().mapToInt(Marks::getObtainedMarks).sum();
        totalMax = marksList.stream().mapToInt(m -> m.getSubject().getMaxMarks()).sum();
        percentage = totalMax > 0 ? (totalObtained / totalMax) * 100 : 0;
        grade = calculateGrade(percentage);
    }
    
    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A";
        if (percentage >= 75) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }
    
    // Getters and Setters
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public List<Marks> getMarksList() { return marksList; }
    public void setMarksList(List<Marks> marksList) { 
        this.marksList = marksList;
        calculateTotals();
    }
    
    public double getTotalObtained() { return totalObtained; }
    public double getTotalMax() { return totalMax; }
    public double getPercentage() { return Math.round(percentage * 100.0) / 100.0; }
    public String getGrade() { return grade; }
}
