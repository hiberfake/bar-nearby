package de.piobyte.barnearby.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SixteenTenImageView extends ImageView {

    public SixteenTenImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int fourThreeHeight = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthSpec) * 10 / 16,
                MeasureSpec.EXACTLY);
        super.onMeasure(widthSpec, fourThreeHeight);
    }
}