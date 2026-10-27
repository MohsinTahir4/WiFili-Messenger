package com.example.lanchat.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("deprecation")
public class LanCallManager implements Camera.PreviewCallback {

    public static final int AUDIO_PORT = 45457;
    public static final int VIDEO_PORT = 45458;

    private static LanCallManager instance;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String destIp;
    private InetAddress destInetAddress;
    private volatile boolean isCallActive = false;
    private volatile boolean isMuted = false;
    private volatile boolean isSpeakerOn = false;
    private volatile boolean isCameraOn = true;

    private DatagramSocket audioSendSocket;
    private DatagramSocket audioRecvSocket;
    private DatagramSocket videoSendSocket;
    private DatagramSocket videoRecvSocket;

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Camera camera;
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    public interface VideoFrameListener {
        void onRemoteFrameReceived(Bitmap bitmap);
    }

    public interface CallStateListener {
        void onConnectionStatusChanged(String status);
    }

    private VideoFrameListener videoFrameListener;
    private CallStateListener callStateListener;

    public static synchronized LanCallManager getInstance() {
        if (instance == null) {
            instance = new LanCallManager();
        }
        return instance;
    }

    public void setVideoFrameListener(VideoFrameListener listener) {
        this.videoFrameListener = listener;
    }

    public void setCallStateListener(CallStateListener listener) {
        this.callStateListener = listener;
    }

    private void notifyStatus(String status) {
        if (callStateListener != null) {
            mainHandler.post(() -> {
                if (callStateListener != null) {
                    callStateListener.onConnectionStatusChanged(status);
                }
            });
        }
    }

    public void startCall(Context context, String destIp, boolean isVideoCall, boolean isCaller, TextureView localPreviewView) {
        this.destIp = destIp;
        try {
            this.destInetAddress = InetAddress.getByName(destIp);
        } catch (Exception ignored) {}
        this.isCallActive = true;
        this.isMuted = false;
        this.isSpeakerOn = false;
        this.isCameraOn = true;

        enableSpeakerphone(context, false);
        startAudioEngine();
        if (isVideoCall) {
            startVideoEngine(localPreviewView);
        }
    }

