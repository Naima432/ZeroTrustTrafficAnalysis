@echo off
REM Start Snort in IDS mode
snort.exe -i eth0 -c C:\Snort\etc\snort.conf -A console -l C:\Snort\log
pause
