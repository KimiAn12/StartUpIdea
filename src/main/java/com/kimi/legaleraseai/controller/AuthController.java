package com.kimi.legaleraseai.controller;

import com.kimi.legaleraseai.dto.*;
import com.kimi.legaleraseai.entity.User;
import com.kimi.legaleraseai.repository.UserRepository;
import com.kimi.legaleraseai.security.JwtUtils;
import com.kimi.legaleraseai.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthRequest loginRequest) {
        System.out.println("=== LOGIN ATTEMPT DEBUG ===");
        System.out.println("Username/Email: " + loginRequest.getUsernameOrEmail());
        System.out.println("Password length: " + (loginRequest.getPassword() != null ? loginRequest.getPassword().length() : "null"));
        
        try {
            // Check if user exists first
            var userOpt = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("User found: ID=" + user.getId() + ", Username=" + user.getUsername() + ", Email=" + user.getEmail());
                System.out.println("Stored password hash: " + user.getPassword());
                
                // Test password encoding
                boolean passwordMatches = encoder.matches(loginRequest.getPassword(), user.getPassword());
                System.out.println("Password matches: " + passwordMatches);
                
                if (!passwordMatches) {
                    System.out.println("=== PASSWORD MISMATCH - LOGIN FAILED ===");
                    return ResponseEntity.status(401).body(new MessageResponse("Invalid credentials"));
                }
            } else {
                System.out.println("User NOT found with username/email: " + loginRequest.getUsernameOrEmail());
                return ResponseEntity.status(401).body(new MessageResponse("User not found"));
            }
            
            System.out.println("About to call AuthenticationManager.authenticate()...");
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(), 
                                                           loginRequest.getPassword()));

            System.out.println("Authentication successful, setting security context...");
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            System.out.println("Generating JWT token...");
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
            System.out.println("Authentication successful for user: " + userDetails.getUsername());
            System.out.println("=== LOGIN SUCCESS ===");

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getAuthorities().iterator().next().getAuthority()));
        } catch (Exception e) {
            System.out.println("=== LOGIN ERROR ===");
            System.out.println("Error type: " + e.getClass().getSimpleName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).body(new MessageResponse("Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        System.out.println("=== SIGNUP ATTEMPT DEBUG ===");
        System.out.println("Username: " + signUpRequest.getUsername());
        System.out.println("Email: " + signUpRequest.getEmail());
        System.out.println("Password length: " + (signUpRequest.getPassword() != null ? signUpRequest.getPassword().length() : "null"));
        
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            System.out.println("Username already exists: " + signUpRequest.getUsername());
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            System.out.println("Email already exists: " + signUpRequest.getEmail());
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        String encodedPassword = encoder.encode(signUpRequest.getPassword());
        System.out.println("Original password length: " + signUpRequest.getPassword().length());
        System.out.println("Encoded password hash: " + encodedPassword);
        
        User user = new User(signUpRequest.getUsername(),
                           signUpRequest.getEmail(),
                           encodedPassword);

        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setRole(User.Role.USER);

        User savedUser = userRepository.save(user);
        System.out.println("User saved with ID: " + savedUser.getId());
        System.out.println("=== SIGNUP SUCCESS ===");

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body(new MessageResponse("User not authenticated"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCreatedAt()
        ));
    }
}
