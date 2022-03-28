package com.example.nanotank;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import java.util.Calendar;


public class MyJobScheduler extends JobService {
    private static final String TAG = "MainActivity";
    private boolean jobCancelled = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");
        doBackgroundWork(params);
        return true;
    }
    private void doBackgroundWork(final JobParameters params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Background work");
                Calendar cal = Calendar.getInstance();
                int time = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);

                Log.e(TAG,"Done Scheduling " +time+":"+minute);
                //handler.obtainMessage(NOTIFICATION, "Done Scheduling " +time+":"+minute).sendToTarget();

                if (jobCancelled) {
                    return;
                }
                //jobFinished(params, true);
            }
        }).start();
    }
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job cancelled before completion");
        jobCancelled = true;
        return true;
    }
}