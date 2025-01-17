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

STEP 2 (✅ COMPLETE)
EVSafe System Service
*  Notification on system
*  path: aosp/packages/apps/Car/EVSafeService

STEP 3 (🔥 WORKING ON) 
EVSafe Service on layer of CarService 
*  Adding a function at CarPropertyService (permission error)
   * calling a battery percentage like battery level, capacity
   * The goal is to process the battery percentage calculation within the CarService layer.
     * registeration ID of batterty percentage in VehichlePropertyIds.java (Done, 12/28)
     * modifying CarPropertyService (12/29)
     * Validating calling of battery percentage in EVSafe app (12/30)    
*  A new Service, EVSafeService communicating with CarPropertyService (build error)
*  
Replacing for loop with callback in EVSafeService 

Structure of CarPropertyService
Reference: https://wp.me/p4TZ8b-p8
