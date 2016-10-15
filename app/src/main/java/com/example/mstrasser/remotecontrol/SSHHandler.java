package com.example.mstrasser.remotecontrol;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by mstrasser on 10/9/16.
 */

public class SSHHandler extends Handler {
    private JSch jsch = null;
    private Session session = null;
    private Channel channel = null;
    private InputStream in = null;
    private int lastExitStatus = -1;

    private boolean isConnected = false;

    private String host, user, password;
    private int port = -1;
    private java.util.Properties config;

    private MainActivity.MainHandler mh;

    @Deprecated
    public SSHHandler(Looper looper, String user, String host, String password){
        this(looper, user, host, password, 22, null);
    }

    public SSHHandler(Looper looper, String user, String host, String password, int port,
                      MainActivity.MainHandler mh) {
        super(looper);
        this.jsch = new JSch();
        this.host = host;
        this.user = user;
        this.password = password;
        this.port = port;
        this.mh = mh;
    }

    public void handleMessage(Message msg) {
        String cmd;

        switch(msg.what) {
            case 0:
                if(this.establishConnection()) {
                    reply(msg.obj, "Successfully connected to SSH server.");
                    this.mh.sendMessage(Message.obtain(this.mh, 0));
                } else {
                    reply(msg.obj, "Error connecting to SSH server!");
                }

                break;
            case 1:
                cmd = this.getCommand(msg);
                if(cmd != null) this.execCommand(cmd);
                if(lastExitStatus > -1) {
                    reply(msg.obj, "Successfully executed " + cmd);
                } else {
                    reply(msg.obj, "Error executing " + cmd + "!");
                }

                break;
            case 2:
                cmd = this.getCommand(msg);
                if(cmd != null) this.execInX(cmd);
                if(lastExitStatus > -1) {
                    reply(msg.obj, "Successfully executed " + cmd);
                } else {
                    reply(msg.obj, "Error executing " + cmd + "!");
                }

                break;
            default:
                reply(msg.obj, "Invalid command!");
                break;
        }
    }

    public void setKnownHosts(String path) throws JSchException {
        jsch.setKnownHosts(path);
        config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
    }

    private void reply(Object obj, final String msg) {
        final TextView replyTV = (TextView)obj;

        replyTV.post(new Runnable() {
            public void run() {
                replyTV.setText(msg);
            }
        });
    }

    private String getCommand(Message m) {
        Bundle b = m.getData();

        switch(m.arg1) {
            case 0:
                return "echo hi >> ~/hi.txt";
            case 1:
                String url = b.getString("url");

                if(url != null)
                    return "export DISPLAY=localhost:0 && chromium " + url;
                else
                    return null;
            default:
                return null;
        }
    }

    private boolean establishConnection(){
        try {
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            if(config != null) session.setConfig(config);
            session.connect(30000);
            isConnected = true;

            return isConnected;
        } catch (JSchException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void execInX(String command) {
        if(!this.isConnected) return;

        try {
            Channel channel = session.openChannel("shell");
            channel.setXForwarding(true);

            channel.setInputStream(null);
            channel.setOutputStream(System.out);

            OutputStreamWriter out = new OutputStreamWriter(channel.getOutputStream());
            in = channel.getInputStream();

            channel.connect();
            out.write("export DISPLAY=localhost:0");
            out.write(command);
            out.flush();

            byte[] tmp = new byte[1024];
            while(true) {
                while(in.available()>0) {
                    int i = in.read(tmp, 0, 1024);
                    if(i < 0) break;

                    // DO SOMETHING WITH RESPONSE
                    System.out.println(new String(tmp, 0, i));
                }

                if(channel.isClosed()) {
                    if(in.available() > 0) continue;
                    lastExitStatus = channel.getExitStatus();

                    break;
                }

                try{ Thread.sleep(1000); } catch(Exception ee){}
            }

            channel.disconnect();
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void execCommand(String command) {
        if(!this.isConnected) return;

        try {
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            channel.setXForwarding(true);
            ((ChannelExec) channel).setErrStream(System.err);

            in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            while(true) {
                while(in.available()>0) {
                    int i = in.read(tmp, 0, 1024);
                    if(i < 0) break;

                    // DO SOMETHING WITH RESPONSE
                    System.out.println(new String(tmp, 0, i));
                }

                if(channel.isClosed()) {
                    if(in.available() > 0) continue;
                    lastExitStatus = channel.getExitStatus();

                    break;
                }

                try{ Thread.sleep(1000); } catch(Exception ee){}
            }

            channel.disconnect();
        } catch (JSchException e) {
            e.printStackTrace();
            lastExitStatus = -1;
        } catch (IOException e) {
            e.printStackTrace();
            lastExitStatus = -1;
        } finally {
            if(channel != null && channel.isConnected()) channel.disconnect();
        }
    }
}
