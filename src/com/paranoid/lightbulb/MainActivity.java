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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {

    private static final String TAP_TO_DISABLE = "tap_to_disable";

    private static final int DURATION = 1000; //ms

    private static boolean sTorchOn;
    private static boolean sHasFlash;
    private static Context sContext;

    private static ImageView sLightBulb;
    private static ImageView sTorchLight;

    private static float sOldBrightness;

    private static TorchModeChangedListener sTorchChangedListener;

    private static View.OnClickListener sTorchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            sTorchOn = !sTorchOn;
            Intent i = new Intent(sContext, TorchSwitch.class);
            i.setAction(TorchSwitch.TOGGLE_FLASHLIGHT);
            sContext.sendBroadcast(i);
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sContext = this;

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new TorchFragment())
                    .commit();
        }

        IntentFilter filter = new IntentFilter(FlashDevice.TORCH_STATUS_CHANGED);
        sTorchChangedListener = new TorchModeChangedListener();
        registerReceiver(sTorchChangedListener, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(sTorchChangedListener);
        super.onDestroy();
    }

    public static class TorchFragment extends Fragment {

        public TorchFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            RelativeLayout rootView = (RelativeLayout) inflater.inflate(
                    R.layout.fragment_main, container, false);

            sTorchLight = (ImageView) rootView.findViewById(R.id.torch_light);
            sTorchLight.setOnClickListener(sTorchClickListener);
            sTorchOn = Utils.isServiceRunning(sContext);
            setTorchLighScale();

            sLightBulb = (ImageView) rootView.findViewById(R.id.torch_image);
            sLightBulb.setClickable(true);
            sLightBulb.setOnClickListener(sTorchClickListener);
            return rootView;
        }
    }

    @Override
    protected void onPause() {
        if(!sHasFlash) {
            Intent service = new Intent(sContext, TorchService.class);
            sContext.stopService(service);
            sTorchOn = false;
            setTorchLighScale();
        }
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setTorchLighScale();
    }

    public class TorchModeChangedListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(FlashDevice.TORCH_STATUS_CHANGED)) {
                int mode = intent.getIntExtra(FlashDevice.TORCH_MODE, FlashDevice.OFF);

                sTorchOn = mode == FlashDevice.ON;
                sHasFlash = intent.getBooleanExtra(FlashDevice.HAS_FLASH, false);
                float scale = sTorchOn ? measureScale() : 1f;

                sTorchLight.animate().scaleX(scale).scaleY(scale).setDuration(DURATION);
                if(!sHasFlash) {
                    Window window = getWindow();
                    WindowManager.LayoutParams settings = window.getAttributes();
                    GradientDrawable shape = (GradientDrawable) sTorchLight.getDrawable();
                    if(mode == FlashDevice.ON) {
                        hideSystemUi(sLightBulb);
                        sLightBulb.animate().scaleX(0f).scaleY(0f);
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        sOldBrightness = settings.screenBrightness;
                        settings.screenBrightness = 1f;
                        shape.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                        Utils.showMessageOnce(sContext, TAP_TO_DISABLE, R.string.tap_to_disable);
                    } else {
                        showSystemUi(sLightBulb);
                        sLightBulb.animate().scaleX(1f).scaleY(1f);
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        settings.screenBrightness = sOldBrightness;
                        shape.clearColorFilter();
                    }
                    getWindow().setAttributes(settings);
                }
                setTorchLighScale();
            }
        }
    }

    public static float measureScale() {
        WindowManager wm = (WindowManager) sContext.getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float displayHeight = outMetrics.heightPixels;
        float displayWidth  = outMetrics.widthPixels;
        return (Math.max(displayHeight, displayWidth) /
                sContext.getResources().getDimensionPixelSize(R.dimen.torch_icon_bg)) * 2;
    }

    public static void setTorchLighScale() {
        if(sTorchOn) {
            sTorchLight.setScaleX(measureScale());
            sTorchLight.setScaleY(measureScale());
        } else { // reset size
            sTorchLight.setScaleX(1);
            sTorchLight.setScaleY(1);
        }
    }

    private void hideSystemUi(View v) {
        v.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void showSystemUi(View v) {
        v.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
