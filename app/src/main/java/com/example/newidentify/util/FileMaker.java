package com.example.newidentify.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FileMaker {

    private Context context;

    public FileMaker(Context context) {
        this.context = context;
    }
    
    public void saveByteArrayToFile(ArrayList<Byte> byteList, File file) throws IOException {
        byte[] byteArray = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            Byte byteValue = byteList.get(i);
            byteArray[i] = (byteValue != null) ? byteValue : 0;
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(byteArray);
        }
    }

}
