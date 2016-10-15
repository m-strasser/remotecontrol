package com.example.mstrasser.remotecontrol;

import android.os.HandlerThread;
import android.os.Message;

import com.jcraft.jsch.JSchException;

/**
 * Created by mstrasser on 10/9/16.
 */

public class SSHThread extends HandlerThread {
    private static SSHThread instance = null;
    private SSHHandler handler;
    private String user;
    private String host;
    private String password;
    private int port;
    private MainActivity.MainHandler mh;

    public static SSHThread getInstance() {
        if(SSHThread.instance == null)
            SSHThread.instance = new SSHThread("SSHThread");

        return SSHThread.instance;
    }

    private SSHThread(String name) {
        super(name);
    }

    public void prepareHandler(String user, String host, String password, int port, MainActivity.MainHandler mh) {
        this.handler = new SSHHandler(getLooper(), user, host, password, port, mh);
    }

    public void prepareHandler(String knownHosts, String user, String host, String password, int port,
                               MainActivity.MainHandler mh) throws JSchException {
        this.prepareHandler(user, host, password, port, mh);
        this.handler.setKnownHosts(knownHosts);
    }

    public void postMessage(Message m) {
        this.handler.sendMessage(m);
    }

    public SSHHandler getHandler() {
        return this.handler;
    }
}
