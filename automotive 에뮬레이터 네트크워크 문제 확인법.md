automotive network 문제 확인하는 법

[네트워크 문제 확인]
1. 명령어: adb shell
2. 명령어: ifcofig
- eth0 또는 wlan0 인터페이스가 존재하고 IP 주소가 할당 확인
- IP 주소가 없다면 DHCP 설정 문제
예시)
eth0      Link encap:UNSPEC    Driver virtio_net
          inet addr:10.0.2.15  Bcast:10.255.255.255  Mask:255.0.0.0 << 이부분 확인 네트워크 문제 시 없음 
          UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1
          RX packets:350 errors:0 dropped:0 overruns:0 frame:0 
          TX packets:433 errors:0 dropped:0 overruns:0 carrier:0 
          collisions:0 txqueuelen:1000 
          RX bytes:50739 TX bytes:52182
          
[에뮬레이터 네트워크 강제 초기화]
1. 명령어: adb emu kill
2. 명령어: emulator -netspeed full -netdelay none 
- 네트워크 초기화 후 에뮬레이터 재실행 
