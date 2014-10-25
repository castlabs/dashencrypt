DASH.encrypt
============

Usually when creating a video, all that is needed is to encode it using a codec (for example H.264 or HEVC). However, to transmit a video using MPEG-DASH, an extra segmentation step is required. Typical encoders do not provide this step and produce content which is not compatible with DASH.

Our DASH.encrypt project provides a solution. It takes encoded video and audio from an array of different formats and repackages them as valid DASH streams. It also generates the required manifest which is the table of contents for the stream.

* [DASH Industry Forum](http://dashif.org/)
* [ISO/IEC 23009-1:2014](http://www.iso.org/iso/home/store/catalogue_tc/catalogue_detail.htm?csnumber=65274)
* [DASH.encrypt@castlabs.com](http://castlabs.com/products/dash-encrypt/)

This project is released under the [Mozilla Public License 2.0](http://www.mozilla.org/MPL/2.0/).

Usage
--------

  1. Download https://github.com/castlabs/dashencrypt/releases/download/dash.encrypt-1.1.1/dash.fragmencrypter-1.1.1-exe.jar
  2. Prepare a movie:
    1. Download source file:

        ```$ wget http://mirrorblender.top-ix.org/movies/sintel-1024-surround.mp4```

    1. Create video file encoded at 250k 436p, H.264 codec:

        ```$ ffmpeg -i sintel-1024-surround.mp4 -an -b:v 250k -s:v 1024x436 -keyint_min 24 -g 72 -vcodec libx264 avc1-sintel-436p-250k.mp4```
    
    1. Create video file encoded at 500k 436p, H.264 codec:

        ```$ ffmpeg -i sintel-1024-surround.mp4 -an -b:v 500k -s:v 1024x436 -keyint_min 24 -g 72 -vcodec libx264 avc1-sintel-436p-500k.mp4```
		
    1. Create video file encoded at 1000k 436p, H.264 codec:

        ```$ ffmpeg -i sintel-1024-surround.mp4 -an -b:v 1000k -s:v 1024x436 -keyint_min 24 -g 72 -vcodec libx264 avc1-sintel-436p-1000k.mp4```
		
    1. Create audio file encoded at 69k stereo, HE-AAC codec:

       ```$ ffmpeg -i sintel-1024-surround.mp4 -vn -ac 2 -acodec libfdk_aac -profile:a aac_he -ab 69k -f mp4 mp4a-sintel-69k.mp4```
  3. Execute 
  
       ```java -jar dash.fragmencrypter-1.1.1-exe.jar dash mp4a-sintel-69k.mp4 avc1-sintel-436p-1000k.mp4 avc1-sintel-436p-500k.mp4 avc1-sintel-436p-250k.mp4```
  4. To encrypt execute: 
       ```java -jar dash.fragmencrypter-1.1.1-exe.jar encrypt avc1-sintel-436p-250k.mp4 avc1-sintel-436p-500k.mp4 avc1-sintel-436p-1000k.mp4 mp4a-sintel-69k.mp4 --secretKey 000102030405060708090a0b0c0d0e0f --uuid 0696f5b0-b612-11e3-a5e2-0800200c9a66```
  
