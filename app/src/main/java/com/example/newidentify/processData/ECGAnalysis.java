package com.example.newidentify.processData;

import android.os.Build;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ECGAnalysis {

    private List<Double> convertRRIntervalsToSeconds(List<Integer> rrIntervals) {
        List<Double> rrIntervalsInSeconds = new ArrayList<>();
        for (Integer interval : rrIntervals) {
            rrIntervalsInSeconds.add(interval / 1000.0); // 假設RR間隔以毫秒為單位
        }
        return rrIntervalsInSeconds;
    }

    /**
     * 插值RR間隔
     * */
    private double[] interpolateRRIntervals(List<Double> rrIntervals) {
        List<Double> interpolatedSignal = new ArrayList<>();
        for (double interval : rrIntervals) {
            int points = (int) (interval * 1000); // 取樣率1000Hz
            for (int i = 0; i < points; i++) {
                interpolatedSignal.add(1.0); // 這裡簡化處理，實際上應根據RR間隔的變化插值
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return interpolatedSignal.stream().mapToDouble(d -> d).toArray();
        } else {
            double[] result = new double[interpolatedSignal.size()];
            for (int i = 0; i < interpolatedSignal.size(); i++) {
                result[i] = interpolatedSignal.get(i);
            }
            return result;
        }
    }

    public double calculateLFHF(List<Integer> rrIntervals) {
        List<Double> rrIntervalsInSeconds = convertRRIntervalsToSeconds(rrIntervals);
        double[] interpolatedRR = interpolateRRIntervals(rrIntervalsInSeconds);
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fftResult = fft.transform(interpolatedRR, TransformType.FORWARD);

        double lfPower = 0.0, hfPower = 0.0;
        double resolution = 1000.0 / interpolatedRR.length; // 頻率分辨率

        for (int i = 0; i < fftResult.length / 2; i++) { // 只遍歷一半頻率，因為FFT結果是對稱的
            double freq = i * resolution;
            // 計算目前頻率點的功率譜密度（PSD），即絕對值平方
            double power = fftResult[i].abs() * fftResult[i].abs();
            if (freq >= 0.04 && freq <= 0.15) { // 低頻範圍
                lfPower += power;
            } else if (freq >= 0.15 && freq <= 0.4) { // 高頻範圍
                hfPower += power;
            }
        }

        // 防止分母為零
        if (hfPower == 0) {
            return Double.POSITIVE_INFINITY;
        } else {
            return lfPower / hfPower; // 計算並傳回LF/HF比率
        }
    }

    public Map<String, Double> calculateNormalizedLFHF(double[] fftResult, double totalPower, double vlfPower, double samplingRate) {
        double lfPower = 0.0, hfPower = 0.0;
        double resolution = samplingRate / fftResult.length;

        for (int i = 0; i < fftResult.length / 2; i++) {
            double freq = i * resolution;
            double power = Math.pow(fftResult[i], 2); // 假設fftResult已經表示了功率譜密度

            if (freq >= 0.04 && freq <= 0.15) {
                lfPower += power;
            } else if (freq >= 0.15 && freq <= 0.4) {
                hfPower += power;
            }
        }

        double totalPowerMinusVLF = totalPower - vlfPower;
        double nLF = (lfPower / totalPowerMinusVLF) * 100.0;
        double nHF = (hfPower / totalPowerMinusVLF) * 100.0;

        Map<String, Double> result = new HashMap<>();
        result.put("nLF", nLF);
        result.put("nHF", nHF);
        return result;
    }

    public double calculateTP(List<Integer> rrIntervals) {
        List<Double> rrIntervalsInSeconds = convertRRIntervalsToSeconds(rrIntervals);
        double[] interpolatedRR = interpolateRRIntervals(rrIntervalsInSeconds);
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fftResult = fft.transform(interpolatedRR, TransformType.FORWARD);

        double tpPower = 0.0;
        double resolution = 1000.0 / interpolatedRR.length; // 頻率分辨率

        // 計算總功率，考慮FFT結果的一半，因為它是對稱的
        for (int i = 0; i < fftResult.length / 2; i++) {
            double power = fftResult[i].abs() * fftResult[i].abs();
            tpPower += power;
        }

        return tpPower;
    }


    /**
     * 計算RMSSD
     */
    public float calculateRMSSD(List<Integer> rrIntervals) {
        float sum = 0;
        for (int i = 1; i < rrIntervals.size(); i++) {
            int diff = rrIntervals.get(i) - rrIntervals.get(i - 1);
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum / (rrIntervals.size() - 1));
    }

    /**
     * 計算SDNN
     */
    public float calculateSDNN(List<Integer> rrIntervals) {
        float sum = 0;
        for (int i : rrIntervals) {
            sum += i;
        }
        float mean = sum / rrIntervals.size();

        float sumOfSquares = 0;
        for (int i : rrIntervals) {
            sumOfSquares += (i - mean) * (i - mean);
        }
        return (float) Math.sqrt(sumOfSquares / (rrIntervals.size() - 1));
    }

    /**
     * 計算AVNN
     */

    public float calculateAVNN(List<Integer> rrIntervals) {
        float sum = 0;
        for (int i : rrIntervals) {
            sum += i;
        }
        return sum / rrIntervals.size();
    }

    /**
     * 計算CV
     */
    public float calculateCV(List<Integer> rrIntervals) {
        return calculateSDNN(rrIntervals) / calculateAVNN(rrIntervals);
    }

    /**
     * 計算PNN50
     */

    public float calculatePNN50(List<Integer> rrIntervals) {
        int count = 0;
        for (int i = 1; i < rrIntervals.size(); i++) {
            if (Math.abs(rrIntervals.get(i) - rrIntervals.get(i - 1)) > 50) {
                count++;
            }
        }
        return (float) count / rrIntervals.size();
    }

    /**
     * 計算NN50
     */

    public int calculateNN50(List<Integer> rrIntervals) {
        int nn50 = 0;
        for (int i = 1; i < rrIntervals.size(); i++) {
            if (Math.abs(rrIntervals.get(i) - rrIntervals.get(i - 1)) > 50) {
                nn50++;
            }
        }
        return nn50;
    }

    /**
     * 計算SDSD
     */

    public double calculateSDSD(List<Double> rrIntervals) {
        if (rrIntervals.size() < 2) {
            return 0.0; // 至少需要兩個RR間隔來計算SDSD
        }
        double[] diff = new double[rrIntervals.size() - 1];
        for (int i = 0; i < rrIntervals.size() - 1; i++) {
            diff[i] = rrIntervals.get(i + 1) - rrIntervals.get(i);
        }
        StandardDeviation sd = new StandardDeviation();
        return sd.evaluate(diff);
    }

    public double calculateSD1(List<Double> rrIntervals) {
        if (rrIntervals.size() < 2) {
            return 0.0; // 至少要兩個RR間隔來計算SD1
        }
        double[] diff = new double[rrIntervals.size() - 1];
        for (int i = 0; i < rrIntervals.size() - 1; i++) {
            diff[i] = rrIntervals.get(i + 1) - rrIntervals.get(i);
        }
        StandardDeviation sd = new StandardDeviation();
        double sdsd = sd.evaluate(diff);
        return Math.sqrt(2) * sdsd / 2.0; // SD1 = SDSD / sqrt(2)
    }

    public double calculateSD2(List<Double> rrIntervals) {
        double mean = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            mean = rrIntervals.stream().mapToDouble(a -> a).average().orElse(0.0);
        } else {
            for (Double rrInterval : rrIntervals) {
                mean += rrInterval;
            }
            mean /= rrIntervals.size();
        }

        double sumOfSquares = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            double finalMean = mean;
            sumOfSquares = rrIntervals.stream().mapToDouble(a -> (a - finalMean) * (a - finalMean)).sum();
        } else {
            for (Double rrInterval : rrIntervals) {
                sumOfSquares += (rrInterval - mean) * (rrInterval - mean);
            }
        }

        double sdnn = Math.sqrt(sumOfSquares / (rrIntervals.size() - 1));
        double sd1 = calculateSD1(rrIntervals);
        return Math.sqrt(2) * sdnn - sd1; // SD2 = sqrt(2) * SDNN - SD1
    }


    /**
     * 計算T波振幅
     */
    public List<Float> calculateTWaveAmplitudes(Float[] ecgSignal, List<Integer> tWaveIndices) {
        List<Float> tWaveAmplitudes = new ArrayList<>();
        for (int index : tWaveIndices) {
            tWaveAmplitudes.add(ecgSignal[index]);
        }
        return tWaveAmplitudes;
    }

    /**
     * 計算P波振幅
     */
    public List<Float> calculatePWaveAmplitudes(Float[] ecgSignal, List<Integer> pWaveIndices) {
        List<Float> pWaveAmplitudes = new ArrayList<>();
        for (int index : pWaveIndices) {
            pWaveAmplitudes.add(ecgSignal[index]);
        }
        return pWaveAmplitudes;
    }



}
