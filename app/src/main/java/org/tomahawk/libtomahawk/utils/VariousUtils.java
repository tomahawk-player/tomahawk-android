package org.tomahawk.libtomahawk.utils;

import java.io.File;
import java.io.FileNotFoundException;

public class VariousUtils {

    public static final String TAG = VariousUtils.class.getSimpleName();

    public static boolean containsIgnoreCase(String str1, String str2) {
        return str1.toLowerCase().contains(str2.toLowerCase());
    }

    /**
     * By default File#delete fails for non-empty directories, it works like "rm". We need something
     * a little more brutal - this does the equivalent of "rm -r"
     *
     * @param path Root File Path
     * @return true if the file and all sub files/directories have been removed
     */
    public static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists()) {
            throw new FileNotFoundException(path.getAbsolutePath());
        }
        boolean ret = true;
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }

}
