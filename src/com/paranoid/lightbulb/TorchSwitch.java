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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TorchSwitch extends BroadcastReceiver {

    public static final String TOGGLE_FLASHLIGHT = "TOGGLE_FLASHLIGHT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TOGGLE_FLASHLIGHT)) {
            Intent i = new Intent(context, TorchService.class);
            if (Utils.isServiceRunning(context)) {
                context.stopService(i);
            } else {
                context.startService(i);
            }
        }
    }
}
