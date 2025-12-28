package mh.cyb.root.rms.repository;

import mh.cyb.root.rms.entity.Attendance;
import mh.cyb.root.rms.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Find specific record
    Optional<Attendance> findByStudentAndDate(Student student, LocalDate date);

    // Get daily attendance for a list of students (e.g. whole class)
    List<Attendance> findByDateAndStudentIn(LocalDate date, List<Student> students);

    // Get monthly attendance for a student
    List<Attendance> findByStudentAndDateBetween(Student student, LocalDate startDate, LocalDate endDate);

    // Count present days
    long countByStudentAndSessionIdAndStatus(Student student, Long sessionId,
            mh.cyb.root.rms.entity.AttendanceStatus status);

    // Find all records for a student in a session ordered by date
    List<Attendance> findByStudentAndSessionOrderByDateDesc(Student student, mh.cyb.root.rms.entity.Session session);
}
