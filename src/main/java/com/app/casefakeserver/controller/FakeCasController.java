package com.app.casefakeserver.controller;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.*;

@Controller
@RequestMapping("/cas")
public class FakeCasController {
    private final Key key = Keys.hmacShaKeyFor("MySuperSecretKeyMySuperSecretKey".getBytes());
    private final Set<String> validTokens = new HashSet<>();
    private final Map<String, String> serviceTickets = new HashMap<>();

    @GetMapping("/login")
    public String loginPage(@RequestParam String service, Model model) {
        model.addAttribute("service", service);
        return "login";
    }

    @PostMapping("/login")
    public ResponseEntity<Void> loginSubmit(@RequestParam String service,
                                            @RequestParam String username,
                                            @RequestParam String password) {
        // For testing purposes, accept any credentials
        String ticket = "ST-" + UUID.randomUUID();
        serviceTickets.put(ticket, username);
        URI redirect = URI.create(service + "?ticket=" + ticket);

        System.out.println("Url is "+redirect.getPath().toString());
        return ResponseEntity.status(HttpStatus.FOUND).location(redirect).build();
    }

//    @GetMapping("/serviceValidate")
//    @ResponseBody
//    public String serviceValidate(@RequestParam String service,
//                                  @RequestParam String ticket) {
//        if (serviceTickets.containsKey(ticket)) {
//            String user = serviceTickets.get(ticket);
//            return "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>"
//                    + "<cas:authenticationSuccess>"
//                    + "<cas:user>" + user + "</cas:user>"
//                    + "</cas:authenticationSuccess>"
//                    + "</cas:serviceResponse>";
//        } else {
//            return "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>"
//                    + "<cas:authenticationFailure code='INVALID_TICKET'>"
//                    + "Invalid ticket " + ticket +
//                    "</cas:authenticationFailure>"
//                    + "</cas:serviceResponse>";
//        }
//    }

    @GetMapping("/validate")
    public String validatePage(@RequestParam(required = false) String service,
                               @RequestParam(required = false) String ticket,
                               Model model) {
        System.out.println("Ticket is "+ticket);

        if (service != null && ticket != null) {
            String result;
            if (serviceTickets.containsKey(ticket)) {
                String user = serviceTickets.get(ticket);
                String jwt = Jwts.builder()
                        .setSubject(user)
                        .claim("service", service)
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + 3600_000)) // 1 hour
                        .signWith(key, SignatureAlgorithm.HS256)
                        .compact();
                validTokens.add(jwt); // track issued token
                System.out.println("jwt is "+jwt);

                result = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>" +
                        "<cas:authenticationSuccess>" +
                        "<cas:user>" + "User: "+user +"JWT: "+jwt+ "</cas:user>" +
                        "</cas:authenticationSuccess>" +
                        "</cas:serviceResponse>";
                model.addAttribute("status", "ACTIVE");
                model.addAttribute("user", user);
                model.addAttribute("token", jwt);
                return "session-status";
            } else {
                result = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>" +
                        "<cas:authenticationFailure code='INVALID_TICKET'>" +
                        "Ticket " + ticket + " not recognized" +
                        "</cas:authenticationFailure>" +
                        "</cas:serviceResponse>";
            }
            model.addAttribute("result", result);
        } else if(ticket != null) {
            model.addAttribute("ticket", ticket);
        }
        return "validate";
    }

    @GetMapping("/session-status")
    public String sessionStatus(@RequestParam(required = false) String token,
                                Model model) {
        if (token == null || token.isEmpty() || !validTokens.contains(token)) {
            model.addAttribute("status", "INACTIVE");
            model.addAttribute("message", "No valid session");
            return "session-status";
        }

        try {
            var claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            String user = claims.getBody().getSubject();
            model.addAttribute("status", "ACTIVE");
            model.addAttribute("user", user);
            model.addAttribute("token", token);
            model.addAttribute("message", "Token is valid");
        } catch (JwtException e) {
            model.addAttribute("status", "INACTIVE");
            model.addAttribute("message", "Invalid or expired token");
        }

        return "session-status";
    }

    @PostMapping("/logout")
    public String logout(@RequestParam String token, Model model) {
        validTokens.remove(token);
        model.addAttribute("status", "INACTIVE");
        model.addAttribute("message", "You have been logged out.");
        return "session-status";
    }
}
