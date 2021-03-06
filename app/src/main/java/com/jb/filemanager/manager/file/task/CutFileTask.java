package com.jb.filemanager.manager.file.task;


import android.media.MediaScannerConnection;
import android.text.TextUtils;

import com.jb.filemanager.TheApplication;
import com.jb.filemanager.eventbus.FileOperateEvent;
import com.jb.filemanager.util.Logger;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by bill wang on 2017/6/29.
 *
 */

public class CutFileTask {

    private static final String LOG_TAG = "CutFileTask";

    private Thread mWorkerThread;
    private Listener mListener;

    private ArrayList<File> mSource;
    private ArrayList<File> mFailed;
    private String mDest;
    private final Object mLocker = new Object();
    private boolean mIsSkip = false;
    private boolean mIsStop = false;
    private boolean mIsApplyToAll = false;

    public CutFileTask(ArrayList<File> source, String dest, Listener listener) {

        mListener = listener;
        mSource = new ArrayList<> (source);
        mFailed = new ArrayList<>();
        mDest = dest;
        mWorkerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result = false;
                if (mSource != null && mSource.size() > 0 && !TextUtils.isEmpty(mDest)) {
                    result = true;
                    for (final File file : mSource) {
                        TheApplication.postRunOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mListener != null) {
                                    mListener.onProgressUpdate(file);
                                }
                            }
                        });

                        if (file.isDirectory() && mDest.startsWith(file.getAbsolutePath())) {
                            Logger.i(LOG_TAG, "发现子路径:" + file.getAbsolutePath() + "  " + mDest);
                            try {
                                Logger.i(LOG_TAG, "暂停询问处理方式:" + file.getAbsolutePath());
                                synchronized (mLocker) {
                                    TheApplication.postRunOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mListener != null) {
                                                mListener.onSubFolderCopy(CutFileTask.this, file, mDest);
                                            }
                                        }
                                    });
                                    mLocker.wait();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        if (mIsStop) {
                            Logger.i(LOG_TAG, "停止");
                            break;
                        }

                        File test = new File(mDest + File.separator + file.getName());
                        if (test.exists() && ((test.isDirectory() && file.isDirectory()) || ((test.isFile() && file.isFile())))) {
                            Logger.i(LOG_TAG, "发现重名文件:" + file.getAbsolutePath());
                            if (!mIsApplyToAll) {
                                try {
                                    Logger.i(LOG_TAG, "暂停询问处理方式:" + file.getAbsolutePath());
                                    synchronized (mLocker) {
                                        TheApplication.postRunOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (mListener != null) {
                                                    mListener.onDuplicate(CutFileTask.this, file, mSource);
                                                }
                                            }
                                        });
                                        mLocker.wait();
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Logger.i(LOG_TAG, "应用之前的选择:" + file.getAbsolutePath());
                            }
                        }

                        if (!mIsSkip) {
                            Logger.i(LOG_TAG, "覆盖:" + file.getAbsolutePath());
                            File targetFile = new File(mDest + File.separator + file.getName());
                            boolean success = file.renameTo(targetFile);
                            if (!success) {
                                mFailed.add(file);
                            }else {
                                TheApplication.postEvent(new FileOperateEvent(file, targetFile, FileOperateEvent.OperateType.CUT));//add by 李启发  告知页面cut完成
                            }
                            result = result && success;
                        } else {
                            Logger.i(LOG_TAG, "跳过:" + file.getAbsolutePath());
                        }
                    }

                    String[] paths = new String[mSource.size() + 1];
                    for (int i = 0; i < mSource.size(); i++) {
                        paths[i] = mSource.get(i).getParentFile().getAbsolutePath();
                    }
                    paths[mSource.size()] = mDest;
                    MediaScannerConnection.scanFile(TheApplication.getInstance(), paths, null, null); // 修改后的文件添加到系统数据库
                }

                final boolean finalResult = result;
                TheApplication.postRunOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mListener != null) {
                            mListener.onPostExecute(CutFileTask.this, finalResult, mFailed);
                        }
                    }
                });
            }
        });
    }

    public void start() {
        if (mWorkerThread != null) {
            mWorkerThread.start();
        }
    }

    public void continueCut(boolean isSkip, Boolean isApplyToAll) {
        synchronized (mLocker) {
            mIsSkip = isSkip;

            if (isApplyToAll != null) {
                mIsApplyToAll = isApplyToAll;
            }

            mLocker.notify();
        }
    }

    public void stopCut() {
        mIsStop = true;
        mLocker.notify();
    }

    public String getDest() {
        return mDest;
    }

    public interface Listener {
        void onSubFolderCopy(CutFileTask task, File file, String dest);
        void onDuplicate(CutFileTask task, File file, ArrayList<File> cutSource);
        void onProgressUpdate(File file);
        void onPostExecute(CutFileTask task, Boolean isSuccess, ArrayList<File> failedArray);
    }
}
