package com.example_api.epc.service;

import com.example_api.epc.dto.RegisterRequest;
import com.example_api.epc.entity.User;

public interface UserService {

    User register(RegisterRequest req);

    User authenticate(String email, String rawPassword);
}
