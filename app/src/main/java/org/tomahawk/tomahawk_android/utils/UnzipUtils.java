package org.tomahawk.tomahawk_android.utils;

import com.squareup.okhttp.Response;

import org.tomahawk.libtomahawk.utils.NetworkUtils;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This utility extracts files and directories of a standard zip file to a destination directory.
 *
 * @author www.codejava.net
 */
public class UnzipUtils {

    private final static String TAG = UnzipUtils.class.getSimpleName();

    /**
     * Size of the buffer (in bytes) to read/write data
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by destDirectory
     * (will be created if it doesn't exist)
     */
    public static boolean unzip(Uri zipFilePath, String destDirectory) {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            boolean success = destDir.mkdirs();
            if (!success) {
                Log.e(TAG, "unzip - Wasn't able to create directory: " + destDirectory);
            }
        }
        ZipInputStream zipIn = null;
        Response response = null;
        try {
            InputStream inputStream;
            if (zipFilePath.getScheme().contains("file")) {
                inputStream = new FileInputStream(zipFilePath.getPath());
            } else if (zipFilePath.getScheme().contains("http")) {
                response = NetworkUtils.httpRequest("GET", zipFilePath.toString(), null,
                        null, null, null, true, null);
                inputStream = response.body().byteStream();
            } else {
                Log.e(TAG, "unzip - Can't handle URI scheme");
                return false;
            }
            zipIn = new ZipInputStream(inputStream);
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    extractFile(zipIn, filePath);
                } else {
                    // if the entry is a directory, make the directory
                    File dir = new File(filePath);
                    boolean success = dir.mkdirs();
                    if (!success) {
                        Log.e(TAG, "unzip - Wasn't able to create directory: " + filePath);
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        } catch (IOException e) {
            Log.e(TAG, "unzip: " + e.getClass() + ": " + e.getLocalizedMessage());
        } finally {
            try {
                if (zipIn != null) {
                    zipIn.close();
                }
                if (response != null) {
                    response.body().close();
                }
            } catch (IOException e) {
                Log.e(TAG, "unzip: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
        return true;
    }

    /**
     * Extracts a zip entry (file entry)
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        File dir = new File(filePath).getParentFile();
        boolean success = dir.mkdirs();
        if (!success) {
            Log.e(TAG, "extractFile - Wasn't able to create directory: " + filePath);
        }
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}