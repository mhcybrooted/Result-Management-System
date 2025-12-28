package mh.cyb.root.rms.dto;

import mh.cyb.root.rms.entity.Student;
import mh.cyb.root.rms.entity.Session;
import java.time.LocalDate;
import java.util.List;

public class ReportCardData {

    private Student student;
    private Session session;
    private List<SubjectReport> subjects;
    private double overallPercentage;
    private String overallGrade;
    private String result;
    private LocalDate reportDate;

    public ReportCardData() {
        this.reportDate = LocalDate.now();
    }

    public ReportCardData(Student student, Session session, List<SubjectReport> subjects) {
        this.student = student;
        this.session = session;
        this.subjects = subjects;
        this.reportDate = LocalDate.now();
        // Calculation moved to Service to ensure consistency
    }

    // Removed calculateOverall() - Service must set these exclusively

    // Removed internal calculateGrade

    // Getters and Setters
    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public List<SubjectReport> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectReport> subjects) {
        this.subjects = subjects;
        // Calculation moved to Service
    }

    public double getOverallPercentage() {
        return Math.round(overallPercentage * 100.0) / 100.0;
    }

    public void setOverallPercentage(double overallPercentage) {
        this.overallPercentage = overallPercentage;
    }

    public String getOverallGrade() {
        return overallGrade;
    }

    public void setOverallGrade(String overallGrade) {
        this.overallGrade = overallGrade;
    }

    private double gpa;

    public double getGpa() {
        return gpa;
    }

    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }
}
