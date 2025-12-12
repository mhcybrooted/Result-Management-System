package mh.cyb.root.rms.service;

import mh.cyb.root.rms.entity.AdminUser;
import mh.cyb.root.rms.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class AdminUserService implements UserDetailsService {

    @Autowired
    private AdminUserRepository adminUserRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin user not found: " + username));

        // Dynamically assign role based on DB value
        String roleName = "ROLE_" + (adminUser.getRole() != null ? adminUser.getRole() : "SUPER_ADMIN");

        return org.springframework.security.core.userdetails.User.builder()
                .username(adminUser.getUsername())
                .password(adminUser.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(roleName)))
                .accountExpired(false)
                .accountLocked(!adminUser.getActive())
                .credentialsExpired(false)
                .disabled(!adminUser.getActive())
                .build();
    }

    public AdminUser createAdmin(String username, String password, String role, Long teacherId) {
        AdminUser adminUser = new AdminUser(username, passwordEncoder.encode(password));
        adminUser.setRole(role != null ? role : "SUPER_ADMIN");
        adminUser.setTeacherId(teacherId);
        return adminUserRepository.save(adminUser);
    }

    // Overload for backward compatibility
    public AdminUser createAdmin(String username, String password) {
        return createAdmin(username, password, "SUPER_ADMIN", null);
    }

    public boolean existsByUsername(String username) {
        return adminUserRepository.existsByUsername(username);
    }

    public java.util.List<AdminUser> getAllAdminUsers() {
        return adminUserRepository.findAll();
    }

    public void deleteAdminUser(Long id) {
        adminUserRepository.deleteById(id);
    }
}
