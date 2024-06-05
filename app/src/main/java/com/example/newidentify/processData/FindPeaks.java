package com.example.newidentify.processData;

import android.util.Log;

import com.example.newidentify.util.EcgMath;

import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.me.berndporr.iirj.Butterworth;


public class FindPeaks {

    private static final String TAG = "FindPeaks";

    EcgMath ecgMath = new EcgMath();


    // T波峰值數據和索引
    public List<Float> tWavePeaks = new ArrayList<>();
    public List<Integer> tWaveIndices = new ArrayList<>();

    // Q波索引
    public List<Integer> qWaveIndices = new ArrayList<>();

    public ArrayList<Float> filedData(ArrayList<Float> dataList) {

        for (int i = 0; i < dataList.size(); i++) {
            Float value = dataList.get(i);
            if (value.isNaN()) {
                dataList.set(i, 0.0f); // Replace NaN with zero
            }
        }

        int fs = 1000;

        // 帶通濾波，適用於大波和小波
        List<Float> bandpassFilteredData = butter_bandpass_filter(dataList, 0.5f, 40f, fs, 2);

        // 帶阻濾波，適用於大波和小波
        List<Float> bandstopFilteredData = butter_bandStop_filter(bandpassFilteredData, 49f, 51f, fs, 2);

        // 高通濾波，適用於小波
        List<Float> highpassFilteredData = Arrays.asList(butter_highpass_filter(bandstopFilteredData, 0.5f, fs, 2));

        // 低通濾波，適用於小波
        List<Float> lowPassFilteredData = Arrays.asList(butter_lowPass_filter(highpassFilteredData, 30f, fs, 2));


        ArrayList<Float> finalData = new ArrayList<>(lowPassFilteredData);

        return finalData;
    }

    public List<Integer> findPPoints(List<Integer> rWaveIndices, ArrayList<Float> ecgSignal) {
        List<Integer> tWaveIndices = new ArrayList<>();

        if (rWaveIndices.size() < 2) {
            System.out.println("findTWavePositions: R波數量不足，無法計算P波");
            return tWaveIndices;
        }

        for (int i = 0; i < rWaveIndices.size() - 1; i++) {
            int start = rWaveIndices.get(i) + 50;
            int end = rWaveIndices.get(i + 1) - 50;
            if (start >= ecgSignal.size() || end >= ecgSignal.size()) {
                System.out.println("findPWavePositions: Index out of bounds");
                continue; // Skip this iteration if indices are out of bounds
            }

            int tIndex = getIndex(ecgSignal, start, end);

            tWaveIndices.add(tIndex);
        }

        return tWaveIndices;
    }

    public List<Integer> findQPoints(List<Float> ecgData, List<Integer> rIndices) {
        List<Integer> qPoints = new ArrayList<>();
        for (int rIndex : rIndices) {
            int qIndex = rIndex;
            float minVal = ecgData.get(rIndex);
            for (int i = rIndex - 1; i >= 0; i--) {
                if (ecgData.get(i) < minVal) {
                    minVal = ecgData.get(i);
                    qIndex = i;
                } else {
                    break; // 找到局部最小值
                }
            }
            qPoints.add(qIndex);
        }
        return qPoints;
    }

    public List<Integer> filterRPeaks(List<Integer> rPeaksList, int minDistance) {
        List<Integer> filteredRPeaks = new ArrayList<>();
        int previousRPeak = -1;

        for (Integer rPeak : rPeaksList) {
            if (previousRPeak == -1 || rPeak - previousRPeak >= minDistance) {
                filteredRPeaks.add(rPeak);
                previousRPeak = rPeak;
            }
        }

        return filteredRPeaks;
    }

    //根據R波的位置找到R波的值
    public List<Double> findRPeaks(ArrayList<Float> rawEcgSignal, List<Integer> rPeaksList) {
        List<Double> rValuesList = new ArrayList<>();
        for (int i = 0; i < rawEcgSignal.size(); i++) {
            if (rPeaksList.contains(i)) {
                rValuesList.add((double) rawEcgSignal.get(i));
            }
        }
        return rValuesList;
    }

