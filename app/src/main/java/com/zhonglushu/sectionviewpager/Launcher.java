package com.zhonglushu.sectionviewpager;

import android.content.res.Resources;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Launcher extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Launcher";
    private LauncherModel mModel;
    private Workspace mWorkspace;
    private HashMap<Long, CellLayout> mWorkspaceScreens = new HashMap<Long, CellLayout>();
    private ArrayList<Runnable> mBindOnResumeCallbacks = new ArrayList<Runnable>();
    private HashMap<Integer, Integer> mItemIdToViewId = new HashMap<Integer, Integer>();
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    private LayoutInflater mInflater = null;
    private Resources mRes = null;
    private boolean mPaused = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mInflater = LayoutInflater.from(this);
        mRes = this.getResources();
        mWorkspace = (Workspace) this.findViewById(R.id.workspace);
        mModel = new LauncherModel(this);
        mModel.initialize(mCallback);
        mModel.startLoader();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPaused = false;
        if (mBindOnResumeCallbacks.size() > 0) {
            for (int i = 0; i < mBindOnResumeCallbacks.size(); i++) {
                mBindOnResumeCallbacks.get(i).run();
            }
            mBindOnResumeCallbacks.clear();
        }
    }

    private LauncherModel.Callbacks mCallback = new LauncherModel.Callbacks() {

        @Override
        public void bindItems(final ArrayList<SectionInfo> sections, final int start, final int end, final boolean forceAnimateIcons) {
            Runnable r = new Runnable() {
                public void run() {
                    bindItems(sections, start, end, forceAnimateIcons);
                }
            };
            if (waitUntilResume(r)) {
                return;
            }

            Workspace workspace = mWorkspace;
            for (int i = start; i < end; i++) {
                final SectionInfo item = sections.get(i);
                View sectionLayout = createSection(item);
                workspace.addInScreenFromBind(sectionLayout, item.screenId,
                        item.cellX, item.cellY, item.spanX, item.spanY);
            }
            workspace.requestLayout();
        }

        @Override
        public void bindScreens(ArrayList<Long> orderedScreenIds) {
            bindAddScreens(orderedScreenIds);

            // If there are no screens, we need to have an empty screen
            if (orderedScreenIds.size() == 0) {
                mWorkspace.addExtraEmptyScreen();
            }
        }
    };

    public void bindAddScreens(ArrayList<Long> orderedScreenIds) {
        int count = orderedScreenIds.size();
        for (int i = 0; i < count; i++) {
            mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(orderedScreenIds.get(i));
        }
    }

    View createSection(SectionInfo info) {
        return createSection(info.getLayoutName(), (ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    View createSection(String layoutName, ViewGroup parent, SectionInfo info) {
        View sectionLayout = mInflater.inflate(getLayoutIdByName(layoutName), parent, false);
        sectionLayout.setTag(info);
        sectionLayout.setOnClickListener(this);
        return sectionLayout;
    }

    int getLayoutIdByName(String layoutName) {
        return mRes.getIdentifier(layoutName, "layout", this.getPackageName());
    }

    /**
     * If the activity is currently paused, signal that we need to run the
     * passed Runnable in onResume.
     * <p/>
     * This needs to be called from incoming places where resources might have
     * been loaded while we are paused. That is becaues the Configuration might
     * be wrong when we're not running, and if it comes back to what it was when
     * we were paused, we are not restarted.
     * <p/>
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused. The caller might be able to skip
     * some work in that case since we will come back again.
     */
    private boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if (mPaused) {
            Log.i(TAG, "Deferring update until onResume");
            if (deletePreviousRunnables) {
                while (mBindOnResumeCallbacks.remove(run)) {
                }
            }
            mBindOnResumeCallbacks.add(run);
            return true;
        } else {
            return false;
        }
    }

    private boolean waitUntilResume(Runnable run) {
        return waitUntilResume(run, false);
    }

    @Override
    public void onClick(View v) {

    }

    public int getViewIdForItem(SectionInfo info) {
        // This cast is safe given the > 2B range for int.
        int itemId = (int) info.id;
        if (mItemIdToViewId.containsKey(itemId)) {
            return mItemIdToViewId.get(itemId);
        }
        int viewId = generateViewId();
        mItemIdToViewId.put(itemId, viewId);
        return viewId;
    }

    public static int generateViewId() {
        if (Build.VERSION.SDK_INT >= 17) {
            return View.generateViewId();
        } else {
            // View.generateViewId() is not available. The following fallback logic is a copy
            // of its implementation.
            for (; ; ) {
                final int result = sNextGeneratedId.get();
                // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
                int newValue = result + 1;
                if (newValue > 0x00FFFFFF)
                    newValue = 1; // Roll over to 1, not 0.
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result;
                }
            }
        }
    }
}
