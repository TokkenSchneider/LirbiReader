package com.foobnix.tts;

import org.ebookdroid.LirbiApp;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.reader.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;

public class TTSService extends Service {

    public static final String EXTRA_PATH = "EXTRA_PATH";
    public static final String EXTRA_INT = "INT";

    private static final String TAG = "TTSService";

    public static String ACTION_PLAY_CURRENT_PAGE = "ACTION_PLAY_CURRENT_PAGE";

    public TTSService() {
        LOG.d(TAG, "Create constructor");

    }

    AudioManager mAudioManager;
    MediaSessionCompat mMediaSessionCompat;
    boolean isActivated;

    @Override
    public void onCreate() {
        LOG.d(TAG, "Create");
        AppState.get().load(getApplicationContext());
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(listener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);

        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag");
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                LOG.d(TAG, "onMediaButtonEvent", isActivated, intent);
                if (isActivated) {
                    KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                    if (KeyEvent.ACTION_UP == event.getAction() && KeyEvent.KEYCODE_HEADSETHOOK == event.getKeyCode()) {
                        LOG.d(TAG, "onStartStop", "KEYCODE_HEADSETHOOK");
                        boolean isPlaying = TTSEngine.get().isPlaying();
                        if (isPlaying) {
                            TTSEngine.get().stop();
                        } else {
                            playPage("", AppState.get().lastBookPage);
                        }
                    }
                }
                return isActivated;
            }

        });

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

    }

    boolean isPlaying;
    OnAudioFocusChangeListener listener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            LOG.d("onAudioFocusChange", focusChange);
            if (focusChange < 0) {
                isPlaying = TTSEngine.get().isPlaying();
                LOG.d("onAudioFocusChange", "Is playing", isPlaying);
                TTSEngine.get().stop();
            } else {
                if (isPlaying) {
                    playPage("", AppState.get().lastBookPage);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void playBookPage(int page, String path) {
        TTSEngine.get().stop();

        Intent intent = playBookIntent(page, path);

        LirbiApp.context.startService(intent);

    }

    public static Intent playBookIntent(int page, String path) {
        Intent intent = new Intent(LirbiApp.context, TTSService.class);
        intent.setAction(TTSService.ACTION_PLAY_CURRENT_PAGE);
        intent.putExtra(EXTRA_INT, page);
        intent.putExtra(EXTRA_PATH, path);
        return intent;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        LOG.d(TAG, "onStartCommand");
        if (intent == null) {
            return START_STICKY;
        }
        LOG.d(TAG, "onStartCommand", intent.getAction());
        if (intent.getExtras() != null) {
            LOG.d(TAG, "onStartCommand", intent.getAction(), intent.getExtras());
            for (String key : intent.getExtras().keySet())
                LOG.d(TAG, key, "=>", intent.getExtras().get(key));
        }

        if (TTSNotification.TTS_STOP.equals(intent.getAction())) {
            TTSEngine.get().stop();
        }
        if (TTSNotification.TTS_READ.equals(intent.getAction())) {
            TTSEngine.get().stop();
            playPage("", AppState.get().lastBookPage);
        }
        if (TTSNotification.TTS_NEXT.equals(intent.getAction())) {
            TTSEngine.get().stop();
            playPage("", AppState.get().lastBookPage + 1);
        }

        if (ACTION_PLAY_CURRENT_PAGE.equals(intent.getAction())) {
            mMediaSessionCompat.setActive(true);
            isActivated = true;
            int pageNumber = intent.getIntExtra(EXTRA_INT, -1);
            AppState.get().lastBookPath = intent.getStringExtra(EXTRA_PATH);

            if (pageNumber != -1) {
                playPage("", pageNumber);
            }

        }

        return START_STICKY;
    }

    public CodecDocument getDC() {
        try {
            return ImageExtractor.getCodecContext(AppState.get().lastBookPath, "", Dips.screenWidth(), Dips.screenHeight());
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void playPage(String preText, int pageNumber) {
        if (pageNumber != -1) {
            EventBus.getDefault().post(new MessagePageNumber(pageNumber));
            AppState.get().lastBookPage = pageNumber;
            CodecDocument dc = getDC();
            if (dc == null) {
                LOG.d(TAG, "CodecDocument", "is NULL");
                return;
            }

            LOG.d(TAG, "CodecDocument", pageNumber, dc.getPageCount());
            if (pageNumber >= dc.getPageCount()) {

                TempHolder.get().timerFinishTime = 0;

                Vibrator v = (Vibrator) LirbiApp.context.getSystemService(Context.VIBRATOR_SERVICE);
                if (AppState.get().isVibration) {
                    v.vibrate(1000);
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(null);
                TTSEngine.get().speek(LirbiApp.context.getString(R.string.the_book_is_over));

                EventBus.getDefault().post(new TtsStatus());
                return;
            }



            CodecPage page = dc.getPage(pageNumber);
            String pageHTML = page.getPageHTML();
            pageHTML = TxtUtils.replaceHTMLforTTS(pageHTML);
            LOG.d(TAG, pageHTML);

            if (TxtUtils.isEmpty(pageHTML)) {
                LOG.d("empty page play next one");
                playPage("", AppState.get().lastBookPage + 1);
                return;
            }

            String[] parts = TxtUtils.getParts(pageHTML);
            String firstPart = parts[0];
            final String secondPart = parts[1];

            if (TxtUtils.isNotEmpty(preText)) {
                firstPart = preText + firstPart;
            }

            if (Build.VERSION.SDK_INT >= 15) {
                TTSEngine.get().getTTS().setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    @Override
                    public void onStart(String utteranceId) {

                    }

                    @Override
                    public void onError(String utteranceId) {
                        TTSEngine.get().stop();
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        LOG.d(TAG, "onUtteranceCompleted");
                        if (TempHolder.get().timerFinishTime != 0 && System.currentTimeMillis() > TempHolder.get().timerFinishTime) {
                            LOG.d(TAG, "Timer");
                            TempHolder.get().timerFinishTime = 0;
                            return;
                        }

                        playPage(secondPart, AppState.get().lastBookPage + 1);
                        SettingsManager.updateTempPage(AppState.get().lastBookPath, AppState.get().lastBookPage + 1);

                    }
                });
            } else {
                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

                    @Override
                    public void onUtteranceCompleted(String utteranceId) {
                        LOG.d(TAG, "onUtteranceCompleted");
                        if (TempHolder.get().timerFinishTime != 0 && System.currentTimeMillis() > TempHolder.get().timerFinishTime) {
                            LOG.d(TAG, "Timer");
                            TempHolder.get().timerFinishTime = 0;
                            return;
                        }
                        playPage(secondPart, AppState.get().lastBookPage + 1);
                        SettingsManager.updateTempPage(AppState.get().lastBookPath, AppState.get().lastBookPage + 1);

                    }

                });
            }

            TTSNotification.show(TempHolder.get().path, pageNumber + 1);
            TTSEngine.get().speek(firstPart);
            EventBus.getDefault().post(new TtsStatus());
            AppState.get().save(getApplicationContext());

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isActivated = false;
        TempHolder.get().timerFinishTime = 0;
        mMediaSessionCompat.setActive(false);
        LOG.d(TAG, "onDestroy");
    }

}
