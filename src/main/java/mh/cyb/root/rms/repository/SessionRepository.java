package mh.cyb.root.rms.repository;

import mh.cyb.root.rms.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    Optional<Session> findByActiveTrue();
    
    @Modifying
    @Query("UPDATE Session s SET s.active = false")
    void deactivateAllSessions();
    
    @Modifying
    @Query("UPDATE Session s SET s.active = true WHERE s.id = :id")
    void activateSession(@Param("id") Long id);
}
