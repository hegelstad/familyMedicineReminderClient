package com.example.sondrehj.familymedicinereminderclient.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.sondrehj.familymedicinereminderclient.api.MyCyFAPPServiceAPI;
import com.example.sondrehj.familymedicinereminderclient.api.RestService;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.network.NetworkEventProvider;
import com.path.android.jobqueue.network.NetworkUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import retrofit2.Call;

/**
 * Created by Eirik on 30.04.2016.
 */
public class ServerStatusChangeReceiver extends BroadcastReceiver implements NetworkUtil, NetworkEventProvider {

    public NetworkEventProvider.Listener listener;
    private static Boolean previousServerStatus;
    private static Boolean currentServerStatus;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            PollingTask poll = new PollingTask();
            Future pollingFuture = Executors.newFixedThreadPool(1).submit(poll);
            while (!pollingFuture.isDone()) {
                System.out.println("chill");
                Thread.sleep(1000);
            }
            Boolean pollingResult = (Boolean) pollingFuture.get();

            currentServerStatus = ServiceManager.isNetworkAvailable(context) && pollingResult;
        }
        catch (Exception e) {
            System.out.println("Exception " + e.toString());
            currentServerStatus = false;
        }

        if(currentServerStatus != previousServerStatus) {
            System.out.println("HELLO WORLD");
            Configuration configuration = new Configuration.Builder(context)
                    .networkUtil(this)
                    .build();
            JobManager jobManager = new JobManager(context, configuration);

            listener.onNetworkChange(currentServerStatus);
        }
        System.out.println("Previous server status: " + previousServerStatus);
        System.out.println("Current server status: " + currentServerStatus);
        previousServerStatus = currentServerStatus;
    }

    @Override
    public boolean isConnected(Context context) {
        return currentServerStatus;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private static class PollingTask implements Callable {
        @Override
        public Boolean call() throws Exception {
            MyCyFAPPServiceAPI api = RestService.createRestService();
            Call<Void> call = api.sendPollingRequest();
            Boolean result = call.clone().execute().isSuccessful();
            System.out.println("Poll result: " + result);
            return result;
        }
    }
}
