package mh.cyb.root.rms.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mh.cyb.root.rms.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private ActivityLogService activityLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String username = authentication.getName();
        String ipAddress = request.getRemoteAddr();

        activityLogService.logAction(
                "LOGIN_SUCCESS",
                "User logged in successfully.",
                username,
                ipAddress);

        // Redirect based on role (preserving existing logic logic)
        String targetUrl = "/admin/dashboard"; // Default to admin dashboard as that's the main entry

        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (isSuperAdmin) {
            targetUrl = "/admin/dashboard";
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
