package com.example.helloagorademo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class VideoCallActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_ID = 22;
    private RtcEngine mRtcEngine;
    private RelativeLayout mRemoteContainer;
    private SurfaceView mRemoteView;
    private FrameLayout mLocalContainer;
    private SurfaceView mLocalView;
    private ImageView mMuteBtn;
    private boolean isCalling = true;
    private boolean isMuted = false;

    private IRtcEngineEventHandler mRtcEngineEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoCallActivity.this, "Join channel successfully!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onRemoteVideoStateChanged(final int uid, int state, int reason, int elapsed) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed);
            if (state == Constants.REMOTE_VIDEO_STATE_STARTING) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setUpRemoteView(uid);
                    }
                });
            }
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    removeRemoteView();
                }
            });
        }
    };

    // Ask for Android device permissions at runtime.
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);
        initUI();

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)) {
            initEngineAndJoinChannel();
        }
    }

    private void initUI() {
        mLocalContainer = findViewById(R.id.local_video_view_container);
        mRemoteContainer = findViewById(R.id.remote_video_view_container);
        mMuteBtn = findViewById(R.id.btn_mute);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQ_ID: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    break;
                }
                initEngineAndJoinChannel();
                break;
            }
        }
    }

    private void initEngineAndJoinChannel() {
        initializeEngine();
        setUpLocalView();
        joinChannel();
    }

    private void removeRemoteView() {
        if (mRemoteView != null) {
            mRemoteContainer.removeView(mRemoteView);
        }
        mRemoteView = null;
    }

    private void setUpRemoteView(int uid) {
        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
        mRemoteContainer.addView(mRemoteView);
        VideoCanvas remoteVideoCanvas = new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid);
        mRtcEngine.setupRemoteVideo(remoteVideoCanvas);
    }

    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEngineEventHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpLocalView() {
        mRtcEngine.enableVideo();
        mLocalView = RtcEngine.CreateRendererView(getBaseContext());
        mLocalContainer.addView(mLocalView);
        mLocalView.setZOrderMediaOverlay(true);

        VideoCanvas localVideoCanvas = new VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0);
        mRtcEngine.setupLocalVideo(localVideoCanvas);
    }

    private void joinChannel() {
        mRtcEngine.joinChannel(null, "test", "", 0);
    }

    public void onCallClicked(View view) {
        if (isCalling) {
            isCalling = false;
            removeRemoteView();
            removeLocalView();
            leaveChannel();
        }else {
            isCalling = true;
            setUpLocalView();
            joinChannel();
        }
    }

    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    private void removeLocalView() {
        if (mLocalView != null) {
            mLocalContainer.removeView(mLocalView);
        }
        mLocalView = null;
    }

    public void onSwitchCameraClicked(View view) {
        mRtcEngine.switchCamera();
    }

    public void onLocalAudioMuteClicked(View view) {
        isMuted = !isMuted;
        mRtcEngine.muteLocalAudioStream(isMuted);
        mMuteBtn.setImageResource(isMuted ? R.drawable.btn_mute : R.drawable.btn_unmute);
    }
}
