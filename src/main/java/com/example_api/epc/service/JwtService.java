package com.example_api.epc.service;

import com.example_api.epc.entity.User;

public interface JwtService {
    String generateToken(User user);
    String extractEmail(String token);
}
