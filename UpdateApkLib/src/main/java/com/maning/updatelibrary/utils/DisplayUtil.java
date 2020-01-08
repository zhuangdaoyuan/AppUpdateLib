package com.maning.updatelibrary.utils;

import android.content.Context;

/**
 * @author :庄道园
 * Date :2020/1/8
 * 安静撸码，淡定做人
 */
public class DisplayUtil {
    public static int dip2px(Context context, float dpValue) {
        try {
            float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dpValue * scale + 0.5f);
        } catch (Throwable throwable) {
        }
        return 0;
    }

    public static int sp2px(Context context, float spValue) {
        try {
            float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
            return (int) (spValue * fontScale + 0.5f);
        } catch (Throwable throwable) {
        }
        return 0;
    }

    /**
     * 获取屏幕宽度
     */
    public static int getWindowWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getWindowHeight(Context contex) {
        return contex.getResources().getDisplayMetrics().heightPixels;
    }
}
