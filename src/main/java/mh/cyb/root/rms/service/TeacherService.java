package mh.cyb.root.rms.service;

import mh.cyb.root.rms.entity.Teacher;
import mh.cyb.root.rms.entity.Subject;
import mh.cyb.root.rms.repository.TeacherRepository;
import mh.cyb.root.rms.repository.TeacherAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TeacherService {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherAssignmentRepository teacherAssignmentRepository;

    public List<Teacher> getAllActiveTeachers() {
        return teacherRepository.findByActiveTrue();
    }

    public org.springframework.data.domain.Page<Teacher> getAllActiveTeachers(
            org.springframework.data.domain.Pageable pageable) {
        return teacherRepository.findByActiveTrue(pageable);
    }

    public Teacher saveTeacher(Teacher teacher) {
        return teacherRepository.save(teacher);
    }

    public Teacher findById(Long id) {
        Optional<Teacher> teacher = teacherRepository.findById(id);
        return teacher.orElse(null);
    }

    public void deleteTeacher(Long id) {
        Teacher teacher = findById(id);
        if (teacher != null) {
            teacher.setActive(false);
            teacherRepository.save(teacher);
        }
    }

    public List<Subject> getTeacherSubjects(Long teacherId, Long sessionId) {
        return teacherAssignmentRepository.findByTeacherIdAndSessionIdAndActiveTrue(teacherId, sessionId)
                .stream()
                .map(assignment -> assignment.getSubject())
                .collect(Collectors.toList());
    }
}
