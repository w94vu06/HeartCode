package com.example.newidentify.Util;

public interface CheckIDCallback {
    void onStatusUpdate(String result);
    void onCheckIDError(String result);
    void onResult(String result);
}
