package com.jb.filemanager.manager.file;

import android.content.Intent;
import android.text.TextUtils;

import com.jb.filemanager.TheApplication;
import com.jb.filemanager.function.paste.GlobalDialogActivity;
import com.jb.filemanager.manager.file.task.CopyFileTask;
import com.jb.filemanager.manager.file.task.CutFileTask;
import com.jb.filemanager.util.FileUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by bill wang on 2017/6/23.
 *
 */

public class FileManager {

    // 其他文件
    public static final int OTHERS = 0;

    // 图片
    public static final int IMAGE = 1;

    // 视频
    public static final int VIDEO = 2;

    // 音乐
    public static final int AUDIO = 3;

    // 应用
    public static final int APP = 4;

    // 文档 txt
    public static final int TXT = 5;

    // 文档 doc, docx
    public static final int DOC = 6;

    // 文档 pdf
    public static final int PDF = 7;

    // 压缩包
    public static final int ZIP = 8;



    public static final int LOADER_IMAGE = 0;
    public static final int LOADER_VIDEO = 1;
    public static final int LOADER_APP = 2;
    public static final int LOADER_AUDIO = 3;
    public static final int LOADER_DOC = 4;
    public static final int LOADER_FILES = 5;
    public static final int LOADER_ZIP = 6;

    private static FileManager sInstance;

    private ArrayList<File> mCopyFiles;
    private ArrayList<File> mCutFiles;

    private Comparator<File> mFileSort;

    private HashMap<String, Object> mPasteLockers;

    public static FileManager getInstance() {
        synchronized (FileManager.class) {
            if (sInstance == null) {
                sInstance = new FileManager();
            }
            return sInstance;
        }
    }


    public void setCopyFiles(ArrayList<File> copyFiles) {
        clearCopyFiles();
        mCopyFiles = new ArrayList<>(copyFiles);
    }

    public void setCutFiles(ArrayList<File> cutFiles) {
        clearCutFiles();
        mCutFiles = new ArrayList<>(cutFiles);
    }

    public ArrayList<File> getCopyFiles() {
        return mCopyFiles;
    }

    public ArrayList<File> getCutFiles() {
        return mCutFiles;
    }

    public void clearCopyFiles() {
        if (mCopyFiles != null) {
            mCopyFiles.clear();
            mCopyFiles = null;
        }
    }

    public void clearCutFiles() {
        if (mCutFiles != null) {
            mCutFiles.clear();
            mCutFiles = null;
        }
    }

    public void setFileSort(Comparator<File> sort) {
        mFileSort = sort;
    }

    public Comparator<File> getFileSort() {
        return mFileSort;
    }

