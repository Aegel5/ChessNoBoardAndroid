package com.example.alex.chessnoboardandroid;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {
    static public String unzipExeFromAsset(String filename, Context context) throws IOException {
        File f = new File(context.getCacheDir() + "/" + filename);
        if (!f.exists()) {

            Log.d("unzipExeFromAsset", "not exist try create");

            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        }
        f.setExecutable(true);
        return f.getPath();
    }

    static public String printException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        return e.toString() + "\n" + sStackTrace;
    }
}
