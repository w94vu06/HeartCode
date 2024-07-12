package com.example.newidentify.process;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    private double iqrnn;
    private double ap_en;
    private double shan_en;
    private double fuzzy_en;
    private double samp_en;
    private double ulf;
    private double vlf;
    private double lf;
    private double hf;
    private double tp;
    private double lfhf;
    private double lfn;
    private double hfn;
    private double ln_hf;
    private double sdann1;
    private double sdann2;
    private double sdann5;
    private double af;

    public double diffSelf;
    public double R_Med; // R波電壓中位數
    public double voltStd; // 電壓標準差
    public double T_Med; // T波電壓中位數
    public double halfWidth;
    public double RT_Volt;
    public double RT_Interval;

    public double setScale(double num) {
        return new BigDecimal(num).setScale(5, RoundingMode.HALF_UP).doubleValue();
    }

    // Getters
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

    public double getIqrnn() {
        return setScale(iqrnn);
    }

    public double getAp_en() {
        return setScale(ap_en);
    }

    public double getShan_en() {
        return setScale(shan_en);
    }

    public double getFuzzy_en() {
        return setScale(fuzzy_en);
    }

    public double getSamp_en() {
        return setScale(samp_en);
    }

    public double getUlf() {
        return setScale(ulf);
    }

    public double getVlf() {
        return setScale(vlf);
    }

    public double getLf() {
        return setScale(lf);
    }

    public double getHf() {
        return setScale(hf);
    }

    public double getTp() {
        return setScale(tp);
    }

    public double getLfhf() {
        return setScale(lfhf);
    }

    public double getLfn() {
        return setScale(lfn);
    }

    public double getHfn() {
        return setScale(hfn);
    }

    public double getLn_hf() {
        return setScale(ln_hf);
    }

    public double getSdann1() {
        return setScale(sdann1);
    }

    public double getSdann2() {
        return setScale(sdann2);
    }

    public double getSdann5() {
        return setScale(sdann5);
    }

    public double getAf() {
        return setScale(af);
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

    // Setters
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

    public void setIqrnn(double iqrnn) {
        this.iqrnn = setScale(iqrnn);
    }

    public void setAp_en(double ap_en) {
        this.ap_en = setScale(ap_en);
    }

    public void setShan_en(double shan_en) {
        this.shan_en = setScale(shan_en);
    }

    public void setFuzzy_en(double fuzzy_en) {
        this.fuzzy_en = setScale(fuzzy_en);
    }

    public void setSamp_en(double samp_en) {
        this.samp_en = setScale(samp_en);
    }

    public void setUlf(double ulf) {
        this.ulf = setScale(ulf);
    }

    public void setVlf(double vlf) {
        this.vlf = setScale(vlf);
    }

    public void setLf(double lf) {
        this.lf = setScale(lf);
    }

    public void setHf(double hf) {
        this.hf = setScale(hf);
    }

    public void setTp(double tp) {
        this.tp = setScale(tp);
    }

    public void setLfhf(double lfhf) {
        this.lfhf = setScale(lfhf);
    }

    public void setLfn(double lfn) {
        this.lfn = setScale(lfn);
    }

    public void setHfn(double hfn) {
        this.hfn = setScale(hfn);
    }

    public void setLn_hf(double ln_hf) {
        this.ln_hf = setScale(ln_hf);
    }

    public void setSdann1(double sdann1) {
        this.sdann1 = setScale(sdann1);
    }

    public void setSdann2(double sdann2) {
        this.sdann2 = setScale(sdann2);
    }

    public void setSdann5(double sdann5) {
        this.sdann5 = setScale(sdann5);
    }

    public void setAf(double af) {
        this.af = setScale(af);
    }

    public void setRT_Volt(double RT_Volt) {
        this.RT_Volt = setScale(RT_Volt);
    }

    public void setRT_Interval(double RT_Interval) {
        this.RT_Interval = setScale(RT_Interval);
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
