package com.example.gridviewapplication;

import java.io.Serializable;

public class POJO_Transaction implements Serializable {
    private int cust_id;
    private int car_id;
    private String receipt_number;
    private int operator_id;

    public String getReceipt_number() {
        return receipt_number;
    }

    public void setReceipt_number(String receipt_number) {
        this.receipt_number = receipt_number;
    }

    public int getPump_id() {
        return pump_id;
    }

    public void setPump_id(int pump_id) {
        this.pump_id = pump_id;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    private int pump_id;
    private String cust_type;
    private String cust_name;
    private String car_plate_no;
    private String fuel_type, shift;

    public String getNozzle_qr() {
        return nozzle_qr;
    }

    public void setNozzle_qr(String nozzle_qr) {
        this.nozzle_qr = nozzle_qr;
    }

    private String nozzle_qr;
    private String cust_qr;
    private String cust_qr_type;
    private double amount;
    private double litres;

    public double getFuel_rate() {
        return fuel_rate;
    }

    public void setFuel_rate(double fuel_rate) {
        this.fuel_rate = fuel_rate;
    }

    private double fuel_rate;
    private long scan_timestamp, transaction_duration;

    public boolean hasCarQR() {
        return hasCarQR;
    }

    public void setHasCarQR(boolean hasCarQR) {
        this.hasCarQR = hasCarQR;
    }

    private boolean hasCarQR;

    public int getCust_id() {
        return cust_id;
    }

    public void setCust_id(int cust_id) {
        this.cust_id = cust_id;
    }

    public int getCar_id() {
        return car_id;
    }

    public void setCar_id(int car_id) {
        this.car_id = car_id;
    }



    public int getOperator_id() {
        return operator_id;
    }

    public void setOperator_id(int operator_id) {
        this.operator_id = operator_id;
    }

    public String getCust_type() {
        return cust_type;
    }

    public void setCust_type(String cust_type) {
        this.cust_type = cust_type;
    }

    public String getCust_name() {
        return cust_name;
    }

    public void setCust_name(String cust_name) {
        this.cust_name = cust_name;
    }

    public String getCar_plate_no() {
        return car_plate_no;
    }

    public void setCar_plate_no(String car_plate_no) {
        this.car_plate_no = car_plate_no;
    }

    public String getFuel_type() {
        return fuel_type;
    }

    public void setFuel_type(String fuel_type) {
        this.fuel_type = fuel_type;
    }


    public String getCust_qr() {
        return cust_qr;
    }

    public void setCust_qr(String cust_qr) {
        this.cust_qr = cust_qr;
    }

    public String getCust_qr_type() {
        return cust_qr_type;
    }

    public void setCust_qr_type(String cust_qr_type) {
        this.cust_qr_type = cust_qr_type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getLitres() {
        return litres;
    }

    public void setLitres(double litres) {
        this.litres = litres;
    }

    public long getScan_timestamp() {
        return scan_timestamp;
    }

    public void setScan_timestamp(long scan_timestamp) {
        this.scan_timestamp = scan_timestamp;
    }

    public long getTransaction_duration() {
        return transaction_duration;
    }

    public void setTransaction_duration(long transaction_duration) {
        this.transaction_duration = transaction_duration;
    }
}
