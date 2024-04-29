package com.example.newidentify.util;

public interface FindPeaksCallback {
    void onFindPeaksCallback(int[] peaks);

    void onFindPeaksErrorCallback(String message);
}
