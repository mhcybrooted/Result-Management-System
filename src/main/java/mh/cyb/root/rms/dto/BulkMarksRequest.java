package mh.cyb.root.rms.dto;

import java.util.List;

public class BulkMarksRequest {
    private Long teacherId;
    private Long subjectId;
    private Long examId;
    private List<StudentMark> studentMarks;
    
    public static class StudentMark {
        private Long studentId;
        private Integer obtainedMarks;
        
        public StudentMark() {}
        
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        
        public Integer getObtainedMarks() { return obtainedMarks; }
        public void setObtainedMarks(Integer obtainedMarks) { this.obtainedMarks = obtainedMarks; }
    }
    
    public BulkMarksRequest() {}
    
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    
    public List<StudentMark> getStudentMarks() { return studentMarks; }
    public void setStudentMarks(List<StudentMark> studentMarks) { this.studentMarks = studentMarks; }
}
