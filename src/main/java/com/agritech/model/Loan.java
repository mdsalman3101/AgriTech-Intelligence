package com.agritech.model;

public class Loan {
    private final String type;
    private final double principal;
    private final double annualInterestRate;
    private final int tenureMonths;
    private final int paidMonths;

    public Loan(String type, double principal, double annualInterestRate,
                int tenureMonths, int paidMonths) {
        this.type = type;
        this.principal = principal;
        this.annualInterestRate = annualInterestRate;
        this.tenureMonths = tenureMonths;
        this.paidMonths = paidMonths;
    }

    public String getType() { return type; }
    public double getPrincipal() { return principal; }

    public double calculateEmi() {
        double r = annualInterestRate / 12.0 / 100.0;
        if (r == 0) return principal / tenureMonths;
        double factor = Math.pow(1 + r, tenureMonths);
        return principal * r * factor / (factor - 1);
    }

    public double estimatedRemainingAmount() {
        return calculateEmi() * Math.max(0, tenureMonths - paidMonths);
    }
    public double getAnnualInterestRate(){return annualInterestRate;}public int getTenureMonths(){return tenureMonths;}public double totalPayable(){return calculateEmi()*tenureMonths;}public double totalInterest(){return totalPayable()-principal;}
}
