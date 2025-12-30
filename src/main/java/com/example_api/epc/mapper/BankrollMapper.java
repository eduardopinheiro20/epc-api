package com.example_api.epc.mapper;

import com.example_api.epc.dto.BankrollDto;
import com.example_api.epc.entity.Bankroll;

public class BankrollMapper {

    public static BankrollDto toDto(Bankroll b) {
        return new BankrollDto(
                        b.getId(),
                        b.getName(),
                        b.getInitialAmount(),
                        b.getCurrentAmount(),
                        b.getStatus(),
                        b.getCreatedAt(),
                        b.getUpdatedAt()
        );
    }
}

