package org.ebookdroid.ui.viewer;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.ToastPosition;
import org.ebookdroid.ui.viewer.viewers.PdfSurfaceView;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.utils.LengthUtils;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Analytics;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.reader.R;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.MainTabs2;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.NativeExpressAdView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class ViewerActivity extends AbstractActionActivity<ViewerActivity, ViewerActivityController> {
    public static final String PERCENT_EXTRA = "percent";
    public static final DisplayMetrics DM = new DisplayMetrics();

    IView view;

    private FrameLayout frameLayout;

    /**
     * Instantiates a new base viewer activity.
     */
    public ViewerActivity() {
        super();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        if (TTSNotification.ACTION_TTS.equals(intent.getAction())) {
            return;
        }
        if (!intent.filterEquals(getIntent())) {
            finish();
            startActivity(intent);
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.ui.AbstractActionActivity#createController()
     */
    @Override
    protected ViewerActivityController createController() {
        return new ViewerActivityController(this);
    }

    private AdView adView;
    private NativeExpressAdView adViewNative;
    private Handler handler;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        AppState.getInstance().load(this);

        FileMetaCore.checkOrCreateMetaInfo(this);

        if (AppState.getInstance().isRememberMode && AppState.getInstance().isAlwaysOpenAsMagazine) {
            super.onCreate(savedInstanceState);
            finish();
            ExtUtils.showDocument(this, getIntent().getData());
            return;
        } else {
            if (getIntent().getData() != null) {
                final BookSettings bs = SettingsManager.getBookSettings(getIntent().getData().getPath());
                // AppState.getInstance().setNextScreen(bs.isNextScreen);
                if (bs != null) {
                    // AppState.getInstance().isLocked = bs.isLocked;
                    AppState.getInstance().autoScrollSpeed = bs.speed;
                    AppState.get().isCut = bs.splitPages;
                    AppState.get().isCrop = bs.cropPages;
                    AppState.get().isDouble = bs.doublePages;
                    AppState.get().isDoubleCoverAlone = bs.doublePagesCover;
                    AppState.get().isLocked = bs.isLocked;
                }
            }

            getController().beforeCreate(this);

            DocumentController.applyBrigtness(this);

            if (AppState.getInstance().isInvert) {
                setTheme(R.style.StyledIndicatorsWhite);
            } else {
                setTheme(R.style.StyledIndicatorsBlack);
            }
            super.onCreate(savedInstanceState);
        }

        setContentView(R.layout.document_view);

        Android6.checkPermissions(this);

        getController().createWrapper(this);
        frameLayout = (FrameLayout) findViewById(R.id.documentView);

        view = new PdfSurfaceView(getController());

        frameLayout.addView(view.getView());

        getController().afterCreate(this);

        // ADS.activate(this, adView);
        ADS.activateNative(this, adViewNative);

        RecentUpates.updateAll(this);

        handler = new Handler();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DocumentController.doRotation(this);
        ADS.onResume(adView);
        ADS.onResumeNative(adViewNative);

        if (AppState.getInstance().isFullScrean()) {
            Keyboards.hideNavigation(this);
        }
        getController().onResume();
        handler.removeCallbacks(closeRunnable);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            getController().getDocumentController().goToPage(data.getIntExtra("page", 0));
        }
    }

    boolean needToRestore = false;

    @Override
    protected void onPause() {
        super.onPause();
        LOG.d("onPause", this.getClass());
        ADS.onPause(adView);
        ADS.onPauseNative(adViewNative);
        getController().afterPause();
        needToRestore = AppState.get().isAutoScroll;
        AppState.get().isAutoScroll = false;
        AppState.get().save(this);
        TempHolder.isSeaching = false;
        handler.postDelayed(closeRunnable, AppState.APP_CLOSE_AUTOMATIC);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Analytics.onStart(this);
        if (needToRestore) {
            AppState.get().isAutoScroll = true;
            getController().getListener().onAutoScroll();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        Analytics.onStop(this);

    }

    Runnable closeRunnable = new Runnable() {

        @Override
        public void run() {
            if (TTSEngine.get().isPlaying()) {
                LOG.d("TTS is playing");
                return;
            }

            LOG.d("Close App");
            // AppState.get().lastA = ViewerActivity.class.getSimpleName();
            getController().closeActivity(null);
            MainTabs2.closeApp(ViewerActivity.this);
        }
    };

    @Override
    protected void onDestroy() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (AppState.getInstance().isRememberMode && AppState.getInstance().isAlwaysOpenAsMagazine) {
            super.onDestroy();
        } else {
            getController().beforeDestroy();
            super.onDestroy();
            getController().afterDestroy(isFinishing());
            ADS.destory(adView);
            ADS.destoryNative(adViewNative);
            getController().getListener().onDestroy();
        }
    }

    public void currentPageChanged(final String pageText, final String bookTitle) {
        if (LengthUtils.isEmpty(pageText)) {
            return;
        }
        final AppSettings app = AppSettings.getInstance();

        if (app.pageNumberToastPosition == ToastPosition.Invisible) {
            return;
        }

    }

    Dialog rotatoinDialog;
    boolean isInitPosistion;

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (ExtUtils.isTextFomat(getIntent())) {

            final boolean currentPosistion = Dips.screenHeight() > Dips.screenWidth();

            if (rotatoinDialog != null) {
                try {
                    rotatoinDialog.dismiss();
                } catch (Exception e) {
                    LOG.e(e);
                }
            }

            if (isInitPosistion != currentPosistion) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setCancelable(false);
                dialog.setMessage(R.string.apply_a_new_screen_orientation_);
                dialog.setPositiveButton(R.string.yes, new AlertDialog.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doConifChange();
                        isInitPosistion = currentPosistion;
                    }
                });
                rotatoinDialog = dialog.show();
            }
        } else {
            doConifChange();
        }

    }

    public void doConifChange() {
        try {
            if (!getController().getDocumentController().isInitialized()) {
                LOG.d("Skip onConfigurationChanged");
                return;
            }
        } catch (Exception e) {
            LOG.e(e);
            return;
        }

        ADS.activateNative(this, adViewNative);
        AppState.get().save(this);

        if (ExtUtils.isTextFomat(getIntent())) {

            double value = getController().getDocumentModel().getPercentRead();
            getIntent().putExtra(PERCENT_EXTRA, value);

            LOG.d("READ PERCEnt", value);

            if (Build.VERSION.SDK_INT >= 11) {
                recreate();
            } else {
                finish();
                startActivity(getIntent());
            }
        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getController().afterPostCreate();
        isInitPosistion = Dips.screenHeight() > Dips.screenWidth();

    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if (Integer.parseInt(Build.VERSION.SDK) >= 12) {
            return GenericMotionEvent12.onGenericMotionEvent(event, this);
        }
        return false;
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        LOG.d("onKeyUp");
        if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking() && !event.isCanceled()) {
            getController().getWrapperControlls().checkBack(event);
            return true;
        }

        if (getController().getWrapperControlls().dispatchKeyEventUp(event)) {
            return true;
        }

        return isMyKey;
    }

    private boolean isMyKey = false;

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        LOG.d("onKeyDown");
        isMyKey = false;
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            event.startTracking();
            return true;
        }

        if (getController().getWrapperControlls().dispatchKeyEventDown(event)) {
            isMyKey = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        if (CloseAppDialog.checkLongPress(this, event)) {
            CloseAppDialog.showOnLongClickDialog(getController().getActivity(), null, getController().getListener());
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

}
