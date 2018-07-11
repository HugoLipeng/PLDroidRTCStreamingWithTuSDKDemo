package com.qiniu.pili.droid.rtcstreaming.demo.activity.streaming;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.qiniu.pili.droid.rtcstreaming.RTCAudioLevelCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCAudioSource;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceOptions;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceState;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceStateChangedListener;
import com.qiniu.pili.droid.rtcstreaming.RTCFrameCapturedCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCFrameMixedCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCMediaStreamingManager;
import com.qiniu.pili.droid.rtcstreaming.RTCRemoteWindowEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCStartConferenceCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCStreamStatsCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCSurfaceView;
import com.qiniu.pili.droid.rtcstreaming.RTCUserEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCVideoWindow;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.core.QiniuAppServer;
import com.qiniu.pili.droid.rtcstreaming.demo.ui.CameraPreviewFrameView;
import com.qiniu.pili.droid.rtcstreaming.demo.ui.RotateLayout;
import com.qiniu.pili.droid.rtcstreaming.demo.ui.tusdk.ConfigViewSeekBar;
import com.qiniu.pili.droid.rtcstreaming.demo.ui.tusdk.FilterCellView;
import com.qiniu.pili.droid.rtcstreaming.demo.ui.tusdk.FilterConfigSeekbar;
import com.qiniu.pili.droid.rtcstreaming.demo.ui.tusdk.FilterConfigView;
import com.qiniu.pili.droid.rtcstreaming.demo.ui.tusdk.FilterListView;
import com.qiniu.pili.droid.rtcstreaming.demo.ui.tusdk.StickerListAdapter;
import com.qiniu.pili.droid.rtcstreaming.demo.utils.StreamingSettings;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.StreamStatusCallback;
import com.qiniu.pili.droid.streaming.StreamingPreviewCallback;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;
import com.qiniu.pili.droid.streaming.SurfaceTextureCallback;
import com.qiniu.pili.droid.streaming.WatermarkSetting;

import org.json.JSONArray;
import org.json.JSONObject;
import org.lasque.tusdk.api.video.preproc.filter.TuSDKFilterEngine;
import org.lasque.tusdk.core.TuSdkContext;
import org.lasque.tusdk.core.seles.SelesEGLContextFactory;
import org.lasque.tusdk.core.seles.SelesParameters;
import org.lasque.tusdk.core.seles.tusdk.FilterWrap;
import org.lasque.tusdk.core.struct.TuSdkSize;
import org.lasque.tusdk.core.utils.ThreadHelper;
import org.lasque.tusdk.core.utils.hardware.CameraConfigs;
import org.lasque.tusdk.core.utils.hardware.InterfaceOrientation;
import org.lasque.tusdk.core.utils.image.ImageOrientation;
import org.lasque.tusdk.core.utils.json.JsonHelper;
import org.lasque.tusdk.core.video.TuSDKVideoCaptureSetting;
import org.lasque.tusdk.core.view.recyclerview.TuSdkTableView;
import org.lasque.tusdk.core.view.widget.button.TuSdkTextButton;
import org.lasque.tusdk.impl.view.widget.TuSeekBar;
import org.lasque.tusdk.modules.view.widget.sticker.StickerGroup;
import org.lasque.tusdk.modules.view.widget.sticker.StickerLocalPackage;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 *  演示使用 SDK 内部的 Video/Audio 采集，实现连麦 & 推流
 */
public class RTCStreamingActivity extends AppCompatActivity implements SurfaceTextureCallback, StreamingPreviewCallback {

    private static final String TAG = "RTCStreamingActivity";

    private static final int MESSAGE_ID_RECONNECTING = 0x01;

    private static final int MIN_BITRATE = 10 * 1024;
    private static final int MAX_BITRATE = 10000 * 1024;

    public static final String PREVIEW_SIZE_RATIO = "PreviewSizeRatio";
    public static final String PREVIEW_SIZE_LEVEL = "PreviewSizeLevel";
    public static final String ENCODING_SIZE_RATIO = "EncodingSizeRatio";
    public static final String ENCODING_SIZE_LEVEL = "EncodingSizeLevel";
    public static final String ENCODING_CONFIG = "EncodingConfig";

    private TextView mStatusTextView;
    private TextView mStatTextView;
    private Button mControlButton;
    private CheckBox mMuteCheckBox;
    private CheckBox mConferenceCheckBox;
    private FloatingActionButton mMuteSpeakerButton;
    private FloatingActionButton mBitrateAdjustButton;
    private FloatingActionButton mGetParticipantsButton;
    private FloatingActionButton mTogglePlaybackButton;

    private Toast mToast = null;
    private ProgressDialog mProgressDialog;

    private RTCMediaStreamingManager mRTCStreamingManager;

    private StreamingProfile mStreamingProfile;

    private boolean mIsActivityPaused = true;
    private boolean mIsPublishStreamStarted = false;
    private boolean mIsConferenceStarted = false;
    private boolean mIsInReadyState = false;
    private int mCurrentCamFacingIndex;

    private CameraPreviewFrameView mCameraPreviewFrameView;
    private RotateLayout mRotateLayout;
    private int mCurrentZoom = 0;
    private int mMaxZoom = 0;
    private RTCVideoWindow mRTCVideoWindowA;
    private RTCVideoWindow mRTCVideoWindowB;

    private int mRole;
    private String mRoomName;

    private boolean mIsPreviewMirror = false;
    private boolean mIsEncodingMirror = false;
    private boolean mIsSpeakerMuted = false;
    private boolean mIsAudioLevelCallbackEnabled = false;
    private boolean mIsPictureStreaming = false;
    private boolean mIsPreviewOnTop = false;
    private boolean mIsWindowAOnBottom = false;

    private String mRemoteUserId;
    private String mBitrateControl;

    private boolean mIsPlayingBack = false;
    private boolean mIsFilter = false;

