# de.persosim.android.app
PersoSim App for Android

This Readme comes along with the source code of the PersoSim App [1]. The app
itself is part of the PersoSim project [2] which provides a simulator for the
German electronic identity card (nPA or ePA). The PersoSim App is a port of the
simulator component [3] to Android integrating NFC host card emulation and a
graphical command and control user interface.

The following lines are intended as an overview of the PersoSim-specific
characteristics concerning building, modifying and running the app for any
interested developer. We presume a developer to be familiar with the general
Android build process as well as a basic understanding of smartcard
communication on APDU level.


Build and run PersoSim app:

In order to build the PersoSim App for Android a development environment for
API level 19 and above is required (we recommend Eclipse with Android
development tools set to API level 21). API level 19 is a fixed minimum
requirement as it is the first one to support native Host card Emulation (HCE)
mode.
In order to run the app an Android device or simulator is required running
Android 4.4 (KitKat) or above that supports NFC communication in HCE mode.
Furthermore the Android operating system hosting the app is required not to
reply to any valid ISO-7816 command APDU itself but to forward it to the app
and only respond with the response APDU provided by the app in return.
Per default Android's NFC system library preprocesses any incoming command
APDU and especially prematurely discards certain types of syntactically valid
command APDUs. It e.g. discards SELECT APDUs for certain combinations of P1-P2
parameter combinations or SELECT APDUs for any unregistered Application Id (AID)
as well as any non-SELECT APDU preceding the first accepted SELECT after any
card reset. For the simulator to work without any restrictions all APDUs need
to be received, processed and replied to by the app. Unfortunately up to now
there exists no known overall way to enable this behaviour without modifications
to the native system library. As a preliminary workaround one may send a valid
and accepted SELECT for a registered AID as first APDU following any card reset,
e.g. 0x00A4040007F04E66E75C02D8.


Modify PersoSim app and build components:

PersoSim's basic simulator component as well as its sub-components or e.g. the
provider of log data are implemented as OSGI service bundles [4]. These bundles
are run on top of the Apache Felix OSGI framework [5] integrated into the app.
All OSGI component bundles are stored as *.jar files within the project's
resources (res/raw) sub-folder. As the default Android build process exports the
bundles' *.jar files unprocessed they need to be manually precompiled (dexed)
prior to adding them to the resource folder. This can be achieved by the
use of dx and aapt commands from Android sdk platform-tools.
Run "dx --dex --output=classes.dex myBundle.jar" to precompile the bundle's code
and "aapt add myBundle.jar classes.dex" to add the compilation to the bundle.



[1]
https://github.com/PersoSim/de.persosim.android.app

[2]
http://persosim.de/

[3]
https://github.com/PersoSim/de.persosim.simulator

[4]
http://www.osgi.org/Technology/WhatIsOSGi

[5]
https://felix.apache.org/