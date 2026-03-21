Emulight LCP
============

This repository contains the source code of the Emulight LCP (Laser/Light Control Program).


Features
--------

- Realtime laser control via Laserworld ShowNET
- MIDI sequencing
- Remote control of Emulight hardware using dedicated interface hardware
- Audio reference tracks (WAV only for now), mainly intended for synchronization of laser/light shows


Bonus Feature: Laser Pong
-------------------------

Although this is currently *not* integrated into the UI, you can play around with a standalone implementation of Pong. The source code for this demo is available [here](https://github.com/unknown-technologies/lcp/blob/master/projects/com.unknown.emulight.lcp/src/com/unknown/emulight/lcp/laser/Pong.java).
Keep in mind this only plays a Pong *animation*, it does *not* allow you to play the game at the moment. But feel free to take this code and extend it in any way you like. It has no dependencies on anything from the LCP, it only requires the [common](https://github.com/unknown-technologies/common) library for direct interaction with the laser.


Building
--------

- Make sure GNU Make and a GCC toolchain for the local system is installed, otherwise building of the native library for RS232 fails (this is only used for Emulight hardware)
- Download a suitable JDK (JDK21+ at the moment)
- Install [mx](https://github.com/graalvm/mx) (install Python, clone the mx repository in a folder next to the lcp and add it to the `PATH`)
- Clone the [common](https://github.com/unknown-technologies/common) repository in a folder next to the lcp
- Create the file `mx.lcp/env` with a line like `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` (set the JDK path appropriately)
- `mx build`

The compiled JAR file is generated in the `build` folder as `lcp.jar`. To run the LCP, the native library is *not* required unless you want to interact with Emulight hardware.


Development
-----------

You can easily import the code into your IDE of choice. To do this, set up the build environment according to the instructions in the "Building" section. Then run `mx eclipseinit` or `mx ideinit` to generate IDE project files. Once the project files are generated, import all project files from the root of the repository + subfolders into your IDE. In addition, import all project files from the common library too, otherwise you will get build errors in your IDE.

This will not only set up classpaths and JDK compatibility levels, it will also set up code formatting rules as well as various rules related to compiler warnings.


License
-------

This program is licensed under the terms of the GNU GPLv3.
