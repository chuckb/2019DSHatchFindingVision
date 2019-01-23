echo on
if "%~1"=="" (goto :BLANK) else (goto :NOTBLANK)

:BLANK
set DEVICEINDEX=0
goto LAUNCH

:NOTBLANK
set DEVICEINDEX=%1

:LAUNCH
gst-launch-1.0 ksvideosrc device-index=%DEVICEINDEX% num-buffers=-1 ! video/x-raw,framerate=30/1,width=640,height=480 ! jpegenc ! queue ! multipartmux boundary=spionisto ! queue leaky=2 ! tcpclientsink host=127.0.0.1 port=9999
