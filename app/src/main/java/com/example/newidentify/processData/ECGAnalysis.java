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
    public double lfHfRatio;
    public double lfPower;
    public double hfPower;
    public double[] interpolatedRR;
    public Map<Double, Double> powerSpectrum;

    private List<Double> convertRRIntervalsToSeconds(List<Integer> rrIntervals) {
        List<Double> rrIntervalsInSeconds = new ArrayList<>();
        for (Integer interval : rrIntervals) {
            rrIntervalsInSeconds.add(interval / 1000.0); // 假設RR間隔以毫秒為單位
        }
        return rrIntervalsInSeconds;
    }

    /**
     * 插值RR間隔
     */
    private double[] interpolateRRIntervals(List<Double> rrIntervals) {
        List<Double> interpolatedSignal = new ArrayList<>();
        for (double interval : rrIntervals) {
            int points = (int) (interval * 1000); // 取樣率1000Hz
            for (int i = 0; i < points; i++) {
                interpolatedSignal.add(1.0); // 這裡簡化處理，實際上應根據RR間隔的變化插值
            }
        }

        // 決定需要填入的長度（最近的2的冪）
        int originalLength = interpolatedSignal.size();
        int paddedLength = (int) Math.pow(2, Math.ceil(Math.log(originalLength) / Math.log(2)));

        // 建立填滿後的數組
        double[] paddedArray = new double[paddedLength];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            double[] tempArray = interpolatedSignal.stream().mapToDouble(d -> d).toArray();
            System.arraycopy(tempArray, 0, paddedArray, 0, tempArray.length);
        } else {
            for (int i = 0; i < interpolatedSignal.size(); i++) {
                paddedArray[i] = interpolatedSignal.get(i);
            }
        }

        // 剩餘的部分已經自動初始化為0
        return paddedArray;
    }

    public double calculateLFHF(List<Integer> rrIntervals) {
        List<Double> rrIntervalsInSeconds = convertRRIntervalsToSeconds(rrIntervals);
        interpolatedRR = interpolateRRIntervals(rrIntervalsInSeconds);
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fftResult = fft.transform(interpolatedRR, TransformType.FORWARD);

        lfPower = 0.0;
        hfPower = 0.0;
        powerSpectrum = new HashMap<>();
        double resolution = 1000.0 / interpolatedRR.length; // 頻率分辨率

        for (int i = 0; i < fftResult.length / 2; i++) {
            double freq = i * resolution;
            double power = fftResult[i].abs() * fftResult[i].abs(); // 計算功率譜密度（PSD）
            powerSpectrum.put(freq, power); // 保存頻率和對應的功率

            if (freq >= 0.04 && freq <= 0.15) {
                lfPower += power;
            } else if (freq >= 0.15 && freq <= 0.4) {
                hfPower += power;
            }
        }
        lfHfRatio = hfPower == 0 ? Double.POSITIVE_INFINITY : lfPower / hfPower;

        return lfHfRatio;
    }

    public Map<String, Float> calculateNormalizedLFHF(double[] fftResult, double totalPower, double vlfPower, double samplingRate) {
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

        Map<String, Float> result = new HashMap<>();
        result.put("nLF", (float) nLF);
        result.put("nHF", (float) nHF);
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

    public double calculateSDSD(List<Integer> rrIntervals) {
        List<Double> rrIntervalsInSeconds = convertRRIntervalsToSeconds(rrIntervals);
        if (rrIntervalsInSeconds.size() < 2) {
            return 0.0; // 至少需要兩個RR間隔來計算SDSD
        }
        double[] diff = new double[rrIntervalsInSeconds.size() - 1];
        for (int i = 0; i < rrIntervalsInSeconds.size() - 1; i++) {
            diff[i] = rrIntervalsInSeconds.get(i + 1) - rrIntervalsInSeconds.get(i);
        }
        StandardDeviation sd = new StandardDeviation();
        return sd.evaluate(diff);
    }

    public double calculateSD1(List<Integer> rrIntervals) {
        List<Double> rrIntervalsInSeconds = convertRRIntervalsToSeconds(rrIntervals);
        if (rrIntervalsInSeconds.size() < 2) {
            return 0.0; // 至少要兩個RR間隔來計算SD1
        }
        double[] diff = new double[rrIntervalsInSeconds.size() - 1];
        for (int i = 0; i < rrIntervalsInSeconds.size() - 1; i++) {
            diff[i] = rrIntervalsInSeconds.get(i + 1) - rrIntervalsInSeconds.get(i);
        }
        StandardDeviation sd = new StandardDeviation();
        double sdsd = sd.evaluate(diff);
        return Math.sqrt(2) * sdsd / 2.0; // SD1 = SDSD / sqrt(2)
    }

    public double calculateSD2(List<Integer> rrIntervals) {
        List<Double> rrIntervalsInSeconds = convertRRIntervalsToSeconds(rrIntervals);
        double mean = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            mean = rrIntervalsInSeconds.stream().mapToDouble(a -> a).average().orElse(0.0);
        } else {
            for (Double rrInterval : rrIntervalsInSeconds) {
                mean += rrInterval;
            }
            mean /= rrIntervals.size();
        }

        double sumOfSquares = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            double finalMean = mean;
            sumOfSquares = rrIntervals.stream().mapToDouble(a -> (a - finalMean) * (a - finalMean)).sum();
        } else {
            for (Integer rrInterval : rrIntervals) {
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
