package mh.cyb.root.rms.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mh.cyb.root.rms.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private ActivityLogService activityLogService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        String ipAddress = request.getRemoteAddr();

        activityLogService.logAction(
                "LOGIN_FAILURE",
                "Failed login attempt. Reason: " + exception.getMessage(),
                username != null ? username : "Unknown",
                ipAddress);

        super.setDefaultFailureUrl("/login?error=true");
        super.onAuthenticationFailure(request, response, exception);
    }
}