    public boolean doPaste(String destDir, Listener listener) {
        boolean result = false;
        final WeakReference<Listener> listenerRef = new WeakReference<>(listener);
        if (mCopyFiles != null && mCopyFiles.size() > 0) {
            long moreThenNeed = FileUtil.checkSpacePaste(mCopyFiles, destDir);

            if (moreThenNeed > 0) {
                new CopyFileTask(mCopyFiles, destDir, new CopyFileTask.Listener() {

                    @Override
                    public void onSubFolderCopy(CopyFileTask task, File file, String dest) {
                        if (mPasteLockers == null) {
                            mPasteLockers = new HashMap<>();
                        }
                        mPasteLockers.put(file.getAbsolutePath(), task);

                        Intent intent = new Intent(TheApplication.getInstance(), GlobalDialogActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(GlobalDialogActivity.DIALOG_TYPE, GlobalDialogActivity.TYPE_PASTE_SUB_FOLDER);
                        intent.putExtra(GlobalDialogActivity.PASTE_SUB_FOLDER_PASTE_SOURCE_PATH, file.getAbsolutePath());
                        TheApplication.getInstance().startActivity(intent);
                    }

                    @Override
                    public void onDuplicate(CopyFileTask task, File file, ArrayList<File> copySource) {
                        if (mPasteLockers == null) {
                            mPasteLockers = new HashMap<>();
                        }
                        mPasteLockers.put(file.getAbsolutePath(), task);

                        Intent intent = new Intent(TheApplication.getInstance(), GlobalDialogActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(GlobalDialogActivity.DIALOG_TYPE, GlobalDialogActivity.TYPE_PASTE_DUPLICATE_FILE);
                        intent.putExtra(GlobalDialogActivity.PASTE_DUPLICATE_FILE_PATH, file.getAbsolutePath());
                        intent.putExtra(GlobalDialogActivity.PASTE_DUPLICATE_FILE_IS_SINGLE, copySource.size() == 1);
                        TheApplication.getInstance().startActivity(intent);
                    }

                    @Override
                    public void onProgressUpdate(File file) {
                        // TODO 是否有其他操作还不清楚
                        if (listenerRef.get() != null) {
                            listenerRef.get().onPasteProgressUpdate(file);
                        }
                    }

                    @Override
                    public void onPostExecute(CopyFileTask task, Boolean isSuccess, ArrayList<File> failedArray) {
                        if (!isSuccess && failedArray != null && failedArray.size() > 0) {
                            ArrayList<String> failedFilePath = new ArrayList<>();
                            for (File file : failedArray) {
                                failedFilePath.add(file.getAbsolutePath());
                            }

                            Intent intent = new Intent(TheApplication.getInstance(), GlobalDialogActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(GlobalDialogActivity.DIALOG_TYPE, GlobalDialogActivity.TYPE_PASTE_FAILED);
                            intent.putExtra(GlobalDialogActivity.PASTE_FAILED_SOURCE_PATH, failedFilePath);
                            intent.putExtra(GlobalDialogActivity.PASTE_FAILED_IS_COPY, true);
                            intent.putExtra(GlobalDialogActivity.PASTE_FAILED_DEST_PATH, task.getDest());
                            TheApplication.getInstance().startActivity(intent);
                        }
                    }
                }).start();
                result = true;
            } else {
                if (listenerRef.get() != null) {
                    listener.onPasteNeedMoreSpace(Math.abs(moreThenNeed));
                }
            }
        } else if (mCutFiles != null && mCutFiles.size() > 0) {
            new CutFileTask(mCutFiles, destDir, new CutFileTask.Listener() {

                @Override
                public void onSubFolderCopy(CutFileTask task, File file, String dest) {
                    if (mPasteLockers == null) {
                        mPasteLockers = new HashMap<>();
                    }
                    mPasteLockers.put(file.getAbsolutePath(), task);

                    Intent intent = new Intent(TheApplication.getInstance(), GlobalDialogActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(GlobalDialogActivity.DIALOG_TYPE, GlobalDialogActivity.TYPE_PASTE_SUB_FOLDER);
                    intent.putExtra(GlobalDialogActivity.PASTE_SUB_FOLDER_PASTE_SOURCE_PATH, file.getAbsolutePath());
                    TheApplication.getInstance().startActivity(intent);
                }

                @Override
                public void onDuplicate(CutFileTask task, File file, ArrayList<File> cutSource) {
                    if (mPasteLockers == null) {
                        mPasteLockers = new HashMap<>();
                    }
                    mPasteLockers.put(file.getAbsolutePath(), task);

                    Intent intent = new Intent(TheApplication.getInstance(), GlobalDialogActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(GlobalDialogActivity.DIALOG_TYPE, GlobalDialogActivity.TYPE_PASTE_DUPLICATE_FILE);
                    intent.putExtra(GlobalDialogActivity.PASTE_DUPLICATE_FILE_PATH, file.getAbsolutePath());
                    intent.putExtra(GlobalDialogActivity.PASTE_DUPLICATE_FILE_IS_SINGLE, cutSource.size() == 1);
                    TheApplication.getInstance().startActivity(intent);
                }

                @Override
                public void onProgressUpdate(File file) {
                    // TODO 是否有其他操作还不清楚
                    if (listenerRef.get() != null) {
                        listenerRef.get().onPasteProgressUpdate(file);
                    }
                }

                @Override
                public void onPostExecute(CutFileTask task, Boolean isSuccess, ArrayList<File> failedArray) {
                    if (!isSuccess && failedArray != null && failedArray.size() > 0) {
                        ArrayList<String> failedFilePath = new ArrayList<>();
                        for (File file : failedArray) {
                            failedFilePath.add(file.getAbsolutePath());
                        }

                        Intent intent = new Intent(TheApplication.getInstance(), GlobalDialogActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(GlobalDialogActivity.DIALOG_TYPE, GlobalDialogActivity.TYPE_PASTE_FAILED);
                        intent.putExtra(GlobalDialogActivity.PASTE_FAILED_SOURCE_PATH, failedFilePath);
                        intent.putExtra(GlobalDialogActivity.PASTE_FAILED_IS_COPY, false);
                        intent.putExtra(GlobalDialogActivity.PASTE_FAILED_DEST_PATH, task.getDest());
                        TheApplication.getInstance().startActivity(intent);
                    }
                }
            }).start();
            result = true;
        }
        return result;
    }

    public void continuePaste(String path, boolean skip, Boolean applyToAll) {
        if (!TextUtils.isEmpty(path) && mPasteLockers != null && mPasteLockers.containsKey(path)) {
            Object task = mPasteLockers.remove(path);
            if (task instanceof CopyFileTask) {
                ((CopyFileTask) task).continueCopy(skip, applyToAll);
            } else if (task instanceof CutFileTask) {
                ((CutFileTask) task).continueCut(skip, applyToAll);
            }
        }
    }

    public void stopPast(String path) {
        if (!TextUtils.isEmpty(path) && mPasteLockers != null && mPasteLockers.containsKey(path)) {
            Object task = mPasteLockers.remove(path);
            if (task instanceof CopyFileTask) {
                ((CopyFileTask) task).stopCopy();
            } else if (task instanceof CutFileTask) {
                ((CutFileTask) task).stopCut();
            }
        }
    }

    public interface Listener {
        void onPasteNeedMoreSpace(long needMoreSpace);
        void onPasteProgressUpdate(File file);
    }
}
