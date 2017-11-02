package com.foobnix.ui2.adapter;

import java.io.File;

import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.reader.R;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.pdf.search.activity.msg.OpenDirMessage;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.fragment.UIFragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class DefaultListeners {

    public static void bindAdapter(Activity a, final FileMetaAdapter searchAdapter) {
        searchAdapter.setOnItemClickListener(getOnItemClickListener(a));
        searchAdapter.setOnItemLongClickListener(getOnItemLongClickListener(a, searchAdapter));
        searchAdapter.setOnMenuClickListener(getOnMenuClick(a, searchAdapter));
        searchAdapter.setOnStarClickListener(getOnStarClick(a));
    }

    public static void bindAdapterAuthorSerias(Activity a, final FileMetaAdapter searchAdapter) {
        searchAdapter.setOnAuthorClickListener(getOnAuthorClickListener(a));
        searchAdapter.setOnSeriesClickListener(getOnSeriesClickListener(a));

    }

    public static ResultResponse<FileMeta> getOnItemClickListener(final Activity a) {
        return new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(FileMeta result) {
                File item = new File(result.getPath());
                if (item.isDirectory()) {
                    Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                            .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.BrowseFragment));//
                    LocalBroadcastManager.getInstance(a).sendBroadcast(intent);

                    EventBus.getDefault().post(new OpenDirMessage(result.getPath()));

                } else {
                    ExtUtils.openFile(a, item);
                }
                return false;
            }
        };
    };

    public static ResultResponse<FileMeta> getOnItemLongClickListener(final Activity a, final FileMetaAdapter searchAdapter) {
        return new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(final FileMeta result) {
                File item = new File(result.getPath());

                if (item.isDirectory()) {
                    Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                            .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.BrowseFragment));//
                    LocalBroadcastManager.getInstance(a).sendBroadcast(intent);

                    EventBus.getDefault().post(new OpenDirMessage(result.getPath()));
                    return true;
                }

                Runnable onDeleteAction = new Runnable() {

                    @Override
                    public void run() {
                        deleteFile(a, searchAdapter, result);
                    }

                };
                FileInformationDialog.showFileInfoDialog(a, item, onDeleteAction);
                return true;
            }
        };
    };

    private static void deleteFile(Activity a, final FileMetaAdapter searchAdapter, final FileMeta result) {
        final File file = new File(result.getPath());

        boolean delete = file.delete();
        if (!delete) {
            delete = delete(a, file);
        }
        if (delete) {
            TempHolder.listHash++;
            AppDB.get().delete(result);
            searchAdapter.getItemsList().remove(result);
            searchAdapter.notifyDataSetChanged();

        } else {
            Toast.makeText(a, R.string.can_t_delete_file, Toast.LENGTH_LONG).show();
        }
    }

    public static boolean delete(final Context context, final File file) {
        final String where = MediaStore.MediaColumns.DATA + "=?";
        final String[] selectionArgs = new String[] { file.getAbsolutePath() };
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri filesUri = MediaStore.Files.getContentUri("external");

        contentResolver.delete(filesUri, where, selectionArgs);

        if (file.exists()) {

            contentResolver.delete(filesUri, where, selectionArgs);
        }
        return !file.exists();
    }

    public static ResultResponse<FileMeta> getOnMenuClick(final Activity a, final FileMetaAdapter searchAdapter) {
        return new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(final FileMeta result) {
                final File file = new File(result.getPath());
                Runnable onDeleteAction = new Runnable() {

                    @Override
                    public void run() {
                        deleteFile(a, searchAdapter, result);
                    }

                };

                if (ExtUtils.isNotSupportedFile(file)) {
                    ShareDialog.showArchive(a, file, onDeleteAction);
                } else {
                    ShareDialog.show(a, file, onDeleteAction, -1, null);
                }

                return false;
            }
        };
    };

    public static ResultResponse<String> getOnAuthorClickListener(final Activity a) {
        return new ResultResponse<String>() {

            @Override
            public boolean onResultRecive(String result) {

                result = AppDB.SEARCH_IN.AUTHOR.getDotPrefix() + " " + result;

                Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                        .putExtra(MainTabs2.EXTRA_SEACH_TEXT, result)//
                        .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.SearchFragment));//

                LocalBroadcastManager.getInstance(a).sendBroadcast(intent);
                return false;
            }
        };
    }

    public static ResultResponse<String> getOnSeriesClickListener(final Activity a) {
        return new ResultResponse<String>() {

            @Override
            public boolean onResultRecive(String result) {

                result = AppDB.SEARCH_IN.SERIES.getDotPrefix() + " " + result;

                Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                        .putExtra(MainTabs2.EXTRA_SEACH_TEXT, result)//
                        .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.SearchFragment));//

                LocalBroadcastManager.getInstance(a).sendBroadcast(intent);
                return false;
            }
        };
    }

    public static ResultResponse2<FileMeta, FileMetaAdapter> getOnStarClick(final Activity a) {
        return new ResultResponse2<FileMeta, FileMetaAdapter>() {

            @Override
            public boolean onResultRecive(FileMeta fileMeta, FileMetaAdapter adapter) {
                Boolean isStar = fileMeta.getIsStar();
                if (isStar == null) {
                    isStar = false;
                }
                fileMeta.setIsStar(!isStar);
                fileMeta.setIsStarTime(System.currentTimeMillis());
                AppDB.get().updateOrSave(fileMeta);

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                TempHolder.listHash++;

                return false;
            }
        };
    };

}
