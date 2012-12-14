package io.vov.vitamio.utils;

import java.lang.reflect.Method;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;

public class DeviceUtils {
	@SuppressWarnings("deprecation")
	public static int getScreenWidth(Activity ctx) {
		int width;
		Display display = ctx.getWindowManager().getDefaultDisplay();
		try {
			Method mGetRawW = Display.class.getMethod("getRawWidth");
			width = (Integer) mGetRawW.invoke(display);
		} catch (Exception e) {
			width = display.getWidth();
		}
		return width;
	}

	@SuppressWarnings("deprecation")
	public static int getScreenHeight(Activity ctx) {
		int height;
		Display display = ctx.getWindowManager().getDefaultDisplay();
		try {
			Method mGetRawH = Display.class.getMethod("getRawHeight");
			height = (Integer) mGetRawH.invoke(display);
		} catch (Exception e) {
			height = display.getHeight();
		}
		return height;
	}

	public static double getScreenPhysicalSize(Activity ctx) {
		DisplayMetrics dm = new DisplayMetrics();
		ctx.getWindowManager().getDefaultDisplay().getMetrics(dm);
		double diagonalPixels = Math.sqrt(Math.pow(dm.widthPixels, 2) + Math.pow(dm.heightPixels, 2));
		return diagonalPixels / (160 * dm.density);
	}
}
