package com.aztechsynergy.crickScore.controllers;

import com.aztechsynergy.crickScore.message.request.LoginForm;
import com.aztechsynergy.crickScore.message.request.SignUpForm;
import com.aztechsynergy.crickScore.message.response.JwtResponse;
import com.aztechsynergy.crickScore.model.Role;
import com.aztechsynergy.crickScore.model.RoleName;
import com.aztechsynergy.crickScore.model.User;
import com.aztechsynergy.crickScore.repository.RoleRepository;
import com.aztechsynergy.crickScore.repository.UserRepository;
import com.aztechsynergy.crickScore.security.jwt.JwtProvider;
import com.aztechsynergy.crickScore.security.services.UserPrinciple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 86400)
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtProvider jwtProvider;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginForm loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword().equals("") ? loginRequest.getUsername() : loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtProvider.generateJwtToken(authentication);
        HttpHeaders headers = new HttpHeaders();
        headers.add("name", ((UserPrinciple) authentication.getPrincipal()).getName() );
        headers.add("Access-Control-Expose-Headers" , "name");
        return new ResponseEntity<>(new JwtResponse(jwt), headers, HttpStatus.OK);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpForm signUpRequest) {
        if(userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.ok(false);
        }

       /* if(signUpRequest.getRole().contains("user") && (Objects.isNull(signUpRequest.getMatchNumber()) || signUpRequest.getMatchNumber().equals(""))){
            return ResponseEntity.badRequest().body("Match Number missing");
        }
        if(userRepository.existsByEmail(signUpRequest.getEmail())) {
            return new ResponseEntity<String>("Fail -> Email is already in use!",
                    HttpStatus.BAD_REQUEST);
        }*/

        // Creating user's account
        User user = User.builder()
                .username(signUpRequest.getUsername())
                .name(signUpRequest.getName())
                .email(signUpRequest.getEmail())
                .password(encoder.encode(signUpRequest.getPassword()))
                .matchNumber(signUpRequest.getMatchNumber())
                .phone(signUpRequest.getPhone())
                .build();

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        strRoles.forEach(role -> {
        	switch(role) {
	    		case "admin":
	    			Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
	                .orElseThrow(() -> new RuntimeException("Fail! -> Cause: User Role not find."));
	    			roles.add(adminRole);
	    			
	    			break;
	    		case "pm":
	            	Role pmRole = roleRepository.findByName(RoleName.ROLE_PM)
	                .orElseThrow(() -> new RuntimeException("Fail! -> Cause: User Role not find."));
	            	roles.add(pmRole);
	            	
	    			break;
	    		default:
	        		Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
	                .orElseThrow(() -> new RuntimeException("Fail! -> Cause: User Role not find."));
	        		roles.add(userRole);        			
        	}
        });
        
        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(true);
    }
    @PostMapping("/signUpGamers")
    public ResponseEntity<?> signUpGamers(@Valid @RequestBody List<SignUpForm> signUpRequests) {
        List<User> users = new ArrayList<>();
        Optional<Role> userRole = roleRepository.findByName(RoleName.ROLE_USER);
        // Creating user's account
        for(SignUpForm signUpRequest : signUpRequests) {
            User user = User.builder()
                    .username(signUpRequest.getUsername())
                    .name(signUpRequest.getName())
                    .password(encoder.encode(signUpRequest.getUsername()))
                    .build();
            Set<String> strRoles = signUpRequest.getRole();
            Set<Role> roles = new HashSet<>();
            roles.add(userRole.orElseThrow(null));
            user.setRoles(roles);
            users.add(user);
        }
        userRepository.saveAll(users);
        return ResponseEntity.ok(true);
    }
    @GetMapping("/test")
    public ResponseEntity<?> test() {

        return ResponseEntity.ok("Test");
    }
}