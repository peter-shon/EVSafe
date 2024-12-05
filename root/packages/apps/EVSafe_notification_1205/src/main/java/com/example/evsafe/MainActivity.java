package com.example.evsafe;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager; //Toast 기능 위한 추가: mj
import android.app.Notification; // notification 기능 추가: mj
import android.app.NotificationChannel; // notification 기능 추가: mj
import android.app.NotificationManager; // notification 기능 추가: mj
import android.app.PendingIntent; // notification 기능 추가: mj
import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.VehicleGear;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context; //Toast 기능 위한 추가: mj
import android.content.Intent; // notification 기능 추가: mj
import android.content.pm.ApplicationInfo; //Toast 기능 위한 추가: mj
import android.content.pm.PackageManager; //Toast 기능 위한 추가: mj
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast; //Toast 기능 위한 추가: mj

public class MainActivity extends Activity {
    // Constants
    private static final String TAG = "EVSafe";
    private static final int UPDATE_INTERVAL_MS = 1000;
    private static final String CHANNEL_ID = "speed_warning_channel";
    private static final int NOTIFICATION_ID = 1;

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
        createNotificationChannel(); // Notification 채널 초기화: mj
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
        checkAndToastNonSystemApps();
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
            speedText.setText(String.format("Speed: %.0f km/h", speedKmh));
            rangeText.setText(String.format("Range: %.0f km", estimatedRange));
            // 속도 초과 시 알림 전송
            if (speedKmh > 150) {
                try {
                    sendSpeedWarningNotification(); // 속도 초과 알림
                } catch (Exception e) {
                    Log.e(TAG, "Error sending speed warning notification", e);
                    // 알림 오류 발생 시에도 UI 업데이트는 계속 진행
                }
            }
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

    //앱 체크 및 메시지 토스트 기능 추가: mj
    private void checkAndToastNonSystemApps() {
    try {
        int gear = propertyManager.getIntProperty(VehiclePropertyIds.GEAR_SELECTION, 0);

        if (gear != VehicleGear.GEAR_PARK) {
            // 기어 변경 조건 수정하기, 속도0 D, R이면 알림 
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            PackageManager packageManager = getPackageManager();

            if (activityManager != null) {
                List<ActivityManager.RunningAppProcessInfo> runningApps = activityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo app : runningApps) {
                    try {
                        ApplicationInfo appInfo = packageManager.getApplicationInfo(app.processName, 0);
                        
                        // 시스템 앱이 아닌 경우 처리
                        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                            // 시스템 앱이 아닌 경우 Toast 메시지 표시 및 앱 종료
                            Toast.makeText(this, "주행 중에는 게임 앱을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "앱 정보 로드 실패: " + app.processName, e);
                    }
                }
            }
        }
    } catch (SecurityException e) {
        Log.e(TAG, "Permission denied for gear reading", e);
    } catch (Exception e) {
        Log.e(TAG, "Error checking gear and non-system apps", e);
    }
    }
    // 속도 150km 이상시 notification "속도가 너무 빠릅니다" 최종구현 앱

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

    // 속도에 따른 가중치 계산 함수 : mj
    private float calculateSpeedFactor(float speed) {
        if (speed < 80) {
            return 1.0f;
        } else if (speed <= 100) {
            return 0.7f;
        } else {
            return 0.5f;
        }
    }

    // notification 기능 추가: mj
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Speed Warning";
            String description = "Notifies when the vehicle speed exceeds 150 km/h.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // notification 기능 추가: mj
    private void sendSpeedWarningNotification() {
        try{
            Intent intent = new Intent(this, MainActivity.class); // 클릭 시 메인 액티비티로 이동
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
             PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); // Android 12 이상 호환(Immutable)

            Notification.Builder builder = new Notification.Builder(this) //channel id 삭제
                    .setSmallIcon(R.drawable.ic_warning) // 알림 아이콘
                    .setContentTitle("과속 경고")
                    .setContentText("속도가 너무 빠릅니다! 과속 주의!")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true); // 알림 클릭 시 자동 삭제

            // Android 8.0 이상에서는 채널 ID가 필요
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID);
            }

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send notification", e);
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