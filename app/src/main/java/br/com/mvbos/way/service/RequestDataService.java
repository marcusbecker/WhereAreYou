package br.com.mvbos.way.service;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by Marcus Becker on 18/08/2016.
 */
public class RequestDataService extends IntentService {

    public RequestDataService() {
        super(RequestDataService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }
}
