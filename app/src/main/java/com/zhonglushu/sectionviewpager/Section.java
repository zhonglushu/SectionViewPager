package com.zhonglushu.sectionviewpager;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by zhonglushu on 16/9/7.
 */
public class Section extends FrameLayout {

    View section;

    public Section(Context context) {
        super(context);
    }

    public Section(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Section(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Section(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initViews();
    }

    private void initViews() {
        int sectionLayoutId = getSectionLayoutId();
        if (sectionLayoutId > 0) {
            section = LayoutInflater.from(this.getContext()).inflate(getSectionLayoutId(), null, false);
            addView(section, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
    }

    public int getSectionLayoutId() {
        return R.layout.section;
    }
}
