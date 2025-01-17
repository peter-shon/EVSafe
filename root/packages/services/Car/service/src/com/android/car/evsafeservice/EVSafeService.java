package com.android.car.evsafeservice;


import android.car.evsafeservice.IEVSafeServiceManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;


import android.app.Service;
import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.hardware.property.CarPropertyManager;


public class EVSafeService extends Service {
    private final static String LOG_TAG = "EVSafeService";
 
    private final Object mLock = new Object();
 
    private final Context mContext;
    private Car car;
    private CarPropertyManager propertyManager;
    
    public EVSafeService(Context context) {
        mContext = context;
    }   
    
    private void initCar() {
        if (car == null) {
            car = Car.createCar(mContext);
            propertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Slog.i(LOG_TAG, "EVSafeService created");
        initCar();
    }

    private final IEVSafeServiceManager.Stub binder = new IEVSafeServiceManager.Stub() {
        public int getBatteryPercentage() {
            synchronized (mLock) {
                Slog.i(LOG_TAG, "Get Battery Percentage");
    
                if (propertyManager == null) {
                    initCar();
                }
    
                try {
                    float batteryLevel = propertyManager.getFloatProperty(
                        VehiclePropertyIds.EV_BATTERY_LEVEL,
                        0
                    );
                    float batteryCapacity = propertyManager.getFloatProperty(
                        VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY,
                        0
                    );

                    if (batteryCapacity <= 0) {
                        Slog.e(LOG_TAG, "Invalid battery capacity: " + batteryCapacity);
                        return 0.0f;
                    }

                    return (batteryLevel / batteryCapacity) * 100.0f;
                } catch (Exception e) {
                    Slog.e(LOG_TAG, "Error while getting battery percentage", e);
                    return 0.0f;
                }
            }
        }        
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }    
    
}
