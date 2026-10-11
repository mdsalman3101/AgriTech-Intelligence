package com.agritech.service;

import com.agritech.model.Loan;
import java.util.List;

public class LoanService {
    private final List<Loan> demoLoans;

    public LoanService(List<Loan> demoLoans) {
        this.demoLoans = demoLoans;
    }

    public List<Loan> getDemoLoans() { return List.copyOf(demoLoans); }

    public Loan create(String type, double principal, double rate, int months) {
        if (principal <= 0 || rate < 0 || months <= 0)
            throw new IllegalArgumentException("Invalid loan data.");
        return new Loan(type, principal, rate, months, 0);
    }
}
