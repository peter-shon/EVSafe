package com.example.evsafe;

import android.app.Activity;
import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.VehicleGear;
import android.car.hardware.property.CarPropertyManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {
    // Constants
    private static final String TAG = "EVSafe";
    private static final int UPDATE_INTERVAL_MS = 1000;

    // Car related members
    private Car car;
    private CarPropertyManager propertyManager;
    
    // UI elements
    private TextView gearText;
    private TextView batteryText;
    private TextView rangeText;
    private TextView speedText;

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
        
        handler = new Handler(Looper.getMainLooper());
        initViews();
        initCar();
    }

    private void initViews() {
        gearText = findViewById(R.id.gear_status);
        batteryText = findViewById(R.id.battery_status);
        rangeText = findViewById(R.id.range_status);
        speedText = findViewById(R.id.speed_status);        
    }

    private void initCar() {
        try {
            car = Car.createCar(this);
            propertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create car manager", e);
        }
    }

    private void updateVehicleStatus() {
        if (propertyManager == null) {
            Log.e(TAG, "PropertyManager is null");
            return;
        }    
        updateGearStatus();
        updateBatterySpeedStatus();
    }
    
    private void updateGearStatus() {
        try {
            int gear = propertyManager.getIntProperty(
                VehiclePropertyIds.GEAR_SELECTION, 
                0
            );
            gearText.setText("Gear: " + getGearString(gear));
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for gear reading", e);
            gearText.setText("Gear: No permission");
        } catch (Exception e) {
            Log.e(TAG, "Error reading gear", e);
            gearText.setText("Gear: Error");
        }
    }
    
    private void updateBatterySpeedStatus() {        
        try {
            float speedMs = propertyManager.getFloatProperty(
                VehiclePropertyIds.PERF_VEHICLE_SPEED,
                0
            );
            float batteryLevel = propertyManager.getFloatProperty(
                VehiclePropertyIds.EV_BATTERY_LEVEL,
                0
            );
            float batteryCapacity = propertyManager.getFloatProperty(
                VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY,
                0
            );              
            float speedKmh = convertMsToKmh(speedMs);
            float batteryPercentage = calculateBatteryPercentage(batteryLevel, batteryCapacity);
            float estimatedRange = calculateEstimatedRange(batteryPercentage, speedKmh); // 주행거리 계산
            Log.d(TAG, "Battery Level: " + batteryLevel + 
                  " Capacity: " + batteryCapacity + 
                  " Percentage: " + batteryPercentage + "%"+
                  "Speed(m/s): " + speedMs + " -> Speed(km/h): " + speedKmh +
                  "Estimated Range: " + estimatedRange + " km");            
            batteryText.setText(String.format("Battery: %.0f%%", batteryPercentage));
            speedText.setText(String.format("Speed: %.1f km/h", speedKmh));
            rangeText.setText(String.format("Range: %.0f km", estimatedRange));
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for battery or speed reading", e);
            batteryText.setText("Battery : No permission");
            speedText.setText("Speed: No permission");
        } catch (Exception e) {
            Log.e(TAG, "Error reading battery or speed", e);
            batteryText.setText("Battery: Error");
            speedText.setText("Speed: Error");
        }
    }

    private String getGearString(int gear) {
        if (gear == VehicleGear.GEAR_PARK) return "P";
        if (gear == VehicleGear.GEAR_REVERSE) return "R";
        if (gear == VehicleGear.GEAR_NEUTRAL) return "N";
        if (gear == VehicleGear.GEAR_DRIVE) return "D";
        return "Unknown";
    }

    private float convertMsToKmh(float speedMs) {
        return speedMs * 3.6f; // m/s * (3600/1000) = km/h
    }

    private float calculateBatteryPercentage(float batteryLevel, float batteryCapacity) {
        if (batteryCapacity <= 0) {
            Log.w(TAG, "Invalid battery capacity: " + batteryCapacity);
            return 0.0f;
        }
        return (batteryLevel / batteryCapacity) * 100.0f;
    }

    // 배터리와 속도에 따른 주행 가능 거리 계산 함수
    private float calculateEstimatedRange(float batteryPercentage, float currentSpeed) {
        final float MAX_RANGE = 450.0f; // 100%일 때 최대 주행거리 (km)
        float speedFactor = calculateSpeedFactor(currentSpeed); // 속도에 따른 가중치 계산
        return (batteryPercentage / 100.0f) * MAX_RANGE * speedFactor;
    }

    // 속도에 따른 가중치 계산 함수
    private float calculateSpeedFactor(float speed) {
        if (speed < 80) {
            return 1.0f;
        } else if (speed <= 100) {
            return 0.7f;
        } else {
            return 0.5f;
        }
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