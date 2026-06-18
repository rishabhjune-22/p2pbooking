package com.example.roombooking;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ActivityOrientationInstrumentedTest {

    @Test
    public void everyDeclaredActivityIsLockedToPortrait() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageInfo(
                context.getPackageName(),
                PackageManager.GET_ACTIVITIES
        );

        for (ActivityInfo activity : packageInfo.activities) {
            assertEquals(
                    activity.name,
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                    activity.screenOrientation
            );
        }
    }
}
