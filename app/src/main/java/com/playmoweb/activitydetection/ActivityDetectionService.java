package com.playmoweb.activitydetection;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;

import java.util.ArrayList;
import java.util.List;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;

/**
 * ActivityDetectionService
 *
 * @author Playmoweb
 */
public class ActivityDetectionService extends Service {

    public static final String TAG = ActivityDetectionService.class.getSimpleName();

    private List<DatedActivity> datedActivityList = new ArrayList<>();

    private final IBinder mBinder = new LocalBinder();

    private Subscription subscription;

    private Observable<DatedActivity> observable;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, ActivityDetectionService.class);
    }

    public void startForeground() {
        Notification notification = new Notification();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        startForeground(1000, notification);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();

        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(this);
        observable = locationProvider.getDetectedActivity(0)
                .map(new Func1<ActivityRecognitionResult, DatedActivity>() {
                    @Override
                    public DatedActivity call(ActivityRecognitionResult activityRecognitionResult) {
                        return new DatedActivity(activityRecognitionResult.getMostProbableActivity());
                    }
                });

        subscription = observable
                .subscribe(new Subscriber<DatedActivity>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, e.getMessage());
                    }

                    @Override
                    public void onNext(DatedActivity datedActivity) {
                        Log.d(TAG, "Activity detected : " + datedActivity.getType() + " " + datedActivity.getConfidence() + "%");
                        datedActivityList.add(datedActivity);
                    }
                });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    /**
     * Get observable
     *
     * @return rx.Observable<com.playmoweb.activitydetection.DatedActivity> observable
     */
    public Observable<DatedActivity> getObservable() {
        return observable.startWith(datedActivityList);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        ActivityDetectionService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ActivityDetectionService.this;
        }
    }
}
