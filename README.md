dashencrypt
===========

DASH fragmenter and encrypter.

Usage
--------

  1. Download https://github.com/castlabs/dashencrypt/releases/download/dash.encrypt-1.0.6/dash.fragmencrypter-1.0.6.jar
  2. Prepare a movie:
    1. Download source file:

        ```$ wget http://mirrorblender.top-ix.org/movies/sintel-1024-surround.mp4```

    1. Create video file encoded at 250k 436p, H.264 codec:

        ```$ ffmpeg -i sintel-1024-surround.mp4 -an -b:v 250k -s 1024x436 -vcodec libx264 avc1-sintel-436p-250k.mp4```
    
    1. Create audio file encoded at 500k 436p, H.264 codec:

        ```$ ffmpeg -i sintel-1024-surround.mp4 -an -b:v 500k -s 1024x436 -vcodec libx264 avc1-sintel-436p-500k.mp4```
		
    1. Create video file encoded at 1000k 436p, H.264 codec:

        ```$ ffmpeg -i sintel-1024-surround.mp4 -an -b:v 1000k -s 1024x436 -vcodec libx264 avc1-sintel-436p-1000k.mp4```
		
    1. Create audio file encoded at 69k stereo, HE-AAC codec:

       ```$ ffmpeg -i sintel-1024-surround.mp4 -vn -ac 2 -acodec libfdk_aac -profile:a aac_he -f mp4 mp4a-sintel-69k.mp4```
  3. Execute 
  
       ```java -jar dash.fragmencrypter-1.0.6.jar dash mp4a-sintel-69k.mp4 avc1-sintel-436p-1000k.mp4 avc1-sintel-436p-500k.mp4 avc1-sintel-436p-250k.mp4```
  4. To encrypt execute: 
       ```java -jar dash.fragmencrypter-1.0.6.jar encrypt avc1-sintel-436p-250k.mp4 avc1-sintel-436p-500k.mp4 avc1-sintel-436p-1000k.mp4 mp4a-sintel-69k.mp4 --secretKey 000102030405060708090a0b0c0d0e0f --uuid 0696f5b0-b612-11e3-a5e2-0800200c9a66```
  
