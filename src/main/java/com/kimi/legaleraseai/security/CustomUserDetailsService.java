package com.kimi.legaleraseai.security;

import com.kimi.legaleraseai.entity.User;
import com.kimi.legaleraseai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        System.out.println("=== CustomUserDetailsService.loadUserByUsername ===");
        System.out.println("Looking for user with username/email: " + usernameOrEmail);
        
        try {
            User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                    .orElseThrow(() -> {
                        System.out.println("User NOT found in database");
                        return new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
                    });

            System.out.println("User found: ID=" + user.getId() + ", Username=" + user.getUsername() + ", Email=" + user.getEmail());
            System.out.println("User role: " + user.getRole());
            System.out.println("Password hash: " + user.getPassword());
            
            UserDetails userDetails = UserPrincipal.create(user);
            System.out.println("UserDetails created successfully with authorities: " + userDetails.getAuthorities());
            System.out.println("=== CustomUserDetailsService SUCCESS ===");
            
            return userDetails;
        } catch (Exception e) {
            System.out.println("=== CustomUserDetailsService ERROR ===");
            System.out.println("Error type: " + e.getClass().getSimpleName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new UsernameNotFoundException("User not found with id: " + id)
        );

        return UserPrincipal.create(user);
    }
}
