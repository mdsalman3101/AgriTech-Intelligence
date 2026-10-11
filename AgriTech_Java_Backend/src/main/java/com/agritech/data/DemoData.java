package com.agritech.data;

import com.agritech.enums.SoilType;
import com.agritech.model.*;
import com.agritech.model.crop.*;
import com.agritech.model.equipment.*;
import java.time.LocalDate;
import java.util.List;

public final class DemoData {
    private DemoData() {}

    public static List<Crop> crops() {
        return List.of(new Wheat(), new Rice(), new Maize(), new Mustard());
    }

    public static List<MandiPrice> mandiPrices() {
        return List.of(
                new MandiPrice("Wheat","Demo Mehsana Mandi",2320,2520,2450,2.4,LocalDate.now()),
                new MandiPrice("Rice","Demo Ahmedabad Mandi",2550,2800,2700,0.9,LocalDate.now()),
                new MandiPrice("Maize","Demo Mehsana Mandi",2100,2350,2250,-1.2,LocalDate.now()),
                new MandiPrice("Mustard","Demo Ahmedabad Mandi",5100,5450,5300,1.8,LocalDate.now())
        );
    }

    public static List<FarmEquipment> equipment() {
        return List.of(
                new Tractor("EQ001",1500,3),
                new Harvester("EQ002",3000,2),
                new IrrigationPump("EQ003",500,5),
                new Rotavator("EQ004",2000,2)
        );
    }

    public static List<Loan> loans() {
        return List.of(
                new Loan("Kisan Credit Card",100000,7.0,24,4),
                new Loan("Tractor Loan",400000,9.5,60,12)
        );
    }

    public static List<Farmer> farmers() {
        return List.of(
                new Farmer("FRM1000000001","Demo Farmer 1","Village A","Mehsana","Gujarat",4.0,SoilType.LOAMY,"9000000001"),
                new Farmer("FRM1000000002","Demo Farmer 2","Village B","Ahmedabad","Gujarat",6.5,SoilType.CLAY,"9000000002")
        );
    }
}
