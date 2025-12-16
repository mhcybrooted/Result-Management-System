package mh.cyb.root.rms.service;

import mh.cyb.root.rms.dto.Result;
import mh.cyb.root.rms.dto.ResultBuilder;
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

    @Autowired
    private GradeCalculatorService gradeCalculatorService;

    // Session management
    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }

    public org.springframework.data.domain.Page<Session> getAllSessions(
            org.springframework.data.domain.Pageable pageable) {
        return sessionRepository.findAll(pageable);
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

    public org.springframework.data.domain.Page<Student> getAllStudents(
            org.springframework.data.domain.Pageable pageable) {
        return studentRepository.findByActiveSession(pageable);
    }

    // Get all subjects
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    public org.springframework.data.domain.Page<Subject> getAllSubjects(
            org.springframework.data.domain.Pageable pageable) {
        return subjectRepository.findAll(pageable);
    }

    // Get exams for active session
    public List<Exam> getAllActiveExams() {
        return examRepository.findByActiveTrue();
    }

    // Get all exams for active session
    // Get all exams for active session
    public List<Exam> getAllExams() {
        return examRepository.findByActiveSession();
    }

    public org.springframework.data.domain.Page<Exam> getAllExams(org.springframework.data.domain.Pageable pageable) {
        return examRepository.findByActiveSession(pageable);
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

    // Reactivate exam
    public boolean activateExam(Long id) {
        Optional<Exam> exam = examRepository.findById(id);
        if (exam.isPresent()) {
            exam.get().setActive(true);
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

            // Validate Exam is active
            if (!exam.get().getActive()) {
                return false;
            }

            // Check if marks already exist for this student-subject-exam combination
            Optional<Marks> existingMarks = marksRepository.findByStudentIdAndSubjectIdAndExamId(studentId, subjectId,
                    examId);

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
        // First find the student (assuming unique roll number in active session or
        // taking the first one found)
        // Ideally, this should find by Roll Number AND Active Session to be more
        // specific,
        // but studentRepository.findByRollNumber returns Optional<Student>, implying
        // unique roll no constraint or limit 1.
        Optional<Student> student = studentRepository.findByRollNumber(rollNumber);

        if (student.isPresent()) {
            Student s = student.get();
            // FIX: Query marks by Student ID and Session ID to avoid collisions with other
            // students having same roll no
            List<Marks> marksList = marksRepository.findByStudentIdAndSessionId(s.getId(), s.getSession().getId());

            if (!marksList.isEmpty()) {
                // Use new ResultBuilder to pass GradeCalculatorService
                Result result = ResultBuilder.buildResult(s.getName(), s.getRollNumber(),
                        s.getClassName(), marksList, gradeCalculatorService);
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    // Student promotion
    @Transactional
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
        // Robust promotion logic: Try to increment number, fallback to same class if
        // failed
        if (currentClass == null)
            return "Unknown Class";

        // Handle "Class X" patter
        if (currentClass.startsWith("Class ")) {
            try {
                int classNum = Integer.parseInt(currentClass.substring(6).trim());
                if (classNum < 12) {
                    return "Class " + (classNum + 1);
                } else {
                    return "Graduated";
                }
            } catch (NumberFormatException e) {
                // Ignore, fall through
            }
        }

        // Simple switch case as fallback for safety
        switch (currentClass) {
            case "Class 1":
                return "Class 2";
            case "Class 2":
                return "Class 3";
            case "Class 3":
                return "Class 4";
            case "Class 4":
                return "Class 5";
            case "Class 5":
                return "Class 6";
            case "Class 6":
                return "Class 7";
            case "Class 7":
                return "Class 8";
            case "Class 8":
                return "Class 9";
            case "Class 9":
                return "Class 10";
            case "Class 10":
                return "Class 11";
            case "Class 11":
                return "Class 12";
            default:
                return currentClass; // Stay in same class if unknown
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
        if (student.getActive() == null) {
            student.setActive(true);
        }
        return studentRepository.save(student);
    }

    public boolean deleteStudent(Long id) {
        Optional<Student> student = studentRepository.findById(id);
        if (student.isPresent()) {
            // Soft delete
            student.get().setActive(false);
            studentRepository.save(student.get());
            return true;
        }
        return false;
    }

    public List<String> getAvailableClasses() {
        return studentRepository.findDistinctClassNamesByActiveSession();
    }

    public org.springframework.data.domain.Page<Student> getStudentsForPromotion(String className, boolean onlyPassed,
            org.springframework.data.domain.Pageable pageable) {

        if (onlyPassed) {
            // Fetch ALL students (in specific class or all active)
            List<Student> candidates;
            if (className != null && !className.isEmpty()) {
                candidates = studentRepository.findBySessionIdAndClassNameInAndActiveTrue(
                        getActiveSession().get().getId(), List.of(className));
            } else {
                candidates = getAllStudents(); // Already scoped to Active Session
            }

            // In-Memory Filter for PASS
            List<Student> passedStudents = candidates.stream()
                    .filter(student -> {
                        // Efficient marks fetch
                        List<Marks> marks = marksRepository.findByStudentIdAndSessionId(student.getId(),
                                student.getSession().getId());
                        if (marks.isEmpty())
                            return false;

                        Result result = ResultBuilder.buildResult(student.getName(), student.getRollNumber(),
                                student.getClassName(), marks, gradeCalculatorService);
                        return "PASS".equalsIgnoreCase(result.getResult());
                    })
                    .collect(Collectors.toList());

            // Manual Pagination
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), passedStudents.size());
            List<Student> pageContent;
            if (start > passedStudents.size()) {
                pageContent = List.of();
            } else {
                pageContent = passedStudents.subList(start, end);
            }

            return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, passedStudents.size());

        } else {
            // Standard DB Pagination
            if (className != null && !className.isEmpty()) {
                return studentRepository.findByActiveSessionAndClassName(className, pageable);
            }
            return studentRepository.findByActiveSession(pageable);
        }
    }

    // Class management
    public List<mh.cyb.root.rms.entity.Class> getAllClasses() {
        return classRepository.findAll();
    }

    public org.springframework.data.domain.Page<mh.cyb.root.rms.entity.Class> getAllClasses(
            org.springframework.data.domain.Pageable pageable) {
        return classRepository.findAll(pageable);
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
