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
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(adminUser.getUsername())
                .password(adminUser.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .accountExpired(false)
                .accountLocked(!adminUser.getActive())
                .credentialsExpired(false)
                .disabled(!adminUser.getActive())
                .build();
    }
    
    public AdminUser createAdmin(String username, String password) {
        AdminUser adminUser = new AdminUser(username, passwordEncoder.encode(password));
        return adminUserRepository.save(adminUser);
    }
    
    public boolean existsByUsername(String username) {
        return adminUserRepository.existsByUsername(username);
    }
}
