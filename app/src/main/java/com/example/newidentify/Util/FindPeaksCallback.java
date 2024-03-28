package com.example.newidentify.Util;

public interface FindPeaksCallback {
    void onFindPeaksCallback(int[] peaks);

    void onFindPeaksErrorCallback(String message);
}
