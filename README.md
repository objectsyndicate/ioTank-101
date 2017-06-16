# ioTank-101
Arduino 101 environment monitor.


# What does it do?

It uses affordable, off the shelf, sensors and hardware to monitor light quality and other metrics for indoor agriculture in a handheld and bluetooth package.

<img class='img-responsive' src="https://github.com/objectsyndicate/ioTank-101/raw/master/screen1.jpg">

It does this by detecting particular wavelenghts of light, as well as soil humidity and temperature.

<img class='img-responsive' src="https://github.com/objectsyndicate/ioTank-101/raw/master/APDS-9960_spec.png">
<img class='img-responsive' src="https://github.com/objectsyndicate/ioTank-101/raw/master/GYML8511.png">

These wavelengths are critical in photosynthesis,

<img class='img-responsive' src="https://upload.wikimedia.org/wikipedia/commons/2/23/Chlorophyll_ab_spectra-en.svg">

UV is important for other chemical reactions in various plants. 

Detecting these subtilites is almost impossible with the naked eye. More expensive quantum sensors offer better resolution, but for a high pricepoint. The ioTank101 provides similar functionality in a more affordable and open package. Not to mention an open platform to hook up a quantum sensor if you choose. 

ioTank-101 is now among the eco-system of open source agriculture solutions Object Syndicate provides to the masses! 

This was written for the Hackster.io [2017 Co-Making the Future competition](https://www.hackster.io/contests/2017chinausyoungmakercompetition ).

Code includes Arduino Firmware, Android BLE app, and Fritzing/Gerber shield PCB.

# BOM

1 Arduino 101

1 APDS-9960 breakout 

1 GY-ML8511 breakout

1 Funduino soil mositure sensor

1 10k Thermistor

1 10k Resistor

# Build
Produce the PCB using provided Fritzing/Gerber files and place breakouts as labeled on PCB. 

<img class='img-responsive' src="https://github.com/objectsyndicate/ioTank-101/raw/master/gerber-viewer.easyeda.com.png">

# Things left to do
Find a way to calculate aprox PAR/PUR, find an affordable deep red (600-700nm) sensor, add apogee quantum sensor (e-mailed them for this, should be possible with their 0-5v model), field test and re-iterate after end-user feedback.

# Copyright/License 

This was all written in June 2017 by John Spounias for OBJECT SYNDICATE LLC.

Released under the GPLv3 license.



