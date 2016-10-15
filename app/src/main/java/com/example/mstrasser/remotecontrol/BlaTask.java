package com.example.mstrasser.remotecontrol;

import android.os.AsyncTask;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mstrasser on 10/8/16.
 */

class BlaTask extends AsyncTask<String, Void, Integer> {
    @Override
    protected Integer doInBackground(String... params) {
        int count = params.length;
        Session session = null;
        Channel channel = null;
        InputStream in = null;
        int exitStatus = -1;

        JSch jsch = new JSch();
        try {
            jsch.setKnownHosts(".known_hosts");
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session = jsch.getSession("phone", "10.0.2.2", 22);
            session.setPassword("?~r8{Yn!G!s(6.O!");
            session.setConfig(config);
            session.connect(30000);

            channel = session.openChannel("exec");

            for(int i=0; i<count; i++) {
                ((ChannelExec) channel).setCommand(params[i]);
                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);

                in = channel.getInputStream();
                channel.connect();

                byte[] tmp=new byte[1024];
                while(true) {
                    while(in.available()>0){
                        int j=in.read(tmp, 0, 1024);
                        if(j<0)break;
                        System.out.print(new String(tmp, 0, j));
                    }

                    if(channel.isClosed()){
                        if(in.available()>0) continue;
                        exitStatus = channel.getExitStatus();
                        break;
                    }

                    try{Thread.sleep(1000);}catch(Exception ee){}
                }

                channel.disconnect();
                session.disconnect();
            }
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(channel != null && channel.isConnected()) channel.disconnect();
            if(session != null && session.isConnected()) session.disconnect();
        }

        return exitStatus;
    }
    @Override
    protected void onPostExecute(Integer result) {
    }
}
