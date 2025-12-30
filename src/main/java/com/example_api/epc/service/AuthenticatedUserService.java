package com.example_api.epc.service;

import com.example_api.epc.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserService {

    public User getCurrentUser() {

        Authentication auth =
                        SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Usuário não autenticado");
        }

        Object principal = auth.getPrincipal();

        if (!(principal instanceof User)) {
            throw new RuntimeException("Principal inválido");
        }

        return (User) principal;
    }
}

