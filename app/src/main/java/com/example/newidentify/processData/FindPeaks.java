package com.example.newidentify.processData;

import android.util.Log;

import com.example.newidentify.util.EcgMath;

import java.util.ArrayList;
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

        // 進行帶阻濾波處理
        List<Float> bandStop = (butter_bandStop_filter(dataList, 55, 65, 1000, 1));

        // 進行帶通濾波處理
        List<Float> floats = (butter_bandpass_filter(bandStop, 2, 10, 1000, 1));

        return (ArrayList<Float>) floats;
    }

//    public void findTWavePositions(List<Integer> rWaveIndices, double[] ecgSignal) {
//        if (rWaveIndices.size() < 2) {
//            Log.d(TAG, "findTWavePositions: R波數量不足，無法計算T波");
//            return;
//        }
//
//        for (int i = 0; i < rWaveIndices.size() - 1; i++) {
//            int start = rWaveIndices.get(i) + 50;
//            int end = rWaveIndices.get(i + 1) - 50;
//            if (start >= ecgSignal.length || end >= ecgSignal.length) {
//                Log.d(TAG, "findTWavePositions: Index out of bounds");
//                continue; // Skip this iteration if indices are out of bounds
//            }
//
//            int midPoint = (start + (end - start) / 2) - 50;
//            double maxBetweenR = Double.NEGATIVE_INFINITY; // Use negative infinity to handle negative values
//            int tIndex = midPoint;
//
//            for (int j = Math.max(midPoint, start); j < end && j < ecgSignal.length; j++) { // Ensure j does not go out of bounds
//                double value = ecgSignal[j];
//                if (value > maxBetweenR) {
//                    maxBetweenR = value;
//                    tIndex = j;
//                }
//            }
//
//            tWavePeaks.add((float) maxBetweenR);
//            tWaveIndices.add(tIndex);
//        }
//    }

    public void findTWavePositions(List<Integer> rWaveIndices, ArrayList<Float> ecgSignal) {
        if (rWaveIndices.size() < 2) {
            Log.d(TAG, "findTWavePositions: R波数量不足，无法计算T波");
            return;
        }

        for (int i = 0; i < rWaveIndices.size() - 1; i++) {
            int start = rWaveIndices.get(i);
            int end = rWaveIndices.get(i + 1);
            if (start >= ecgSignal.size() || end >= ecgSignal.size()) {
                Log.d(TAG, "findTWavePositions: Index out of bounds");
                continue;
            }

            int midPoint = start + (end - start) / 2;
            int minIndex = start;
            double minVal = Double.POSITIVE_INFINITY;
            for (int j = start; j <= midPoint && j < ecgSignal.size(); j++) {
                double value = ecgSignal.get(j);
                if (value < minVal) {
                    minVal = value;
                    minIndex = j;
                }
            }

            int maxIndex = minIndex;
            double maxVal = Double.NEGATIVE_INFINITY;
            for (int j = minIndex; j <= midPoint && j < ecgSignal.size(); j++) {
                double value = ecgSignal.get(j);
                if (value > maxVal) {
                    maxVal = value;
                    maxIndex = j;
                }
            }

            // Log for debugging
            Log.d(TAG, "R-Wave from " + start + " to " + end + ", Min at " + minIndex + " value " + minVal + ", Max T-Wave at " + maxIndex + " value " + maxVal);

            tWavePeaks.add((float) maxVal);
            tWaveIndices.add(maxIndex);
        }
    }

    public List<Integer> findQWavePositions(List<Integer> rWaveIndices ,double[] ecgSignal) {
        List<Integer> qWaveIndexes = new ArrayList<>();

        // 對於每個R波索引
        for (Integer rIndex : rWaveIndices) {
            int searchStartIndex = Math.max(rIndex - 50, 0); // 假設Q波位於R波前最多50個資料點
            float minValue = Float.MAX_VALUE;
            int qIndex = -1;

            // 從R波索引向前搜尋局部最小值
            for (int i = rIndex; i >= searchStartIndex; i--) {
                if (ecgSignal[i] < minValue) {
                    minValue = (float) ecgSignal[i];
                    qIndex = i;
                }
            }
            // 新增找到的Q波索引
            qWaveIndices.add(qIndex);
        }

        return qWaveIndexes;
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

    /**
     * 巴特沃斯濾波器
     */

    public List<Float> butter_bandpass_filter(List<Float> data, int lowCut, int highCut, int fs, int order) {
        Butterworth butterworth = new Butterworth();
        float widthFrequency = highCut - lowCut;
        float centerFrequency = (highCut + lowCut) / 2.0f;
        butterworth.bandPass(order, fs, centerFrequency, widthFrequency);

        List<Float> filteredData = new ArrayList<>(data.size());
        for (Float x : data) {
            float y = (float) butterworth.filter(x);
            y = (float) butterworth.filter(y);
            filteredData.add(y);
        }

        return filteredData;
    }

    public List<Float> butter_bandStop_filter(List<Float> data, int lowCut, int highCut, int fs, int order) {
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

    public Float[] butter_highpass_filter(List<Float> data, int cutoffFrequency, int fs, int order) {
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

}

