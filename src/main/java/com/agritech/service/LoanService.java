package com.agritech.service;

import com.agritech.model.Loan;
import com.agritech.model.LoanAffordability;

public class LoanService {
    private static final double PLANNING_EMI_RATIO = 0.35;

    public Loan create(String type, double principal, double rate, int months) {
        if (principal <= 0 || rate < 0 || months <= 0) {
            throw new IllegalArgumentException("Loan amount, rate aur tenure valid hone chahiye.");
        }
        return new Loan(type, principal, rate, months, 0);
    }

    public LoanAffordability assessAffordability(
            double monthlyFarmIncome,
            double otherMonthlyIncome,
            double existingMonthlyEmi,
            double requestedLoanAmount,
            double annualRate,
            int tenureMonths
    ) {
        if (monthlyFarmIncome < 0 || otherMonthlyIncome < 0 || existingMonthlyEmi < 0) {
            throw new IllegalArgumentException("Income aur existing EMI negative nahi ho sakte.");
        }
        if (requestedLoanAmount <= 0 || annualRate < 0 || tenureMonths <= 0) {
            throw new IllegalArgumentException("Requested loan, rate aur tenure valid hone chahiye.");
        }

        double totalIncome = monthlyFarmIncome + otherMonthlyIncome;
        if (totalIncome <= 0) {
            throw new IllegalArgumentException("Affordability estimate ke liye monthly income required hai.");
        }

        double safeTotalEmi = totalIncome * PLANNING_EMI_RATIO;
        double safeNewEmi = Math.max(0, safeTotalEmi - existingMonthlyEmi);

        Loan requested = create("Affordability estimate", requestedLoanAmount, annualRate, tenureMonths);
        double requestedEmi = requested.calculateEmi();
        double maxLoan = principalFromEmi(safeNewEmi, annualRate, tenureMonths);

        String status;
        if (safeNewEmi <= 0 || requestedEmi > safeNewEmi) {
            status = "HIGH PRESSURE";
        } else if (requestedEmi > safeNewEmi * 0.80) {
            status = "CAUTION";
        } else {
            status = "COMFORTABLE";
        }

        String note = "Planning rule: entered monthly income ka 35% tak total EMI cap use kiya gaya hai. "
                + "Ye bank eligibility, approval ya credit decision nahi hai.";

        return new LoanAffordability(
                totalIncome,
                existingMonthlyEmi,
                safeTotalEmi,
                safeNewEmi,
                requestedEmi,
                maxLoan,
                status,
                note
        );
    }

    private double principalFromEmi(double emi, double annualRate, int months) {
        if (emi <= 0) return 0;
        double r = annualRate / 12.0 / 100.0;
        if (r == 0) return emi * months;
        double factor = Math.pow(1 + r, months);
        return emi * (factor - 1) / (r * factor);
    }
}
