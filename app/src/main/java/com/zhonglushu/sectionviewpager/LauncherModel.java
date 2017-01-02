package com.zhonglushu.sectionviewpager;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class LauncherModel {

    static final String TAG = "Launcher.Model";
    private static final int ITEMS_CHUNK = 6; // batch size for the workspace icons
    private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");

    static {
        sWorkerThread.start();
    }

    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    private ArrayList<Long> screenIds = new ArrayList<Long>();

    ArrayList<SectionInfo> mShowSectionsInfo = new ArrayList<SectionInfo>();
    private final Object mLock = new Object();
    private WeakReference<Callbacks> mCallbacks;
    private Context mContext;
    private DeferredHandler mHandler = new DeferredHandler();

    LauncherModel(Context context) {
        mContext = context;
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Callbacks callbacks) {
        synchronized (mLock) {
            mCallbacks = new WeakReference<Callbacks>(callbacks);
        }
    }

    public void startLoader() {
        if (screenIds == null) {
            screenIds = new ArrayList<Long>();
        }

        if (mShowSectionsInfo == null || mShowSectionsInfo.size() <= 0) {
            /** 第一次从配置文件获取 */
            mShowSectionsInfo = LauncherProvider.loadFavoritesRecursive(mContext, R.xml.default_workspace, screenIds);
        } else {
            for (SectionInfo info : mShowSectionsInfo) {
                long screenId = info.getScreenId();
                if (screenId >= 0 && !screenIds.contains(screenId)) {
                    screenIds.add(screenId);
                }
            }
        }

        bindWorkspaceScreens(mCallbacks.get(), screenIds);
        bindWorkspaceItems(mCallbacks.get(), mShowSectionsInfo, null);
    }

    private void bindWorkspaceItems(final Callbacks oldCallbacks,
                                    final ArrayList<SectionInfo> workspaceItems,
                                    ArrayList<Runnable> deferredBindRunnables) {

        final boolean postOnMainThread = (deferredBindRunnables != null);

        // Bind the workspace items
        int N = workspaceItems.size();
        for (int i = 0; i < N; i += ITEMS_CHUNK) {
            final int start = i;
            final int chunkSize = (i + ITEMS_CHUNK <= N) ? ITEMS_CHUNK
                    : (N - i);
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindItems(workspaceItems, start, start
                                + chunkSize, false);
                    }
                }
            };
            if (postOnMainThread) {
                synchronized (deferredBindRunnables) {
                    deferredBindRunnables.add(r);
                }
            } else {
                runOnMainThread(r);
            }
        }
    }

    private void bindWorkspaceScreens(final Callbacks oldCallbacks,
                                      final ArrayList<Long> orderedScreens) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                if (callbacks != null) {
                    callbacks.bindScreens(orderedScreens);
                }
            }
        };
        runOnMainThread(r);
    }

    /**
     * Runs the specified runnable immediately if called from the main thread,
     * otherwise it is posted on the main thread handler.
     */
    private void runOnMainThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            // If we are on the worker thread, post onto the main handler
            mHandler.post(r);
        } else {
            r.run();
        }
    }

    /**
     * Gets the callbacks object. If we've been stopped, or if the launcher
     * object has somehow been garbage collected, return null instead. Pass in
     * the Callbacks object that was around when the deferred message was
     * scheduled, and if there's a new Callbacks object around then also return
     * null. This will save us from calling onto it with data that will be
     * ignored.
     */
    Callbacks tryGetCallbacks(Callbacks oldCallbacks) {
        synchronized (mLock) {
            /*
             * if (mStopped) { return null; }
             */

            if (mCallbacks == null) {
                return null;
            }

            final Callbacks callbacks = mCallbacks.get();
            if (callbacks != oldCallbacks) {
                return null;
            }
            if (callbacks == null) {
                Log.w(TAG, "no mCallbacks");
                return null;
            }

            return callbacks;
        }
    }

    public interface Callbacks {

        public void bindItems(ArrayList<SectionInfo> sections, int start,
                              int end, boolean forceAnimateIcons);

        public void bindScreens(ArrayList<Long> orderedScreenIds);
    }
}
