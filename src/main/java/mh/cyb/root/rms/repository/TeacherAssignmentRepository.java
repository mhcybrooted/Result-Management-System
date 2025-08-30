package mh.cyb.root.rms.repository;

import mh.cyb.root.rms.entity.TeacherAssignment;
import mh.cyb.root.rms.entity.Teacher;
import mh.cyb.root.rms.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {
    List<TeacherAssignment> findByTeacherAndSessionAndActiveTrue(Teacher teacher, Session session);
    List<TeacherAssignment> findByTeacherIdAndSessionIdAndActiveTrue(Long teacherId, Long sessionId);
}