    @SuppressLint("MissingPermission")
    private void startAudioEngine() {
        pool.execute(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            int sampleRate = 16000;
            int channelIn = AudioFormat.CHANNEL_IN_MONO;
            int channelOut = AudioFormat.CHANNEL_OUT_MONO;
            int encoding = AudioFormat.ENCODING_PCM_16BIT;

            int minRecSize = AudioRecord.getMinBufferSize(sampleRate, channelIn, encoding);
            int minTrackSize = AudioTrack.getMinBufferSize(sampleRate, channelOut, encoding);
            int pcmChunkSize = 640;

            try {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelIn, encoding, Math.max(minRecSize, pcmChunkSize * 4));
                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    notifyStatus("Connection Failed (Mic Error)");
                    return;
                }

                audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelOut, encoding, Math.max(minTrackSize, pcmChunkSize * 4), AudioTrack.MODE_STREAM);
                if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                    notifyStatus("Connection Failed (Audio Error)");
                    return;
                }

                audioRecord.startRecording();
                audioTrack.play();

                try {
                    audioSendSocket = new DatagramSocket();
                    audioRecvSocket = new DatagramSocket(null);
                    audioRecvSocket.setReuseAddress(true);
                    audioRecvSocket.bind(new InetSocketAddress(AUDIO_PORT));
                } catch (Exception e) {
                    if (audioRecvSocket == null || !audioRecvSocket.isBound()) {
                        audioRecvSocket = new DatagramSocket(AUDIO_PORT);
                    }
                }

                notifyStatus("Connected • 00:00");

                // UDP Audio Sender Thread
                pool.execute(() -> {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                    byte[] buffer = new byte[pcmChunkSize];
                    while (isCallActive) {
                        try {
                            int read = audioRecord.read(buffer, 0, buffer.length);
                            if (read > 0 && !isMuted && destInetAddress != null && audioSendSocket != null) {
                                DatagramPacket packet = new DatagramPacket(buffer, read, destInetAddress, AUDIO_PORT);
                                audioSendSocket.send(packet);
                            } else if (read == AudioRecord.ERROR_DEAD_OBJECT) {
                                try {
                                    audioRecord.stop();
                                    audioRecord.startRecording();
                                } catch (Exception ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }
                });

                // UDP Audio Receiver Thread
                pool.execute(() -> {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                    byte[] recvBuf = new byte[2048];
                    while (isCallActive) {
                        try {
                            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                            audioRecvSocket.receive(packet);
                            if (packet.getLength() > 0 && audioTrack != null) {
                                audioTrack.write(packet.getData(), 0, packet.getLength());
                            }
                        } catch (Exception e) {
                            if (isCallActive) {
                                notifyStatus("Reconnecting...");
                            }
                        }
                    }
                });

            } catch (Exception e) {
                notifyStatus("Connection Failed (Audio Error)");
            }
        });
    }

    private void startVideoEngine(TextureView localPreviewView) {
        pool.execute(() -> {
            try {
                videoSendSocket = new DatagramSocket();
                videoRecvSocket = new DatagramSocket(null);
                videoRecvSocket.setReuseAddress(true);
                videoRecvSocket.bind(new InetSocketAddress(VIDEO_PORT));
            } catch (Exception e) {
                try {
                    if (videoRecvSocket == null || !videoRecvSocket.isBound()) {
                        videoRecvSocket = new DatagramSocket(VIDEO_PORT);
                    }
                } catch (Exception ignored) {}
            }

            // UDP Video Receiver Thread
            pool.execute(() -> {
                byte[] recvBuf = new byte[65535];
                while (isCallActive) {
                    try {
                        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                        videoRecvSocket.receive(packet);
                        int len = packet.getLength();
                        if (len > 4) {
                            byte[] data = packet.getData();
                            int senderCamId = ByteBuffer.wrap(data, 0, 4).getInt();
                            int jpegLen = len - 4;
                            Bitmap bmp = BitmapFactory.decodeByteArray(data, 4, jpegLen);
                            if (bmp != null && videoFrameListener != null) {
                                Matrix matrix = new Matrix();
                                if (senderCamId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                    matrix.postRotate(270);
                                } else {
                                    matrix.postRotate(90);
                                }
                                final Bitmap rotatedBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                                mainHandler.post(() -> {
                                    if (videoFrameListener != null) {
                                        videoFrameListener.onRemoteFrameReceived(rotatedBmp);
                                    }
                                });
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });

            // Setup Camera Preview
            if (localPreviewView != null) {
                mainHandler.post(() -> setupCamera(localPreviewView));
            }
        });
    }

    private void setupCamera(TextureView localPreviewView) {
        try {
            if (camera != null) {
                camera.stopPreview();
                camera.release();
            }
            camera = Camera.open(cameraId);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(320, 240);
            camera.setParameters(parameters);

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + 360) % 360;
                result = (360 - result) % 360;
            } else {
                result = (info.orientation + 360) % 360;
            }
            camera.setDisplayOrientation(result);

            camera.setPreviewTexture(localPreviewView.getSurfaceTexture());
            camera.setPreviewCallback(this);
            camera.startPreview();
        } catch (Exception ignored) {}
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!isCallActive || !isCameraOn || data == null || destInetAddress == null || videoSendSocket == null) return;
        pool.execute(() -> {
            try {
                Camera.Parameters parameters = camera.getParameters();
                int width = parameters.getPreviewSize().width;
                int height = parameters.getPreviewSize().height;

                YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 35, out);
                byte[] jpegData = out.toByteArray();

                byte[] packetData = new byte[4 + jpegData.length];
                ByteBuffer.wrap(packetData).putInt(cameraId);
                System.arraycopy(jpegData, 0, packetData, 4, jpegData.length);

                DatagramPacket packet = new DatagramPacket(packetData, packetData.length, destInetAddress, VIDEO_PORT);
                videoSendSocket.send(packet);
            } catch (Exception ignored) {}
        });
    }

    public void toggleMute() {
        isMuted = !isMuted;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void toggleSpeaker(Context context) {
        isSpeakerOn = !isSpeakerOn;
        enableSpeakerphone(context, isSpeakerOn);
    }

    public boolean isSpeakerOn() {
        return isSpeakerOn;
    }

    private void enableSpeakerphone(Context context, boolean enable) {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                am.setSpeakerphoneOn(enable);
            }
        } catch (Exception ignored) {}
    }

    public void toggleCamera(TextureView localPreviewView) {
        isCameraOn = !isCameraOn;
        if (!isCameraOn && camera != null) {
            camera.stopPreview();
        } else if (isCameraOn && localPreviewView != null) {
            setupCamera(localPreviewView);
        }
    }

    public void switchCamera(TextureView localPreviewView) {
        cameraId = (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) ?
                Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        if (isCameraOn && localPreviewView != null) {
            setupCamera(localPreviewView);
        }
    }

    public void stopCall(Context context) {
        isCallActive = false;
        if (context != null) {
            try {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    am.setSpeakerphoneOn(false);
                    am.setMode(AudioManager.MODE_NORMAL);
                }
            } catch (Exception ignored) {}
        }
        if (audioRecord != null) {
            try { audioRecord.stop(); audioRecord.release(); } catch (Exception ignored) {}
            audioRecord = null;
        }
        if (audioTrack != null) {
            try { audioTrack.stop(); audioTrack.release(); } catch (Exception ignored) {}
            audioTrack = null;
        }
        if (camera != null) {
            try { camera.setPreviewCallback(null); camera.stopPreview(); camera.release(); } catch (Exception ignored) {}
            camera = null;
        }
        try { if (audioSendSocket != null) audioSendSocket.close(); } catch (Exception ignored) {}
        try { if (audioRecvSocket != null) audioRecvSocket.close(); } catch (Exception ignored) {}
        try { if (videoSendSocket != null) videoSendSocket.close(); } catch (Exception ignored) {}
        try { if (videoRecvSocket != null) videoRecvSocket.close(); } catch (Exception ignored) {}
        audioSendSocket = null;
        audioRecvSocket = null;
        videoSendSocket = null;
        videoRecvSocket = null;
        videoFrameListener = null;
        callStateListener = null;
    }
}
