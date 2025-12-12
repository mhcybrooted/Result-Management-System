package mh.cyb.root.rms.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mh.cyb.root.rms.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    @Autowired
    private ActivityLogService activityLogService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        if (authentication != null) {
            String username = authentication.getName();
            String ipAddress = request.getRemoteAddr();

            activityLogService.logAction(
                    "LOGOUT",
                    "User logged out successfully.",
                    username,
                    ipAddress);
        }

        super.setDefaultTargetUrl("/login?logout");
        super.onLogoutSuccess(request, response, authentication);
    }
}
