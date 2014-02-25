/*
 *   Copyright (C) 2013 ParanoidAndroid
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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.RemoteViews;

public class LightbulbWidgetProvider extends AppWidgetProvider {

    public void disable(Context context) {
        ComponentName componentName = new ComponentName(context, LightbulbWidgetProvider.class);
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        packageManager.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    public void update(Context context) {
        AppWidgetManager am = AppWidgetManager.getInstance(context);
        int[] ids = am.getAppWidgetIds(new ComponentName(context, getClass()));
        for (int id : ids) {
            update(context, id);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            update(context, id);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (!Utils.deviceHasCameraFlash(context)) {
            disable(context);
        }
        if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            String part = intent.getData().getSchemeSpecificPart();

            if ("button".equals(part)) {
                Intent pendingIntent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
                context.sendBroadcast(pendingIntent);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            update(context);
        } else {
            update(context);
        }
    }

    private void update(Context context, int id) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        Intent launchIntent = new Intent();
        launchIntent.setClass(context, getClass());
        launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        launchIntent.setData(Uri.parse("custom:button"));
        views.setOnClickPendingIntent(R.id.layout,
                PendingIntent.getBroadcast(context, 0, launchIntent, 0));

        if (Utils.isServiceRunning(context)) {
            views.setImageViewResource(R.id.image, R.drawable.ic_light_bulb_on);
        } else {
            views.setImageViewResource(R.id.image, R.drawable.ic_light_bulb_off);
        }

        AppWidgetManager.getInstance(context).updateAppWidget(id, views);
    }
}
