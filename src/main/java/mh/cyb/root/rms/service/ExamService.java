package mh.cyb.root.rms.service;

import mh.cyb.root.rms.dto.Result;
import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExamService {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;
    
    @Autowired
    private MarksRepository marksRepository;
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @Autowired
    private ClassRepository classRepository;
    
    @Autowired
    private TeacherRepository teacherRepository;
    
    // Session management
    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }
    
    public Optional<Session> getActiveSession() {
        return sessionRepository.findByActiveTrue();
    }
    
    public Session saveSession(Session session) {
        return sessionRepository.save(session);
    }
    
    @Transactional
    public boolean activateSession(Long sessionId) {
        sessionRepository.deactivateAllSessions();
        sessionRepository.activateSession(sessionId);
        return true;
    }
    
    public Optional<Session> getSessionById(Long id) {
        return sessionRepository.findById(id);
    }
    
    // Get students for active session
    public List<Student> getAllStudents() {
        return studentRepository.findByActiveSession();
    }
    
    // Get all subjects
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }
    
    // Get exams for active session
    public List<Exam> getAllActiveExams() {
        return examRepository.findByActiveTrue();
    }
    
    // Get all exams for active session
    public List<Exam> getAllExams() {
        return examRepository.findByActiveSession();
    }
    
    // Add/Update exam (with active session)
    public Exam saveExam(Exam exam) {
        if (exam.getSession() == null) {
            Optional<Session> activeSession = getActiveSession();
            if (activeSession.isPresent()) {
                exam.setSession(activeSession.get());
            }
        }
        return examRepository.save(exam);
    }
    
    // Get exam by ID
    public Optional<Exam> getExamById(Long id) {
        return examRepository.findById(id);
    }
    
    // Soft delete exam
    public boolean deleteExam(Long id) {
        Optional<Exam> exam = examRepository.findById(id);
        if (exam.isPresent()) {
            exam.get().setActive(false);
            examRepository.save(exam.get());
            return true;
        }
        return false;
    }
    
    // Add marks for a student (updated for session support)
    public boolean addMarks(Long studentId, Long subjectId, Long examId, Integer obtainedMarks, Long teacherId) {
        Optional<Student> student = studentRepository.findById(studentId);
        Optional<Subject> subject = subjectRepository.findById(subjectId);
        Optional<Exam> exam = examRepository.findById(examId);
        Optional<Teacher> teacher = teacherRepository.findById(teacherId);
        
        if (student.isPresent() && subject.isPresent() && exam.isPresent() && teacher.isPresent()) {
            // Validate marks don't exceed max marks
            if (obtainedMarks > subject.get().getMaxMarks()) {
                return false;
            }
            
            // Check if marks already exist for this student-subject-exam combination
            Optional<Marks> existingMarks = marksRepository.findByStudentIdAndSubjectIdAndExamId(studentId, subjectId, examId);
            
            Marks marks;
            if (existingMarks.isPresent()) {
                // Update existing marks
                marks = existingMarks.get();
                marks.setObtainedMarks(obtainedMarks);
                marks.setExamDate(LocalDate.now());
                marks.setEnteredBy(teacher.get());
                marks.setEnteredDate(java.time.LocalDateTime.now());
            } else {
                // Create new marks entry
                marks = new Marks(student.get(), subject.get(), exam.get(), obtainedMarks, LocalDate.now());
                marks.setEnteredBy(teacher.get());
            }
            
            marksRepository.save(marks);
            return true;
        }
        return false;
    }
    
    // Get all active teachers
    public List<Teacher> getAllActiveTeachers() {
        return teacherRepository.findByActiveTrue();
    }
    
    // Get result by roll number (active session)
    public Optional<Result> getResultByRollNumber(String rollNumber) {
        Optional<Student> student = studentRepository.findByRollNumber(rollNumber);
        
        if (student.isPresent()) {
            List<Marks> marksList = marksRepository.findByStudentRollNumber(rollNumber);
            
            if (!marksList.isEmpty()) {
                Student s = student.get();
                Result result = new Result(s.getName(), s.getRollNumber(), s.getClassName(), marksList);
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }
    
    // Student promotion
    public boolean promoteStudents(List<Long> studentIds, Long targetSessionId) {
        Optional<Session> targetSession = sessionRepository.findById(targetSessionId);
        if (!targetSession.isPresent()) {
            return false;
        }
        
        for (Long studentId : studentIds) {
            Optional<Student> oldStudent = studentRepository.findById(studentId);
            if (oldStudent.isPresent()) {
                Student newStudent = new Student();
                newStudent.setName(oldStudent.get().getName());
                newStudent.setRollNumber(oldStudent.get().getRollNumber());
                newStudent.setClassName(getNextClass(oldStudent.get().getClassName()));
                newStudent.setSession(targetSession.get());
                studentRepository.save(newStudent);
            }
        }
        return true;
    }
    
    private String getNextClass(String currentClass) {
        // Simple promotion logic
        switch (currentClass) {
            case "Class 1": return "Class 2";
            case "Class 2": return "Class 3";
            case "Class 3": return "Class 4";
            case "Class 4": return "Class 5";
            case "Class 5": return "Class 6";
            case "Class 6": return "Class 7";
            case "Class 7": return "Class 8";
            case "Class 8": return "Class 9";
            case "Class 9": return "Class 10";
            case "Class 10": return "Class 11";
            case "Class 11": return "Class 12";
            default: return currentClass;
        }
    }
    
    // Student management
    public Student saveStudent(Student student) {
        if (student.getSession() == null) {
            Optional<Session> activeSession = getActiveSession();
            if (activeSession.isPresent()) {
                student.setSession(activeSession.get());
            }
        }
        return studentRepository.save(student);
    }
    
    public boolean deleteStudent(Long id) {
        Optional<Student> student = studentRepository.findById(id);
        if (student.isPresent()) {
            studentRepository.delete(student.get());
            return true;
        }
        return false;
    }
    
    public List<String> getAvailableClasses() {
        return getAllStudents().stream()
                .map(Student::getClassName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    // Class management
    public List<mh.cyb.root.rms.entity.Class> getAllClasses() {
        return classRepository.findAll();
    }
    
    public List<mh.cyb.root.rms.entity.Class> getAllActiveClasses() {
        return classRepository.findByActiveTrue();
    }
    
    public mh.cyb.root.rms.entity.Class saveClass(mh.cyb.root.rms.entity.Class classEntity) {
        return classRepository.save(classEntity);
    }
    
    public boolean deleteClass(Long id) {
        Optional<mh.cyb.root.rms.entity.Class> classEntity = classRepository.findById(id);
        if (classEntity.isPresent()) {
            classEntity.get().setActive(false);
            classRepository.save(classEntity.get());
            return true;
        }
        return false;
    }
    
    public Optional<mh.cyb.root.rms.entity.Class> getClassById(Long id) {
        return classRepository.findById(id);
    }
    
    // Subject management (additional methods)
    public Subject saveSubject(Subject subject) {
        return subjectRepository.save(subject);
    }
    
    public boolean deleteSubject(Long id) {
        Optional<Subject> subject = subjectRepository.findById(id);
        if (subject.isPresent()) {
            subjectRepository.delete(subject.get());
            return true;
        }
        return false;
    }
    
    public long getStudentCountByClass(String className) {
        return getAllStudents().stream()
                .filter(s -> s.getClassName().equals(className))
                .count();
    }
    
    // Get student by ID
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }
    
    // Get subject by ID
    public Optional<Subject> getSubjectById(Long id) {
        return subjectRepository.findById(id);
    }
}
