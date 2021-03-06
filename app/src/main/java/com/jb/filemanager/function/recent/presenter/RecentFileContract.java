package com.jb.filemanager.function.recent.presenter;

import com.jb.filemanager.function.recent.bean.BlockBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiaoyu on 2017/7/17 14:28.
 */

public interface RecentFileContract {
    interface View {
        void switchWidgetsState(boolean isLoadingData);
        void setListViewData(List<BlockBean> data);
        void notifyListDataChanged();
        void switchSelectMode(boolean isToSelectMode);
        void setSearchTitleSelectBtnState(int state);
        void setSearchTitleSelectCount(int count);
    }
    interface Presenter {
        void onCreate();
        void onDestroy();
        void onItemCheckChanged();
        void onTitleCancelBtnClick();
        void onTitleSelectBtnClick();
        ArrayList<File> getCurrentSelectFile();
        void afterCopy();
        void afterCut();
        void afterRename();
        void afterDelete();
        void reloadData();
    }
}
