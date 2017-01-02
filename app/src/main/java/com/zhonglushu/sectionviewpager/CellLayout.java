/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhonglushu.sectionviewpager;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;

public class CellLayout extends ViewGroup {
    static final String TAG = "CellLayout";

    private Launcher mLauncher;
    private int mCellWidth;
    private int mCellHeight;
    private int mFixedCellWidth;
    private int mFixedCellHeight;

    private int mCountX = 2;
    private int mCountY = 2;

    private int mOriginalWidthGap;
    private int mOriginalHeightGap;
    private int mWidthGap;
    private int mHeightGap;
    private int mMaxGap;

    private OnTouchListener mInterceptTouchListener;
    // These values allow a fixed measurement to be set on the CellLayout.
    private int mFixedWidth = 0;
    private int mFixedHeight = 0;

    private TimeInterpolator mEaseOutInterpolator;
    private ShortcutAndWidgetContainer mShortcutsAndWidgets;

    public CellLayout(Context context) {
        this(context, null);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // A ViewGroup usually does not draw, but CellLayout needs to draw a rectangle to show
        // the user where a dragged item will land when dropped.
        setWillNotDraw(false);
        setClipToPadding(false);
        mLauncher = (Launcher) context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);

        mCellWidth = mCellHeight = -1;
        mFixedCellWidth = mFixedCellHeight = -1;
        mWidthGap = mOriginalWidthGap = 0;
        mHeightGap = mOriginalHeightGap = 0;
        mMaxGap = Integer.MAX_VALUE;

