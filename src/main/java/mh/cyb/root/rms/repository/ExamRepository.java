package mh.cyb.root.rms.repository;

import mh.cyb.root.rms.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    
    @Query("SELECT e FROM Exam e WHERE e.active = true AND e.session.active = true")
    List<Exam> findByActiveTrue();
    
    @Query("SELECT e FROM Exam e WHERE e.session.active = true")
    List<Exam> findByActiveSession();
}
