package com.example.newidentify;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.newidentify.util.CleanFile;

import java.util.ArrayList;
import java.util.List;

public class BeginActivity extends AppCompatActivity {
    Button btn_signUp,btn_clear_lp4;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.begin_page);
        btn_signUp = findViewById(R.id.btn_signUp);
        btn_clear_lp4 = findViewById(R.id.btn_clear_lp4);
        initPermission();
        checkStorageManagerPermission();//檢查儲存權限
        Intent it = new Intent();

        btn_signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                it.setClass(BeginActivity.this, MainActivity.class);
                startActivity(it);
            }
        });

        btn_clear_lp4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(BeginActivity.this, "清除中...", Toast.LENGTH_SHORT).show();
                CleanFile cleanFile = new CleanFile();
                cleanFile.cleanFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "");
                Toast.makeText(BeginActivity.this, "已清除量測檔案", Toast.LENGTH_SHORT).show();
            }
        });

        //初始化python環境
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    /**
     * 檢查權限
     **/
    private void initPermission() {
        List<String> mPermissionList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mPermissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            mPermissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            mPermissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        mPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        mPermissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        mPermissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        ActivityCompat.requestPermissions(this, mPermissionList.toArray(new String[0]), 1001);
    }

    private void checkStorageManagerPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) {
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("本程式需要您同意允許存取所有檔案權限");
            builder.setPositiveButton("同意", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            });
            builder.show();
        }
    }



}
