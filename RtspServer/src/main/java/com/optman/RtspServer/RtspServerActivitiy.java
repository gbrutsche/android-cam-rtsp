package com.optman.RtspServer;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.TextView;

import com.optman.rtp.player.Statistics;
import com.optman.rtp.receiver.UdpServer;
import com.optman.rtsp.TcpReceiver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class RtspServerActivitiy extends Activity implements Callback, StreamInfo {
    private MediaPlayer mediaPlayer;
    private RtpSessionManager videoSessions = new RtpSessionManager();
    private RtpSessionManager audioSessions = new RtpSessionManager();

    private com.optman.rtp.receiver.RtpAvcStream videoReceiverStream;
    private com.optman.rtp.sender.RtpAvcStream   videoSenderStream;

    private com.optman.rtp.receiver.RtpAacStream audioReceiverStream;
    private com.optman.rtp.sender.RtpAacStream   audioSenderStream;

    private List<byte[]> spspps = new ArrayList<>();

    private Handler    statsHandler = new Handler();
    private Statistics st           = new Statistics();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtsp_server);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        SurfaceView   surfaceView   = findViewById(R.id.surfaceview);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        spspps.add(null);
        spspps.add(null);

        startStatsTimer();

        RtspServer svr = new RtspServer(getApplicationContext(), 9554, videoSessions, audioSessions, this);
        svr.start();

        startRtp();

        //(new Thread(this::playMovie)).start();
    }

    private void startRtp() {
        videoSenderStream = new com.optman.rtp.sender.RtpAvcStream(videoSessions);

        videoReceiverStream = new com.optman.rtp.receiver.RtpAvcStream(sample -> {
            //long timeUs = System.nanoTime()/1000;
            long timeUs = sample.timestampUs;

            byte naluType = (byte) (sample.data[4] & 0x1f);
            if (naluType == 7 || naluType == 8) {
                byte[] data = new byte[sample.data.length - 4];
                System.arraycopy(sample.data, 4, data, 0, data.length);

                spspps.set(naluType - 7, data);
            }

            try {
                videoSenderStream.addNalu(sample.data, 4, sample.data.length - 4, timeUs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, st);

        UdpServer videoReceiver = new UdpServer(/*50000*/59526, videoReceiverStream);
        try {
            videoReceiver.open();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        audioSenderStream = new com.optman.rtp.sender.RtpAacStream(44100, audioSessions);
        audioReceiverStream = new com.optman.rtp.receiver.RtpAacStream(44100, sample -> {
            try {
                audioSenderStream.addAU(sample.data, 0, sample.data.length, sample.timestampUs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, st);

        UdpServer audioReceiver = new UdpServer(40272, audioReceiverStream);


        try {
            audioReceiver.open();
        } catch (SocketException e) {
            e.printStackTrace();
        }


        //tcp receiver

        try {
            final ServerSocket server = new ServerSocket(8001);

            (new Thread(() -> {
                while (true) {
                    try {
                        Socket s = server.accept();

                        TcpReceiver client = new TcpReceiver(s);

                        client.run((channel, data, dataSize) -> {
                            if (channel == 0)
                                videoReceiverStream.onRtp(data, dataSize);
                            else if (channel == 1)
                                audioReceiverStream.onRtp(data, dataSize);

                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            })).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playMovie() {
        //String url = "rtsp://192.168.199.99:8554/";
        String url = "rtsp://127.0.0.1:9554/";
        //String url = "http://192.168.199.99/C%2b%2b/VisualStudioEmulatorforAndroid_high.mp4";
        mediaPlayer = new MediaPlayer();

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.reset();
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare(); // might take long! (for buffering, etc)
            mediaPlayer.start();
        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mediaPlayer == null)
            return;

        mediaPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public List<byte[]> getSpsPps() {
        return spspps;
    }

    private void startStatsTimer() {
        statsHandler.postDelayed(() -> {
            showStats();
            startStatsTimer();
        }, 1000);
    }

    private void showStats() {
        TextView textView = findViewById(R.id.infoText);

        String s = " v:\r\n" + videoSessions.dump() + "\r\n" +
                " a:\r\n " + audioSessions.dump() +
                " \r\n" +
                " v " + st.videoFrameCount + " a " + st.audioFrameCount + " drop " + st.transportDropPacketCount +
                " sync " + st.syncframeCount;

        textView.setText(s);
    }
}