
package com.lister.flavourpackagetest;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;

public class FlavorUtils {

    private static final int CENTRAL_DIRECTORY_END_SIGN = 0x06054b50;
    private static final String SIGNATURE_MAGIC_NUMBER = "APK Sig Block 42";
    private static final int CHANNEL_KV_ID = 0x010101;
    private static boolean sChannelInited = false;
    private static String sChannel = "unknown";

    public static String getSignatureInfo(Context context) {
        if (sChannelInited) {
            return sChannel;
        }
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        String sourceDir = applicationInfo.sourceDir;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(sourceDir);
            String zipComment = zipFile.getComment();
            int commentLength = 0;
            if (!TextUtils.isEmpty(zipComment)) {
                commentLength = zipComment.getBytes().length;
            }
            File file = new File(sourceDir);

            long fileLength = file.length();
            byte[] centralEndSignBytes = readReserveData(file, fileLength - 22, 4);
            int centralEndSign = ByteBuffer.wrap(centralEndSignBytes).getInt();
            if (centralEndSign != CENTRAL_DIRECTORY_END_SIGN) {
                sChannel = "unknown1";
                sChannelInited = true;
                return sChannel;
            }

            long eoCdrLength = commentLength + 22;
            long eoCdrOffset = file.length() - eoCdrLength;
            long pointer = eoCdrOffset + 16;
            byte[] pointerBuffer = readReserveData(file, pointer, 4);
            int centralDirectoryOffset = ByteBuffer.wrap(pointerBuffer).getInt();

            byte[] buffer = readDataByOffset(file, centralDirectoryOffset - 16, 16);
            String checkV2Signature = new String(buffer, StandardCharsets.UTF_8);
            if (!TextUtils.equals(checkV2Signature, SIGNATURE_MAGIC_NUMBER)) {
                sChannel = "unknown_v2_error";
                sChannelInited = true;
                return sChannel;
            }

            long signBlockEnd = centralDirectoryOffset - 24;
            byte[] sigSizeInEndBuffer = readReserveData(file, signBlockEnd, 8);
            long sigSizeInEnd = ByteBuffer.wrap(sigSizeInEndBuffer).getLong();

            long signBlockStart = signBlockEnd - sigSizeInEnd + 16;
            byte[] sigSizeInStartBuffer = readReserveData(file, signBlockStart, 8);
            long sigSizeInStart = ByteBuffer.wrap(sigSizeInStartBuffer).getLong();

            if (sigSizeInEnd != sigSizeInStart) {
                sChannel = "unknown_sigSize_error";
                sChannelInited = true;
                return sChannel;
            }

            long curKvOffset = signBlockStart + 8;
            for (int i = 0; i < 5; i++) {
                byte[] kvSizeBytes = readReserveData(file, curKvOffset, 8);
                long kvSize = ByteBuffer.wrap(kvSizeBytes).getLong();
                byte[] idBuffer = readReserveData(file, curKvOffset + 8, 4);
                int id = ByteBuffer.wrap(idBuffer).getInt();
                if (id == CHANNEL_KV_ID) {
                    int channelSize = (int) (kvSize - 4);
                    byte[] channelBytes = readDataByOffset(file, curKvOffset + 12, channelSize);
                    sChannel = new String(channelBytes, StandardCharsets.UTF_8);
                    sChannelInited = true;
                    return sChannel;
                }
                curKvOffset = curKvOffset + 8 + kvSize;
                if (curKvOffset >= signBlockEnd) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sChannel;
    }

    private static byte[] readDataByOffset(File file, long offset, int length) throws Exception {
        InputStream is = new FileInputStream(file);
        is.skip(offset);
        byte[] buffer = new byte[length];
        is.read(buffer, 0, length);
        is.close();
        return buffer;
    }

    private static byte[] readReserveData(File file, long offset, int length) throws Exception {
        byte[] buffer = readDataByOffset(file, offset, length);
        reserveByteArray(buffer);
        return buffer;
    }

    private static void reserveByteArray(byte[] bytes) {
        int length = bytes.length;
        for (int i = 0; i < length / 2; i++) {
            byte temp = bytes[i];
            bytes[i] = bytes[length - 1 - i];
            bytes[length - 1 - i] = temp;
        }
    }

}
