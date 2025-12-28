package mh.cyb.root.rms.dto;

import mh.cyb.root.rms.entity.Student;

public class MeritListItem {
    private int rank;
    private Student student;
    private double gpa;
    private double totalObtained;
    private double totalMax;
    private String overallGrade;
    private String result;

    public MeritListItem(int rank, Student student, double gpa, double totalObtained, double totalMax,
            String overallGrade, String result) {
        this.rank = rank;
        this.student = student;
        this.gpa = gpa;
        this.totalObtained = totalObtained;
        this.totalMax = totalMax;
        this.overallGrade = overallGrade;
        this.result = result;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public double getGpa() {
        return gpa;
    }

    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    public double getTotalObtained() {
        return totalObtained;
    }

    public void setTotalObtained(double totalObtained) {
        this.totalObtained = totalObtained;
    }

    public double getTotalMax() {
        return totalMax;
    }

    public void setTotalMax(double totalMax) {
        this.totalMax = totalMax;
    }

    public String getOverallGrade() {
        return overallGrade;
    }

    public void setOverallGrade(String overallGrade) {
        this.overallGrade = overallGrade;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
