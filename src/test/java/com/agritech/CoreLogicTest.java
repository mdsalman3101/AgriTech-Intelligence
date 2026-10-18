package com.agritech;

import com.agritech.engine.SowingDecisionEngine;
import com.agritech.enums.RiskLevel;
import com.agritech.enums.SoilType;
import com.agritech.model.DailyForecast;
import com.agritech.model.Farmer;
import com.agritech.model.Loan;
import com.agritech.model.LoanAffordability;
import com.agritech.model.ProfitEstimate;
import com.agritech.model.SowingAdvisory;
import com.agritech.model.crop.Wheat;
import com.agritech.repository.FarmerRepository;
import com.agritech.service.FarmerService;
import com.agritech.service.LoanService;
import com.agritech.service.ProfitService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CoreLogicTest {
    static class MemoryFarmers implements FarmerRepository {
        final List<Farmer> data = new ArrayList<>();
        public void save(Farmer f) { data.add(f); }
        public void update(Farmer f) {
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).getFarmerId().equals(f.getFarmerId())) data.set(i, f);
            }
        }
        public Optional<Farmer> findById(String id) {
            return data.stream().filter(f -> f.getFarmerId().equals(id)).findFirst();
        }
        public List<Farmer> findAll() { return List.copyOf(data); }
    }

    @Test
    void validatesFarmerAndDuplicatePhone() {
        FarmerService service = new FarmerService(new MemoryFarmers());
        assertThrows(IllegalArgumentException.class,
                () -> service.register("", "v", "d", "Gujarat", 1, SoilType.LOAMY, "9876543210"));
        service.register("A", "v", "d", "Gujrat", 1, SoilType.LOAMY, "9876543210");
        assertThrows(IllegalArgumentException.class,
                () -> service.register("B", "v", "d", "Gujarat", 1, SoilType.LOAMY, "9876543210"));
        assertEquals("Gujarat", service.getAll().get(0).getState());
    }

    @Test
    void calculatesReducingBalanceEmi() {
        Loan loan = new Loan("plan", 100000, 12, 12, 0);
        assertEquals(8884.88, loan.calculateEmi(), .02);
        assertEquals(loan.calculateEmi() * 12, loan.totalPayable(), .01);
    }

    @Test
    void calculatesLoanAffordabilityWithoutCallingItBankEligibility() {
        LoanAffordability result = new LoanService().assessAffordability(
                40000, 0, 5000, 300000, 8, 36
        );
        assertTrue(result.safeNewEmi() > 0);
        assertTrue(result.estimatedMaxAffordableLoan() > 0);
        assertNotNull(result.status());
        assertTrue(result.note().toLowerCase().contains("bank eligibility"));
    }

    @Test
    void calculatesProfitWithBigDecimal() {
        ProfitEstimate estimate = new ProfitService().calculate(
                2, 10, new BigDecimal("2500"), new BigDecimal("15000")
        );
        assertEquals(new BigDecimal("50000.00"), estimate.revenue());
        assertEquals(new BigDecimal("20000.00"), estimate.profit());
    }

    @Test
    void classifiesSafeAndRiskySowing() {
        var safe = List.of(
                new DailyForecast(LocalDate.now(), 15, 25, 1, 10, 60, 5),
                new DailyForecast(LocalDate.now().plusDays(1), 15, 25, 2, 20, 60, 5)
        );
        SowingAdvisory advice = new SowingDecisionEngine().advise(new Wheat(), safe);
        assertEquals("RECOMMENDED", advice.status());
        assertEquals(RiskLevel.LOW, advice.riskLevel());

        var bad = List.of(new DailyForecast(LocalDate.now(), 35, 42, 50, 95, 95, 50));
        assertEquals("AVOID", new SowingDecisionEngine().advise(new Wheat(), bad).status());
    }
}
