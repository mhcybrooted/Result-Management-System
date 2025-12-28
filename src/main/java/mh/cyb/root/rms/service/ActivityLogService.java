package mh.cyb.root.rms.service;

import mh.cyb.root.rms.entity.ActivityLog;
import mh.cyb.root.rms.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    public void logAction(String action, String details, String username, String ipAddress) {
        try {
            ActivityLog log = new ActivityLog(action, details, username, ipAddress);
            activityLogRepository.save(log);
        } catch (Exception e) {
            // Silently fail to ensure logging doesn't break main flow
            System.err.println("Failed to save activity log: " + e.getMessage());
        }
    }

    public List<ActivityLog> getAllLogs() {
        return activityLogRepository.findAllByOrderByTimestampDesc();
    }
}
