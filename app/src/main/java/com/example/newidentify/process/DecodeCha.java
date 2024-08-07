package com.example.newidentify.process;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DecodeCha extends Thread {
    public String filePath;
    public CellData cell;
    public double[] ecgSignal;
    private static final String TAG = "DecodeCha";

    public DecodeCha(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void run() {
        File chaFile = new File(filePath);
        // 檢查CHA檔案是否存在
        if (!chaFile.exists()) {
            Log.e(TAG, "DecodeCha: " + "CHA file not exist");
            return;
        }
        super.run();
        try {
            int x = 64;
            char[] a = new char[32 * 1024 * 1024];
            int cha_size = len(filePath, a);
            byte[] content = readFromByteFile(filePath);
            /** Lead1 */
            ArrayList CHA_LI_dataNum = new ArrayList();
            ArrayList CHA_LI_data16 = new ArrayList();
            ArrayList CHA_LI_dataSample = new ArrayList();
            ArrayList CHA_LI_data4 = new ArrayList();
            byte[] c1 = new byte[cha_size];
            for (int L1 = 0; L1 < cha_size; L1 += 136) {
                int y = L1 + x;
                int j = 0;
                for (int k = L1; k < y; k++) {
                    c1[j] = content[k];
                    j++;
                }
                oneCellData(c1, c1.length, 1);
                CHA_LI_dataNum.add(cell.getLen());
                CHA_LI_data16.add(cell.getList(1));
                CHA_LI_dataSample.add(cell.getList(2));
                CHA_LI_data4.add(cell.getList(3));
            }
            int startS = 0;
            double dou = Math.floor((startS * 1000) / 32);
            int pic = (int) dou;
            double sample_len = CHA_LI_dataSample.size();
            double dou_2 = (sample_len / 32) * 31 - 5;
            int picEnd = (int) dou_2;

            /** Lead2 */
            ArrayList CHA_LII_dataNum = new ArrayList();
            ArrayList CHA_LII_data16 = new ArrayList();
            ArrayList CHA_LII_dataSample = new ArrayList();
            ArrayList CHA_LII_data4 = new ArrayList();
            ArrayList<String> sampleValue = new ArrayList();
            List<Float> floatData = new ArrayList<>();
            byte[] c2 = new byte[cha_size];
            for (int L2 = 64; L2 < cha_size; L2 += 136) {
                int y = L2 + x;
                int j = 0;
                for (int k = L2; k < y; k++) {
                    c2[j] = content[k];
                    j++;
                }
                oneCellData(c2, c2.length, 2);
                CHA_LII_dataNum.add(cell.getLen());
                CHA_LII_data16.add(cell.getList(1));
                CHA_LII_dataSample.add(cell.getList(2));
                CHA_LII_data4.add(cell.getList(3));
            }
            for (int i = pic; i < picEnd; i++) {
                List s = Collections.singletonList(CHA_LII_dataSample.get(i));
                int y = s.get(0).toString().length();
                String f = s.get(0).toString().substring(1, y - 1).replaceAll(" ", "");
                List<String> myList = new ArrayList<>(Arrays.asList(f.split(",")));
                sampleValue.addAll(myList);
            }
            for (int i = 0; i < sampleValue.size(); i++) {
                int k = Integer.parseInt(sampleValue.get(i));
                floatData.add(Float.valueOf(sampleValue.get(i)));
                floatData.set(i, (float) (((k - 2048) * 5) * 0.001));
            }

            if (floatData.size() > 8000) { // 確保subList的範圍有效
                List<Float> subList = floatData.subList(6000, floatData.size() - 2000);
                List<Float> cleanedList = new ArrayList<>();

                for (Float value : subList) {
                    if (!value.isNaN()) {
                        cleanedList.add(value);
                    }
                }

                ecgSignal = new double[cleanedList.size()];
                for (int i = 0; i < cleanedList.size(); i++) {
                    ecgSignal[i] = cleanedList.get(i);
                }
            } else {
                System.out.println("floatData的長度小於8000，沒有足夠的資料添加。");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 解析CHA檔案
    public void oneCellData(byte[] cha, int len, int c) {
        ArrayList<String> onecell = new ArrayList();
        for (int i = 0; i < 64; i++) {
            String newcha = Integer.toBinaryString(cha[i]);
            int wordnum = newcha.length();
            if (wordnum != 8) {
                for (int k = 0; k < (8 - wordnum); k++) {
                    String x = '0' + newcha;
                    newcha = x;
                }
                if (wordnum > 8) {
                    String x = newcha.substring(24);
                    newcha = x;
                }
            } else {
                newcha = Integer.toBinaryString(cha[i]);
                String x = newcha.substring(0, 8);
                newcha = x;
            }
            onecell.add(newcha);
        }

        ArrayList<String> newCell = new ArrayList<>();
        for (int i = 0; i < 64; i += 2) {
            String low = onecell.get(i);
            String hi = onecell.get(i + 1);
            String new16 = hi + low;
            newCell.add(new16);
        }

        ArrayList<Integer> spv = new ArrayList<>();
        ArrayList<String> dataV = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            String sp = newCell.get(i).substring(4, 16);
            String dat = newCell.get(i).substring(0, 4);
            int x = Integer.parseInt(sp, 2);
            spv.add(x);
            dataV.add(dat);
        }
        cell = new CellData(len, newCell, spv, dataV);
    }

    //拿到cha檔案的長度
    public static int len(String path, char[] a) {
        File f = new File(path);
        try {
            FileInputStream fs = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fs);
            InputStreamReader isr = new InputStreamReader(in, StandardCharsets.ISO_8859_1);
            BufferedReader br = new BufferedReader(isr);
            int length;
            length = br.read(a, 0, 32 * 1024 * 1024);
            br.close();
            isr.close();
            in.close();
            fs.close();
            return length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 讀取CHA檔案
    public static byte[] readFromByteFile(String pathname) {
        File filename = new File(pathname);
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            byte[] temp = new byte[1024];
            int size = 0;
            while ((size = in.read(temp)) != -1) {
                out.write(temp, 0, size);
            }
            in.close();
            byte[] content = out.toByteArray();
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] a = new byte[1024];
        return a;
    }
}


class CellData {
    private final int len;
    private final ArrayList<String> newcell;
    private final ArrayList<String> datav;
    private final ArrayList<Integer> spv;

    public CellData(int len, ArrayList newcell, ArrayList spv, ArrayList datav) {
        this.len = len;
        this.newcell = newcell;
        this.spv = spv;
        this.datav = datav;
    }

    public ArrayList getList(int i) {
        if (i == 1) {
            return newcell;
        } else if (i == 2) {
            return spv;
        } else {
            return datav;
        }
    }

    public int getLen() {
        return len;
    }
}
