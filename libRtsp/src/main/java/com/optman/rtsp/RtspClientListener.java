package com.optman.rtsp;

public interface RtspClientListener {
    public void onReady(RtspClient client);

    public void onRtpPacket(byte[] data, int dataSize);
}
