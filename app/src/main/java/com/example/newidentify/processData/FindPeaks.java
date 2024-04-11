package com.example.newidentify.processData;

import android.os.Build;
import android.util.Log;

import com.example.newidentify.Util.EcgMath;
import com.example.newidentify.Util.FindPeaksCallback;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import uk.me.berndporr.iirj.Butterworth;


public class FindPeaks extends Thread {

    private static final String TAG = "FindPeaks";
    private final List<Float> dataList;

    EcgMath ecgMath = new EcgMath();

    // 原始ECG信號數據
    public Float[] ecgSignal;

    private int MAX_ATTEMPTS = 3;

    // R波峰值數據和索引
    public List<Float> rWavePeaks = new ArrayList<>();
    public List<Integer> rWaveIndices = new ArrayList<>();

    public List<Float> peakList = new ArrayList<>();

    // T波峰值數據和索引
    public List<Float> tWavePeaks = new ArrayList<>();
    public List<Integer> tWaveIndices = new ArrayList<>();

    // Q波索引
    public List<Integer> qWaveIndices = new ArrayList<>();

    // RR間隔列表
    public List<Integer> rrIntervals = new ArrayList<>();

    float minValue = Float.MAX_VALUE;
    float maxValue = Float.MIN_VALUE;

    float minValueA = Float.MAX_VALUE;
    float maxValueA = Float.MIN_VALUE;

    float minValueB = Float.MAX_VALUE;
    float maxValueB = Float.MIN_VALUE;

    public FindPeaks(List<Float> dataList) {
        this.dataList = dataList;
    }

    @Override
    public void run() {
        super.run();
        /** 宣告*/
        processECGData();
    }

    private void processECGData() {
        // 進行ECG數據處理
        List<Float> bandStop = new ArrayList<>(Arrays.asList(butter_bandStop_filter(dataList, 55, 65, 1000, 1)));

//        List<Float> highpass = Arrays.asList(butter_highpass_filter(bandStop,  1, 1000, 2));

        Float[] floats = butter_bandpass_filter(bandStop, 2, 10, 1000, 1);

        Float[] floats2 = butter_bandpass_filter2(bandStop, 2, 10, 1000, 1);

        if (!Float.isNaN(maxValueA)) {
            Log.d("maxValueA", "isNOTNULL");
            maxValue = maxValueA;
            minValue = minValueA;
        } else {
            Log.d("maxValueA", "isNULL");
            floats = floats2;
            maxValue = maxValueB;
            minValue = minValueB;
        }

        if (floats.length >= 20000) {
            ecgSignal = Arrays.copyOfRange(floats, 4000, 22000);
        } else {
            ecgSignal = floats;
            Log.d(TAG, "訊號長度不足");
        }

        findPeaks(ecgSignal, 1.5, 0);
        findTWavePositions(rWaveIndices);

        //初始化
        minValue = Float.MAX_VALUE;
        maxValue = 0;
        minValueA = Float.MAX_VALUE;
        maxValueA = 0;
        minValueB = Float.MAX_VALUE;
        maxValueB = 0;
    }

    private void findPeaks(Float[] ecg_signal_origin, double peakThresholdFactor, int attempt) {
        // 遍歷小數組中的每個元素，將符合條件的值加入到對應的列表中
        int chunkSize = 4500;
        for (int i = 0; i < ecg_signal_origin.length; i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, ecg_signal_origin.length);
            Float[] chunk = Arrays.copyOfRange(ecg_signal_origin, i, endIndex);

            // 找到該小數組的最大值和最小值
            float maxValue = Float.NEGATIVE_INFINITY;
            float minValue = Float.POSITIVE_INFINITY;
            for (Float value : chunk) {
                maxValue = Math.max(maxValue, value);
                minValue = Math.min(minValue, value);
            }
            // 遍歷小數組中的每個元素，將符合條件的值加入到對應的列表中
            for (Float value : chunk) {
                if (value > maxValue / peakThresholdFactor) {
                    peakList.add(value);

                } else if (value < minValue / peakThresholdFactor) {
                    peakList.add(0F);    // 添加零值到 peakListUp
                } else {
                    peakList.add(0F);
                }
            }
        }