    public List<Integer> findSPoints(List<Float> ecgData, List<Integer> rIndices) {
        List<Integer> sPoints = new ArrayList<>();
        for (int rIndex : rIndices) {
            int sIndex = rIndex;
            float minVal = ecgData.get(rIndex);
            for (int i = rIndex + 1; i < ecgData.size(); i++) {
                if (ecgData.get(i) < minVal) {
                    minVal = ecgData.get(i);
                    sIndex = i;
                } else {
                    break; // 找到局部最小值
                }
            }
            sPoints.add(sIndex);
        }
        return sPoints;
    }

    private static int getIndex(ArrayList<Float> ecgSignal, int start, int end) {
        int midPoint = (start + (end - start) / 2) - 50;
        double maxBetweenR = Double.NEGATIVE_INFINITY; // Use negative infinity to handle negative values
        int tIndex = midPoint;

        for (int j = Math.max(midPoint, start); j < end && j < ecgSignal.size(); j++) { // Ensure j does not go out of bounds
            double value = ecgSignal.get(j);
            if (value > maxBetweenR) {
                maxBetweenR = value;
                tIndex = j;
            }
        }
        return tIndex;
    }

    public static List<Integer> findTPoints(List<Float> ecgData, List<Integer> rIndices) {
        List<Integer> tPoints = new ArrayList<>();

        for (int rIndex : rIndices) {
            int tPointIndex = findTPoint(ecgData, rIndex);
            tPoints.add(tPointIndex);
            if (isLocalMaximum(ecgData, tPointIndex, 10)) {
            }
        }

        return tPoints;
    }

    private static int findTPoint(List<Float> ecgData, int rPeakIndex) {
        int searchStart = rPeakIndex + 40; // R波後20個數據點開始搜索
        int searchEnd = rPeakIndex + 300;  // R波後200個數據點內結束搜索
        if (searchEnd > ecgData.size()) {
            searchEnd = ecgData.size();
        }

        double maxAmplitude = Double.NEGATIVE_INFINITY;
        int tPointIndex = -1;

        for (int i = searchStart; i < searchEnd; i++) {
            if (ecgData.get(i) > maxAmplitude) {
                maxAmplitude = ecgData.get(i);
                tPointIndex = i;
            }
        }

        return tPointIndex;
    }

    private static boolean isLocalMaximum(List<Float> ecgData, int index, int range) {
        int start = Math.max(0, index - range);
        int end = Math.min(ecgData.size(), index + range);

        for (int i = start; i < end; i++) {
            if (ecgData.get(i) > ecgData.get(index)) {
                return false;
            }
        }
        return true;
    }

    /**
     * RRi計算
     */
    public List<Integer> calculateRRi(List<Integer> r_index_up) {
        List<Integer> rr_intervals = new ArrayList<>();
        for (int i = 0; i < r_index_up.size() - 1; i++) {
            int r1 = r_index_up.get(i);
            int r2 = r_index_up.get(i + 1);
            int rri = r2 - r1;
            rr_intervals.add(rri);
        }
        return rr_intervals;
    }

    /**
     * 計算電壓差異中位數
     */
    public float calVoltDiffMed(ArrayList<Float> ecg_signal_origin, List<Integer> R_index_up, List<Integer> T_index_up) {
        List<Float> voltageDifferences = new ArrayList<>();

        for (int i = 0; i < Math.min(R_index_up.size(), T_index_up.size()); i++) {
            float rVoltage = ecg_signal_origin.get(R_index_up.get(i));
            float tVoltage = ecg_signal_origin.get(T_index_up.get(i));
            float difference = Math.abs(rVoltage - tVoltage);
            voltageDifferences.add(difference);
        }

        return ecgMath.calculateMedian(voltageDifferences);
    }

    /**
     * 計算距離差異中位數
     */
    public float calDistanceDiffMed(List<Integer> R_index_up, List<Integer> T_index_up) {
        List<Float> distanceDifferences = new ArrayList<>();

        for (int i = 0; i < Math.min(R_index_up.size(), T_index_up.size()); i++) {
            float difference = Math.abs(R_index_up.get(i) - T_index_up.get(i));
            distanceDifferences.add(difference);
        }

        return ecgMath.calculateMedian(distanceDifferences);
    }

