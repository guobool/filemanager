package com.jb.filemanager.function.zipfile;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;

import com.jb.filemanager.BaseActivity;
import com.jb.filemanager.Const;
import com.jb.filemanager.R;
import com.jb.filemanager.TheApplication;
import com.jb.filemanager.eventbus.FileOperateEvent;
import com.jb.filemanager.function.filebrowser.FileBrowserActivity;
import com.jb.filemanager.function.search.view.SearchActivity;
import com.jb.filemanager.function.zipfile.adapter.ZipListAdapter;
import com.jb.filemanager.function.zipfile.bean.ZipFileGroupBean;
import com.jb.filemanager.function.zipfile.bean.ZipFileItemBean;
import com.jb.filemanager.function.zipfile.dialog.ZipFileOperationDialog;
import com.jb.filemanager.function.zipfile.listener.ZipListAdapterClickListener;
import com.jb.filemanager.function.zipfile.presenter.ZipActivityContract;
import com.jb.filemanager.function.zipfile.presenter.ZipFileActivityPresenter;
import com.jb.filemanager.ui.view.SearchTitleView;
import com.jb.filemanager.ui.view.SearchTitleViewCallback;
import com.jb.filemanager.ui.widget.BottomOperateBar;
import com.jb.filemanager.ui.widget.CommonLoadingView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiaoyu on 2017/6/29 17:01.
 */

public class ZipFileActivity extends BaseActivity implements ZipActivityContract.View {

    private ZipFileActivityPresenter mPresenter = new ZipFileActivityPresenter(this);
    private CommonLoadingView mProgress;
    private ExpandableListView mListView;
    private ZipListAdapter mAdapter;
    private ZipFileOperationDialog mOperationDialog;
    private SearchTitleView mSearchTitle;
    private BottomOperateBar mOperateBar;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FileOperateEvent event) {
        Log.e("event", "ZipActivity FileOperateEvent");
        mPresenter.onCreate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zip_file);

        TheApplication.getGlobalEventBus().register(this);
        mProgress = (CommonLoadingView) findViewById(R.id.loading);
        mListView = (ExpandableListView) findViewById(R.id.zip_expand_lv);
        mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                if (mPresenter != null) {
                    mPresenter.onItemClick(groupPosition, childPosition);
                }
                return true;
            }
        });
        mListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                ZipStatistics.upload(ZipStatistics.ZIP_SORT);
            }
        });

        mSearchTitle = (SearchTitleView) findViewById(R.id.search_title);
        mSearchTitle.setClickCallBack(new SearchTitleViewCallback() {
            @Override
            public void onSearchClick() {
                SearchActivity.showSearchResult(getApplicationContext(), Const.CategoryType.CATEGORY_TYPE_ZIP);
            }

            @Override
            public void onIvBackClick() {
                finish();
            }

            @Override
            public void onIvCancelSelectClick() {
                mPresenter.onTitleCancelBtnClick();
            }

            @Override
            public void onSelectBtnClick() {
                mPresenter.onTitleSelectBtnClick();
            }
        });
        mOperateBar = (BottomOperateBar) findViewById(R.id.bob_bottom_operator);
        mOperateBar.setListener(new BottomOperateBar.Listener() {
            @Override
            public ArrayList<File> getCurrentSelectedFiles() {
                return mPresenter.getCurrentSelectFile();
            }

            @Override
            public Activity getActivity() {
                return ZipFileActivity.this;
            }

            @Override
            public void afterCopy() {
                FileBrowserActivity.startBrowserForPaste(ZipFileActivity.this, "");
                mPresenter.afterCopy();
            }

            @Override
            public void afterCut() {
                FileBrowserActivity.startBrowserForPaste(ZipFileActivity.this, "");
                mPresenter.afterCut();
            }

            @Override
            public void afterRename() {
                mPresenter.afterRename();
            }

            @Override
            public void afterDelete() {
                mPresenter.afterDelete();
            }

            @Override
            public void statisticsClickCopy() {
            }

            @Override
            public void statisticsClickCut() {
            }

            @Override
            public void statisticsClickDelete() {
            }

            @Override
            public void statisticsClickMore() {
            }

            @Override
            public void statisticsClickRename() {
            }

            @Override
            public void statisticsClickDetail() {
            }
        });

        mPresenter.onCreate();
    }

    @Override
    protected void onDestroy() {
        try {
            TheApplication.getGlobalEventBus().unregister(this);
        } catch (Exception e) {
        }
        mPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void setWidgetsState(boolean isLoading) {
        mProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mListView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) {
            mProgress.startLoading();
        } else {
            mProgress.stopLoading();
        }
    }

    @Override
    public void setListData(List<ZipFileGroupBean> data) {
        mAdapter = new ZipListAdapter(data);
        mAdapter.setListener(new ZipListAdapterClickListener() {
            @Override
            public void onSwitchClick() {
                if (mPresenter != null) {
                    mPresenter.onItemStateChange();
                }
            }
        });
        mListView.setAdapter(mAdapter);
        // 展开所有分组
        int count = mListView.getCount();
        for (int i = 0; i < count; i++) {
            mListView.expandGroup(i);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void showOperationDialog(ZipFileItemBean fileItem) {
        if (mOperationDialog != null && mOperationDialog.isShowing()) {
            mOperationDialog.dismiss();
            mOperationDialog = null;
        }
        mOperationDialog = new ZipFileOperationDialog(this, fileItem);
        mOperationDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (ExtractManager.getInstance().isProgressDialogAttached()) {
            ExtractManager.getInstance().hideProgressDialogFromWindow();
        } else if (mSearchTitle.isSelectMode()) {
            mPresenter.onTitleCancelBtnClick();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void switchSelectMode(boolean isToSelectMode) {
        mSearchTitle.switchTitleMode(isToSelectMode);
        mOperateBar.setVisibility(isToSelectMode ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setSearchTitleSelectBtnState(int state) {
        mSearchTitle.setSelectBtnResId(state);
    }

    @Override
    public void setSearchTitleSelectCount(int count) {
        mSearchTitle.setSelectedCount(count);
    }

}
