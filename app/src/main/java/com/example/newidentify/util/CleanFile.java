package com.example.newidentify.util;

import java.io.File;

public class CleanFile {

    public void cleanFile(String filePath) {
        deleteFile(filePath);
    }

    public void deleteFile(String filePath) {
        File folder = new File(filePath);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".cha")
                        || file.getName().endsWith(".CHA")
                        || file.getName().endsWith(".lp4")
                        || file.getName().endsWith(".txt")
                        || file.getName().endsWith(".csv")) {
                    if (file.delete()) {
                        System.out.println("Deleted: " + file.getAbsolutePath());
                    } else {
                        System.out.println("Failed to delete: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }
}
