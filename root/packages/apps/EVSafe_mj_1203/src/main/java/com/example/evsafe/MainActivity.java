package com.example.evsafe;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.content.Context;
import android.widget.TextView;
import android.util.Log;
import android.content.Intent;
import android.content.IntentFilter;
//11.30 기어 관련 코드 추가: mj
import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.VehicleGear;
import android.car.hardware.property.CarPropertyManager;

public class MainActivity extends Activity {
    // 로그 관련 변수 선언
    private static final String TAG = "EVSafe";
    private static final int UPDATE_INTERVAL_MS = 1000;
    // 기어 관련 변수 선언
    private Car car;
    private CarPropertyManager propertyManager;
    private TextView gearStatusTextView;
    private TextView batteryStatusTextView;
    private TextView speedStatusTextView;
    private TextView distanceStatusTextView;

    // Update handler
    private Handler handler;
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateVehicleStatus();
            handler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);        
        // 로그 출력으로 앱 초기화 확인
        Log.d(TAG, "MainActivity onCreate called");
        handler = new Handler(Looper.getMainLooper());
        // 배터리 상태를 표시할 TextView 연결
        batteryStatusTextView = findViewById(R.id.battery_status);
        // 기어 상태를 표시할 TextView 연결
        gearStatusTextView = findViewById(R.id.gear_status);
        // 속도 상태를 표시할 TextView 연결                                     
        speedStatusTextView = findViewById(R.id.speed_status);
        distanceStatusTextView = findViewById(R.id.distance_status);
        // Car API 초기화
        initializeCarApi();        
    }

    // car api 초기화 메소드
    private void initializeCarApi() {
        car = Car.createCar(this);
        propertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);

        if (propertyManager == null) {
            Log.e(TAG, "CarSensorManager is not available. Sensor features will not work.");
            return;
        }
    }

    private void updateVehicleStatus() {
        if (propertyManager == null) {
            Log.e(TAG, "PropertyManager is null");
            return;
        }    
        updateGearStatus();
        updateSpeedStatus();
        updateBatteryStatus();
        updateDistanceStatus();
    }
    
    private void displayBatteryLevel(String battertValue) {
        String formattedBatteryStatus = getString(R.string.battery_status_format, battertValue);
        batteryStatusTextView.setText(formattedBatteryStatus);
    }

    private void displayGearStatus(String gearValue) {
        String formattedText = getString(R.string.gear_status_format, gearValue);
        gearStatusTextView.setText(formattedText);
    }
    
    private void displaySpeedStatus(String speedValue) {
        String formattedText = getString(R.string.speed_status_format, speedValue);
        speedStatusTextView.setText(formattedText);
    }

    private void updateGearStatus() {        
        int gear = propertyManager.getIntProperty(
            VehiclePropertyIds.GEAR_SELECTION, 
            0
        );
        String gearName = getGearStatusText(gear);
        displayGearStatus(gearName);        
    }
    
    private void updateSpeedStatus() {
        float speedMs = propertyManager.getFloatProperty(
            VehiclePropertyIds.PERF_VEHICLE_SPEED,
            0
        );
        float speedKmh = convertMsToKmh(speedMs);
        Log.d(TAG, "Speed(m/s): " + speedMs + " -> Speed(km/h): " + speedKmh);
        displaySpeedStatus(String.format("%.2f km/h", speedKmh));
    }
    
    private void updateBatteryStatus() {        
        float batteryLevel = propertyManager.getFloatProperty(
            VehiclePropertyIds.EV_BATTERY_LEVEL,
            0
        );
        float batteryCapacity = propertyManager.getFloatProperty(
            VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY,
            0
        );
        
        float batteryPercentage = calculateBatteryPercentage(batteryLevel, batteryCapacity);
        Log.d(TAG, "Battery Level: " + batteryLevel + 
                " Capacity: " + batteryCapacity + 
                " Percentage: " + batteryPercentage + "%");
        displayBatteryLevel(String.format("%.2f%%", batteryPercentage));
        updateDistanceStatus(batteryPercentage); // 배터리 퍼센티지 기반으로 주행거리 업데이트 호출
    }

    // 주행 가능 거리 상태 업데이트 함수
    private void updateDistanceStatus(float batteryPercentage) {
        final float MAX_RANGE = 500.0f; // 100%일 때 최대 주행거리 (km)
        float estimatedRange = calculateEstimatedRange(batteryPercentage);
        Log.d(TAG, "Estimated Range: " + estimatedRange + " km");
        displayDistanceStatus(String.format("주행 가능 거리: %.2f km", estimatedRange));
    }

    // 가져온 기어 데이터를 텍스트로 변환 메소드
    private String getGearStatusText(int gearValue) {
        switch (gearValue) {
            case 0:
                return "N";
            case 1:
                return "D";
            case 2:
                return "R";
            case 3:
                return "P";
            default:
                return "Unknown";
        }
    }

    private float convertMsToKmh(float speedMs) {
        return speedMs * 3.6f; // m/s * (3600/1000) = km/h
    }

    // 주행거리 계산
    private float calculateBatteryPercentage(float batteryLevel, float batteryCapacity) {
        if (batteryCapacity <= 0) {
            Log.w(TAG, "Invalid battery capacity: " + batteryCapacity);
            return 0.0f;
        }
        return (batteryLevel / batteryCapacity) * 100.0f;
    }

    private float calculateEstimatedRange(float batteryPercentage) {
        final float MAX_RANGE = 500.0f; // 100%일 때 최대 주행거리 (km)
        return (batteryPercentage / 100.0f) * MAX_RANGE;
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        if (car != null) {
            car.disconnect();
        }
        super.onDestroy();
    }
}