package com.example.lanchat.utils;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class InsetsUtils {

    public static void applySystemBarInsets(View rootView) {
        if (rootView == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    public static void applyTopAndBottomInsets(View rootView, View topView, View bottomView) {
        if (rootView == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (topView != null) {
                topView.setPadding(
                        topView.getPaddingLeft(),
                        insets.top,
                        topView.getPaddingRight(),
                        topView.getPaddingBottom()
                );
            }
            if (bottomView != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomView.getLayoutParams();
                if (lp != null) {
                    lp.bottomMargin = insets.bottom + 24;
                    bottomView.setLayoutParams(lp);
                }
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
