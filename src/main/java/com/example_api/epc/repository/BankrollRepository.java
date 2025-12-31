package com.example_api.epc.repository;

import com.example_api.epc.entity.Bankroll;
import com.example_api.epc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BankrollRepository extends JpaRepository<Bankroll, Long> {

    @Query("SELECT b FROM Bankroll b WHERE b.status = 'ACTIVE'")
    Optional<Bankroll> findActiveBankroll();

    Optional<Bankroll> findById(Long id);

    Bankroll findByStatus(String status);

    Optional<Bankroll> findByUserAndStatus(User pUser, String pACTIVE);
}
