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
    public void assignSubjectsToTeacher(Long teacherId, List<Long> subjectIds, Long sessionId) {
        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        Session session = sessionRepository.findById(sessionId).orElse(null);
        
        if (teacher == null || session == null) return;
        
        // Deactivate existing assignments for this teacher in this session
        List<TeacherAssignment> existing = teacherAssignmentRepository
                .findByTeacherIdAndSessionIdAndActiveTrue(teacherId, sessionId);
        existing.forEach(assignment -> assignment.setActive(false));
        teacherAssignmentRepository.saveAll(existing);
        
        // Create new assignments
        for (Long subjectId : subjectIds) {
            Subject subject = subjectRepository.findById(subjectId).orElse(null);
            if (subject != null) {
                TeacherAssignment assignment = new TeacherAssignment(teacher, subject, session);
                teacherAssignmentRepository.save(assignment);
            }
        }
    }
    
    public List<Subject> getAssignedSubjects(Long teacherId, Long sessionId) {
        return teacherAssignmentRepository.findByTeacherIdAndSessionIdAndActiveTrue(teacherId, sessionId)
                .stream()
                .map(TeacherAssignment::getSubject)
                .collect(Collectors.toList());
    }
}