        mShortcutsAndWidgets = new ShortcutAndWidgetContainer(context);
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap,
                mCountX, mCountY);
        addView(mShortcutsAndWidgets);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params,
                                       boolean markCells) {
        final LayoutParams lp = params;

        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        if (lp.cellX >= 0 && lp.cellX <= mCountX - 1 && lp.cellY >= 0 && lp.cellY <= mCountY - 1) {
            // If the horizontal or vertical span is set to -1, it is taken to
            // mean that it spans the extent of the CellLayout
            if (lp.cellHSpan < 0) lp.cellHSpan = mCountX;
            if (lp.cellVSpan < 0) lp.cellVSpan = mCountY;

            child.setId(childId);

            mShortcutsAndWidgets.addView(child, index, lp);

            return true;
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // First we clear the tag to ensure that on every touch down we start with a fresh slate,
        // even in the case where we return early. Not clearing here was causing bugs whereby on
        // long-press we'd end up picking up an item from a previous drag operation.
        if (mInterceptTouchListener != null && mInterceptTouchListener.onTouch(this, ev)) {
            return true;
        }

        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int childWidthSize = widthSize - (getPaddingLeft() + getPaddingRight());
        int childHeightSize = heightSize - (getPaddingTop() + getPaddingBottom());
        if (mFixedCellWidth < 0 || mFixedCellHeight < 0) {
            int cw = calculateCellWidth(childWidthSize, mCountX);
            int ch = calculateCellHeight(childHeightSize, mCountY);
            if (cw != mCellWidth || ch != mCellHeight) {
                mCellWidth = cw;
                mCellHeight = ch;
                mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap,
                        mHeightGap, mCountX, mCountY);
            }
        }

        int newWidth = childWidthSize;
        int newHeight = childHeightSize;
        if (mFixedWidth > 0 && mFixedHeight > 0) {
            newWidth = mFixedWidth;
            newHeight = mFixedHeight;
        } else if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        int numWidthGaps = mCountX - 1;
        int numHeightGaps = mCountY - 1;

        if (mOriginalWidthGap < 0 || mOriginalHeightGap < 0) {
            int hSpace = childWidthSize;
            int vSpace = childHeightSize;
            int hFreeSpace = hSpace - (mCountX * mCellWidth);
            int vFreeSpace = vSpace - (mCountY * mCellHeight);
            mWidthGap = Math.min(mMaxGap, numWidthGaps > 0 ? (hFreeSpace / numWidthGaps) : 0);
            mHeightGap = Math.min(mMaxGap, numHeightGaps > 0 ? (vFreeSpace / numHeightGaps) : 0);
            mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap,
                    mHeightGap, mCountX, mCountY);
        } else {
            mWidthGap = mOriginalWidthGap;
            mHeightGap = mOriginalHeightGap;
        }
        int count = getChildCount();
        int maxWidth = 0;
        int maxHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(newWidth,
                    MeasureSpec.EXACTLY);
            int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(newHeight,
                    MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
            maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
            maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
        }
        if (mFixedWidth > 0 && mFixedHeight > 0) {
            setMeasuredDimension(maxWidth, maxHeight);
        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int offset = getMeasuredWidth() - getPaddingLeft() - getPaddingRight() -
                (mCountX * mCellWidth);
        int left = getPaddingLeft() + (int) Math.ceil(offset / 2f);
        int top = getPaddingTop();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.layout(left, top,
                    left + r - l,
                    top + b - t);
        }
    }

    void setOverScrollAmount(float r, boolean left) {
        invalidate();
    }

    int calculateCellWidth(int width, int countX) {
        return countX == 0 ? countX : width / countX;
    }

    int calculateCellHeight(int height, int countY) {
        return height / countY;
    }

    public ShortcutAndWidgetContainer getShortcutsAndWidgets() {
        return mShortcutsAndWidgets;
    }

    public View getChildAt(int x, int y) {
        return mShortcutsAndWidgets.getChildAt(x, y);
    }

    public static class LayoutParams extends MarginLayoutParams {
        /**
         * Horizontal location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellX;

        /**
         * Vertical location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellY;

        /**
         * Temporary horizontal location of the item in the grid during reorder
         */
        public int tmpCellX;

        /**
         * Temporary vertical location of the item in the grid during reorder
         */
        public int tmpCellY;

        /**
         * Indicates that the temporary coordinates should be used to layout the items
         */
        public boolean useTmpCoords;

        /**
         * Number of cells spanned horizontally by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellHSpan;

        /**
         * Number of cells spanned vertically by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellVSpan;

        /**
         * Indicates whether the item will set its x, y, width and height parameters freely,
         * or whether these will be computed based on cellX, cellY, cellHSpan and cellVSpan.
         */
        public boolean isLockedToGrid = true;

        /**
         * Indicates that this item should use the full extents of its parent.
         */
        public boolean isFullscreen = false;

        /**
         * Indicates whether this item can be reordered. Always true except in the case of the
         * the AllApps button.
         */
        public boolean canReorder = true;

        // X coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        int x;
        // Y coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        int y;

        boolean dropped;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.cellX = source.cellX;
            this.cellY = source.cellY;
            this.cellHSpan = source.cellHSpan;
            this.cellVSpan = source.cellVSpan;
        }

        public LayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            this.cellX = cellX;
            this.cellY = cellY;
            this.cellHSpan = cellHSpan;
            this.cellVSpan = cellVSpan;
        }

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap,
                          boolean invertHorizontally, int colCount) {
            if (isLockedToGrid) {
                final int myCellHSpan = cellHSpan;
                final int myCellVSpan = cellVSpan;
                int myCellX = useTmpCoords ? tmpCellX : cellX;
                int myCellY = useTmpCoords ? tmpCellY : cellY;

                if (invertHorizontally) {
                    myCellX = colCount - myCellX - cellHSpan;
                }

                width = myCellHSpan * cellWidth + ((myCellHSpan - 1) * widthGap) -
                        leftMargin - rightMargin;
                height = myCellVSpan * cellHeight + ((myCellVSpan - 1) * heightGap) -
                        topMargin - bottomMargin;
                x = (int) (myCellX * (cellWidth + widthGap) + leftMargin);
                y = (int) (myCellY * (cellHeight + heightGap) + topMargin);
            }
        }

        public String toString() {
            return "(" + this.cellX + ", " + this.cellY + ")";
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return y;
        }
    }
}