    private int mEncodingFps = 20;
    private int mEncodingBitrate = 1000 * 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture_streaming);

        /**
         * Step 1: find & init views
         */
        mCameraPreviewFrameView = (CameraPreviewFrameView) findViewById(R.id.cameraPreview_surfaceView);
        mCameraPreviewFrameView.setListener(mCameraPreviewListener);

        mRole = getIntent().getIntExtra("role", QiniuAppServer.RTC_ROLE_VICE_ANCHOR);
        mRoomName = getIntent().getStringExtra("roomName");
        boolean isSwCodec = getIntent().getBooleanExtra("swcodec", true);
        mIsLandscape = getIntent().getBooleanExtra("orientation", false);
        setRequestedOrientation(mIsLandscape ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        boolean isBeautyEnabled = getIntent().getBooleanExtra("beauty", false);
        boolean isWaterMarkEnabled = getIntent().getBooleanExtra("watermark", false);
        boolean isQuicEnable = getIntent().getBooleanExtra("quic", false);
        boolean isDebugModeEnabled = getIntent().getBooleanExtra("debugMode", false);
        boolean isCustomSettingEnabled = getIntent().getBooleanExtra("customSetting", false);
        boolean isStatsEnabled = getIntent().getBooleanExtra("enableStats", false);
        mIsAudioLevelCallbackEnabled = getIntent().getBooleanExtra("audioLevelCallback", false);
        mBitrateControl = getIntent().getStringExtra("bitrateControl");

        int previewSizeRatio = getIntent().getIntExtra(PREVIEW_SIZE_RATIO, 0);
        int previewSizeLevel = getIntent().getIntExtra(PREVIEW_SIZE_LEVEL, 0);
        int encodingSizeRatio = getIntent().getIntExtra(ENCODING_SIZE_RATIO, 0);
        int encodingSizeLevel = getIntent().getIntExtra(ENCODING_SIZE_LEVEL, 0);
        getEncodingConfig(getIntent().getIntExtra(ENCODING_CONFIG, 0));

        mControlButton = (Button) findViewById(R.id.ControlButton);
        mStatusTextView = (TextView) findViewById(R.id.StatusTextView);
        mStatTextView = (TextView) findViewById(R.id.StatTextView);
        mMuteCheckBox = (CheckBox) findViewById(R.id.MuteCheckBox);
        mMuteCheckBox.setOnClickListener(mMuteButtonClickListener);
        mConferenceCheckBox = (CheckBox) findViewById(R.id.ConferenceCheckBox);
        mConferenceCheckBox.setOnClickListener(mConferenceButtonClickListener);
        mMuteSpeakerButton = (FloatingActionButton) findViewById(R.id.muteSpeaker);
        mBitrateAdjustButton = (FloatingActionButton) findViewById(R.id.adjust_bitrate);
        mGetParticipantsButton = (FloatingActionButton) findViewById(R.id.get_participants);
        mTogglePlaybackButton = (FloatingActionButton) findViewById(R.id.toggle_playback);

        if (mRole == QiniuAppServer.RTC_ROLE_ANCHOR) {
            mConferenceCheckBox.setVisibility(View.VISIBLE);
        } else {
            mBitrateAdjustButton.setVisibility(View.GONE);
            mTogglePlaybackButton.setVisibility(View.GONE);
        }

        CameraStreamingSetting.CAMERA_FACING_ID facingId = chooseCameraFacingId();
        mCurrentCamFacingIndex = facingId.ordinal();

        /**
         * Step 2: config camera & microphone settings
         */
        CameraStreamingSetting cameraStreamingSetting = new CameraStreamingSetting();
        cameraStreamingSetting.setCameraFacingId(facingId)
                .setContinuousFocusModeEnabled(true)
                .setRecordingHint(false)
                .setResetTouchFocusDelayInMs(3000)
                .setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setCameraPrvSizeLevel(getPreviewSizeLevel(previewSizeLevel))
                .setCameraPrvSizeRatio(getPreviewSizeRatio(previewSizeRatio))
                .setPreviewAdaptToEncodingSize(false);

        if (isBeautyEnabled) {
            // 使用TuSdk前需要配置
            cameraStreamingSetting.setBuiltInFaceBeautyEnabled(false); // Using sdk built in face beauty algorithm
            cameraStreamingSetting.setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(0.8f, 0.8f, 0.6f)); // sdk built in face beauty settings
            cameraStreamingSetting.setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY); // set the beauty on/off
        }

        /**
         * Step 3: create streaming manager and set listeners
         */
        AVCodecType codecType = isSwCodec ? AVCodecType.SW_VIDEO_WITH_SW_AUDIO_CODEC : AVCodecType.HW_VIDEO_YUV_AS_INPUT_WITH_HW_AUDIO_CODEC;
        mCameraPreviewFrameView.setEGLContextFactory(new TuSDKEGLContextFactory());
        mRTCStreamingManager = new RTCMediaStreamingManager(getApplicationContext(), mCameraPreviewFrameView, codecType);
        mRTCStreamingManager.setConferenceStateListener(mRTCStreamingStateChangedListener);
        mRTCStreamingManager.setRemoteWindowEventListener(mRTCRemoteWindowEventListener);
        mRTCStreamingManager.setUserEventListener(mRTCUserEventListener);
        mRTCStreamingManager.setDebugLoggingEnabled(isDebugModeEnabled);

        if (mIsAudioLevelCallbackEnabled) {
            mRTCStreamingManager.setAudioLevelCallback(mRTCAudioLevelCallback);
        }

        /**
         * Step 4: set conference options
         */
        RTCConferenceOptions options = new RTCConferenceOptions();
        if (mRole == QiniuAppServer.RTC_ROLE_ANCHOR) {
            // anchor should use a bigger size, must equals to `StreamProfile.setPreferredVideoEncodingSize` or `StreamProfile.setEncodingSizeLevel`
            // RATIO_16_9 & VIDEO_ENCODING_SIZE_HEIGHT_480 means the output size is 848 x 480
            options.setVideoEncodingSizeRatio(getEncodingSizeRatio(encodingSizeRatio));
            options.setVideoEncodingSizeLevel(encodingSizeLevel);
            options.setVideoBitrateRange(500 * 1024, 800 * 1024);
            // 15 fps is enough
            options.setVideoEncodingFps(mEncodingFps);
        } else {
            // vice anchor can use a smaller size
            // RATIO_4_3 & VIDEO_ENCODING_SIZE_HEIGHT_240 means the output size is 320 x 240
            // 4:3 looks better in the mix frame
            options.setVideoEncodingSizeRatio(getEncodingSizeRatio(encodingSizeRatio));
            options.setVideoEncodingSizeLevel(encodingSizeLevel);
            options.setVideoBitrateRange(300 * 1024, 500 * 1024);
            // 15 fps is enough
            options.setVideoEncodingFps(mEncodingFps);
        }
        options.setHWCodecEnabled(!isSwCodec);
        if (isStatsEnabled) {
            options.setStreamStatsInterval(500);
            mRTCStreamingManager.setRTCStreamStatsCallback(mRTCStreamStatsCallback);
        }

        if (mIsLandscape) {
            options.setVideoEncodingOrientation(RTCConferenceOptions.VIDEO_ENCODING_ORIENTATION.LAND);
        } else {
            options.setVideoEncodingOrientation(RTCConferenceOptions.VIDEO_ENCODING_ORIENTATION.PORT);
        }

        mRTCStreamingManager.setConferenceOptions(options);

        /**
         * Step 5: create the remote windows
         */
        RTCVideoWindow windowA = new RTCVideoWindow(findViewById(R.id.RemoteWindowA), (RTCSurfaceView)findViewById(R.id.RemoteGLSurfaceViewA));
        RTCVideoWindow windowB = new RTCVideoWindow(findViewById(R.id.RemoteWindowB), (RTCSurfaceView)findViewById(R.id.RemoteGLSurfaceViewB));

        /**
         * Step 6: configure the mix stream position and size (only anchor)
         */
        if (mRole == QiniuAppServer.RTC_ROLE_ANCHOR) {
            // set mix overlay params with absolute value
            // the w & h of remote window equals with or smaller than the vice anchor can reduce cpu consumption
            if (mIsLandscape) {
                windowA.setAbsoluteMixOverlayRect(options.getVideoEncodingWidth() - 320, 100, 320, 240);
                windowB.setAbsoluteMixOverlayRect(0, 100, 320, 240);
            } else {
                windowA.setAbsoluteMixOverlayRect(options.getVideoEncodingHeight() - 240, 100, 240, 320);
                windowB.setAbsoluteMixOverlayRect(options.getVideoEncodingHeight() - 240, 420, 240, 320);
            }

            // set mix overlay params with relative value
            // windowA.setRelativeMixOverlayRect(0.65f, 0.2f, 0.3f, 0.3f);
            // windowB.setRelativeMixOverlayRect(0.65f, 0.5f, 0.3f, 0.3f);
        }

        /**
         * Step 7: add the remote windows
         */
        mRTCStreamingManager.addRemoteWindow(windowA);
        mRTCStreamingManager.addRemoteWindow(windowB);

        mRTCVideoWindowA = windowA;
        mRTCVideoWindowB = windowB;

        /**
         * Step 8: do prepare, anchor should config streaming profile first
         */
        mRTCStreamingManager.setMixedFrameCallback(new RTCFrameMixedCallback() {
            @Override
            public void onVideoFrameMixed(byte[] data, int width, int height, int fmt, long timestamp) {
//                Log.d(TAG, "Mixed video: " + data.toString() + "  Format: " + fmt);
            }

            @Override
            public void onAudioFrameMixed(byte[] pcm, long timestamp) {
//                Log.i(TAG, "Mixed audio");
            }
        });

        if (mRole == QiniuAppServer.RTC_ROLE_ANCHOR) {
            mRTCStreamingManager.setStreamStatusCallback(mStreamStatusCallback);
            mRTCStreamingManager.setStreamingStateListener(mStreamingStateChangedListener);
            mRTCStreamingManager.setStreamingSessionListener(mStreamingSessionListener);

            mStreamingProfile = new StreamingProfile();
            mStreamingProfile.setVideoQuality(StreamingProfile.VIDEO_QUALITY_MEDIUM2)
                    .setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM1)
                    .setEncoderRCMode(StreamingProfile.EncoderRCModes.BITRATE_PRIORITY)
                    .setFpsControllerEnable(true)
                    .setQuicEnable(isQuicEnable)
                    .setYuvFilterMode(StreamingSettings.YUV_FILTER_MODE_MAPPING[getIntent().getIntExtra("yuvFilterMode", 0)])
                    .setPictureStreamingResourceId(R.drawable.pause_publish)
                    .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000))
                    .setBitrateAdjustMode(
                            mBitrateControl.equals("auto") ? StreamingProfile.BitrateAdjustMode.Auto
                                    : (mBitrateControl.equals("manual") ? StreamingProfile.BitrateAdjustMode.Manual
                                    : StreamingProfile.BitrateAdjustMode.Disable));

            // Set AVProfile Manually, which will cover `setXXXQuality`
            if (isCustomSettingEnabled) {
                StreamingProfile.AudioProfile aProfile = new StreamingProfile.AudioProfile(44100, 96 * 1024);
                StreamingProfile.VideoProfile vProfile = new StreamingProfile.VideoProfile(mEncodingFps, mEncodingBitrate, mEncodingFps * 3, StreamingSettings.VIDEO_QUALITY_PROFILES_MAPPING[getIntent().getIntExtra("videoProfile", 0)]);
                StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, aProfile);
                mStreamingProfile.setAVProfile(avProfile);
            }

            // options.getVideoEncodingWidth() > options.getVideoEncodingHeight() defaulted, so if we use StreamingProfile.setPreferredVideoEncodingSize
            // to config the encoding size,we should pass the parameters flexibility according to the orientation
            if (mIsLandscape) {
                mStreamingProfile.setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.LAND);
                mStreamingProfile.setPreferredVideoEncodingSize(options.getVideoEncodingWidth(), options.getVideoEncodingHeight());
            } else {
                mStreamingProfile.setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.PORT);
                mStreamingProfile.setPreferredVideoEncodingSize(options.getVideoEncodingHeight(), options.getVideoEncodingWidth());
            }

            WatermarkSetting watermarksetting = null;
            if (isWaterMarkEnabled) {
                watermarksetting = new WatermarkSetting(this);
                watermarksetting.setResourceId(R.drawable.qiniu_logo)
                        .setSize(WatermarkSetting.WATERMARK_SIZE.MEDIUM)
                        .setAlpha(100)
                        .setCustomPosition(0.5f, 0.5f);
            }
            mRTCStreamingManager.prepare(cameraStreamingSetting, null, watermarksetting, mStreamingProfile);
        } else {
            mControlButton.setText("开始连麦");
            mRTCStreamingManager.prepare(cameraStreamingSetting, null);
        }

        mProgressDialog = new ProgressDialog(this);

        /// ========================= TuSDK 相关 ========================= ///

        mRTCStreamingManager.setSurfaceTextureCallback(this);
        mRTCStreamingManager.setStreamingPreviewCallback(this);

        initTuSDK();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityPaused = false;
        /**
         * Step 9: You must start capture before conference or streaming
         * You will receive `Ready` state callback when capture started success
         */
        mRTCStreamingManager.startCapture();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActivityPaused = true;
        /**
         * Step 10: You must stop capture, stop conference, stop streaming when activity paused
         */
        mRTCStreamingManager.stopCapture();
        mIsInReadyState = false;
        stopConference();
        stopPublishStreaming();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * Step 11: You must call destroy to release some resources when activity destroyed
         */
        mRTCStreamingManager.destroy();
    }

    public void onClickKickoutUserA(View v) {
        mRTCStreamingManager.kickoutUser(R.id.RemoteGLSurfaceViewA);
    }

    public void onClickKickoutUserB(View v) {
        mRTCStreamingManager.kickoutUser(R.id.RemoteGLSurfaceViewB);
    }

    public void onClickCaptureFrame(View v) {
        if (isPictureStreaming()) {
            return;
        }
        mRTCStreamingManager.captureFrame(new RTCFrameCapturedCallback() {
            @Override
            public void onFrameCaptureSuccess(Bitmap bitmap) {
                String filepath = Environment.getExternalStorageDirectory() + "/captured.jpg";
                saveBitmapToSDCard(filepath, bitmap);
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filepath)));
                showToast("截帧成功, 存放在 " + filepath, Toast.LENGTH_SHORT);
            }

            @Override
            public void onFrameCaptureFailed(int errorCode) {
                showToast("截帧失败，错误码：" + errorCode, Toast.LENGTH_SHORT);
            }
        });
    }

    public void onClickPreviewMirror(View v) {
        if (isPictureStreaming()) {
            return;
        }
        if (mRTCStreamingManager.setPreviewMirror(!mIsPreviewMirror)) {
            mIsPreviewMirror = !mIsPreviewMirror;
            showToast(getString(R.string.mirror_success), Toast.LENGTH_SHORT);
        }
    }

    public void onClickEncodingMirror(View v) {
        if (isPictureStreaming()) {
            return;
        }
        if (mRTCStreamingManager.setEncodingMirror(!mIsEncodingMirror)) {
            mIsEncodingMirror = !mIsEncodingMirror;
            showToast(getString(R.string.mirror_success), Toast.LENGTH_SHORT);
        }
    }

    public void onClickSwitchCamera(View v) {
        if (isPictureStreaming()) {
            return;
        }
        mCurrentCamFacingIndex = (mCurrentCamFacingIndex + 1) % CameraStreamingSetting.getNumberOfCameras();
        CameraStreamingSetting.CAMERA_FACING_ID facingId;
        if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK.ordinal()) {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        } else if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT.ordinal()) {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        }
        Log.i(TAG, "switchCamera:" + facingId);
        mRTCStreamingManager.switchCamera(facingId);
        mIsEncodingMirror = false;
        mIsPreviewMirror = false;
    }

    public void onClickAdjustBitrate(View v) {
        if (mBitrateControl.equals("manual")) {
            MaterialDialog.Builder builder = new MaterialDialog.Builder(this);
            builder.title("请输入目标码率")
                    .customView(R.layout.dialog_bitrate_adjust)
                    .positiveText("确认")
                    .positiveColor(Color.parseColor("#03a9f4"))
                    .negativeText("取消")
                    .callback(new MaterialDialog.Callback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            String textInput = ((EditText) dialog.findViewById(R.id.adjust_bitrate)).getText().toString().trim();
                            int bitrate = 0;
                            if (!textInput.isEmpty()) {
                                bitrate = Integer.parseInt(textInput);
                            }
                            if (bitrate < MIN_BITRATE || bitrate > MAX_BITRATE) {
                                Toast.makeText(RTCStreamingActivity.this, "请输入规定范围内的码率", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            boolean result = mRTCStreamingManager.adjustVideoBitrate(bitrate);
                            if (result) {
                                Toast.makeText(RTCStreamingActivity.this, "调整成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RTCStreamingActivity.this, "调整失败", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            dialog.dismiss();
                        }
                    })
                    .build()
                    .show();
        } else {
            Toast.makeText(this, "当前模式为非手动调节码率模式", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickMuteSpeaker(View v) {
        if (mIsConferenceStarted) {
            if (mIsSpeakerMuted) {
                mRTCStreamingManager.unMute(RTCAudioSource.SPEAKER);
                mRTCStreamingManager.unMute(RTCAudioSource.MIXAUDIO);
            } else {
                mRTCStreamingManager.mute(RTCAudioSource.SPEAKER);
                mRTCStreamingManager.mute(RTCAudioSource.MIXAUDIO);
            }
            mIsSpeakerMuted = !mIsSpeakerMuted;
            mMuteSpeakerButton.setTitle(mIsSpeakerMuted ? getResources().getString(R.string.button_unmute_speaker) : getResources().getString(R.string.button_mute_speaker));
            showToast(getString(mIsSpeakerMuted ? R.string.others_muted : R.string.others_unmuted),
                    Toast.LENGTH_SHORT);
        } else {
            showToast(getString(R.string.others_fail), Toast.LENGTH_SHORT);
        }
    }

    public void onClickTogglePlayback(View v) {
        if (!mIsPublishStreamStarted) {
            showToast("请先开始直播！", Toast.LENGTH_SHORT);
            return;
        }
        if (mIsPlayingBack) {
            mRTCStreamingManager.stopPlayback();
            mTogglePlaybackButton.setTitle("开启返听");
        } else {
            mRTCStreamingManager.startPlayback();
            mTogglePlaybackButton.setTitle("关闭返听");
        }
        mIsPlayingBack = !mIsPlayingBack;
    }

    public void onClickRemoteWindowA(View v) {
        if (!mIsPreviewOnTop) {
            mRTCStreamingManager.switchRenderView(mRTCVideoWindowA.getRTCSurfaceView(), mCameraPreviewFrameView);
            mIsPreviewOnTop = true;
            mIsWindowAOnBottom = true;
        }
    }

    public void onClickRemoteWindowB(View v) {
        if (!mIsPreviewOnTop) {
            mRTCStreamingManager.switchRenderView(mRTCVideoWindowB.getRTCSurfaceView(), mCameraPreviewFrameView);
            mIsPreviewOnTop = true;
            mIsWindowAOnBottom = false;
        }
    }

    public void onClickGetParticipants(View v) {
        Toast.makeText(this, "count = " + mRTCStreamingManager.getParticipantsCount(), Toast.LENGTH_SHORT).show();
    }

    public void onClickExit(View v) {
        finish();
    }

    private CameraStreamingSetting.PREVIEW_SIZE_RATIO getPreviewSizeRatio(int position) {
        return position == 0 ? CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_4_3 : CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9;
    }

    private CameraStreamingSetting.PREVIEW_SIZE_LEVEL getPreviewSizeLevel(int position) {
        return position == 0 ? CameraStreamingSetting.PREVIEW_SIZE_LEVEL.SMALL
                : (position == 1 ? CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM : CameraStreamingSetting.PREVIEW_SIZE_LEVEL.LARGE);
    }

    private RTCConferenceOptions.VIDEO_ENCODING_SIZE_RATIO getEncodingSizeRatio(int position) {
        return position == 0 ? RTCConferenceOptions.VIDEO_ENCODING_SIZE_RATIO.RATIO_4_3 : RTCConferenceOptions.VIDEO_ENCODING_SIZE_RATIO.RATIO_16_9;
    }

    private void getEncodingConfig(int position) {
        switch (position) {
            case 0:
                mEncodingFps = 15;
                mEncodingBitrate = 800 * 1024;
                break;
            case 1:
                mEncodingFps = 15;
                mEncodingBitrate = 1200 * 1024;
                break;
            case 2:
                mEncodingFps = 24;
                mEncodingBitrate = 800 * 1024;
                break;
            case 3:
                mEncodingFps = 24;
                mEncodingBitrate = 1200 * 1024;
                break;
            case 4:
                mEncodingFps = 30;
                mEncodingBitrate = 800 * 1024;
                break;
            case 5:
                mEncodingFps = 30;
                mEncodingBitrate = 1200 * 1024;
                break;
        }
    }

    private boolean isPictureStreaming() {
        if (mIsPictureStreaming) {
            Toast.makeText(RTCStreamingActivity.this, "is picture streaming, operation failed!", Toast.LENGTH_SHORT).show();
        }
        return mIsPictureStreaming;
    }

    private boolean startConference() {
        if (!QiniuAppServer.isNetworkAvailable(this)) {
            Toast.makeText(RTCStreamingActivity.this, "network is unavailable!!!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mIsConferenceStarted) {
            return true;
        }
        mProgressDialog.setMessage("正在加入连麦 ... ");
        mProgressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                startConferenceInternal();
            }
        }).start();
        return true;
    }

    private boolean startConferenceInternal() {
        String roomToken = QiniuAppServer.getInstance().requestRoomToken(QiniuAppServer.getTestUserId(this), mRoomName);
        if (roomToken == null) {
            dismissProgressDialog();
            showToast("无法获取房间信息 !", Toast.LENGTH_SHORT);
            return false;
        }

        mRTCStreamingManager.startConference(QiniuAppServer.getTestUserId(this), mRoomName, roomToken, new RTCStartConferenceCallback() {
            @Override
            public void onStartConferenceSuccess() {
                dismissProgressDialog();
                showToast(getString(R.string.start_conference), Toast.LENGTH_SHORT);
                updateControlButtonText();
                mIsConferenceStarted = true;
                mRTCStreamingManager.setAudioLevelMonitorEnabled(mIsAudioLevelCallbackEnabled);
                mRTCStreamingManager.setMixedFrameCallbackEnabled(true);
                /**
                 * Because `startConference` is called in child thread
                 * So we should check if the activity paused.
                 */
                if (mIsActivityPaused) {
                    stopConference();
                }
            }

            @Override
            public void onStartConferenceFailed(int errorCode) {
                setConferenceBoxChecked(false);
                dismissProgressDialog();
                showToast(getString(R.string.failed_to_start_conference) + errorCode, Toast.LENGTH_SHORT);
            }
        });
        return true;
    }

    private boolean stopConference() {
        if (!mIsConferenceStarted) {
            return true;
        }
        mRTCStreamingManager.stopConference();
        mIsConferenceStarted = false;
        setConferenceBoxChecked(false);
        showToast(getString(R.string.stop_conference), Toast.LENGTH_SHORT);
        updateControlButtonText();
        return true;
    }

    private boolean startPublishStreaming() {
        if (!QiniuAppServer.isNetworkAvailable(this)) {
            Toast.makeText(RTCStreamingActivity.this, "network is unavailable!!!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mIsPublishStreamStarted) {
            return true;
        }
        if (!mIsInReadyState) {
            showToast(getString(R.string.stream_state_not_ready), Toast.LENGTH_SHORT);
            return false;
        }
        mProgressDialog.setMessage("正在准备推流... ");
        mProgressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                startPublishStreamingInternal();
            }
        }).start();
        return true;
    }

    private boolean startPublishStreamingInternal() {
        String publishAddr = QiniuAppServer.getInstance().requestPublishAddress(mRoomName);
        if (publishAddr == null) {
            dismissProgressDialog();
            showToast("无法获取房间信息/推流地址 !", Toast.LENGTH_SHORT);
            return false;
        }

        try {
            mStreamingProfile.setPublishUrl(publishAddr);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            dismissProgressDialog();
            showToast("无效的推流地址 !", Toast.LENGTH_SHORT);
            return false;
        }

        mRTCStreamingManager.setStreamingProfile(mStreamingProfile);
        if (!mRTCStreamingManager.startStreaming()) {
            dismissProgressDialog();
            showToast(getString(R.string.failed_to_start_streaming), Toast.LENGTH_SHORT);
            return false;
        }
        dismissProgressDialog();
        showToast(getString(R.string.start_streaming), Toast.LENGTH_SHORT);
        updateControlButtonText();
        mIsPublishStreamStarted = true;
        /**
         * Because `startPublishStreaming` need a long time in some weak network
         * So we should check if the activity paused.
         */
        if (mIsActivityPaused) {
            stopPublishStreaming();
        }
        return true;
    }

    private boolean stopPublishStreaming() {
        if (!mIsPublishStreamStarted) {
            return true;
        }
        mRTCStreamingManager.stopStreaming();
        mIsPublishStreamStarted = false;
        showToast(getString(R.string.stop_streaming), Toast.LENGTH_SHORT);
        updateControlButtonText();
        return false;
    }

    private StreamingStateChangedListener mStreamingStateChangedListener = new StreamingStateChangedListener() {
        @Override
        public void onStateChanged(final StreamingState state, Object o) {
            switch (state) {
                case PREPARING:
                    setStatusText(getString(R.string.preparing));
                    Log.d(TAG, "onStateChanged state:" + "preparing");
                    break;
                case READY:
                    mIsInReadyState = true;
                    mMaxZoom = mRTCStreamingManager.getMaxZoom();
                    setStatusText(getString(R.string.ready));
                    Log.d(TAG, "onStateChanged state:" + "ready");
                    break;
                case CONNECTING:
                    Log.d(TAG, "onStateChanged state:" + "connecting");
                    break;
                case STREAMING:
                    setStatusText(getString(R.string.streaming));
                    Log.d(TAG, "onStateChanged state:" + "streaming");
                    break;
                case SHUTDOWN:
                    mIsInReadyState = true;
                    setStatusText(getString(R.string.ready));
                    Log.d(TAG, "onStateChanged state:" + "shutdown");
                    break;
                case UNKNOWN:
                    Log.d(TAG, "onStateChanged state:" + "unknown");
                    break;
                case SENDING_BUFFER_EMPTY:
                    Log.d(TAG, "onStateChanged state:" + "sending buffer empty");
                    break;
                case SENDING_BUFFER_FULL:
                    Log.d(TAG, "onStateChanged state:" + "sending buffer full");
                    break;
                case OPEN_CAMERA_FAIL:
                    Log.d(TAG, "onStateChanged state:" + "open camera failed");
                    showToast(getString(R.string.failed_open_camera), Toast.LENGTH_SHORT);
                    break;
                case AUDIO_RECORDING_FAIL:
                    Log.d(TAG, "onStateChanged state:" + "audio recording failed");
                    showToast(getString(R.string.failed_open_microphone), Toast.LENGTH_SHORT);
                    break;
                case IOERROR:
                    /**
                     * Network-connection is unavailable when `startStreaming`.
                     * You can do reconnecting or just finish the streaming
                     */
                    Log.d(TAG, "onStateChanged state:" + "io error");
                    showToast(getString(R.string.io_error), Toast.LENGTH_SHORT);
                    sendReconnectMessage();
                    // stopPublishStreaming();
                    break;
                case DISCONNECTED:
                    /**
                     * Network-connection is broken after `startStreaming`.
                     * You can do reconnecting in `onRestartStreamingHandled`
                     */
                    Log.d(TAG, "onStateChanged state:" + "disconnected");
                    setStatusText(getString(R.string.disconnected));
                    // we will process this state in `onRestartStreamingHandled`
                    break;
            }
        }
    };

    private StreamingSessionListener mStreamingSessionListener = new StreamingSessionListener() {
        @Override
        public boolean onRecordAudioFailedHandled(int code) {
            return false;
        }

        /**
         * When the network-connection is broken, StreamingState#DISCONNECTED will notified first,
         * and then invoked this method if the environment of restart streaming is ready.
         *
         * @return true means you handled the event; otherwise, given up and then StreamingState#SHUTDOWN
         * will be notified.
         */
        @Override
        public boolean onRestartStreamingHandled(int code) {
            Log.d(TAG, "onRestartStreamingHandled, reconnect ...");
            return mRTCStreamingManager.startStreaming();
        }

        @Override
        public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
            for (Camera.Size size : list) {
                if (size.height >= 480) {
                    return size;
                }
            }
            return null;
        }

        @Override
        public int onPreviewFpsSelected(List<int[]> list) {
            return -1;
        }
    };

    private RTCStreamStatsCallback mRTCStreamStatsCallback = new RTCStreamStatsCallback() {
        @Override
        public void onStreamStatsChanged(String userId, int statsType, int value) {
            Log.i(TAG, "userId = " + userId + "statsType = " + statsType + " value = " + value);
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MESSAGE_ID_RECONNECTING || mIsActivityPaused || !mIsPublishStreamStarted) {
                return;
            }
            if (!QiniuAppServer.isNetworkAvailable(RTCStreamingActivity.this)) {
                sendReconnectMessage();
                return;
            }
            Log.d(TAG, "do reconnecting ...");
            mRTCStreamingManager.startStreaming();
        }
    };

    private void sendReconnectMessage() {
        showToast("正在重连...", Toast.LENGTH_SHORT);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_ID_RECONNECTING), 500);
    }

    private RTCConferenceStateChangedListener mRTCStreamingStateChangedListener = new RTCConferenceStateChangedListener() {
        @Override
        public void onConferenceStateChanged(RTCConferenceState state, int extra) {
            switch (state) {
                case READY:
                    // You must `StartConference` after `Ready`
                    mIsInReadyState = true;
                    mMaxZoom = mRTCStreamingManager.getMaxZoom();
                    showToast(getString(R.string.ready), Toast.LENGTH_SHORT);
                    break;
                case RECONNECTING:
                    showToast(getString(R.string.reconnecting), Toast.LENGTH_SHORT);
                    break;
                case RECONNECTED:
                    showToast(getString(R.string.reconnected), Toast.LENGTH_SHORT);
                    break;
                case RECONNECT_FAIL:
                    showToast(getString(R.string.reconnect_failed), Toast.LENGTH_SHORT);
                    break;
                case VIDEO_PUBLISH_FAILED:
                case AUDIO_PUBLISH_FAILED:
                    showToast(getString(R.string.failed_to_publish_av_to_rtc) + extra, Toast.LENGTH_SHORT);
                    finish();
                    break;
                case VIDEO_PUBLISH_SUCCESS:
                    showToast(getString(R.string.success_publish_video_to_rtc), Toast.LENGTH_SHORT);
                    break;
                case AUDIO_PUBLISH_SUCCESS:
                    showToast(getString(R.string.success_publish_audio_to_rtc), Toast.LENGTH_SHORT);
                    break;
                case USER_JOINED_AGAIN:
                    showToast(getString(R.string.user_join_other_where), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case USER_KICKOUT_BY_HOST:
                    showToast(getString(R.string.user_kickout_by_host), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case OPEN_CAMERA_FAIL:
                    showToast(getString(R.string.failed_open_camera), Toast.LENGTH_SHORT);
                    break;
                case AUDIO_RECORDING_FAIL:
                    showToast(getString(R.string.failed_open_microphone), Toast.LENGTH_SHORT);
                    break;
                default:
                    return;
            }
        }
    };

    private RTCUserEventListener mRTCUserEventListener = new RTCUserEventListener() {
        @Override
        public void onUserJoinConference(String remoteUserId) {
            Log.d(TAG, "onUserJoinConference: " + remoteUserId);
        }

        @Override
        public void onUserLeaveConference(String remoteUserId) {
            Log.d(TAG, "onUserLeaveConference: " + remoteUserId);
        }
    };

    private RTCRemoteWindowEventListener mRTCRemoteWindowEventListener = new RTCRemoteWindowEventListener() {
        @Override
        public void onRemoteWindowAttached(RTCVideoWindow window, String remoteUserId) {
            Log.d(TAG, "onRemoteWindowAttached: " + remoteUserId);
            mRemoteUserId = remoteUserId;
            if (mIsPictureStreaming) {
                mRTCStreamingManager.unsubscribeVideoStream(mRemoteUserId);
            }
        }

        @Override
        public void onRemoteWindowDetached(RTCVideoWindow window, String remoteUserId) {
            Log.d(TAG, "onRemoteWindowDetached: " + remoteUserId);
            if (!mIsPictureStreaming) {
                mRemoteUserId = null;
            }
        }

        @Override
        public void onFirstRemoteFrameArrived(String remoteUserId) {
            Log.d(TAG, "onFirstRemoteFrameArrived: " + remoteUserId);
        }
    };

    private RTCAudioLevelCallback mRTCAudioLevelCallback = new RTCAudioLevelCallback() {
        @Override
        public void onAudioLevelChanged(String userId, int level) {
            Log.d(TAG, "onAudioLevelChanged: userId = " + userId + " level = " + level);
        }
    };

    private CameraPreviewFrameView.Listener mCameraPreviewListener = new CameraPreviewFrameView.Listener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mIsPreviewOnTop) {
                RTCVideoWindow window = mIsWindowAOnBottom ? mRTCVideoWindowA : mRTCVideoWindowB;
                mRTCStreamingManager.switchRenderView(mCameraPreviewFrameView, window.getRTCSurfaceView());
                mIsPreviewOnTop = false;
                mIsWindowAOnBottom = false;
                return true;
            }
            Log.i(TAG, "onSingleTapUp X:" + e.getX() + ",Y:" + e.getY());
            if (mIsInReadyState) {
                setFocusAreaIndicator();
                mRTCStreamingManager.doSingleTapUp((int) e.getX(), (int) e.getY());
                return true;
            }
            return false;
        }

        @Override
        public boolean onZoomValueChanged(float factor) {
            if (mIsInReadyState && mRTCStreamingManager.isZoomSupported() && !mIsPreviewOnTop) {
                mCurrentZoom = (int) (mMaxZoom * factor);
                mCurrentZoom = Math.min(mCurrentZoom, mMaxZoom);
                mCurrentZoom = Math.max(0, mCurrentZoom);
                Log.d(TAG, "zoom ongoing, scale: " + mCurrentZoom + ",factor:" + factor + ",maxZoom:" + mMaxZoom);
                mRTCStreamingManager.setZoomValue(mCurrentZoom);
            }
            return false;
        }
    };

    protected void setFocusAreaIndicator() {
        if (mRotateLayout == null) {
            mRotateLayout = (RotateLayout) findViewById(R.id.focus_indicator_rotate_layout);
            mRTCStreamingManager.setFocusAreaIndicator(mRotateLayout,
                    mRotateLayout.findViewById(R.id.focus_indicator));
        }
    }

    private View.OnClickListener mMuteButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mMuteCheckBox.isChecked()) {
                mRTCStreamingManager.mute(RTCAudioSource.MIC);
            } else {
                mRTCStreamingManager.unMute(RTCAudioSource.MIC);
            }
        }
    };

    private View.OnClickListener mConferenceButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConferenceCheckBox.isChecked()) {
                startConference();
            } else {
                stopConference();
            }
        }
    };
    
    public void onClickStreaming(View v) {
        if (mRole == QiniuAppServer.RTC_ROLE_ANCHOR) {
            if (!mIsPublishStreamStarted) {
                startPublishStreaming();
            } else {
                stopPublishStreaming();
            }
        } else {
            if (!mIsConferenceStarted) {
                startConference();
            } else {
                stopConference();
            }
        }
    }

    private void setStatusText(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatusTextView.setText(status);
            }
        });
    }

    private void updateControlButtonText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRole == QiniuAppServer.RTC_ROLE_ANCHOR) {
                    if (mIsPublishStreamStarted) {
                        mControlButton.setText(getString(R.string.stop_streaming));
                    } else {
                        mControlButton.setText(getString(R.string.start_streaming));
                    }
                } else {
                    if (mIsConferenceStarted) {
                        mControlButton.setText(getString(R.string.stop_conference));
                    } else {
                        mControlButton.setText(getString(R.string.start_conference));
                    }
                }
            }
        });
    }

    private void setConferenceBoxChecked(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConferenceCheckBox.setChecked(enabled);
            }
        });
    }

    private StreamStatusCallback mStreamStatusCallback = new StreamStatusCallback() {
        @Override
        public void notifyStreamStatusChanged(final StreamingProfile.StreamStatus streamStatus) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String stat = "bitrate: " + streamStatus.totalAVBitrate / 1024 + " kbps"
                            + "\naudio: " + streamStatus.audioFps + " fps"
                            + "\nvideo: " + streamStatus.videoFps + " fps";
                    mStatTextView.setText(stat);
                }
            });
        }
    };

    private void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }

    private void showToast(final String text, final int duration) {
        if (mIsActivityPaused) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(RTCStreamingActivity.this, text, duration);
                mToast.show();
            }
        });
    }

    private CameraStreamingSetting.CAMERA_FACING_ID chooseCameraFacingId() {
        if (CameraStreamingSetting.hasCameraFacing(CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD)) {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        } else if (CameraStreamingSetting.hasCameraFacing(CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT)) {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        }
    }

    private static boolean saveBitmapToSDCard(String filepath, Bitmap bitmap) {
        try {
            FileOutputStream fos = new FileOutputStream(filepath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void onSurfaceCreated()
    {
        prepareFilterEngine();
        mFilterEngine.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(int width, int height)
    {

        mFilterEngine.onSurfaceChanged(width, height);
if(mIsFilter){
    //prepareFilterEngine();
    mIsFilter = false;
}else{
    mIsFilter = true;
}

        Log.d(TAG, "hugo onSurfaceChanged: "+mIsFilter+"  width==>"+width+"  height==>"+height);
    }

    @Override
    public void onSurfaceDestroyed()
    {
        destroyFilterEngine();
    }

    /**
     * 处理 OES 纹理
     *
     * @param texId
     * @param texWidth
     * @param texHeight
     * @param transformMatrix
     * @return
     */
    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight, float[] transformMatrix)
    {
        Log.d(TAG, "hugo onDrawFrame: texWidth"+texWidth+"texHeight"+texHeight);
        if (mFilterEngine == null) return texId;

        int newTexId = mFilterEngine.processFrame(texId, texWidth, texHeight);

        return newTexId;
    }

    /**
     * 获取抓取帧的输出方向
     *
     * @return ImageOrientation
     */
    private ImageOrientation getSnatchFrameOrienation()
    {
        if (mIsLandscape) return ImageOrientation.Up;

        if(mFilterEngine.getCameraFacing() == CameraConfigs.CameraFacing.Front)
            return ImageOrientation.LeftMirrored;
        else
            return ImageOrientation.RightMirrored;
    }

    /**
     * 处理 YUV 数据
     * @param bytes
     * @param width
     * @param height
     * @param rotation
     * @param fmt
     * @param tsInNanoTime
     * @return
     */
    @Override
    public boolean onPreviewFrame(byte[] bytes, int width, int height, int rotation, int fmt, long tsInNanoTime)
    {
        Log.d(TAG, "hugo onPreviewFrame: wid"+width+"height"+height);
        mFilterEngine.snatchFrame(bytes, TuSDKVideoCaptureSetting.ImageFormatType.NV21, getSnatchFrameOrienation());
        return true;
    }

    private void updateTuSDKFilterEngineCameraInfo(CameraConfigs.CameraFacing facing)
    {
        mFilterEngine.setCameraFacing(facing);

        if (mIsLandscape)
        {
            mFilterEngine.setInputImageOrientation(facing == CameraConfigs.CameraFacing.Back ? ImageOrientation.Up : ImageOrientation.UpMirrored);
            mFilterEngine.setOutputImageOrientation(facing == CameraConfigs.CameraFacing.Back ? ImageOrientation.Up : ImageOrientation.UpMirrored);
        }
        else
        {
            mFilterEngine.setInputImageOrientation(facing == CameraConfigs.CameraFacing.Back ? ImageOrientation.RightMirrored : ImageOrientation.LeftMirrored);
            mFilterEngine.setOutputImageOrientation(facing == CameraConfigs.CameraFacing.Back ? ImageOrientation.RightMirrored : ImageOrientation.LeftMirrored);
        }
    }

    private class TuSDKEGLContextFactory extends SelesEGLContextFactory
    {
        public TuSDKEGLContextFactory()
        {
            super(2);
        }

        @Override
        public void destroyContext(EGL10 egl, EGLDisplay display, javax.microedition.khronos.egl.EGLContext context)
        {
            if (mFilterEngine != null)
            {
                mFilterEngine.onSurfaceDestroy();
            }
            super.destroyContext(egl, display, context);
        }
    }

    /// ========================= TuSDK 相关 ========================= ///

    // 滤镜 code 列表, 每个 code 代表一种滤镜效果, 具体 code 可在 lsq_tusdk_configs.json 查看 (例如:lsq_filter_SkinNature02 滤镜的 code 为 SkinNature02)
    private static final String[] VIDEOFILTERS = new String[]{"none","nature01", "pink01", "jelly01", "ruddy01", "sugar01",
            "honey01", "clear01","timber01","whitening01","porcelain01","Skinwhitening_1"};

    /** 参数调节视图 */
    protected FilterConfigView mConfigView;
    /** 滤镜栏视图 */
    protected FilterListView mFilterListView;
    /** 滤镜底部栏 */
    private View mFilterBottomView;

    // 记录是否是首次进入录制页面
    private boolean mIsFirstEntry = true;
    // 记录当前滤镜
    private FilterWrap mSelesOutInput;
    // 滤镜Tab
    private TuSdkTextButton mFilterTab;
    // 美颜布局
    private RelativeLayout mFilterLayout;
    // 美颜Tab
    private TuSdkTextButton mBeautyTab;
    // 美颜布局
    private LinearLayout mBeautyLayout;
    // 磨皮调节栏
    private ConfigViewSeekBar mSmoothingBarLayout;
    // 大眼调节栏
    private ConfigViewSeekBar mEyeSizeBarLayout;
    // 瘦脸调节栏
    private ConfigViewSeekBar mChinSizeBarLayout;
    // 用于记录当前调节栏效果系数
    private float mMixiedProgress = -1.0f;
    // 用于记录当前调节栏磨皮系数
    private float mSmoothingProgress = -1.0f;
    // 用于记录当前调节栏大眼系数
    private float mEyeSizeProgress = -1.0f;
    // 用于记录当前调节栏瘦脸系数
    private float mChinSizeProgress = -1.0f;

    // 用于记录焦点位置
    private int mFocusPostion = 1;

    // TuSDK Filter Engine
    private TuSDKFilterEngine mFilterEngine;

    //贴纸底部栏
    private RecyclerView mStickerBottomView;
    private StickerListAdapter stickerListAdapter;

    // 是否是横屏
    private boolean mIsLandscape = false;

    /**
     * 准备滤镜引擎
     */
    private void prepareFilterEngine()
    {
//        if(mFilterEngine != null) return;

        mFilterEngine = new TuSDKFilterEngine(getBaseContext(), true);

        mFilterEngine.setDelegate(mFilterDelegate);
        mFilterEngine.setInterfaceOrientation(mIsLandscape ? InterfaceOrientation.LandscapeRight : InterfaceOrientation.Portrait);
        mFilterEngine.setOutputOriginalImageOrientation(false);
        mFilterEngine.setEnableLiveSticker(true);
        mFilterEngine.setEnableOutputYUVData(true);

        CameraConfigs.CameraFacing facing = (mCurrentCamFacingIndex ==  CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT.ordinal())  ?  CameraConfigs.CameraFacing.Front : CameraConfigs.CameraFacing.Back;

        updateTuSDKFilterEngineCameraInfo(facing);

    }

    /**
     * 销毁 TuSDKFilterEngine
     */
    private void destroyFilterEngine()
    {
        if (mFilterEngine != null)
        {
            mFilterEngine.onSurfaceDestroy();
            mFilterEngine.destroy();
            mFilterEngine = null;
        }
    }

    private void initTuSDK() {
        mIsFirstEntry = true;
        initFilterListView();
        initStickerListView();
        //动态加载版本初始化
        initBottomView();
    }

    public void onClick(View view) {
        switch (view.getId()){
            case R.id.lsq_beauty_btn:
                showBeautySeekBar();
                break;
            case R.id.lsq_filter_btn:
                showFilterLayout();
                break;
            case R.id.lsq_smart_beauty_btn:
                if(mFilterBottomView.getVisibility() == View.VISIBLE){
                    hideFilterStaff();
                }else {
                    hideStickerStaff();
                    showSmartBeautyLayout();
                }
                break;
            case R.id.sticker_list_btn:
                if(mStickerBottomView.getVisibility()==View.VISIBLE){
                    hideStickerStaff();
                }else {
                    hideFilterStaff();
                    showStickerLayout();
                }
                break;
        }
    }

    public void initBottomView(){

        mBeautyTab = (TuSdkTextButton) findViewById(R.id.lsq_beauty_btn);
        mBeautyLayout = (LinearLayout) findViewById(R.id.lsq_beauty_content);

        mFilterTab = (TuSdkTextButton) findViewById(R.id.lsq_filter_btn);
        mFilterLayout = (RelativeLayout) findViewById(R.id.lsq_filter_content);

        //美颜
        mSmoothingBarLayout = (ConfigViewSeekBar) mBeautyLayout.findViewById(R.id.lsq_dermabrasion_bar);
        mSmoothingBarLayout.getTitleView().setText(R.string.lsq_dermabrasion);
        mSmoothingBarLayout.getSeekbar().setDelegate(mTuSeekBarDelegate);

        mEyeSizeBarLayout = (ConfigViewSeekBar) mBeautyLayout.findViewById(R.id.lsq_big_eyes_bar);
        mEyeSizeBarLayout.getTitleView().setText(R.string.lsq_big_eyes);
        mEyeSizeBarLayout.getSeekbar().setDelegate(mTuSeekBarDelegate);

        mChinSizeBarLayout = (ConfigViewSeekBar) mBeautyLayout.findViewById(R.id.lsq_thin_face_bar);
        mChinSizeBarLayout.getTitleView().setText(R.string.lsq_thin_face);
        mChinSizeBarLayout.getSeekbar().setDelegate(mTuSeekBarDelegate);

        mFilterBottomView = findViewById(R.id.lsq_filter_group_bottom_view);
    }

    /** 拖动条监听事件 */
    private TuSeekBar.TuSeekBarDelegate mTuSeekBarDelegate = new TuSeekBar.TuSeekBarDelegate()
    {
        @Override
        public void onTuSeekBarChanged(TuSeekBar seekBar, float progress)
        {
            if (seekBar == mSmoothingBarLayout.getSeekbar())
            {
                mSmoothingProgress = progress;
                applyFilter(mSmoothingBarLayout,"smoothing",progress);
            }
            else if (seekBar == mEyeSizeBarLayout.getSeekbar())
            {
                mEyeSizeProgress = progress;
                applyFilter(mEyeSizeBarLayout,"eyeSize",progress);
            }
            else if (seekBar == mChinSizeBarLayout.getSeekbar())
            {
                mChinSizeProgress = progress;
                applyFilter(mChinSizeBarLayout,"chinSize",progress);
            }
        }
    };

    /**
     * 应用滤镜
     * @param viewSeekBar
     * @param key
     * @param progress
     */
    private void applyFilter(ConfigViewSeekBar viewSeekBar,String key,float progress)
    {
        if (viewSeekBar == null || mSelesOutInput == null) return;

        viewSeekBar.getConfigValueView().setText((int)(progress*100) + "%");
        SelesParameters params = mSelesOutInput.getFilterParameter();
        params.setFilterArg(key, progress);
        mSelesOutInput.submitFilterParameter();
    }

    // TuSDKFilterEngine 事件回调
    private TuSDKFilterEngine.TuSDKFilterEngineDelegate mFilterDelegate = new TuSDKFilterEngine.TuSDKFilterEngineDelegate()
    {
        /**
         * 滤镜更改事件，每次调用 switchFilter 切换滤镜后即触发该事件
         *
         * @param filterWrap
         *            新的滤镜对象
         */
        @Override
        public void onFilterChanged(FilterWrap filterWrap) {
            // 获取滤镜参数列表. 如果开发者希望自定义滤镜栏,可通过 ilter.getParameter().getArgs() 对象获取支持的参数列表。
            if (filterWrap == null) return;

            // 默认滤镜参数调节
            SelesParameters params = filterWrap.getFilterParameter();
            List<SelesParameters.FilterArg> list = params.getArgs();
            for (SelesParameters.FilterArg arg : list)
            {
                if (arg.equalsKey("smoothing") && mSmoothingProgress != -1.0f)
                    arg.setPrecentValue(mSmoothingProgress);
                else if (arg.equalsKey("smoothing") && mSmoothingProgress == -1.0f)
                    mSmoothingProgress = arg.getPrecentValue();
                else if (arg.equalsKey("mixied") && mMixiedProgress !=  -1.0f)
                    arg.setPrecentValue(mMixiedProgress);
                else if (arg.equalsKey("mixied") && mMixiedProgress == -1.0f)
                    mMixiedProgress = arg.getPrecentValue();
                else if (arg.equalsKey("eyeSize")&& mEyeSizeProgress != -1.0f)
                    arg.setPrecentValue(mEyeSizeProgress);
                else if (arg.equalsKey("chinSize")&& mChinSizeProgress != -1.0f)
                    arg.setPrecentValue(mChinSizeProgress);
                else if (arg.equalsKey("eyeSize") && mEyeSizeProgress == -1.0f)
                    mEyeSizeProgress = arg.getPrecentValue();
                else if (arg.equalsKey("chinSize") && mChinSizeProgress == -1.0f)
                    mChinSizeProgress = arg.getPrecentValue();
            }
            filterWrap.setFilterParameter(params);

            mSelesOutInput = filterWrap;

            if (getFilterConfigView() != null)
                getFilterConfigView().setSelesFilter(mSelesOutInput.getFilter());

            if (mIsFirstEntry || (mBeautyLayout!=null && mBeautyLayout.getVisibility() == View.VISIBLE))
            {
                mIsFirstEntry = false;
                showBeautySeekBar();
            }
        }

        @Override
        public void onPictureDataCompleted(IntBuffer intBuffer, TuSdkSize tuSdkSize) {

        }

        @Override
        public void onPreviewScreenShot(Bitmap bitmap) {

        }
    };

    private CameraConfigs.CameraFacing mFacing = CameraConfigs.CameraFacing.Front;

    /**
     * 滤镜
     */

    /**
     * 初始化滤镜栏视图
     */
    protected void initFilterListView()
    {
        getFilterListView();

        this.mFilterListView.setModeList(Arrays.asList(VIDEOFILTERS));
        ThreadHelper.postDelayed(new Runnable(){

            @Override
            public void run()
            {
                if (!mIsFirstEntry) return;

                mIsFirstEntry = false;
                changeVideoFilterCode(Arrays.asList(VIDEOFILTERS).get(mFocusPostion));
            }

        }, 1000);
    }

    /**
     * 滤镜栏视图
     *
     * @return
     */
    public FilterListView getFilterListView()
    {
        if (mFilterListView == null)
        {
            mFilterListView = (FilterListView) findViewById(R.id.lsq_filter_list_view);
            mFilterListView.loadView();
            mFilterListView.setCellLayoutId(R.layout.filter_list_cell_view);
            mFilterListView.setCellWidth(TuSdkContext.dip2px(62));
            mFilterListView.setItemClickDelegate(mFilterTableItemClickDelegate);
            mFilterListView.reloadData();
            mFilterListView.selectPosition(mFocusPostion);
        }
        return mFilterListView;
    }

    private String filterCode;

    /**
     * 切换滤镜
     * @param code
     */
    protected void changeVideoFilterCode(final String code)
    {
        if (mFilterEngine == null) return;

        filterCode = code;
        // 切换滤镜效果 code 为滤镜代号可在 lsq_tusdk_configs.json 查看
        mFilterEngine.switchFilter(filterCode);
    }

    /** 滤镜组列表点击事件 */
    private TuSdkTableView.TuSdkTableViewItemClickDelegate<String, FilterCellView> mFilterTableItemClickDelegate = new TuSdkTableView.TuSdkTableViewItemClickDelegate<String, FilterCellView>()
    {
        @Override
        public void onTableViewItemClick(String itemData,
                                         FilterCellView itemView, int position)
        {
            onFilterGroupSelected(itemData, itemView, position);
        }
    };

    /**
     * 滤镜组选择事件
     *
     * @param itemData
     * @param itemView
     * @param position
     */
    protected void onFilterGroupSelected(String itemData,
                                         FilterCellView itemView, int position)
    {
        FilterCellView prevCellView = (FilterCellView) mFilterListView.findViewWithTag(mFocusPostion);
        mFocusPostion = position;
        changeVideoFilterCode(itemData);
        mFilterListView.selectPosition(mFocusPostion);
        deSelectLastFilter(prevCellView);
        selectFilter(itemView, position);
        getFilterConfigView().setVisibility((position == 0)?View.GONE:View.VISIBLE);
    }

    /**
     * 取消上一个滤镜的选中状态
     *
     * @param lastFilter
     */
    private void deSelectLastFilter(FilterCellView lastFilter)
    {
        if (lastFilter == null) return;

        updateFilterBorderView(lastFilter,true);
        lastFilter.getTitleView().setBackground(TuSdkContext.getDrawable(R.drawable.tusdk_view_filter_unselected_text_roundcorner));
        lastFilter.getImageView().invalidate();
    }

    /**
     * 设置滤镜单元边框是否可见
     * @param lastFilter
     * @param isHidden
     */
    private void updateFilterBorderView(FilterCellView lastFilter, boolean isHidden)
    {
        View filterBorderView = lastFilter.getBorderView();
        filterBorderView.setVisibility(isHidden ? View.GONE : View.VISIBLE);
    }

    /**
     * 滤镜选中状态
     *
     * @param itemView
     * @param position
     */
    private void selectFilter(FilterCellView itemView, int position)
    {
        updateFilterBorderView(itemView, false);
        itemView.setFlag(position);
        TextView titleView = itemView.getTitleView();
        titleView.setBackground(TuSdkContext.getDrawable(R.drawable.tusdk_view_filter_selected_text_roundcorner));
    }

    /**
     * 滤镜配置视图
     *
     * @return
     */
    private FilterConfigView getFilterConfigView()
    {
        if (mConfigView == null)
        {
            mConfigView = (FilterConfigView) findViewById(R.id.lsq_filter_config_view);
        }

        return mConfigView;
    }

    /**
     * 显示智能美颜底部栏
     */
    public void showSmartBeautyLayout()
    {
        updateFilterViewStaff(true);

        // 滤镜栏向上动画并显示
        ViewCompat.setTranslationY(mFilterBottomView,
                mFilterBottomView.getHeight());
        ViewCompat.animate(mFilterBottomView).translationY(0).setDuration(200).setListener(mViewPropertyAnimatorListener);
        showBeautySeekBar();
    }

    /**
     * 隐藏滤镜栏
     */
    public void hideFilterStaff()
    {
        if(mFilterBottomView.getVisibility() == View.GONE) return;

        updateFilterViewStaff(false);

        // 滤镜栏向下动画并隐藏
        ViewCompat.animate(mFilterBottomView)
                .translationY(mFilterBottomView.getHeight()).setDuration(200);
    }

    /**
     * 更新滤镜栏相关视图的显示状态
     *
     * @param isShow
     */
    private void updateFilterViewStaff(boolean isShow)
    {
        mFilterBottomView.setVisibility(isShow? View.VISIBLE: View.GONE);
    }

    /** 显示美颜调节栏 */
    private void showBeautySeekBar()
    {
        if (mIsFirstEntry)
        {
            changeVideoFilterCode(Arrays.asList(VIDEOFILTERS).get(mFocusPostion));
        }

        if (mBeautyLayout == null || mFilterLayout == null)
            return;

        mBeautyLayout.setVisibility(View.VISIBLE);
        mFilterLayout.setVisibility(View.GONE);
        updateSmartBeautyTab(mBeautyTab,true);
        updateSmartBeautyTab(mFilterTab,false);

        if (mSelesOutInput == null)
        {
            setEnableAllSeekBar(false);
            return;
        }

        // 滤镜参数
        SelesParameters params = mSelesOutInput.getFilterParameter();
        if (params == null)
        {
            setEnableAllSeekBar(false);
            return;
        }

        List<SelesParameters.FilterArg> list = params.getArgs();
        if (list == null || list.size() == 0)
        {
            setEnableAllSeekBar(false);
            return;
        }

        for(SelesParameters.FilterArg arg : list)
        {
            if (arg.equalsKey("smoothing"))
            {
                setEnableSeekBar(mSmoothingBarLayout,true,arg.getPrecentValue(),
                        R.drawable.tusdk_view_widget_seekbar_drag);
            }
            else if (arg.equalsKey("eyeSize"))
            {
                setEnableSeekBar(mEyeSizeBarLayout,true,arg.getPrecentValue(),
                        R.drawable.tusdk_view_widget_seekbar_drag);
            }
            else if (arg.equalsKey("chinSize"))
            {
                setEnableSeekBar(mChinSizeBarLayout,true,arg.getPrecentValue(),
                        R.drawable.tusdk_view_widget_seekbar_drag);
            }
        }
    }

    /** 显示滤镜列表 */
    private void showFilterLayout()
    {
        if (mBeautyLayout == null || mFilterLayout == null)
            return;

        mFilterLayout.setVisibility(View.VISIBLE);
        mBeautyLayout.setVisibility(View.GONE);
        updateSmartBeautyTab(mBeautyTab,false);
        updateSmartBeautyTab(mFilterTab,true);

        if (mFocusPostion>0 && getFilterConfigView() != null && mSelesOutInput != null)
        {
            getFilterConfigView().post(new Runnable()
            {

                @Override
                public void run() {
                    getFilterConfigView().setSelesFilter(mSelesOutInput.getFilter());
                    getFilterConfigView().setVisibility(View.VISIBLE);
                }});

            getFilterConfigView().setSeekBarDelegate(mConfigSeekBarDelegate);
            getFilterConfigView().invalidate();
        }
    }

    /** 滤镜拖动条监听事件 */
    private FilterConfigView.FilterConfigViewSeekBarDelegate mConfigSeekBarDelegate = new FilterConfigView.FilterConfigViewSeekBarDelegate()
    {

        @Override
        public void onSeekbarDataChanged(FilterConfigSeekbar seekbar, SelesParameters.FilterArg arg)
        {
            if (arg == null) return;

            if (arg.equalsKey("smoothing"))
                mSmoothingProgress = arg.getPrecentValue();
            else if (arg.equalsKey("eyeSize"))
                mEyeSizeProgress = arg.getPrecentValue();
            else if (arg.equalsKey("chinSize"))
                mChinSizeProgress = arg.getPrecentValue();
            else if (arg.equalsKey("mixied"))
                mMixiedProgress = arg.getPrecentValue();
        }

    };

    /**
     * 更新美颜滤镜Tab
     * @param button
     * @param clickable
     */
    private void updateSmartBeautyTab(TuSdkTextButton button, boolean clickable)
    {
        int imgId = 0, colorId = 0;

        switch (button.getId())
        {
            case R.id.lsq_filter_btn:
                imgId = clickable? R.drawable.lsq_style_default_btn_filter_selected
                        : R.drawable.lsq_style_default_btn_filter_unselected;
                colorId = clickable? R.color.lsq_filter_title_color : R.color.lsq_filter_title_default_color;
                break;
            case R.id.lsq_beauty_btn:
                imgId = clickable? R.drawable.lsq_style_default_btn_beauty_selected
                        : R.drawable.lsq_style_default_btn_beauty_unselected;
                colorId = clickable? R.color.lsq_filter_title_color : R.color.lsq_filter_title_default_color;
                break;
        }

        button.setCompoundDrawables(null, TuSdkContext.getDrawable(imgId), null, null);
        button.setTextColor(TuSdkContext.getColor(colorId));
    }

    private void setEnableAllSeekBar(boolean enable)
    {
        setEnableSeekBar(mSmoothingBarLayout,enable,0,R.drawable.tusdk_view_widget_seekbar_none_drag);
        setEnableSeekBar(mEyeSizeBarLayout,enable,0,R.drawable.tusdk_view_widget_seekbar_none_drag);
        setEnableSeekBar(mChinSizeBarLayout,enable,0,R.drawable.tusdk_view_widget_seekbar_none_drag);
    }

    /** 设置调节栏是否有效 */
    private void setEnableSeekBar(ConfigViewSeekBar viewSeekBar,boolean enable,float progress,int id)
    {
        if (viewSeekBar == null) return;

        viewSeekBar.setProgress(progress);
        viewSeekBar.getSeekbar().setEnabled(enable);
        viewSeekBar.getSeekbar().getDragView().setBackgroundResource(id);
    }

    /**
     * 更新贴纸栏相关视图的显示状态
     *
     * @param isShow
     */
    private void updateStickerViewStaff(boolean isShow)
    {
        mStickerBottomView.setVisibility(isShow? View.VISIBLE: View.GONE);
    }

    /**
     * 初始化贴纸组视图
     */
    protected void initStickerListView()
    {
        mStickerBottomView = (RecyclerView) findViewById(R.id.lsq_sticker_list_view);
        mStickerBottomView.setVisibility(View.GONE);

        stickerListAdapter = new StickerListAdapter();
        GridLayoutManager manager = new GridLayoutManager(this,5);
        mStickerBottomView.setLayoutManager(manager);
        mStickerBottomView.setAdapter(stickerListAdapter);

        List<StickerGroup> stickerGroups;
        // 获取打包贴纸资源
//        stickerGroups = StickerLocalPackage.shared().getSmartStickerGroups();
        // 获取在线贴纸配置
        stickerGroups = getRawStickGroupList();

        stickerGroups.add(0,new StickerGroup());
        stickerListAdapter.setStickerList(stickerGroups);

        stickerListAdapter.setOnItemClickListener(new StickerListAdapter.OnItemClickListener() {

            @Override
            public void onClickItem(StickerGroup itemData, StickerListAdapter.StickerHolder
                    stickerHolder, int position) {
                onStickerGroupSelected(itemData,stickerHolder,position);
            }
        });
    }

    /**
     * 获取本地贴纸列表
     * @return
     */
    public List<StickerGroup> getRawStickGroupList()
    {
        List<StickerGroup> list = new ArrayList<StickerGroup>();
        try {
            InputStream stream = getResources().openRawResource(R.raw.square_sticker);
//            if (!isSquareSticker)
//                stream = getResources().openRawResource(R.raw.full_screen_sticker);

            if (stream == null) return null;

            byte buffer[] = new byte[stream.available()];
            stream.read(buffer);
            String json = new String(buffer, "UTF-8");

            JSONObject jsonObject = JsonHelper.json(json);
            JSONArray jsonArray = jsonObject.getJSONArray("stickerGroups");

            for(int i = 0; i < jsonArray.length();i++)
            {
                JSONObject item = jsonArray.getJSONObject(i);
                StickerGroup group = new StickerGroup();
                group.groupId = item.optLong("id");
                group.previewName = item.optString("previewImage");
                group.name = item.optString("name");
                list.add(group);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 贴纸组选择事件
     *
     * @param itemData
     * @param stickerHolder
     * @param position
     */
    protected void onStickerGroupSelected(StickerGroup itemData, StickerListAdapter.StickerHolder
            stickerHolder, int position)
    {
        // 设置点击贴纸时呈现或是隐藏贴纸
        if (position == 0)
        {
            mFilterEngine.removeAllLiveSticker();
            stickerListAdapter.setSelectedPosition(position);
            return;
        }

        // 如果贴纸已被下载到本地
        if (stickerListAdapter.isDownloaded(itemData))
        {
            stickerListAdapter.setSelectedPosition(position);
            // 必须重新获取StickerGroup,否则itemData.stickers为null
            itemData = StickerLocalPackage.shared().getStickerGroup(itemData.groupId);
            mFilterEngine.showGroupSticker(itemData);
        }else
        {
            stickerListAdapter.downloadStickerGroup(itemData,stickerHolder);
        }
    }

    /**
     * 隐藏贴纸栏
     */
    public void hideStickerStaff()
    {
        if(mStickerBottomView.getVisibility() == View.GONE) return;

        updateStickerViewStaff(false);

        // 滤镜栏向下动画并隐藏
        ViewCompat.animate(mStickerBottomView)
                .translationY(mStickerBottomView.getHeight()).setDuration(200);
    }

    /**
     * 显示贴纸底部栏
     */
    public void showStickerLayout()
    {
        updateStickerViewStaff(true);

        // 滤镜栏向上动画并显示
        ViewCompat.setTranslationY(mStickerBottomView,
                mStickerBottomView.getHeight());
        ViewCompat.animate(mStickerBottomView).translationY(0).setDuration(200).setListener(mViewPropertyAnimatorListener);
    }

    /** 属性动画监听事件 */
    private ViewPropertyAnimatorListener mViewPropertyAnimatorListener = new ViewPropertyAnimatorListener()
    {

        @Override
        public void onAnimationCancel(View view)
        {

        }

        @Override
        public void onAnimationEnd(View view)
        {
            ViewCompat.animate(mStickerBottomView).setListener(null);
            ViewCompat.animate(mFilterBottomView).setListener(null);
            mStickerBottomView.clearAnimation();
            mFilterBottomView.clearAnimation();
        }

        @Override
        public void onAnimationStart(View view)
        {

        }
    };
}
