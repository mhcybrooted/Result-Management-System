package mh.cyb.root.rms.service;

import mh.cyb.root.rms.dto.BulkMarksRequest;
import mh.cyb.root.rms.dto.BulkResult;
import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BulkService {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private TeacherRepository teacherRepository;
    
    @Autowired
    private MarksRepository marksRepository;
    
    @Transactional
    public BulkResult saveBulkMarks(BulkMarksRequest request) {
        BulkResult result = new BulkResult();
        
        // Validate request
        if (!validateRequest(request, result)) {
            return result;
        }
        
        // Get entities
        Optional<Teacher> teacher = teacherRepository.findById(request.getTeacherId());
        Optional<Subject> subject = subjectRepository.findById(request.getSubjectId());
        Optional<Exam> exam = examRepository.findById(request.getExamId());
        
        if (!teacher.isPresent() || !subject.isPresent() || !exam.isPresent()) {
            result.addError("Invalid teacher, subject, or exam selected");
            return result;
        }
        
        // Process each student mark
        int processed = 0;
        int success = 0;
        int errors = 0;
        
        for (BulkMarksRequest.StudentMark studentMark : request.getStudentMarks()) {
            if (studentMark.getObtainedMarks() != null && studentMark.getObtainedMarks() >= 0) {
                processed++;
                
                if (processSingleMark(studentMark, teacher.get(), subject.get(), exam.get(), result)) {
                    success++;
                } else {
                    errors++;
                }
            }
        }
        
        result.setTotalProcessed(processed);
        result.setSuccessCount(success);
        result.setErrorCount(errors);
        
        return result;
    }
    
    private boolean validateRequest(BulkMarksRequest request, BulkResult result) {
        if (request.getTeacherId() == null) {
            result.addError("Teacher is required");
            return false;
        }
        if (request.getSubjectId() == null) {
            result.addError("Subject is required");
            return false;
        }
        if (request.getExamId() == null) {
            result.addError("Exam is required");
            return false;
        }
        if (request.getStudentMarks() == null || request.getStudentMarks().isEmpty()) {
            result.addError("No student marks provided");
            return false;
        }
        return true;
    }
    
    private boolean processSingleMark(BulkMarksRequest.StudentMark studentMark, 
                                     Teacher teacher, Subject subject, Exam exam, 
                                     BulkResult result) {
        try {
            Optional<Student> student = studentRepository.findById(studentMark.getStudentId());
            if (!student.isPresent()) {
                result.addError("Student not found for ID: " + studentMark.getStudentId());
                return false;
            }
            
            // Validate marks don't exceed max marks
            if (studentMark.getObtainedMarks() > subject.getMaxMarks()) {
                result.addError("Marks for " + student.get().getName() + 
                               " exceed maximum (" + subject.getMaxMarks() + ")");
                return false;
            }
            
            // Check for existing marks
            Optional<Marks> existingMarks = marksRepository.findByStudentIdAndSubjectIdAndExamId(
                studentMark.getStudentId(), subject.getId(), exam.getId());
            
            Marks marks;
            if (existingMarks.isPresent()) {
                // Update existing marks
                marks = existingMarks.get();
                marks.setObtainedMarks(studentMark.getObtainedMarks());
                marks.setEnteredBy(teacher);
                marks.setEnteredDate(LocalDateTime.now());
                result.addWarning("Updated existing marks for " + student.get().getName());
            } else {
                // Create new marks
                marks = new Marks(student.get(), subject, exam, 
                                studentMark.getObtainedMarks(), LocalDate.now());
                marks.setEnteredBy(teacher);
            }
            
            marksRepository.save(marks);
            return true;
            
        } catch (Exception e) {
            result.addError("Error processing marks: " + e.getMessage());
            return false;
        }
    }
    
    public List<Student> getStudentsForBulkEntry(Long subjectId) {
        Optional<Subject> subject = subjectRepository.findById(subjectId);
        if (subject.isPresent()) {
            return studentRepository.findAll().stream()
                    .filter(s -> s.getClassName().equals(subject.get().getClassName()))
                    .collect(Collectors.toList());
        }
        return studentRepository.findAll();
    }
}
