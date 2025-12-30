package com.example_api.epc.service.impl;

import com.example_api.epc.dto.RegisterRequest;
import com.example_api.epc.entity.User;
import com.example_api.epc.exception.EmailAlreadyExistsException;
import com.example_api.epc.repository.UserRepository;
import com.example_api.epc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(RegisterRequest req) {

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new EmailAlreadyExistsException("Este email já esta cadastrado");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole("USER");
        user.setStatus("ACTIVE");

        return userRepository.save(user);
    }

    @Override
    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Senha inválida");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Usuário bloqueado");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return user;

    }

}
