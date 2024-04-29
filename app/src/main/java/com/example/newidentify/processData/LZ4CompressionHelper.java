package com.example.newidentify.processData;

import net.jpountz.lz4.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LZ4CompressionHelper {
    public static byte[] decompress(byte[] compressedData) throws IOException {
        // 创建一个 ByteArrayInputStream 以便从压缩数据的字节数组中读取数据
        ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);
        // 创建一个 ByteArrayOutputStream 以便写入解压缩后的数据
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 使用 LZ4FrameInputStream 进行解压缩
        try (LZ4FrameInputStream lz4In = new LZ4FrameInputStream(inputStream)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = lz4In.read(buffer)) != -1) {
                // 将解压缩后的数据写入 ByteArrayOutputStream
                outputStream.write(buffer, 0, length);
            }
        }

        // 获取解压缩后的数据
        return outputStream.toByteArray();
    }
}

