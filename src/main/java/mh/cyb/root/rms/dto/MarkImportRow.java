package mh.cyb.root.rms.dto;

public class MarkImportRow {
    private String studentRoll;
    private String studentName;
    private String subjectName;
    private String examName;
    private Integer obtainedMarks;
    private String status = "Valid";
    private String errorMessage;
    
    public MarkImportRow() {}
    
    public String getStudentRoll() { return studentRoll; }
    public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    
    public Integer getObtainedMarks() { return obtainedMarks; }
    public void setObtainedMarks(Integer obtainedMarks) { this.obtainedMarks = obtainedMarks; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
