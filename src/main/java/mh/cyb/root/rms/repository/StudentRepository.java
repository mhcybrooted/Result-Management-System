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

        @Query("SELECT s FROM Student s WHERE s.rollNumber = :rollNumber AND s.session.active = true AND s.active = true")
        Optional<Student> findByRollNumber(@Param("rollNumber") String rollNumber);

        @Query("SELECT s FROM Student s WHERE s.session.active = true AND s.active = true")
        List<Student> findByActiveSession();

        @Query("SELECT s FROM Student s WHERE s.session.id = :sessionId AND s.active = true")
        List<Student> findBySessionId(@Param("sessionId") Long sessionId);

        @Query("SELECT s FROM Student s WHERE s.session.active = true AND s.active = true")
        org.springframework.data.domain.Page<Student> findByActiveSession(
                        org.springframework.data.domain.Pageable pageable);

        @Query("SELECT s FROM Student s WHERE s.session.id = :sessionId AND s.className IN :classNames AND s.active = true")
        List<Student> findBySessionIdAndClassNameInAndActiveTrue(@Param("sessionId") Long sessionId,
                        @Param("classNames") List<String> classNames);

        @Query("SELECT DISTINCT s.className FROM Student s WHERE s.session.active = true AND s.active = true ORDER BY s.className")
        List<String> findDistinctClassNamesByActiveSession();

        @Query("SELECT s FROM Student s WHERE s.session.active = true AND s.className = :className AND s.active = true")
        org.springframework.data.domain.Page<Student> findByActiveSessionAndClassName(
                        @Param("className") String className,
                        org.springframework.data.domain.Pageable pageable);

        @Query("SELECT s FROM Student s WHERE s.session.active = true AND s.active = true AND " +
                        "(:className IS NULL OR s.className = :className) AND " +
                        "(:search IS NULL OR LOWER(CAST(s.name AS string)) LIKE :search OR LOWER(s.rollNumber) LIKE :search)")
        org.springframework.data.domain.Page<Student> searchStudents(@Param("className") String className,
                        @Param("search") String search, org.springframework.data.domain.Pageable pageable);
}
