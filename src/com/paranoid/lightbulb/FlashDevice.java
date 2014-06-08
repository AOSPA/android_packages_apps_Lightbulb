/*
 *   Copyright (C) 2013 The CyanogenMod Project
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.paranoid.lightbulb;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.io.FileWriter;
import java.io.IOException;

import javax.microedition.khronos.opengles.GL10;

public class FlashDevice {

    public static final String TORCH_STATUS_CHANGED = "TORCH_STATUS_CHANGED";
    public static final String TORCH_MODE = "TORCH_MODE";

    public static final int OFF = 0;
    public static final int ON = 1;

    private static boolean mUseCameraInterface;
    private static String mFlashDevice;
    private FileWriter mFlashDeviceWriter = null;

    private static FlashDevice sInstance;

    private boolean mSurfaceCreated = false;
    private int mFlashMode = OFF;
    private Camera mCamera = null;
    private Context mContext;

    private FlashDevice(Context context) {
        mUseCameraInterface = context.getResources().getBoolean(R.bool.useCameraInterface);
        mFlashDevice = context.getResources().getString(R.string.flashDevice);
        mContext = context;
    }

    public static synchronized FlashDevice instance(Context context) {
        if (sInstance == null) {
            sInstance = new FlashDevice(context.getApplicationContext());
        }
        return sInstance;
    }

    public synchronized void setFlashMode(final int mode) {
        if (Utils.deviceHasCameraFlash(mContext)) {
            try {
                mFlashMode = mode;
                if (mUseCameraInterface) {
                    if (mCamera == null) {
                        mCamera = Camera.open();
                    }
                    if (mode == OFF) {
                        Camera.Parameters params = mCamera.getParameters();
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(params);
                        mCamera.stopPreview();
                        mCamera.release();
                        mCamera = null;
                        mSurfaceCreated = false;
                    } else {
                        if (!mSurfaceCreated) {
                            int[] textures = new int[1];
                            // generate one texture pointer and bind it as an
                            // external texture.
                            GLES20.glGenTextures(1, textures, 0);
                            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                                    textures[0]);
                            // No mip-mapping with camera source.
                            GLES20.glTexParameterf(
                                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                                    GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
                            GLES20.glTexParameterf(
                                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                                    GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
                            // Clamp to edge is only option.
                            GLES20.glTexParameteri(
                                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                                    GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
                            GLES20.glTexParameteri(
                                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                                    GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

                            SurfaceTexture surfaceTexture = new SurfaceTexture(textures[0]);
                            try {
                                mCamera.setPreviewTexture(surfaceTexture);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mSurfaceCreated = true;
                            mCamera.startPreview();
                        }
                        Camera.Parameters params = mCamera.getParameters();
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(params);
                    }
                } else {
                    // Devices with sysfs toggle
                    if (mFlashDeviceWriter == null) {
                        mFlashDeviceWriter = new FileWriter(mFlashDevice);
                    }
                    if (mode == OFF) {
                        mFlashDeviceWriter.write(String.valueOf(0));
                        mFlashDeviceWriter.close();
                        mFlashDeviceWriter = null;
                    } else {
                        mFlashDeviceWriter.write(String.valueOf(1));
                        mFlashDeviceWriter.flush();
                    }
                }
            } catch (IOException e) { // no flash?
                if (mCamera != null) {
                    mCamera.release();
                }
            }
        } else {
            mFlashMode = mode;
        }
        Intent i = new Intent();
        i.setAction(TORCH_STATUS_CHANGED);
        i.putExtra(TORCH_MODE, mode);
        mContext.sendBroadcast(i);
    }

    public synchronized int getFlashMode() {
        return mFlashMode;
    }
}
