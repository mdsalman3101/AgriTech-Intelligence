package com.agritech.model;

import com.agritech.enums.SoilType;

public class Farmer {
    private final String farmerId;
    private String name;
    private String village;
    private String district;
    private String state;
    private double landArea;
    private SoilType soilType;
    private String phone;
    private boolean irrigationAvailable;

    public Farmer(String farmerId, String name, String village, String district,
                  String state, double landArea, SoilType soilType, String phone) {
        this(farmerId, name, village, district, state, landArea, soilType, phone, false);
    }

    public Farmer(String farmerId, String name, String village, String district,
                  String state, double landArea, SoilType soilType, String phone,
                  boolean irrigationAvailable) {
        this.farmerId = farmerId;
        this.name = name;
        this.village = village;
        this.district = district;
        this.state = state;
        this.landArea = landArea;
        this.soilType = soilType;
        this.phone = phone;
        this.irrigationAvailable = irrigationAvailable;
    }

    public String getFarmerId() { return farmerId; }
    public String getName() { return name; }
    public String getVillage() { return village; }
    public String getDistrict() { return district; }
    public String getState() { return state; }
    public double getLandArea() { return landArea; }
    public SoilType getSoilType() { return soilType; }
    public String getPhone() { return phone; }
    public boolean isIrrigationAvailable() { return irrigationAvailable; }

    public void updateProfile(String name, String village, String district, String state,
                              double landArea, SoilType soilType, String phone,
                              boolean irrigationAvailable) {
        this.name = name;
        this.village = village;
        this.district = district;
        this.state = state;
        this.landArea = landArea;
        this.soilType = soilType;
        this.phone = phone;
        this.irrigationAvailable = irrigationAvailable;
    }

    @Override
    public String toString() {
        return "Farmer ID: " + farmerId +
                "\nName: " + name +
                "\nPhone: " + phone +
                "\nVillage: " + village +
                "\nDistrict: " + district +
                "\nState: " + state +
                "\nLand: " + String.format("%.2f", landArea) + " acres" +
                "\nSoil: " + soilType +
                "\nIrrigation: " + (irrigationAvailable ? "Available" : "Not available");
    }
}
