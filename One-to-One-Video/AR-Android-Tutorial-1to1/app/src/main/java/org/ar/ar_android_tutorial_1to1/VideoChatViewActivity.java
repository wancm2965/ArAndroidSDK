package org.ar.ar_android_tutorial_1to1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import org.ar.ar_android_tutorial_1to1.R;
import org.ar.rtc.Constants;
import org.ar.rtc.IRtcEngineEventHandler;
import org.ar.rtc.RtcEngine;
import org.ar.rtc.VideoCanvas;
import org.ar.rtc.VideoEncoderConfiguration;
import org.ar.uikit.logger.LoggerRecyclerView;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;


public class VideoChatViewActivity extends AppCompatActivity {
    private static final String TAG = VideoChatViewActivity.class.getSimpleName();

    private static final int PERMISSION_REQ_ID = 22;
    private String userId = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
    private String channelName = "909090";
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private RtcEngine mRtcEngine;
    private boolean mCallEnd;
    private boolean mMuted;

    private RelativeLayout rl_video;

    private ImageView mCallBtn;
    private ImageView mMuteBtn;
    private ImageView mSwitchCameraBtn;
    private LoggerRecyclerView mLogView;

    private ARVideoGroup arVideoGroup;


    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, final String uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("Join channel success, uid: " + (uid ));
                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(final String uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("First remote video decoded, uid: " + (uid ));
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onUserOffline(final String uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("User offline, uid: " + (uid));
                    removeRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onWarning(int warn) {
            super.onWarning(warn);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("onWarning " + (warn));
                }
            });
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("onError " + (err));
                }
            });
        }
    };

    private void setupRemoteVideo(String uid) {

       TextureView mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
        arVideoGroup.addView(uid,mRemoteView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, Constants.RENDER_MODE_FIT,channelName, uid,Constants.VIDEO_MIRROR_MODE_ENABLED));
    }

    private void removeRemoteVideo(String uid) {
        arVideoGroup.removeView(uid);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat_view);
        initUI();

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            initEngineAndJoinChannel();
        }
    }

    private void initUI() {
        rl_video= findViewById(R.id.rl_video);
        mCallBtn = findViewById(R.id.btn_call);
        mMuteBtn = findViewById(R.id.btn_mute);
        mSwitchCameraBtn = findViewById(R.id.btn_switch_camera);

        mLogView = findViewById(R.id.log_recycler_view);

        arVideoGroup = new ARVideoGroup(this,rl_video);
        showSampleLogs();

    }

    private void showSampleLogs() {
        mLogView.logI("Welcome to AR 1v1 video call");
        mLogView.logW("You will see custom logs here");
        mLogView.logE("You can also use this to show errors");
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                showLongToast("Need permissions " + Manifest.permission.RECORD_AUDIO +
                        "/" + Manifest.permission.CAMERA + "/" + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                finish();
                return;
            }

            initEngineAndJoinChannel();
        }
    }

    private void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initEngineAndJoinChannel() {
        initializeEngine();
        setupVideoConfig();
        setupLocalVideo();
        joinChannel();
    }

    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupVideoConfig() {
        mRtcEngine.enableVideo();
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideo() {
        TextureView mLocalView = RtcEngine.CreateRendererView(getBaseContext());
        arVideoGroup.addView("local",mLocalView);
        mRtcEngine.setupLocalVideo(new org.ar.rtc.VideoCanvas(mLocalView, Constants.RENDER_MODE_HIDDEN,channelName, userId,Constants.VIDEO_MIRROR_MODE_AUTO));
        mRtcEngine.startPreview();
    }

    private void joinChannel() {
        String token = getString(R.string.access_token);
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "#YOUR ACCESS TOKEN#")) {
            token = null; // default, no token
        }

        mRtcEngine.joinChannel("", channelName, "Extra Optional Data",userId );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mCallEnd) {
            leaveChannel();
        }
        RtcEngine.destroy();
    }

    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    public void onLocalAudioMuteClicked(View view) {
        mMuted = !mMuted;
        mRtcEngine.muteLocalAudioStream(mMuted);
        int res = mMuted ? R.drawable.btn_mute : R.drawable.btn_unmute;
        mMuteBtn.setImageResource(res);
    }

    public void onSwitchCameraClicked(View view) {
        mRtcEngine.switchCamera();
    }

    public void onCallClicked(View view) {
        if (mCallEnd) {
            startCall();
            mCallEnd = false;
            mCallBtn.setImageResource(R.drawable.img_hang_up);
        } else {
            endCall();
            mCallEnd = true;
            mCallBtn.setImageResource(R.drawable.img_start);
        }

        showButtons(!mCallEnd);
    }

    private void startCall() {
        setupLocalVideo();
        joinChannel();
    }

    private void endCall() {
        arVideoGroup.removeAllView();
        leaveChannel();
    }


    private void showButtons(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        mMuteBtn.setVisibility(visibility);
        mSwitchCameraBtn.setVisibility(visibility);
    }
}
