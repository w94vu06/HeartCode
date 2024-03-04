package com.example.newidentify.processData;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uk.me.berndporr.iirj.Butterworth;

public class BpmCountThread extends Thread {

    private final List<Float> dataList;

    public BpmCountThread(List<Float> dataList) {
        this.dataList = dataList;
    }

    private static final String TAG = "BpmCountThread";

    float minValue = Float.MAX_VALUE;
    float maxValue = Float.MIN_VALUE;

    float minValueA = Float.MAX_VALUE;
    float maxValueA = Float.MIN_VALUE;

    float minValueB = Float.MAX_VALUE;
    float maxValueB = Float.MIN_VALUE;

    //回傳結果
    Float minFloatValue;
    public Float[] ecg_signal_origin;
    double bpmUp;
    double bpmDown;

    List<Float> peakListUp = new ArrayList();
    List<Float> peakListDown = new ArrayList();

    public List<Float> R_dot_up = new ArrayList();//R點數據
    public List<Integer> R_index_up = new ArrayList();//R點索引

    public List<Float> R_dot_down = new ArrayList();
    public List<Integer> R_index_down = new ArrayList();

    public List<Float> T_dot_up = new ArrayList<>(); //T點數據
    public List<Integer> T_index_up = new ArrayList<>(); //T點索引

    List<Integer> RRIUp = new ArrayList<>();
    List<Integer> RRIDown = new ArrayList<>();

