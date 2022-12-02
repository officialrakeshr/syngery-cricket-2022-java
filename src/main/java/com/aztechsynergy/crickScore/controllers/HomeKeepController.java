package com.aztechsynergy.crickScore.controllers;

import com.aztechsynergy.crickScore.model.Tournament;
import com.aztechsynergy.crickScore.model.User;
import com.aztechsynergy.crickScore.repository.TournamentRepository;
import com.aztechsynergy.crickScore.repository.UserRepository;
import com.aztechsynergy.crickScore.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class HomeKeepController {
    private static final Random generator = new Random();
    static final String SOURCE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static SecureRandom secureRnd = new SecureRandom();

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @GetMapping("/api/homekeep/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean adminAccess() {
        return true;
    }

    @GetMapping("/api/homekeep/employee")
    @PreAuthorize("hasRole('USER')")
    public Boolean employeeAccess() {
        return true;
    }

    @GetMapping("/api/homekeep/hasAdminRole")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PM')")
    public Boolean hasAdminRoles() {
        return true;
    }

    @GetMapping("/api/homekeep/superuser")
    @PreAuthorize("hasRole('PM')")
    public Boolean suAccess() {
        return true;
    }

    @GetMapping("/api/homekeep/userMatchAvailable")
    @PreAuthorize("hasRole('USER')")
    public Boolean userMatchAvailable(@RequestHeader(HttpHeaders.AUTHORIZATION) String bear) {
        Optional<User> user = findGuestByToken(bear);
        if(user.isPresent()){
            Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(user.get().getMatchNumber());
            if(Objects.nonNull(tournament)){
                return tournament.getEnable11();
            }
        }
        return false;
    }

    @GetMapping("/api/homekeep/isUniqueIdAvailable/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean isUniqueIdAvailable(@PathVariable("code") String code) {
        Optional<User> user = userRepository.findByUsername(code);
        return !user.isPresent();
    }

    @GetMapping("/api/homekeep/userMatchDetails")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> userMatchDetails(@RequestHeader(HttpHeaders.AUTHORIZATION) String bear) {
        Optional<User> user = findGuestByToken(bear);
        if(user.isPresent()){
            Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(user.get().getMatchNumber());
            if(Objects.nonNull(tournament)){
                return ResponseEntity.ok(tournament);
            }
        }
        return ResponseEntity.ok(null);
    }
    public Optional<User> findGuestByToken(String bear){
        return getGuests(bear, jwtProvider);
    }

    private Optional<User> getGuests(String bear, JwtProvider jwtProvider) {
        if(bear.equals("")) return Optional.empty();
        String token = bear.replaceAll("Bearer ","").trim();
        String user = jwtProvider.getUserNameFromJwtToken(token);
        return userRepository.findByUsername(user);
    }

    public static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(SOURCE.charAt(secureRnd.nextInt(SOURCE.length())));
        return sb.toString();
    }

}