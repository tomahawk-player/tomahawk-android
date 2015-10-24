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
public class UnzipUtility {

    private final static String TAG = UnzipUtility.class.getSimpleName();

    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by destDirectory
     * (will be created if does not exists)
     */
    public static boolean unzip(Uri zipFilePath, String destDirectory) {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        ZipInputStream zipIn = null;
        try {
            InputStream inputStream;
            if (zipFilePath.getScheme().contains("file")) {
                inputStream = new FileInputStream(zipFilePath.getPath());
            } else if (zipFilePath.getScheme().contains("http")) {
                Response response = null;
                try {
                    response = NetworkUtils.httpRequest("GET", zipFilePath.toString(), null,
                            null, null, null, true);
                    inputStream = response.body().byteStream();
                } finally {
                    if (response != null) {
                        try {
                            response.body().close();
                        } catch (IOException e) {
                            Log.e(TAG, "parse: " + e.getClass() + ": " + e.getLocalizedMessage());
                        }
                    }
                }
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
                    dir.mkdirs();
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
        dir.mkdirs();
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}