package mh.cyb.root.rms.repository;

import mh.cyb.root.rms.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    // Find recent logs, ordered by newest first
    List<ActivityLog> findAllByOrderByTimestampDesc();
}
