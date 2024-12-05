/*
    The MIT License (MIT)
    Copyright (c) 2013 Vlad Stirbu
    
    Permission is hereby granted, free of charge, to any person obtaining
    a copy of this software and associated documentation files (the
    "Software"), to deal in the Software without restriction, including
    without limitation the rights to use, copy, modify, merge, publish,
    distribute, sublicense, and/or sell copies of the Software, and to
    permit persons to whom the Software is furnished to do so, subject to
    the following conditions:
    
    The above copyright notice and this permission notice shall be
    included in all copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
    NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
    LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
    OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
    WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.vladstirbu.cordova;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import androidx.core.content.FileProvider;

@TargetApi(Build.VERSION_CODES.FROYO)
public class CDVInstagramPlugin extends CordovaPlugin {
    public static final String SUCCESS = "SUCCESS";

    //packages
    private final String INSTAGRAM_PACKAGE = "com.instagram.android";
    private final String INSTAGRAM_STORY_PACKAGE = "com.instagram.share.ADD_TO_STORY";
    private final String INSTAGRAM_FEED_PACKAGE = "com.instagram.share.ADD_TO_FEED";
    private static final String MEDIA_TYPE_IMAGE = "image/*";

    private static final FilenameFilter OLD_IMAGE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith("instagram");
        }
    };

    CallbackContext cbContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.cbContext = callbackContext;

        if (action.equals("share")) {
            String imageString = args.getString(0);
            String captionString = args.getString(1);
            String topColor = args.getString(2);
            String bottomColor = args.getString(3);

            PluginResult result = new PluginResult(Status.NO_RESULT);
            result.setKeepCallback(true);

            this.share(imageString, captionString, topColor, bottomColor);
            return true;
        } else if (action.equals("isInstalled")) {
            this.isInstalled();
        } else {
            callbackContext.error("Invalid Action");
        }
        return false;
    }

    private void isInstalled() {
        try {
            this.webView.getContext().getPackageManager().getApplicationInfo("com.instagram.android", 0);
            this.cbContext.success(this.webView.getContext().getPackageManager().getPackageInfo("com.instagram.android", 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            this.cbContext.error("Application not installed");
        }
    }

    private void share(String imageString, String captionString, String topBackgroundColor, String
            bottomBackgroundColor) {
        this.shareToStory(imageString, captionString, topBackgroundColor, bottomBackgroundColor);
    }

    private File getFileFromBase64String(String imageString){
        byte[] imageData = Base64.decode(imageString, 0);

        File file = null;
        FileOutputStream os = null;

        File parentDir = this.webView.getContext().getExternalFilesDir(null);
        File[] oldImages = parentDir.listFiles(OLD_IMAGE_FILTER);
        for (File oldImage : oldImages) {
            oldImage.delete();
        }

        try {
            file = File.createTempFile("instagram", ".png", parentDir);
            os = new FileOutputStream(file, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            os.write(imageData);
            os.flush();
            os.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return file;
    }
    private void shareToStory(String imageString, String captionString, String backgroundBottomColor, String backgroundTopColor) {
        try {
            File backgroundFile = null; //getFileFromBase64String(imageString);
            File stickerFile = getFileFromBase64String(imageString);
            String attributionLink = "https://app.brisamusic.com.br/";
            String applicationId = "486708520874477";
            Intent intent = new Intent("com.instagram.share.ADD_TO_STORY");
            intent.putExtra("source_application", applicationId);
            String providerName = this.cordova.getActivity().getPackageName() + ".provider";
            Activity activity = this.cordova.getActivity();

            if (backgroundFile != null){
                Uri backgroundImageUri = FileProvider.getUriForFile(activity, providerName, backgroundFile);

                //intent.setDataAndType(backgroundImageUri, MEDIA_TYPE_IMAGE);
                intent.setDataAndType(backgroundImageUri, MEDIA_TYPE_IMAGE);
            } else {
                intent.setType(MEDIA_TYPE_IMAGE);
            }

            if(stickerFile != null){
                //
                Uri stickerAssetUri = FileProvider.getUriForFile(activity, providerName, stickerFile);

                intent.putExtra("interactive_asset_uri", stickerAssetUri );
                activity.grantUriPermission(
                        "com.instagram.android", stickerAssetUri , Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if(backgroundBottomColor != null){
                intent.putExtra("bottom_background_color", backgroundBottomColor);
            }

            if(backgroundTopColor != null){
                intent.putExtra("top_background_color", backgroundTopColor);
            }

            if(attributionLink != null){
                intent.putExtra("content_url", attributionLink);
            }
            //this.cordova.startActivityForResult((CordovaPlugin) this, shareIntent, 12345);
            if (activity.getPackageManager().resolveActivity(intent, 0) != null) {
                // activity.startActivityForResult(intent, 0);
                this.cordova.startActivityForResult((CordovaPlugin) this, intent, 12345);
            }else{
                throw new Exception("Couldn't open intent");
            }
        }catch (Exception e){
            e.printStackTrace();
            this.cbContext.error("Error sharing to Instagram");
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Log.v("Instagram", "shared ok");
            if(this.cbContext != null) {
                this.cbContext.success();
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.v("Instagram", "share cancelled");
            if(this.cbContext != null) {
                this.cbContext.error("Share Cancelled");
            }
        }
    }
}
