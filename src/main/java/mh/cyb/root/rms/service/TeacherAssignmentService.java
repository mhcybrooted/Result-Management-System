package mh.cyb.root.rms.service;

import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherAssignmentService {

    @Autowired
    private TeacherAssignmentRepository teacherAssignmentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Transactional
    public boolean assignSubjectToTeacher(Long teacherId, Long subjectId, Long sessionId) {
        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        Session session = sessionRepository.findById(sessionId).orElse(null);
        Subject subject = subjectRepository.findById(subjectId).orElse(null);

        if (teacher == null || session == null || subject == null) {
            return false;
        }

        // Check if this exact assignment already exists
        List<TeacherAssignment> existing = teacherAssignmentRepository
                .findByTeacherIdAndSessionIdAndActiveTrue(teacherId, sessionId);

        boolean assignmentExists = existing.stream()
                .anyMatch(assignment -> assignment.getSubject().getId().equals(subjectId));

        // Only create if assignment doesn't exist
        if (!assignmentExists) {
            TeacherAssignment assignment = new TeacherAssignment(teacher, subject, session);
            teacherAssignmentRepository.save(assignment);
            return true;
        }

        return false; // Assignment already exists
    }

    @Transactional
    public void assignSubjectsToTeacher(Long teacherId, List<Long> subjectIds, Long sessionId) {
        for (Long subjectId : subjectIds) {
            assignSubjectToTeacher(teacherId, subjectId, sessionId);
        }
    }

    public List<Subject> getAssignedSubjects(Long teacherId, Long sessionId) {
        return teacherAssignmentRepository.findByTeacherIdAndSessionIdAndActiveTrue(teacherId, sessionId)
                .stream()
                .map(TeacherAssignment::getSubject)
                .collect(Collectors.toList());
    }

    public List<TeacherAssignment> getAllActiveAssignments(Long sessionId) {
        // Get all assignments and filter by session and active status
        return teacherAssignmentRepository.findBySessionIdAndActiveTrue(sessionId);
    }

    @Transactional
    public void removeAssignment(Long assignmentId) {
        TeacherAssignment assignment = teacherAssignmentRepository.findById(assignmentId).orElse(null);
        if (assignment != null) {
            assignment.setActive(false);
            teacherAssignmentRepository.save(assignment);
        }
    }

    @Autowired
    private StudentRepository studentRepository;

    public List<Student> getStudentsForTeacher(Long teacherId, Long sessionId) {
        // 1. Get all assigned subjects
        List<TeacherAssignment> assignments = teacherAssignmentRepository
                .findByTeacherIdAndSessionIdAndActiveTrue(teacherId, sessionId);

        // 2. Extract unique classes from assigned subjects
        List<String> assignedClasses = assignments.stream()
                .map(assignment -> assignment.getSubject().getClassName()) // Assuming Subject has className string or
                                                                           // Class entity
                .distinct()
                .collect(Collectors.toList());

        if (assignedClasses.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 3. Get students for these classes (Optimized)
        return studentRepository.findBySessionIdAndClassNameInAndActiveTrue(sessionId, assignedClasses);
    }
}
