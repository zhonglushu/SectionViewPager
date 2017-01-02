package com.zhonglushu.sectionviewpager;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;

public class Workspace extends PagedView {

    private static final String TAG = "Workspace";
    private HashMap<Long, CellLayout> mWorkspaceScreens = new HashMap<Long, CellLayout>();
    private ArrayList<Long> mScreenOrder = new ArrayList<Long>();
    // The screen id used for the empty screen always present to the right.
    final static long EXTRA_EMPTY_SCREEN_ID = -201;
    private final static long CUSTOM_CONTENT_SCREEN_ID = -301;
    private Launcher mLauncher;
    float mOverScrollEffect = 0f;
    private boolean mOverscrollEffectSet;
    private float mTranslationX;
    private ParallaxTransformer mPageTransformer;

    public Workspace(Context context) {
        this(context, null);
    }

    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = (Launcher) context;
        // With workspace, data is available straight from the get-go
        setDataIsReady();
        mPageTransformer = new ParallaxTransformer(0.1f, 2.8f);
    }

    public boolean addExtraEmptyScreen() {
        if (!mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)) {
            insertNewWorkspaceScreen(EXTRA_EMPTY_SCREEN_ID);
            return true;
        }
        return false;
    }

    public long insertNewWorkspaceScreenBeforeEmptyScreen(long screenId) {
        // Find the index to insert this view into.  If the empty screen exists, then
        // insert it before that.
        int insertIndex = mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID);
        if (insertIndex < 0) {
            insertIndex = mScreenOrder.size();
        }
        return insertNewWorkspaceScreen(screenId, insertIndex);
    }

    public long insertNewWorkspaceScreen(long screenId) {
        return insertNewWorkspaceScreen(screenId, getChildCount());
    }

    public long insertNewWorkspaceScreen(long screenId, int insertIndex) {
        if (mWorkspaceScreens.containsKey(screenId)) {
            throw new RuntimeException("Screen id " + screenId + " already exists!");
        }

        CellLayout newScreen = (CellLayout)
                mLauncher.getLayoutInflater().inflate(R.layout.workspace_screen, null);

        newScreen.setOnLongClickListener(mLongClickListener);
        newScreen.setOnClickListener(mLauncher);
        newScreen.setSoundEffectsEnabled(false);
        mWorkspaceScreens.put(screenId, newScreen);
        mScreenOrder.add(insertIndex, screenId);
        addView(newScreen, insertIndex);
        return screenId;
    }

    void addInScreenFromBind(View child, long screenId, int x, int y,
                             int spanX, int spanY) {
        addInScreen(child, screenId, x, y, spanX, spanY, false, true);
    }

    /**
     * Adds the specified child in the specified screen. The position and
     * dimension of the child are defined by x, y, spanX and spanY.
     *
     * @param child    The child to add in one of the workspace's screens.
     * @param screenId The screen in which to add the child.
     * @param cellX    The cellX position of the child in the screen's grid.
     * @param cellY    The cellY position of the child in the screen's grid.
     */
    void addInScreen(View child, long screenId, int cellX, int cellY,
                     int spanX, int spanY, boolean insert, boolean computeXYFromRank) {
        if (screenId == EXTRA_EMPTY_SCREEN_ID) {
            // This should never happen
            throw new RuntimeException(
                    "Screen id should not be EXTRA_EMPTY_SCREEN_ID");
        }

        final CellLayout layout = getScreenWithId(screenId);
        // child.setOnKeyListener(new IconKeyEventListener());

        ViewGroup.LayoutParams genericLp = child.getLayoutParams();
        CellLayout.LayoutParams lp;
        if (genericLp == null
                || !(genericLp instanceof CellLayout.LayoutParams)) {
            lp = new CellLayout.LayoutParams(cellX, cellY, spanX, spanY);
        } else {
            lp = (CellLayout.LayoutParams) genericLp;
            lp.cellX = cellX;
            lp.cellY = cellY;
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
        }

        if (spanX < 0 && spanY < 0) {
            lp.isLockedToGrid = false;
        }

        // Get the canonical child id to uniquely represent this view in this
        // screen
        SectionInfo info = (SectionInfo) child.getTag();
        int childId = mLauncher.getViewIdForItem(info);

        if (!layout.addViewToCellLayout(child, insert ? 0 : -1, childId, lp,
                true)) {
            // TODO: This branch occurs when the workspace is adding views
            // outside of the defined grid
            // maybe we should be deleting these items from the LauncherModel?
            Log.i(TAG, "Failed to add to item at (" + lp.cellX
                    + "," + lp.cellY + ") to CellLayout");
        }
    }

    public CellLayout getScreenWithId(long screenId) {
        CellLayout layout = mWorkspaceScreens.get(screenId);
        return layout;
    }

    @Override
    protected void screenScrolled(int screenCenter) {
        final boolean isRtl = isLayoutRtl();
        super.screenScrolled(screenCenter);

        boolean shouldOverScroll = mOverScrollX < 0 || mOverScrollX > mMaxScrollX;

        if (shouldOverScroll) {
            int index = 0;
            final int lowerIndex = 0;
            final int upperIndex = getChildCount() - 1;

            final boolean isLeftPage = mOverScrollX < 0;
            index = (!isRtl && isLeftPage) || (isRtl && !isLeftPage) ? lowerIndex : upperIndex;

            CellLayout cl = (CellLayout) getChildAt(index);
            float effect = Math.abs(mOverScrollEffect);
            cl.setOverScrollAmount(Math.abs(effect), isLeftPage);

            mOverscrollEffectSet = true;
        } else {
            if (mOverscrollEffectSet && getChildCount() > 0) {
                mOverscrollEffectSet = false;
                ((CellLayout) getChildAt(0)).setOverScrollAmount(0, false);
                ((CellLayout) getChildAt(getChildCount() - 1)).setOverScrollAmount(0, false);
            }
        }

        if (mPageTransformer != null) {
            final int scrollX = getScrollX();
            final int childCount = getChildCount();
            mTranslationX = getChildAt(mCurrentPageIndex).getLeft() - scrollX;
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final float transformPos = (float) (child.getLeft() - scrollX)
                        / getWidth();
                child.setTag(i);
                mPageTransformer.transformPage(child, transformPos);
            }
        }
    }

    @Override
    protected void overScroll(float amount) {
        boolean shouldOverScroll = (amount < 0 && (!hasCustomContent() || isLayoutRtl())) ||
                (amount > 0 && (!hasCustomContent() || !isLayoutRtl()));
        if (shouldOverScroll) {
            dampedOverScroll(amount);
            mOverScrollEffect = acceleratedOverFactor(amount);
        } else {
            mOverScrollEffect = 0;
        }
    }

    public boolean hasCustomContent() {
        return (mScreenOrder.size() > 0 && mScreenOrder.get(0) == CUSTOM_CONTENT_SCREEN_ID);
    }

    class ParallaxTransformer implements ViewPager.PageTransformer {
        /**
         * y = -a*(x - pageWidth/2)^2 + b
         * <p>
         * b---mMaxParallax
         * a---mFactor
         */
        private float mMaxParallax = 100.0f;
        private float mIncrementFactor = 0.5f;
        float parallaxCoefficient;
        float distanceCoefficient;
        HashMap<View, Float> mapViewPos = new HashMap<View, Float>();
        int sLeftRight = 0;

        private float calcuCarveValue(float x, int pageWidth, float max) {
            float factor = 4.0f * max / (pageWidth * pageWidth);
            return -factor * (x - pageWidth / 2.0f) * (x - pageWidth / 2.0f) + max;
        }

        public ParallaxTransformer(float parallaxCoefficient,
                                   float distanceCoefficient) {
            this.parallaxCoefficient = parallaxCoefficient;
            this.distanceCoefficient = distanceCoefficient;
        }

        void onTouchDown() {
            mapViewPos.clear();
        }

        @Override
        public void transformPage(View page, float position) {
            if (mapViewPos.isEmpty()) {
                mapViewPos.put(page, position);
                sLeftRight = 0;
            } else if (/*sLeftRight == 0 && */mapViewPos.containsKey(page)) {
                float oldPos = mapViewPos.get(page);
                sLeftRight = (oldPos > position ? -1 : 1);
            }
            int pageWidth = page.getWidth();
            final ViewGroup group = ((CellLayout) page).getShortcutsAndWidgets();
            int count = group.getChildCount();

            int tag = (int) page.getTag();
            if (position < 0) {
                //on the left
                if (tag == mCurrentPageIndex) {
                    if (group.getChildCount() == 3) {
                        group.getChildAt(0).setTranslationX(-calcuCarveValue(Math.abs(mTranslationX), pageWidth, mMaxParallax * (1.0f + mIncrementFactor)));
                        group.getChildAt(1).setTranslationX(-calcuCarveValue(Math.abs(mTranslationX), pageWidth, mMaxParallax));
                        group.getChildAt(2).setTranslationX(0);

                    } else if (group.getChildCount() == 1) {
                        group.getChildAt(0).setTranslationX(0);

                    }
                } else if (tag < mCurrentPageIndex) {
                    if (group.getChildCount() == 3) {
                        group.getChildAt(1).setTranslationX(-calcuCarveValue(Math.abs(mTranslationX), pageWidth, mMaxParallax * (1.0f + mIncrementFactor * 2.0f)));
                        group.getChildAt(2).setTranslationX(-calcuCarveValue(Math.abs(mTranslationX), pageWidth, mMaxParallax));
                        group.getChildAt(0).setTranslationX(0);

                    } else if (group.getChildCount() == 1) {
                        group.getChildAt(0).setTranslationX(0);

                    }
                }
            } else if (position > 0) {
                //on the right
                if (tag == mCurrentPageIndex) {
                    if (group.getChildCount() == 3) {
                        group.getChildAt(0).setTranslationX(calcuCarveValue(Math.abs(mTranslationX), pageWidth, mMaxParallax * (1.0f + mIncrementFactor)));
                        group.getChildAt(1).setTranslationX(0);
                        group.getChildAt(2).setTranslationX(calcuCarveValue(Math.abs(mTranslationX), pageWidth, mMaxParallax));

                    } else if (group.getChildCount() == 1) {
                        group.getChildAt(0).setTranslationX(0);

                    }

                } else if (tag > mCurrentPageIndex) {
                    if (group.getChildCount() == 3) {
                        group.getChildAt(0).setTranslationX(0);
                        group.getChildAt(2).setTranslationX(calcuCarveValue(Math.abs(mTranslationX), pageWidth, mMaxParallax * (1.0f + mIncrementFactor * 2.0f)));
                        group.getChildAt(1).setTranslationX(calcuCarveValue(Math.abs(mTranslationX), pageWidth, mMaxParallax));

                    } else if (group.getChildCount() == 1) {
                        group.getChildAt(0).setTranslationX(0);

                    }
                }
            } else {
                for (int i = 0; i < count; i++) {
                    View child = group.getChildAt(i);
                    if (child != null) {
                        child.setTranslationX(0);
                    }
                }
            }
        }
    }
}
