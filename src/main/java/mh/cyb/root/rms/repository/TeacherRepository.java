package mh.cyb.root.rms.repository;

import mh.cyb.root.rms.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    List<Teacher> findByActiveTrue();

    org.springframework.data.domain.Page<Teacher> findByActiveTrue(org.springframework.data.domain.Pageable pageable);
}
