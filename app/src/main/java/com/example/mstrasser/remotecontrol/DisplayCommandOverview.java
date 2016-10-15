package com.example.mstrasser.remotecontrol;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class DisplayCommandOverview extends AppCompatActivity {
    private SSHThread ssht;

    public void shutdown(View view) {
        ssht = SSHThread.getInstance();
        // COMMANDS NEED AN ENUM
        ssht.postMessage(Message.obtain(ssht.getHandler(), 1, 1, -1, findViewById(R.id.reply)));
    }

    public void openInBrowser(View view) {
        ssht = SSHThread.getInstance();
        Message m = Message.obtain(ssht.getHandler(), 1, 1, -1, findViewById(R.id.reply));
        Bundle b = new Bundle();
        EditText url = (EditText) findViewById(R.id.url);

        b.putString("url", String.valueOf(url.getText()));
        m.setData(b);

        ssht.postMessage(m);
    }

    private void getURLFromClipboard() {
        // STRICT REGEX CHECKING!
        // ATTACK VECTOR!
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        EditText url = (EditText) findViewById(R.id.url);
        String pasted = "", defaultURL="https://www.google.com";

        if(clipboard.hasPrimaryClip() &&
                clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            pasted = (String) item.getText();

            if(pasted != null && pasted != "") {
                url.setText(pasted);
            } else {
                url.setText(defaultURL);
            }
        } else {
            url.setText(defaultURL);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_command_overview);
        this.getURLFromClipboard();
    }

    @Override
    public void onResume() {
        super.onResume();

        this.getURLFromClipboard();
    }
}
