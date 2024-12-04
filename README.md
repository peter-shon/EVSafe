This is a DEMO project of AOSP automotive (with Chank0228)

STEP 1 (✅ COMPLETE)
EVSafe App Service
*  Gear Status Check and Display : P/R/N/D
*  EV Battery level Display : (CURRENT_BATTERY_LEVEL / MAX_BATTERY_LEVEL) * 100 (%)
*  Current Speed Display
*  Available Distance Display
   * BASE_RANGE = 450 (km)
   * drive_factor
      * 1 if speed< 80
      * 0.7 if speed >80 and speed < 100
      * 0.5 if speed > 100
   * distance = (CURRENT_BATTERY_LEVEL / MAX_BATTERY_LEVEL) * drive_factor * BASE_RANGE (km)
* path: aosp/packages/apps/EVSafe

STEP 2 (🔥 WORKING ON)
EVSafe System Service
*  Disable Game App on Gear D
   *  1) Gear Status Check on System Service
   *  2) Disable Game App
*  EV Battery Level Check on System Service
   *  1) Input Battery Level
   *  2) EV Battery level Check on System Service

STEP 3 (TBU)
EVSafe System Service + System Alarm
*  Disable Game App on Gear D With Toast Message
*  Low Battery Notification