    @Override
    public void run() {
        super.run();
        /** 宣告*/

        List<Float> bandStop = new ArrayList<>(Arrays.asList(butter_bandStop_filter(dataList, 55, 65, 1000, 1)));

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
            ecg_signal_origin = Arrays.copyOfRange(floats, 4000, 22000);
        } else {
            try {
                throw new Exception("資料錯誤");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        findPeaks(ecg_signal_origin, 1.5);

        findTDot();

        /** 算平均心率 */

        int sumUp = 0;
        int sumDown = 0;

        for (int i : RRIUp) {
            sumUp += i;
        }

        for (int i : RRIDown) {
            sumDown += i;
        }
        if (sumUp != 0) {
            bpmUp = 60.0 / ((sumUp / RRIUp.size()) / 1000.0);
        }
        if (sumDown != 0) {
            bpmDown = 60.0 / ((sumDown / RRIDown.size()) / 1000.0);
        }

        minFloatValue = minValue;
        //初始化
        minValue = Float.MAX_VALUE;
        maxValue = 0;
        minValueA = Float.MAX_VALUE;
        maxValueA = 0;
        minValueB = Float.MAX_VALUE;
        maxValueB = 0;
    }

    private void findPeaks(Float[] resultFloatArray, double peakThresholdFactor) {
        // 遍歷小數組中的每個元素，將符合條件的值加入到對應的列表中
        int chunkSize = 4500;
        for (int i = 0; i < resultFloatArray.length; i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, resultFloatArray.length);
            Float[] chunk = Arrays.copyOfRange(resultFloatArray, i, endIndex);

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
                    peakListUp.add(value);
                    peakListDown.add(0F);  // 添加零值到 peakListDown
                } else if (value < minValue / peakThresholdFactor) {
                    peakListUp.add(0F);    // 添加零值到 peakListUp
                    peakListDown.add(value);
                } else {
                    peakListUp.add(0F);
                    peakListDown.add(0F);
                }
            }
        }

        // 遍歷 peakListUp 尋找 R 點
        calPeakListUp();
        calPeakListDown();

        if (R_index_up.size() <= 13) {
            // 如果找到的R點數量小於13，清空數據並往下繼續找R點
            peakListUp.clear();
            peakListDown.clear();
            R_dot_up.clear();
            R_dot_down.clear();
            R_index_up.clear();
            R_index_down.clear();
            RRIUp.clear();
            RRIDown.clear();
            T_dot_up.clear();
            T_index_up.clear();

            findPeaks(resultFloatArray, 2.5);
        }
        Log.d("Rindex", "findPeaks: " + R_index_up.size());

    }

    public void calPeakListUp() {
        float maxFloat = 0;
        boolean insideR = false;

        // 清空數據
        RRIUp.clear();
        R_dot_up.clear();
        R_index_up.clear();

        // 遍歷 peakListUp 尋找 R 點
        for (int i = 0; i < peakListUp.size(); i++) {
            float value = peakListUp.get(i);

            if (value != 0) {
                maxFloat = Math.max(maxFloat, value);
                insideR = true;
            } else if (insideR) {
                R_dot_up.add(maxFloat);
                R_index_up.add(i - 1);
                maxFloat = 0;
                insideR = false;
            }
        }

        // 計算RR間距
        for (int i = 0; i < R_index_up.size() - 1; i++) {
            RRIUp.add((R_index_up.get(i + 1)) - R_index_up.get(i));
        }
    }


    public void calPeakListDown() {
        float minFloat = 0;
        for (float f : peakListDown) {
            if (f != 0) {
                minFloat = Math.min(minFloat, f);
            } else {
                if (minFloat != 0) {
                    R_dot_down.add(minFloat);
                }
                minFloat = 0;
            }
        }
        for (int i = 0; i < peakListDown.size(); i++) {
            if (R_dot_down.contains(peakListDown.get(i))) {
                R_index_down.add(i);
            }
        }
        for (int i = 0; i < R_index_down.size() - 1; i++) {
            RRIDown.add((R_index_down.get(i + 1)) - R_index_down.get(i));
        }
    }

    public void findTDot() {
        // 遍歷 R_index，找出每個 R 點之間的最大值
        for (int i = 0; i < R_index_up.size() - 1; i++) {
            int start = R_index_up.get(i) + 50;
            int end = R_index_up.get(i + 1);
            int midPoint = (start + (end - start) / 2) - 50; // 計算中點
            float maxBetweenR = Float.MIN_VALUE;
            int tIndex = midPoint; // T點的初始索引為中點

            // 從 R 點的中點開始向前尋找，找出兩個 R 點之間的最大值
            for (int j = midPoint; j >= start; j--) {
                float value = ecg_signal_origin[j];
                if (value > maxBetweenR) {
                    maxBetweenR = value;
                    tIndex = j; // 更新 T 點的索引
                }
            }

            // 將最大值和對應的索引添加到列表中
            T_dot_up.add(maxBetweenR);
            T_index_up.add(tIndex); // 添加 T 點的索引
        }

        // 輸出結果
        for (int i = 0; i < T_dot_up.size(); i++) {
            Log.d("TMaxValue", "Value: " + T_dot_up.get(i) + ", Index: " + T_index_up.get(i));
        }
    }

    public float calVoltDiffMed(Float[] ecg_signal_origin, List<Integer> R_index_up, List<Integer> T_index_up) {
        List<Float> voltageDifferences = new ArrayList<>();

        for (int i = 0; i < Math.min(R_index_up.size(), T_index_up.size()); i++) {
            float rVoltage = ecg_signal_origin[R_index_up.get(i)];
            float tVoltage = ecg_signal_origin[T_index_up.get(i)];
            float difference = Math.abs(rVoltage - tVoltage);
            voltageDifferences.add(difference);
        }

        return calculateMedian(voltageDifferences);
    }


    public float calDistanceDiffMed(List<Integer> R_index_up, List<Integer> T_index_up) {
        List<Float> distanceDifferences = new ArrayList<>();

        for (int i = 0; i < Math.min(R_index_up.size(), T_index_up.size()); i++) {
            float difference = Math.abs(R_index_up.get(i) - T_index_up.get(i));
            distanceDifferences.add(difference);
        }

        return calculateMedian(distanceDifferences);
    }

    /**
     * 取中位數
     */
    public float calculateMedian(List<Float> list) {
        Collections.sort(list);
        if (list.size() % 2 == 0) {
            return (list.get(list.size() / 2 - 1) + list.get(list.size() / 2)) / 2.0f;
        } else {
            return list.get(list.size() / 2);
        }
    }

    /**
     * 取最大值
     */
    public float calculateMax(List<Float> list) {
        return Collections.max(list);
    }

    /**
     * 取標準差
     */
    public float calculateSTD(List<Float> list) {
        float sum = 0, standardDeviation = 0;
        int length = list.size();

        for (float num : list) {
            sum += num;
        }
        float mean = sum / length;

        for (float num : list) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return (float) Math.sqrt(standardDeviation / length);
    }

    /**
     * 取半高寬
     */
    public List<Integer> calculateHalfWidths(Float[] ecg_signal, List<Integer> r_indexes) {
        List<Integer> halfWidths = new ArrayList<>();

        for (Integer r_index : r_indexes) {
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
            int halfWidth = rightIndex - leftIndex;
            halfWidths.add(halfWidth);
        }

        return halfWidths;
    }

    public int calculateHalfWidthsAverage(List<Integer> halfWidths) {
        int sum = 0;
        for (int halfWidth : halfWidths) {
            sum += halfWidth;
        }
        return sum / halfWidths.size();
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

    public Float[] butter_highpass_filter(List<Float> data, int lowCut, int highCut, int fs, int order) {
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
}

