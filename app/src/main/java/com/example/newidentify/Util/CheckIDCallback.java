package com.example.newidentify.Util;

public interface CheckIDCallback {
    void onStatusUpdate(String result);

    void onCheckIDError(String result);

    void onResult(String result);

    void onDetectData(String result, String result2);

    void onDetectDataError(String result);

}
