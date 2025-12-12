package mh.cyb.root.rms.repository;

import mh.cyb.root.rms.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE s.rollNumber = :rollNumber AND s.session.active = true")
    Optional<Student> findByRollNumber(@Param("rollNumber") String rollNumber);

    @Query("SELECT s FROM Student s WHERE s.session.active = true")
    List<Student> findByActiveSession();

    @Query("SELECT s FROM Student s WHERE s.session.id = :sessionId")
    List<Student> findBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT s FROM Student s WHERE s.session.active = true")
    org.springframework.data.domain.Page<Student> findByActiveSession(
            org.springframework.data.domain.Pageable pageable);
}
