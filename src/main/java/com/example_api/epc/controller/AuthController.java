package com.example_api.epc.controller;

import com.example_api.epc.dto.AuthResponse;
import com.example_api.epc.dto.LoginRequest;
import com.example_api.epc.dto.RegisterRequest;
import com.example_api.epc.entity.User;
import com.example_api.epc.service.JwtService;
import com.example_api.epc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        userService.register(req);
        return ResponseEntity.ok(
                        Map.of("message", "Usuário criado com sucesso")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {

        User user = userService.authenticate(req.getEmail(), req.getPassword());
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(token));
    }
}
