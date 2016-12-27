DASHencrypt
============

Usually when creating a video, all that is needed is to encode it using a codec (for example H.264 or HEVC). However, to transmit a video using MPEG-DASH, an extra segmentation step is required. Typical encoders do not provide this step and produce content which is not compatible with DASH.

Our DASHencrypt project provides a solution. It takes encoded video and audio from an array of different formats and repackages them as valid DASH streams. It also generates the required manifest which is the table of contents for the stream.

* [DASH Industry Forum](http://dashif.org/)
* [ISO/IEC 23009-1:2014](http://www.iso.org/iso/home/store/catalogue_tc/catalogue_detail.htm?csnumber=65274)
* [DASH.encrypt@castlabs.com](http://castlabs.com/products/dash-encrypt/)
* [ISO/MP4 Parser](https://github.com/sannies/mp4parser/) 

This project is released under the [Mozilla Public License 2.0](http://www.mozilla.org/MPL/2.0/).

Have a look at the wiki for help.

