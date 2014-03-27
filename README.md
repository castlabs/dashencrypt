dashencrypt
===========

DASH fragmenter and encrypter.

Usage
--------

  1. Download https://github.com/castlabs/dashencrypt/releases/download/dash.encrypt-1.0/dash.fragmencrypter-1.0.jar
  2. Prepare a movie:
    1. Download source file:

        ```$ wget http://mirrorblender.top-ix.org/movies/sintel-1024-surround.mp4```

    1. Create video file encoded at 250k 436p, H.264 codec:

        ```$ ffmpeg -i sintel-1024-surround.mp4 -an -b:v 250k -s 1024x436 -vcodec libx264 avc1-sintel-436p-250k.mp4```
    
    1. Create audio file encoded at 500k 436p, H.264 codec:

        ```$ ffmpeg -i sintel-1024-surround.mp4 -an -b:v 500k -s 1024x436 -vcodec libx264 avc1-sintel-436p-500k.mp4```
		
    1. Create video file encoded at 1000k 436p, H.264 codec:

        ```$ ffmpeg -i sintel-1024-surround.mp4 -an -b:v 1000k -s 1024x436 -vcodec libx264 avc1-sintel-436p-1000k.mp4```
		
    1. Create video file encoded at 69k stereo, HE-AAC codec:

       ```$ ffmpeg -i sintel-1024-surround.mp4 -vn -ac 2 -acodec libfdk_aac -profile:a aac_he -f mp4 mp4a-sintel-69k.mp4```
		
