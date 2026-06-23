package com.example.finalapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import com.example.finalapp.utils.NetworkUtil;

public class NetworkReceiver extends BroadcastReceiver {
    
    public interface NetworkChangeListener {
        void onNetworkChanged(boolean isConnected);
    }

    private NetworkChangeListener listener;

    public NetworkReceiver(NetworkChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean isConnected = NetworkUtil.isNetworkAvailable(context);
            if (listener != null) {
                listener.onNetworkChanged(isConnected);
            }
        }
    }
}
