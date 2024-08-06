package com.example.newidentify.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class HeartRateData {

    private double bpm;
    private double mean_nn;
    private double sdnn;
    private double sdsd;
    private double rmssd;
    private double pnn20;
    private double pnn50;
    private double hr_mad;
    private double sd1;
    private double sd2;
    private double sd1sd2;
    private double shan_en;
    private double af;

    public double t_area;
    public double t_height;

    public double pqr_angle;
    public double qrs_angle;
    public double rst_angle;

    private List<Integer> t_onsets;
    private List<Integer> t_peaks;
    private List<Integer> t_offsets;

    private List<Double> signals;
    public double diffSelf;
    public double R_Med; // R波電壓中位數
    public double voltStd; // 電壓標準差
    public double T_Med; // T波電壓中位數
    public double halfWidth;

    public double setScale(double num) {
        return new BigDecimal(num).setScale(5, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Getters
     */

    public double getBpm() {
        return setScale(bpm);
    }

    public double getMean_nn() {
        return setScale(mean_nn);
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

    public double getShan_en() {
        return setScale(shan_en);
    }

    public double getAf() {
        return setScale(af);
    }

    public double getT_area() {
        return setScale(t_area);
    }

    public double getT_height() {
        return setScale(t_height);
    }

    public double getPqr_angle() {
        return setScale(pqr_angle);
    }

    public double getQrs_angle() {
        return setScale(qrs_angle);
    }

    public double getRst_angle() {
        return setScale(rst_angle);
    }

    public List<Integer> getT_onsets() {
        return t_onsets;
    }

    public List<Integer> getT_peaks() {
        return t_peaks;
    }

    public List<Integer> getT_offsets() {
        return t_offsets;
    }

    public List<Double> getSignals() {
        return signals;
    }

    public double getDiffSelf() {
        return setScale(diffSelf);
    }

    public double getR_Med() {
        return setScale(R_Med);
    }

    public double getVoltStd() {
        return setScale(voltStd);
    }

    public double getHalfWidth() {
        return setScale(halfWidth);
    }

    /**
     * Setters
     */
    public void setBpm(double bpm) {
        this.bpm = setScale(bpm);
    }

    public void setMean_nn(double mean_nn) {
        this.mean_nn = setScale(mean_nn);
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

    public void setShan_en(double shan_en) {
        this.shan_en = setScale(shan_en);
    }

    public void setAf(double af) {
        this.af = setScale(af);
    }

    public void setT_area(double t_area) {
        this.t_area = setScale(t_area);
    }

    public void setT_height(double t_height) {
        this.t_height = setScale(t_height);
    }

    public void setPqr_angle(double pqr_angle) {
        this.pqr_angle = setScale(pqr_angle);
    }

    public void setQrs_angle(double qrs_angle) {
        this.qrs_angle = setScale(qrs_angle);
    }

    public void setRst_angle(double rst_angle) {
        this.rst_angle = setScale(rst_angle);
    }

    public void setT_onsets(List<Integer> t_onsets) {
        this.t_onsets = t_onsets;
    }

    public void setT_peaks(List<Integer> t_peaks) {
        this.t_peaks = t_peaks;
    }

    public void setT_offsets(List<Integer> t_offsets) {
        this.t_offsets = t_offsets;
    }

    public void setSignals(List<Double> signals) {
        this.signals = signals;
    }

    public void setT_Med(double T_Med) {
        this.T_Med = setScale(T_Med);
    }

    public void setDiffSelf(double diffSelf) {
        this.diffSelf = setScale(diffSelf);
    }

    public void setR_Med(double R_Med) {
        this.R_Med = setScale(R_Med);
    }

    public void setVoltStd(double voltStd) {
        this.voltStd = setScale(voltStd);
    }

    public void setHalfWidth(double halfWidth) {
        this.halfWidth = setScale(halfWidth);
    }

}

