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
    static  public String printException(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        return e.toString() + "\n" + sStackTrace;
    }
}

//    int executeCommandLine()
//    {
//        //Log.d("executeCommandLine", "try execute cmd " + commandLine);
//        String path = "ff";
//
//        Log.d("executeCommandLine", Build.HARDWARE);
//        //String abis =
//        for (String item:Build.SUPPORTED_32_BIT_ABIS
//             ) {
//            Log.d("executeCommandLine", item);
//        }
//
//
//        try {
//
//            path = UzipFileFromAsset("stockfish_exe_32");
//            //path = UzipFileFromAsset("stockfish_10_x64");
//            //path = UzipFileFromAsset("test.txt");
//
//            File f = new File(path);
//
//            Log.d("executeCommandLine", "path:" + path);
//
//            Log.d("executeCommandLine", "space:" + f.length());
//
//
//
//            ProcessBuilder pbuilder = new ProcessBuilder(path);
//            //ProcessBuilder pbuilder = new ProcessBuilder("adb", "shell", "cat", "/proc/cpuinfo");
//            //adb shell cat /proc/cpuinfo
//            //Redirect.
//            //pbuilder.redirectOutput()
//            Process process = pbuilder.start();
//
//            //process.
//
//            InputStream output = process.getInputStream();
//            //InputStream output2 = getInputStream();
//
//            Log.d("executeCommandLine", "available:" + output.available());
//            //Log.d("executeCommandLine", "available:" + output.available());
//
//            process.waitFor();
//
//            Log.d("executeCommandLine", "available:" + output.available());
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            StringBuffer output2 = new StringBuffer();
//            char[] buffer = new char[4096];
//            int read;
//
//            while ((read = reader.read(buffer)) > 0) {
//                output2.append(buffer, 0, read);
//            }
//
////            while ((current = output.) >= 0)
////                Console.Write((char)current);
////
//
//
//            Log.d("executeCommandLine", "msg2:" + output2.toString());
//
//            return process.exitValue();
//
//        } catch (Exception e) {
//            Log.d("executeCommandLine", "ошибка: " + e.toString());
//            throw new RuntimeException("Unable to execute '" + path + "'", e);
//        }
//    }
