package com.example.newidentify.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EcgMath {

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