        Log.d(TAG, "peakListUp: " + peakList.size());

        adjustRPointPositions(ecg_signal_origin);

        if (rWaveIndices.size() < 13 && attempt < MAX_ATTEMPTS) {
            // 如果找到的R點數量小於13，清空數據並往下繼續找R點
            peakList.clear();
            rWavePeaks.clear();
            rWaveIndices.clear();
            rrIntervals.clear();
            tWavePeaks.clear();
            tWaveIndices.clear();
            qWaveIndices.clear();
            findPeaks(ecg_signal_origin, peakThresholdFactor + 0.5, attempt + 1); // 增加閾值並記錄嘗試次數
        } else if (attempt >= MAX_ATTEMPTS) {
            // 達到最大嘗試次數，終止尋找峰值
            Log.d(TAG, "達到最大嘗試次數，終止尋找峰值");
        }
        Log.d("Rindex", "findPeaks: " + rWaveIndices.size());
    }

    public void adjustRPointPositions(Float[] ecg_signal_origin) {
        List<Integer> adjustedRIndexUp = new ArrayList<>();
        List<Integer> tempRWaveIndices = new ArrayList<>();
        int windowSize = 100;
        final int MIN_RR_INTERVAL = 200; // 最小RR間隔

        for (int originalIndex : rWaveIndices) {
            int startIndex = Math.max(0, originalIndex - windowSize);
            int endIndex = Math.min(ecg_signal_origin.length - 1, originalIndex + windowSize);
            float maxVal = Float.NEGATIVE_INFINITY;
            int maxIndex = originalIndex;

            for (int i = startIndex; i <= endIndex; i++) {
                if (ecg_signal_origin[i] > maxVal) {
                    maxVal = ecg_signal_origin[i];
                    maxIndex = i;
                }
            }

            tempRWaveIndices.add(maxIndex);
        }

        // 在加入adjustedRIndexUp之前檢查RR間隔是否大於MIN_RR_INTERVAL
        for (int i = 0; i < tempRWaveIndices.size(); i++) {
            if (i == 0 || tempRWaveIndices.get(i) - tempRWaveIndices.get(i - 1) > MIN_RR_INTERVAL) {
                adjustedRIndexUp.add(tempRWaveIndices.get(i));
            }
        }

        // 更新R_index_up為調整後的索引
        rWaveIndices.clear();
        rWaveIndices.addAll(adjustedRIndexUp);

        // 由於R點位置可能有變化，因此對應的RRI、R_dot等也需要依照新的R點位置重新計算
        findRWavePositions();
    }

    public void findRWavePositions() {
        float maxFloat = 0;
        boolean insideR = false;

        // 清空數據
        rrIntervals.clear();
        rWavePeaks.clear();
        rWaveIndices.clear();

        // 遍歷 peakListUp 尋找 R 點
        int maxIndex = -1; // 初始化最大值索引

        for (int i = 0; i < peakList.size(); i++) {
            float value = peakList.get(i);

            if (value != 0) {
                if (value > maxFloat) {
                    maxFloat = value; // 更新最大值
                    maxIndex = i; // 記錄目前最大值的索引
                }
                insideR = true;
            } else if (insideR) {
                if (maxIndex != -1) {
                    rWavePeaks.add(maxFloat);
                    rWaveIndices.add(maxIndex); // 新增記錄的最大值索引
                }
                // 重置變量，準備偵測下一個R波段
                maxFloat = 0;
                maxIndex = -1;
                insideR = false;
            }
        }

        findQWavePositions(ecgSignal, rWaveIndices);

        calculateRRi();
    }


    public List<Integer> findQWavePositions(Float[] ecg_signal_origin, List<Integer> R_index_up) {
        List<Integer> qWaveIndexes = new ArrayList<>();

        // 對於每個R波索引
        for (Integer rIndex : R_index_up) {
            int searchStartIndex = Math.max(rIndex - 50, 0); // 假設Q波位於R波前最多50個資料點
            float minValue = Float.MAX_VALUE;
            int qIndex = -1;

            // 從R波索引向前搜尋局部最小值
            for (int i = rIndex; i >= searchStartIndex; i--) {
                if (ecg_signal_origin[i] < minValue) {
                    minValue = ecg_signal_origin[i];
                    qIndex = i;
                }
            }
            // 新增找到的Q波索引
            qWaveIndices.add(qIndex);
        }

        return qWaveIndexes;
    }

    public void findTWavePositions(List<Integer> rWaveIndices) {
        // 遍歷 R_index，找出每個 R 點之間的最大值
        for (int i = 0; i < rWaveIndices.size() - 1; i++) {
            int start = rWaveIndices.get(i) + 50;
            int end = rWaveIndices.get(i + 1);
            int midPoint = (start + (end - start) / 2) - 50; // 計算中點
            float maxBetweenR = Float.MIN_VALUE;
            int tIndex = midPoint; // T點的初始索引為中點

            // 從 R 點的中點開始向前尋找，找出兩個 R 點之間的最大值
            for (int j = midPoint; j >= start; j--) {
                float value = ecgSignal[j];
                if (value > maxBetweenR) {
                    maxBetweenR = value;
                    tIndex = j; // 更新 T 點的索引
                }
            }

            // 將最大值和對應的索引添加到列表中
            tWavePeaks.add(maxBetweenR);
            tWaveIndices.add(tIndex); // 添加 T 點的索引
        }
    }

    /**
     * RRi計算
     */
    public void calculateRRi() {
        // 計算RR間距
        ArrayList<Integer> rrIntervalsArrayList = new ArrayList<>();

        for (int i = 0; i < rWaveIndices.size() - 1; i++) {
            rrIntervalsArrayList.add((rWaveIndices.get(i + 1)) - rWaveIndices.get(i));
        }
//        rrIntervals.addAll(rrIntervalsArrayList);
        double medianRR = ecgMath.calculateMedianDouble(rrIntervalsArrayList);
        for (int rrInterval : rrIntervalsArrayList) {
            if (rrInterval <= medianRR) {
                rrIntervals.add(rrInterval);
            }
        }
    }

    /**
     * 計算BPM
     */
    public float calculateBPM() {
        float sum = 0;
        for (int i : rrIntervals) {
            sum += 60 * 1000 / i;
        }
        return sum / rrIntervals.size();
    }



    /**
     * 計算電壓差異中位數
     */
    public float calVoltDiffMed(Float[] ecg_signal_origin, List<Integer> R_index_up, List<Integer> T_index_up) {
        List<Float> voltageDifferences = new ArrayList<>();

        for (int i = 0; i < Math.min(R_index_up.size(), T_index_up.size()); i++) {
            float rVoltage = ecg_signal_origin[R_index_up.get(i)];
            float tVoltage = ecg_signal_origin[T_index_up.get(i)];
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
     * Bazett矯正 計算QT。
     */
    public float calculateQTcBazett(List<Integer> Q_index_up, List<Integer> T_index_up, List<Integer> RRIUp) {
        List<Float> qtcIntervals = new ArrayList<>();

        int minSize = Math.min(Q_index_up.size(), T_index_up.size());
        for (int i = 0; i < minSize && i < RRIUp.size(); i++) {
            // QT間隔計算（毫秒）
            float qtInterval = T_index_up.get(i) - Q_index_up.get(i);

            // RR間隔（毫秒轉換為秒）
            float rrInterval = RRIUp.get(i) / 1000f; // 轉換為秒

            if (rrInterval > 0) { // 避免除以零
                // 計算QTc，直接使用毫秒，RR間隔已轉換為秒
                float sqrtRr = (float) Math.sqrt(rrInterval);
                float qtc = qtInterval / sqrtRr; // QTc計算結果自然是毫秒
                qtcIntervals.add(qtc);
            }
        }

        return ecgMath.calculateMedian(qtcIntervals);
    }

    public float calculateRTSlope(List<Float> R_dot_up, List<Integer> R_index_up, List<Float> T_dot_up, List<Integer> T_index_up) {
        List<Float> rtSlopes = new ArrayList<>();

        int size = Math.min(R_dot_up.size(), T_dot_up.size());
        for (int i = 0; i < size; i++) {
            Float rSignal = R_dot_up.get(i);
            Integer rIndex = R_index_up.get(i);
            Float tSignal = T_dot_up.get(i);
            Integer tIndex = T_index_up.get(i);

            Float slope = null;
            if (rIndex != null && tIndex != null && !rIndex.equals(tIndex)) {
                slope = (tSignal - rSignal) / (tIndex - rIndex); // 修正斜率计算公式
            }

            rtSlopes.add(slope);
        }

        return ecgMath.calculateMedian(rtSlopes); // 假设这个方法能正确计算Float列表的中位数
    }


    /**
     * 取半高寬
     */
    public float calculateHalfWidths(Float[] ecg_signal, List<Integer> r_indexes) {
        List<Float> halfWidths = new ArrayList<>();

        for (int r_index : r_indexes) {
            Float r_value = ecg_signal[r_index];
            Float halfMaxValue = r_value / 2;

            int leftIndex = r_index;
            while (leftIndex > 0 && ecg_signal[leftIndex] >= halfMaxValue) {
                leftIndex--;
            }

            int rightIndex = r_index;
            while (rightIndex < ecg_signal.length - 1 && ecg_signal[rightIndex] >= halfMaxValue) {
                rightIndex++;
            }

            // 計算並保存半高寬
            float halfWidth = rightIndex - leftIndex;
            halfWidths.add(halfWidth);
        }

        return calculate3AverageFloat((ArrayList<Float>) halfWidths);
    }

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
    public Float[] butter_bandpass_filter(List<Float> data, int lowCut, int highCut, int fs, int order) {
        Butterworth butterworth = new Butterworth();
        float widthFrequency = highCut - lowCut;
        float centerFrequency = (highCut + lowCut) / 2;
        butterworth.bandPass(order, fs, centerFrequency, widthFrequency);
        int in = 0;
        Float[] floatArray = new Float[data.size()];
        for (float a : data) {
            float b = (float) butterworth.filter(a);
            float c = b * b;
            float d = (float) butterworth.filter(c);
            minValueA = Math.min(minValueA, d);
            maxValueA = Math.max(maxValueA, d);
            floatArray[in] = d;
            in++;
        }
        return floatArray;
    }

    public Float[] butter_bandpass_filter2(List<Float> data, int lowCut, int highCut, int fs, int order) {
        Butterworth butterworth = new Butterworth();
        float widthFrequency = highCut - lowCut;
        float centerFrequency = (highCut + lowCut) / 2;
        butterworth.bandPass(order, fs, centerFrequency, widthFrequency);
        int in = 0;
        Float[] floatArray2 = new Float[data.size()];
        for (float x : data) {
            float y = (float) butterworth.filter(x);
            floatArray2[in] = y;
            in++;
            minValueB = Math.min(minValueB, y);
            maxValueB = Math.max(maxValueB, y);
        }
        return floatArray2;
    }

    public Float[] butter_bandStop_filter(List<Float> data, int lowCut, int highCut, int fs, int order) {
        Butterworth butterworth = new Butterworth();
        float widthFrequency = highCut - lowCut;
        float centerFrequency = (highCut + lowCut) / 2;
        butterworth.bandStop(order, fs, centerFrequency, widthFrequency);
        int in = 0;
        Float[] floatArray = new Float[data.size()];
        for (float a : data) {
            float b = (float) butterworth.filter(a);
            floatArray[in] = b;
            in++;
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

