package com.example.newidentify.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EcgMath {

    /**
     * 取最大值
     */
    public float calculateMax(List<Float> list) {
        return Collections.max(list);
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
     * 取中位數 double
     */
    public double calculateMedianDouble(ArrayList<Integer> values) {
        Collections.sort(values);
        int size = values.size();
        if (size % 2 == 0) {
            return ((double) values.get(size / 2 - 1) + (double) values.get(size / 2)) / 2.0;
        } else {
            return (double) values.get(size / 2);
        }
    }

    public int calculateAverage(List<Integer> halfWidths) {
        int sum = 0;
        for (int halfWidth : halfWidths) {
            sum += halfWidth;
        }
        return sum / halfWidths.size();
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
}
