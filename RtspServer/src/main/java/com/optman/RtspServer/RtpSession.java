package com.optman.RtspServer;

import com.optman.rtp.sender.RtpSocket;

import java.net.InetAddress;

public class RtpSession {
    InetAddress host;
    int         port;
    RtpSocket   tcpSocket;
}
