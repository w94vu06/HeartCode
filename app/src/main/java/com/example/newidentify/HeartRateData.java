package com.example.newidentify;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class HeartRateData {

    public double diffSelf;
    public double R_Med; // R波電壓中位數
    public double T_Med; // T波電壓中位數
    public double halfWidth;
    public double RT_Volt;
    public double RT_Interval;
    private double bpm;
    private double ibi;
    private double sdnn;
    private double sdsd;
    private double rmssd;
    private double pnn20;
    private double pnn50;
    private double hr_mad;
    private double sd1;
    private double sd2;
    private double sd1sd2;
    private double breathingrate;
    private double vlf;
    private double lf;
    private double hf;
    private double lf_hf;
    private double p_total;
    private double vlf_perc;
    private double lf_perc;
    private double hf_perc;
    private double lf_nu;
    private double hf_nu;

    public double setScale(double num) {
      return new BigDecimal(num).setScale(5, RoundingMode.HALF_UP).doubleValue();
    }

    // Getters
    public double getBpm() {
        return setScale(bpm);
    }

    public double getIbi() {
        return setScale(ibi);
    }

    public double getSdnn() {
        return setScale(sdnn);
    }

    public double getSdsd() {
        return setScale(sdsd);
    }

    public double getRmssd() {
        return setScale(rmssd);
    }

    public double getPnn20() {
        return setScale(pnn20);
    }

    public double getPnn50() {
        return setScale(pnn50);
    }

    public double getHrMad() {
        return setScale(hr_mad);
    }

    public double getSd1() {
        return setScale(sd1);
    }

    public double getSd2() {
        return setScale(sd2);
    }


    public double getSd1sd2() {
        return setScale(sd1sd2);
    }

    public double getBreathingrate() {
        return setScale(breathingrate);
    }

    public double getDiffSelf() {
        return setScale(diffSelf);
    }

    public double getR_Med() {
        return setScale(R_Med);
    }

    public double getHalfWidth() {
        return setScale(halfWidth);
    }



    // Setters
    public void setBpm(double bpm) {
        this.bpm = setScale(bpm);
    }

    public void setIbi(double ibi) {
        this.ibi = setScale(ibi);
    }

    public void setSdnn(double sdnn) {
        this.sdnn = setScale(sdnn);
    }

    public void setSdsd(double sdsd) {
        this.sdsd = setScale(sdsd);
    }

    public void setRmssd(double rmssd) {
        this.rmssd = setScale(rmssd);
    }

    public void setPnn20(double pnn20) {
        this.pnn20 = setScale(pnn20);
    }

    public void setPnn50(double pnn50) {
        this.pnn50 = setScale(pnn50);
    }

    public void setHrMad(double hr_mad) {
        this.hr_mad = setScale(hr_mad);
    }

    public void setSd1(double sd1) {
        this.sd1 = setScale(sd1);
    }

    public void setSd2(double sd2) {
        this.sd2 = setScale(sd2);
    }

    public void setSd1sd2(double sd1sd2) {
        this.sd1sd2 = setScale(sd1sd2);
    }

    public void setBreathingrate(double breathingrate) {
        this.breathingrate = setScale(breathingrate);
    }

    public void setDiffSelf(double diffSelf) {
        this.diffSelf = setScale(diffSelf);
    }

    public void setR_Med(double R_Med) {
        this.R_Med = setScale(R_Med);
    }

    public void setHalfWidth(double halfWidth) {
        this.halfWidth = setScale(halfWidth);
    }

}
