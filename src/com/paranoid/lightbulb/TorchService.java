/*
 *   Copyright (C) 2013 The CyanogenMod Project
 *   This code has been modified. Portions copyright (C) 2013, ParanoidAndroid Project.
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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class TorchService extends Service {
    private int mFlashMode;

    private static final int MSG_UPDATE_FLASH = 1;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final FlashDevice flash = FlashDevice.instance(TorchService.this);

            switch (msg.what) {
                case MSG_UPDATE_FLASH:
                    flash.setFlashMode(mFlashMode);
                    break;
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        mFlashMode = FlashDevice.ON;

        mHandler.sendEmptyMessage(MSG_UPDATE_FLASH);

        PendingIntent turnOffIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(TorchSwitch.TOGGLE_FLASHLIGHT), 0);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_torch)
                .setTicker(getString(R.string.torch_enabled))
                .setContentTitle(getString(R.string.torch_enabled))
                .setContentText(getString(R.string.tap_to_turn_off))
                .setContentIntent(turnOffIntent)
                .build();

        startForeground(getString(R.string.app_name).hashCode(), notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        mHandler.removeCallbacksAndMessages(null);
        FlashDevice.instance(this).setFlashMode(FlashDevice.OFF);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
