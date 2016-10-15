package com.example.mstrasser.remotecontrol;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {
    private SSHThread ssht = null;
    private class SendCommandTask extends AsyncTask<String, Integer, Integer> {
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
        protected void onProgressUpdate(Integer... progress) {
            if(progress[0] == 1)
                connEstablished();
        }
        @Override
        protected void onPostExecute(Integer result) {
            updateResult(result);
        }
    }

    public class MainHandler extends Handler {
        // Context needs to be handled differently
        private Context context;

        public MainHandler(Context context) {
            super();
            this.context = context;
        }

        @Override
        public void handleMessage(Message message) {
            switch(message.what) {
                case 0:
                    Intent i = new Intent(this.context, DisplayCommandOverview.class);
                    startActivity(i);
                    break;
            }
        }
    }

    private void connEstablished(){

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final MainHandler mh = new MainHandler(this);

        final Button connBtn = (Button) findViewById(R.id.connectButton);
        connBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText hostname = (EditText) findViewById(R.id.hostField);
                final EditText username = (EditText) findViewById(R.id.username);
                final EditText password = (EditText) findViewById(R.id.passwordField);
                final TextView replyTV = (TextView) findViewById(R.id.resultTV) ;

                System.out.println(hostname.getText().toString());
                ssht = SSHThread.getInstance();

                ssht.start();
                try {
                    ssht.prepareHandler(".known_hosts",
                            username.getText().toString(),
                            hostname.getText().toString(),
                            password.getText().toString(),
                            22,
                            mh);
                    ssht.postMessage(Message.obtain(ssht.getHandler(), 0, replyTV));
                } catch (JSchException e) {
                    e.printStackTrace();
                    ssht.quit();
                }
            }
        });
    }

    protected void updateResult(int exitCode) {
        final TextView resultTV = (TextView) findViewById(R.id.resultTV);

        if(exitCode > 0) {
            resultTV.setText("FAILED! Exit Code: " + exitCode);
            ssht.quit();
        } else if(exitCode == 0) {
            resultTV.setText("Successfully executed command.");
        } else {
            resultTV.setText("FAILED! An internal error occurred.");
            ssht.quit();
        }
    }
}
