package mh.cyb.root.rms.repository;

import mh.cyb.root.rms.entity.Marks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarksRepository extends JpaRepository<Marks, Long> {
    
    @Query("SELECT m FROM Marks m JOIN FETCH m.student JOIN FETCH m.subject JOIN FETCH m.exam WHERE m.student.rollNumber = :rollNumber AND m.student.session.active = true")
    List<Marks> findByStudentRollNumber(@Param("rollNumber") String rollNumber);
    
    Optional<Marks> findByStudentIdAndSubjectIdAndExamId(Long studentId, Long subjectId, Long examId);
}
