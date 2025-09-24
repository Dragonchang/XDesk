/**
 *
 */
package com.xd.lib_can;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author jinyf
 */
public class ShellUtil {

    private static final String TAG = "TAG";

    /**
     * @param command
     * @return
     */
    public static String execCommand(String... command) {
        Process process = null;
        InputStream errIs = null;
        InputStream inIs = null;
        String result = "";
        try {
            process = new ProcessBuilder().command(command).start();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = -1;

            errIs = process.getErrorStream();
            while ((read = errIs.read()) != -1) {
                baos.write(read);
            }
            result = new String(baos.toByteArray());
            Log.e(TAG, result);

            inIs = process.getInputStream();
            while ((read = inIs.read()) != -1) {
                baos.write(read);
            }

            if (inIs != null) {
                inIs.close();
            }
            if (errIs != null) {
                errIs.close();
            }

            process.destroy();
        } catch (IOException e) {
            result = e.getMessage();
            Log.e(TAG, result);
        }
        return result;
    }


    public static String execCommand(String[] commands, boolean isRoot,
                                     boolean isNeedResultMsg) {
        int result = -1;
        if (commands == null || commands.length == 0) {
            return "commands == null || commands.length == 0";
        }

        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;

        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec(
                    isRoot ? "su" : "sh");
            os = new DataOutputStream(process.getOutputStream());

            for (String command : commands) {
                if (command == null) {
                    continue;
                }
                // donnot use os.writeBytes(commmand), avoid chinese charset
                // error
                os.write(command.getBytes());
                os.writeBytes("\n");
                os.flush();
            }
            os.writeBytes("exit\n");
            os.flush();

            result = process.waitFor();
            // get command result
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                successResult = new BufferedReader(new InputStreamReader(
                        process.getInputStream()));
                errorResult = new BufferedReader(new InputStreamReader(
                        process.getErrorStream()));
                String s;
                while ((s = successResult.readLine()) != null) {
                    successMsg.append(s);
                }
                while ((s = errorResult.readLine()) != null) {
                    errorMsg.append(s);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (process != null) {
                process.destroy();
            }
        }


        String sm = successMsg == null ? "null" : successMsg.toString();
        String em = errorMsg == null ? "null" : errorMsg.toString();
        String finalResult = "result:" + result
                + "==successMsg:" + sm
                + "==errorMsg:" + em;

        Log.e("ShellUtil",finalResult);
        return sm;
    }
}
