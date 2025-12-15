package mh.cyb.root.rms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @org.springframework.beans.factory.annotation.Autowired
        private CustomAuthenticationSuccessHandler successHandler;

        @org.springframework.beans.factory.annotation.Autowired
        private CustomAuthenticationFailureHandler failureHandler;

        @org.springframework.beans.factory.annotation.Autowired
        private CustomLogoutSuccessHandler logoutSuccessHandler;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers("/", "/admin-login", "/student-login",
                                                                "/teacher-login", "/login-portal",
                                                                "/developer", "/css/**", "/js/**", "/images/**")
                                                .permitAll()
                                                .requestMatchers("/h2-console/**").permitAll()
                                                .requestMatchers("/view-results", "/search-results").permitAll()

                                                // Role-Based Access Control
                                                .requestMatchers("/admin/delete/**", "/exams/delete/**",
                                                                "/students/delete/**", "/classes/delete/**",
                                                                "/subjects/delete/**")
                                                .hasRole("SUPER_ADMIN")
                                                .requestMatchers("/sessions/**", "/classes/**").hasRole("SUPER_ADMIN")

                                                // Shared Access
                                                .requestMatchers("/add-marks", "/students/**", "/exams/**",
                                                                "/subjects/**", "/bulk/**", "/reports/**")
                                                .hasAnyRole("SUPER_ADMIN", "TEACHER")
                                                .requestMatchers("/admin/dashboard", "/admin/activity-logs")
                                                .hasAnyRole("SUPER_ADMIN", "TEACHER")

                                                .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                                                .anyRequest().hasRole("SUPER_ADMIN"))
                                .formLogin(form -> form
                                                .loginPage("/admin-login")
                                                .successHandler(successHandler)
                                                .failureHandler(failureHandler)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/admin-logout")
                                                .logoutSuccessHandler(logoutSuccessHandler)
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                // CSRF Enabled by default (removed disable())
                                .headers(headers -> headers.frameOptions().disable());

                return http.build();
        }
}
