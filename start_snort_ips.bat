@echo off
REM Start Snort in IPS mode
snort.exe -Q --daq afpacket --daq-var device=eth0 -c C:\Snort\etc\snort.conf -A console
pause
