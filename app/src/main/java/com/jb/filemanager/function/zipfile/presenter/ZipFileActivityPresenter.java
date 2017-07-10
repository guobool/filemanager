package com.jb.filemanager.function.zipfile.presenter;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.jb.filemanager.TheApplication;
import com.jb.filemanager.function.zipfile.bean.ZipFileGroupBean;
import com.jb.filemanager.function.zipfile.bean.ZipFileItemBean;
import com.jb.filemanager.util.StorageUtil;
import com.jb.filemanager.util.TimeUtil;
import com.jb.filemanager.util.file.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by xiaoyu on 2017/6/30 10:23.
 */

public class ZipFileActivityPresenter implements ZipActivityContract.Presenter {

    private ZipActivityContract.View mView;
    private List<ZipFileGroupBean> mGroupList = new ArrayList<>();

    private List<ZipFileItemBean> mSelectedBean = new ArrayList<>();

    public ZipFileActivityPresenter(ZipActivityContract.View view) {
        mView = view;
    }

    @Override
    public void onCreate() {
        mView.setWidgetsState(true);
        ScanZipFileTask task = new ScanZipFileTask();
        task.execute();
    }

    @Override
    public void onItemClick(int groupPosition, int childPosition) {
        if (mGroupList != null) {
            ZipFileItemBean child = mGroupList.get(groupPosition).getChild(childPosition);
            mView.showOperationDialog(child);
        }
    }

    @Override
    public void onItemStateChange() {
        mSelectedBean.clear();
        for (ZipFileGroupBean groupBean : mGroupList) {
            List<ZipFileItemBean> zipFileList = groupBean.getZipFileList();
            for (ZipFileItemBean itemBean : zipFileList) {
                mSelectedBean.add(itemBean);
            }
        }
        if (mSelectedBean.size() > 0) {
            mView.showMoreOperator(mSelectedBean.size());
        } else {
            mView.hideMoreOperator();
        }
    }

    @Override
    public void extractZipFile(ZipFileItemBean fileItem) {

    }

    @Override
    public void onDestroy() {

    }

    /**
     * Created by xiaoyu on 2017/6/29 17:44.
     * <p>
     * 扫描文件系统中的压缩文件:<tt>zip, rar, 7z, lzma</tt>
     * </p>
     */

    private class ScanZipFileTask extends AsyncTask<Void, Integer, Void> {

        private static final int DEPTH_THRESHOLD = 3;

        @Override
        protected void onPreExecute() {
            mGroupList.clear();
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (String path : StorageUtil.getAllExternalPaths(TheApplication.getAppContext())) {
                File root = new File(path);
                scanPath(root, 0);
            }
            return null;
        }

        // 扫描传入的路径
        private void scanPath(File dir, int depth) {
            if (depth > DEPTH_THRESHOLD) return;
            if (!dir.exists() || !dir.isDirectory()) return;
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    scanPath(file, depth++);
                } else {
                    String extension = FileUtil.getExtension(file.getName());
                    if (!TextUtils.isEmpty(extension) && (extension.equalsIgnoreCase("zip")
                            || extension.equalsIgnoreCase("rar")
                            || extension.equalsIgnoreCase("7z"))) {
//                    if (ZipUtils.isValidPackFile(file)) { // 很耗时
                        boolean exist = false;
                        for (ZipFileGroupBean group : mGroupList) {
                            if (group.getGroupTime() == TimeUtil.getYMDTime(file.lastModified())) {
                                exist = true;
                                group.getZipFileList().add(new ZipFileItemBean(file));
                            }
                        }
                        if (!exist) {
                            List<ZipFileItemBean> list = new ArrayList<>();
                            list.add(new ZipFileItemBean(file));
                            mGroupList.add(new ZipFileGroupBean(list));
                        }
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        @Override
        protected void onCancelled() {
        }

        @Override
        protected void onPostExecute(Void v) {
            sortResult();
            mView.setListData(mGroupList);
            mView.setWidgetsState(false);
        }

        private void sortResult() {
            Collections.sort(mGroupList, new Comparator<ZipFileGroupBean>() {
                @Override
                public int compare(ZipFileGroupBean o1, ZipFileGroupBean o2) {
                    return (int) (o1.getGroupTime() - o2.getGroupTime());
                }
            });
        }
    }
}
