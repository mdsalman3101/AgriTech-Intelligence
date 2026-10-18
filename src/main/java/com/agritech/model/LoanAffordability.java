package com.agritech.model;

public record LoanAffordability(
        double monthlyIncome,
        double existingEmi,
        double safeTotalEmi,
        double safeNewEmi,
        double requestedLoanEmi,
        double estimatedMaxAffordableLoan,
        String status,
        String note
) {}