    /**
     * 取半高寬
     */
    public float calculateHalfWidths(ArrayList<Float> ecg_signal, List<Integer> r_indexes) {
        List<Float> halfWidths = new ArrayList<>();

        for (int r_index : r_indexes) {
            Float r_value = ecg_signal.get(r_index);
            Float halfMaxValue = r_value / 2;

            int leftIndex = r_index;
            while (leftIndex > 0 && ecg_signal.get(leftIndex) >= halfMaxValue) {
                leftIndex--;
            }

            int rightIndex = r_index;
            while (rightIndex < ecg_signal.size() - 1 && ecg_signal.get(rightIndex) >= halfMaxValue) {
                rightIndex++;
            }

            // 計算並保存半高寬
            float halfWidth = rightIndex - leftIndex;
            halfWidths.add(halfWidth);
        }

        return calculate3AverageFloat((ArrayList<Float>) halfWidths);
    }

    /**
     * 計算平均值
     */

    public float calculate3AverageFloat(ArrayList<Float> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int count = Math.min(list.size(), 3);
        float sum = 0;
        for (int i = 0; i < count; i++) {
            sum += list.get(i);
        }
        return sum / count;
    }

    public ArrayList<Float> medianFilter(ArrayList<Float> data, int windowSize) {
        double[] filteredData = new double[data.size()];
        Median median = new Median();

        for (int i = 0; i < data.size(); i++) {
            int start = Math.max(0, i - windowSize / 2);
            int end = Math.min(data.size(), i + windowSize / 2 + 1); // Fixed from data.set() to data.size()
            double[] window = new double[end - start];
            for (int j = start; j < end; j++) {
                window[j - start] = data.get(j); // Convert ArrayList to array before using Arrays.copyOfRange
            }
            filteredData[i] = median.evaluate(window);
        }

        // Convert double array to ArrayList<Float> before returning
        ArrayList<Float> result = new ArrayList<>();
        for (double value : filteredData) {
            result.add((float) value);
        }
        return result;
    }


    /**
     * 巴特沃斯濾波器
     */

    public List<Float> butter_bandpass_filter(List<Float> data, float lowCut, float highCut, float fs, int order) {
        Butterworth butterworth = new Butterworth();
        float widthFrequency = highCut - lowCut;
        float centerFrequency = (highCut + lowCut) / 2.0f;
        butterworth.bandPass(order, fs, centerFrequency, widthFrequency);

        List<Float> filteredData = new ArrayList<>(data.size());
        for (Float x : data) {
            float y = (float) butterworth.filter(x);
//            y = (float) butterworth.filter(y);
            filteredData.add(y);
        }

        return filteredData;
    }

    public List<Float> butter_bandStop_filter(List<Float> data, float lowCut, float highCut, float fs, int order) {
        Butterworth butterworth = new Butterworth();
        float widthFrequency = highCut - lowCut;
        float centerFrequency = (highCut + lowCut) / 2;
        butterworth.bandStop(order, fs, centerFrequency, widthFrequency);
        List<Float> floatArray = new ArrayList<>();
        for (float a : data) {
            float b = (float) butterworth.filter(a);
            floatArray.add(b);
        }
        return floatArray;
    }

    public Float[] butter_highpass_filter(List<Float> data, float cutoffFrequency, float fs, int order) {
        Butterworth butterworth = new Butterworth();
        butterworth.highPass(order, fs, cutoffFrequency); // 直接使用高通滤波
        int index = 0;
        Float[] filteredData = new Float[data.size()];
        for (float sample : data) {
            filteredData[index] = (float) butterworth.filter(sample);
            index++;
        }
        return filteredData;
    }

    public Float[] butter_lowPass_filter(List<Float> data, float cutoffFrequency, float fs, int order) {
        Butterworth butterworth = new Butterworth();
        butterworth.lowPass(order, fs, cutoffFrequency); // 直接使用高通滤波
        int index = 0;
        Float[] filteredData = new Float[data.size()];
        for (float sample : data) {
            filteredData[index] = (float) butterworth.filter(sample);
            index++;
        }
        return filteredData;
    }

}

