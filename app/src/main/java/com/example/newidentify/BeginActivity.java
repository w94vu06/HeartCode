package com.example.newidentify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.newidentify.Util.CleanFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BeginActivity extends AppCompatActivity {
    Button btn_login, btn_signUp,btn_clear_lp4;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.begin_page);
        btn_signUp = findViewById(R.id.btn_signUp);
        btn_clear_lp4 = findViewById(R.id.btn_clear_lp4);
        initPermission();
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



}
