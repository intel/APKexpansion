package org.apache.cordova.xapkreader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;
import com.google.android.vending.expansion.downloader.Helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

public class XAPKReader extends CordovaPlugin {

    private static final String LOG_TAG = "XAPKReader";

    private int mainVersion = 1;

    private long mainFileSize = 0L;

    private int patchVersion = 0;

    private long patchFileSize = 0L;

    private boolean downloadOption = true;

    /**
     * Executes the request.
     *
     * This method is called from the WebView thread. To do a non-trivial amount of work, use:
     *     cordova.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     *     cordova.getActivity().runOnUiThread(runnable);
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return                Whether the action was valid.
     * @throws JSONException
     *
     * @sa https://github.com/apache/cordova-android/blob/master/framework/src/org/apache/cordova/CordovaPlugin.java
     */
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        int downloadOptionId = cordova.getActivity().getResources().getIdentifier("download_option", "bool", cordova.getActivity().getPackageName());
        downloadOption = cordova.getActivity().getResources().getBoolean(downloadOptionId);

        int mainversionCodeId = cordova.getActivity().getResources().getIdentifier("main_version", "integer", cordova.getActivity().getPackageName());
        mainVersion = cordova.getActivity().getResources().getInteger(mainversionCodeId);

        int mainFileSizeId = cordova.getActivity().getResources().getIdentifier("main_filesize", "integer", cordova.getActivity().getPackageName());
        mainFileSize = cordova.getActivity().getResources().getInteger(mainFileSizeId);

        //This is where the error may occur.
        int patchVersionCodeId = cordova.getActivity().getResources().getIdentifier("patch_version", "integer", cordova.getActivity().getPackageName());
        patchVersion = cordova.getActivity().getResources().getInteger(patchVersionCodeId);

        int patchFileSizeId = cordova.getActivity().getResources().getIdentifier("patch_filesize", "integer", cordova.getActivity().getPackageName());
        patchFileSize = cordova.getActivity().getResources().getInteger(patchFileSizeId);

        final Bundle bundle = new Bundle();
        bundle.putInt("mainVersion", mainVersion);
        bundle.putInt("patchVersion", patchVersion);
        bundle.putLong("mainFileSize", mainFileSize);
        bundle.putLong("patchFileSize", patchFileSize);
        bundle.putBoolean("downloadOption", downloadOption);

        if (action.equals("get")) {
            final String filename = args.getString(0);
            final Context ctx = cordova.getActivity().getApplicationContext();
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        Context context = cordova.getActivity().getApplicationContext();
                        Intent intent = new Intent(context, XAPKDownloaderActivity.class);
                        intent.putExtras(bundle);
                        cordova.getActivity().startActivity(intent);
                        // Read file
                        PluginResult result = XAPKReader.readFile(ctx, filename, mainVersion, patchVersion, PluginResult.MESSAGE_TYPE_ARRAYBUFFER);
                        callbackContext.sendPluginResult(result);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                        callbackContext.error(e.getLocalizedMessage());
                    }
                }
            });
            return true;
        }

        if (action.equals("checkPermissions")) {

            XAPKReader.verifyStoragePermissions(cordova.getActivity());

            // final String filename = args.getString(0);
            // final Context ctx = cordova.getActivity().getApplicationContext();
            // cordova.getThreadPool().execute(new Runnable() {
            //     public void run() {
            //         try {
            //             Context context = cordova.getActivity().getApplicationContext();
            //             Intent intent = new Intent(context, XAPKDownloaderActivity.class);
            //             intent.putExtras(bundle);
            //             cordova.getActivity().startActivity(intent);
            //             // Read file
            //             PluginResult result = XAPKReader.readFile(ctx, filename, mainVersion, patchVersion, PluginResult.MESSAGE_TYPE_ARRAYBUFFER);
            //             callbackContext.sendPluginResult(result);
            //         }
            //         catch(Exception e) {
            //             e.printStackTrace();
            //             callbackContext.error(e.getLocalizedMessage());
            //         }
            //     }
            // });
            return true;
        }
        return false;
    }

    /**
     * Read file in APK Expansion file.
     *
     * @param ctx      The context of the main Activity.
     * @param filename The filename to read
     * @return         PluginResult
     */
    private static PluginResult readFile(Context ctx, String filename, int mainVersion, int patchVersion, final int resultType) throws IOException {
        // Get APKExpensionFile
        ZipResourceFile expansionFile = APKExpansionSupport.getAPKExpansionZipFile(ctx, mainVersion, patchVersion);

        if (null == expansionFile) {
            Log.e(LOG_TAG, "APKExpansionFile not found.");
            return null;
        }

        // Find file in ExpansionFile
        String fileName = Helpers.getExpansionAPKFileName(ctx, true, mainVersion);
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        AssetFileDescriptor fileDescriptor = expansionFile.getAssetFileDescriptor(fileName + "/" + filename);

        if (null == fileDescriptor) {
			fileDescriptor = expansionFile.getAssetFileDescriptor(filename);
			if (null == fileDescriptor) {
				Log.e(LOG_TAG, "File not found (" + filename + ").");
                return null;
            }
        }

        // Read file
        InputStream inputStream = fileDescriptor.createInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
            os.write(buffer, 0, read);
        }
        os.flush();

        // get file content type
        String contentType = URLConnection.guessContentTypeFromStream(inputStream);

        PluginResult result;
        switch (resultType) {
            case PluginResult.MESSAGE_TYPE_STRING:
                result = new PluginResult(PluginResult.Status.OK, os.toString("UTF-8"));
                break;
            case PluginResult.MESSAGE_TYPE_ARRAYBUFFER:
                result = new PluginResult(PluginResult.Status.OK, os.toByteArray());
                break;
            case PluginResult.MESSAGE_TYPE_BINARYSTRING:
                result = new PluginResult(PluginResult.Status.OK, os.toByteArray(), true);
                break;
            default: // Base64.
                byte[] base64 = Base64.encode(os.toByteArray(), Base64.NO_WRAP);
                String s = "data:" + contentType + ";base64," + new String(base64, "US-ASCII");
                result = new PluginResult(PluginResult.Status.OK, s);
        }

        return result;
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    private static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

}
