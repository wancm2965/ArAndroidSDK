package org.ar.ar_android_tutorial_1to1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
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
import org.json.JSONObject;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.tencent.bugly.crashreport.CrashReport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class VideoChatViewActivity extends AppCompatActivity {
    private static final String TAG = VideoChatViewActivity.class.getSimpleName();

    private static final int PERMISSION_REQ_ID = 22;
    private String userId = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
    private String CHANNEL_NAME = "909090";
    private String APPID = "";
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private RtcEngine mRtcEngine;
    private boolean mCallEnd;
    private boolean mMuted;
    private boolean mVideoMuted;
    private boolean mApplyLine;
    private RelativeLayout rl_video;

    private ImageView mCallBtn;
    private ImageView mMuteAudioBtn;
    private ImageView mMuteVideoBtn;
    private ImageView mSwitchCameraBtn;
    private ImageView mApplyLineBtn;
    private LoggerRecyclerView mLogView;

    private ARVideoGroup arVideoGroup;


    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, final String uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("加入房间成功啦~ 你的ID是：" + (uid ));
                    if (mApplyLineBtn!=null) {
                        mApplyLineBtn.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        @Override
        public void onUserJoined(String uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("欢迎小伙伴" + (uid)+"加入房间~");
                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(final String uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("小伙伴" + (uid)+"的画面来了~");
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onUserOffline(final String uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("小伙伴" + (uid)+"离开了房间~");
                    removeRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onRemoteVideoStateChanged(String uid, int state, int reason, int elapsed) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed);
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
        if (arVideoGroup.getM_list_video().size()<=3){
            TextureView mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
            arVideoGroup.addView(uid,mRemoteView,true);
            mRtcEngine.setRemoteVideoStreamType(uid,0);
            mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, Constants.RENDER_MODE_HIDDEN,CHANNEL_NAME, uid,Constants.VIDEO_MIRROR_MODE_ENABLED));
        }else {
            TextureView mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
            arVideoGroup.addView(uid,mRemoteView,false);
            mRtcEngine.setRemoteVideoStreamType(uid,1);
            mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, Constants.RENDER_MODE_HIDDEN,CHANNEL_NAME, uid,Constants.VIDEO_MIRROR_MODE_ENABLED));
            updateRemoteVideoMode(false);
        }
    }

    private void removeRemoteVideo(String uid) {
        arVideoGroup.removeView(uid);
        if (arVideoGroup.getM_list_video().size()<=4){
            updateRemoteVideoMode(true);
        }else {
            updateRemoteVideoMode(false);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        mMuteAudioBtn = findViewById(R.id.btn_mute);
        mSwitchCameraBtn = findViewById(R.id.btn_switch_camera);
        mMuteVideoBtn = findViewById(R.id.btn_video_mute);
        mLogView = findViewById(R.id.log_recycler_view);
        mApplyLineBtn = findViewById(R.id.btn_apply);
        arVideoGroup = new ARVideoGroup(this,rl_video);
        showSampleLogs();

    }

    private void showSampleLogs() {
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
        joinChannel();
    }

    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), APPID, mRtcEventHandler);
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
        arVideoGroup.addView("local",mLocalView,true);
        mRtcEngine.setupLocalVideo(new org.ar.rtc.VideoCanvas(mLocalView, Constants.RENDER_MODE_HIDDEN,CHANNEL_NAME, userId,Constants.VIDEO_MIRROR_MODE_AUTO));
        mRtcEngine.startPreview();
    }

    private void removeLocal(){
        arVideoGroup.removeView("local");
    }

    private void joinChannel() {
        mRtcEngine.enableDualStreamMode(true);
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
        mRtcEngine.joinChannel("", CHANNEL_NAME, "Extra Optional Data",userId );
        mRtcEngine.setEnableSpeakerphone(true);
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
        if (mRtcEngine!=null) {
            mRtcEngine.leaveChannel();
        }
    }

    public void onLocalAudioMuteClicked(View view) {
        mMuted = !mMuted;
        mRtcEngine.muteLocalAudioStream(mMuted);
        int res = mMuted ? R.drawable.btn_mute : R.drawable.btn_unmute;
        mMuteAudioBtn.setImageResource(res);
    }

    public void onLocalVideoMuteClicked(View view){
        mVideoMuted =!mVideoMuted;
        mRtcEngine.muteLocalVideoStream(mVideoMuted);
        int res = mVideoMuted ? R.drawable.btn_mute_video : R.drawable.btn_unmute_video;
        mMuteVideoBtn.setImageResource(res);
    }

    public void onApplyBtnClicked(View view){
        mApplyLine =!mApplyLine;
        mRtcEngine.setClientRole(mApplyLine ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE);
        int res = mApplyLine ? R.drawable.unapply : R.drawable.apply;
        mApplyLineBtn.setImageResource(res);
        if (mApplyLine){
            setupVideoConfig();
            setupLocalVideo();
            mMuteAudioBtn.setVisibility(View.VISIBLE);
            mSwitchCameraBtn.setVisibility(View.VISIBLE);
            mMuteVideoBtn.setVisibility(View.VISIBLE);
        }else {
            removeLocal();
            mMuteAudioBtn.setVisibility(View.INVISIBLE);
            mSwitchCameraBtn.setVisibility(View.INVISIBLE);
            mMuteVideoBtn.setVisibility(View.INVISIBLE);
            mRtcEngine.muteLocalVideoStream(false);
            mRtcEngine.muteLocalAudioStream(false);
            mMuteVideoBtn.setImageResource(R.drawable.btn_unmute_video);
            mMuteAudioBtn.setImageResource(R.drawable.btn_unmute);
            mApplyLine=false;
            mMuted =false;
            mVideoMuted =false;
        }
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

    }

    private void startCall() {
        joinChannel();
    }

    private void endCall() {
        arVideoGroup.removeAllView();
        leaveChannel();
        mApplyLineBtn.setImageResource(R.drawable.apply);
        mApplyLine=false;
        mMuted =false;
        mVideoMuted =false;
        mRtcEngine.muteLocalVideoStream(false);
        mRtcEngine.muteLocalAudioStream(false);
        mMuteVideoBtn.setImageResource(R.drawable.btn_unmute_video);
        mMuteAudioBtn.setImageResource(R.drawable.btn_unmute);
        mMuteAudioBtn.setVisibility(View.INVISIBLE);
        mSwitchCameraBtn.setVisibility(View.INVISIBLE);
        mApplyLineBtn.setVisibility(View.INVISIBLE);
        mMuteVideoBtn.setVisibility(View.INVISIBLE);
    }



    //更新大小流
    public void updateRemoteVideoMode(boolean isBigStream){
        Iterator<Map.Entry<String, ARVideoGroup.VideoView>> iter = arVideoGroup.getM_list_video().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ARVideoGroup.VideoView> entry = iter.next();
            ARVideoGroup.VideoView render = entry.getValue();
            if (!render.videoId.equals("local")) {
                if (render.isBigStream != isBigStream) {
                    render.isBigStream = isBigStream;
                    mRtcEngine.setRemoteVideoStreamType(render.videoId, isBigStream ? 0 : 1);
                    Log.d("大小流","设置"+render.videoId+"为"+(isBigStream ?"大流":"小流"));
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK){
            RtcEngine.destroy();
            System.exit(0);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
