package com.example.newidentify.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EcgMath {

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
     * 取標準差
     */

    public static double calculateStandardDeviation(double[] data) {
        double sum = 0.0;
        double mean = 0.0;
        double stdDev = 0.0;

        // 計算平均值
        for (double num : data) {
            sum += num;
        }
        mean = sum / data.length;

        // 計算標準差
        for (double num : data) {
            stdDev += Math.pow(num - mean, 2);
        }
        return Math.sqrt(stdDev / data.length);
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

            // 防止計算結果為0
            if (leftIndex == r_index || rightIndex == r_index) {
                continue; // 忽略這些點
            }

            // 計算並保存半高寬
            float halfWidth = rightIndex - leftIndex;
            halfWidths.add(halfWidth);
        }

        // 確保不返回0，至少返回一個合理的最小值
        if (halfWidths.isEmpty()) {
            return 0;
        }

        return averageArrayList((ArrayList<Float>) halfWidths);
    }

    /**
     * 計算平均值
     */

    public float averageArrayList(ArrayList<Float> list) {
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

    //List<Double> to ArrayList<Float>
    public ArrayList<Float> listDoubleToArrayListFloat(List<Double> doubleList) {
        if (doubleList == null) {
            throw new IllegalArgumentException("Input list cannot be null");
        }

        ArrayList<Float> floatList = new ArrayList<>();
        for (double value : doubleList) {
            floatList.add((float) value);
        }
        return floatList;
    }


    public double[] arrayListFloatToDoubleArray(ArrayList<Float> floatList) {
        if (floatList == null) {
            throw new IllegalArgumentException("Input list cannot be null");
        }

        double[] result = new double[floatList.size()];
        for (int i = 0; i < floatList.size(); i++) {
            Float value = floatList.get(i);
            if (value == null || value.isNaN()) {
                result[i] = Double.NaN;  // You could handle this differently depending on requirements
            } else {
                result[i] = value;
            }
        }
        return result;
    }

    public ArrayList<Float> doubleArrayToArrayListFloat(double[] array) {
        if (array == null) {
            throw new IllegalArgumentException("Input array cannot be null");
        }

        ArrayList<Float> floatList = new ArrayList<>();
        for (double value : array) {
            floatList.add((float) value);
        }
        return floatList;
    }

    public double[] listFloatToDoubleArray(List<Float> floatList) {
        if (floatList == null) {
            throw new IllegalArgumentException("Input list cannot be null");
        }

        double[] result = new double[floatList.size()];
        for (int i = 0; i < floatList.size(); i++) {
            result[i] = floatList.get(i);
        }
        return result;
    }

    public List<Float> doubleArrayToListFloat(double[] array) {
        if (array == null) {
            throw new IllegalArgumentException("Input array cannot be null");
        }

        List<Float> floatList = new ArrayList<>();
        for (double value : array) {
            floatList.add((float) value);
        }
        return floatList;
    }

    public List<Double> listFloatToListDouble(List<Float> floatList) {
        if (floatList == null) {
            throw new IllegalArgumentException("Input list cannot be null");
        }


        List<Double> doubleList = new ArrayList<>();
        for (float value : floatList) {
            doubleList.add((double) value);
        }
        return doubleList;
    }

    public List<Double> listDoubleToListFloat(List<Double> doubleList) {
        if (doubleList == null) {
            throw new IllegalArgumentException("Input list cannot be null");
        }

        List<Double> floatList = new ArrayList<>();
        floatList.addAll(doubleList);
        return floatList;
    }

    public List<Integer> listDoubleToListInt(List<Double> doubleList) {
        if (doubleList == null) {
            throw new IllegalArgumentException("Input list cannot be null");
        }

        List<Integer> intList = new ArrayList<>();
        for (double value : doubleList) {
            intList.add((int) Math.round(value)); // 使用 Math.round 來四捨五入
        }
        return intList;
    }

    public double[] listDoubleToDoubleArray(List<Double> doubleList) {
        if (doubleList == null) {
            throw new IllegalArgumentException("Input list cannot be null");
        }

        double[] result = new double[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
            result[i] = doubleList.get(i);
        }
        return result;
    }


    public float[] convertDoubleArrayToFloatArray(double[] input) {
        float[] result = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = (float) input[i];
        }
        return result;
    }

    public double[] convertFloatArrayToDoubleArray(float[] input) {
        double[] result = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = (double) input[i];
        }
        return result;
    }

}
