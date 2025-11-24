package com.example.beatify;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.button.MaterialButton;

public class ThemeHelper {

    private static final String PREFS_NAME = "BeatifyPrefs";
    private static final String KEY_THEME_COLOR = "theme_color";
    public static final int DEFAULT_COLOR = Color.parseColor("#BB86FC");

    public static void saveThemeColor(Context context, int color) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_THEME_COLOR, color).apply();
    }

    public static int getThemeColor(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_THEME_COLOR, DEFAULT_COLOR);
    }

    public static int getDarkerColor(int color) {
        return ColorUtils.blendARGB(color, Color.BLACK, 0.3f);
    }

    public static GradientDrawable getPrimaryGradient(int baseColor) {
        int darker = ColorUtils.blendARGB(baseColor, Color.BLACK, 0.6f);
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{darker, baseColor});
    }

    public static GradientDrawable getSecondaryGradient(int baseColor) {
        int lighter = ColorUtils.blendARGB(baseColor, Color.WHITE, 0.1f);
        int darker = ColorUtils.blendARGB(baseColor, Color.BLACK, 0.3f);
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{lighter, darker});
    }

    public static GradientDrawable getTertiaryGradient(int baseColor) {
        int touch = ColorUtils.blendARGB(Color.BLACK, baseColor, 0.2f);
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.BLACK, touch});
    }

    public static GradientDrawable getDarkBackgroundGradient(int baseColor) {
        // 90%black with 10% color
        int veryDark = ColorUtils.blendARGB(Color.BLACK, baseColor, 0.05f);
        // 80% black with 20% color
        int darkTouch = ColorUtils.blendARGB(Color.BLACK, baseColor, 0.15f);
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{veryDark, darkTouch});
    }

    public static GradientDrawable getBlackGreyGradient() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.BLACK, Color.DKGRAY});
    }

    public static void applyTheme(View view, int type, Context context) {
        if (view == null) return;

        if (view instanceof MaterialButton) {
            applySolidTheme(view, context);
            return;
        }

        int color = getThemeColor(context);
        GradientDrawable gd;
        if (type == 1) {
            gd = getPrimaryGradient(color);
        } else if (type == 2) {
            gd = getSecondaryGradient(color);
        } else if (type == 3) {
            gd = getTertiaryGradient(color);
        } else if (type == 4) {
            gd = getDarkBackgroundGradient(color);
        } else {
            gd = getPrimaryGradient(color);
        }
        gd.setCornerRadius(0f);
        view.setBackground(gd);
    }

    public static void applySolidTheme(View view, Context context) {
        int color = getThemeColor(context);
        if (view instanceof MaterialButton) {
            ((MaterialButton) view).setBackgroundTintList(ColorStateList.valueOf(color));
        } else if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        } else if (view instanceof ImageView) {
            ((ImageView) view).setImageTintList(ColorStateList.valueOf(color));
        }
    }

    public static ColorStateList createBottomNavColorStateList(Context context) {
        int[][] states = new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked} };
        int[] colors = new int[]{
                Color.WHITE,
                Color.parseColor("#B3FFFFFF")
        };
        return new ColorStateList(states, colors);
    }

    public static ColorStateList createToggleColorStateList(Context context) {
        int themeColor = getThemeColor(context);
        int[][] states = new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked} };
        int[] colors = new int[]{ themeColor, Color.TRANSPARENT };
        return new ColorStateList(states, colors);
    }
}