package de.piobyte.barnearby.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;

import de.piobyte.barnearby.R;

public class UiUtils {

    /**
     * Helper method to convert dp to px.
     */
    public static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    public static int getScreenWidth(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public static int getToolbarHeight(Context context) {
        TypedArray attrs = context.getTheme()
                .obtainStyledAttributes(new int[]{R.attr.actionBarSize});
        int toolbarHeight = (int) attrs.getDimension(0, 0);
        attrs.recycle();
        return toolbarHeight;
    }
}