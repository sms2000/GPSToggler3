==========
THIS CODE IS 'UNRESTRICTED PUBLIC DOMAIN UNDER GPLL' LICENCED except 3rd party code of 'AndroidProcesses' library with Apache license.

Let me know if you consider your rights violated.

* GPLL is the 'General Pet Lovers Licence' - the non-restricting license promoting the self control for humans.
If you a pet lover, pet adopter, veterinary personel or in any other way helping cats, dogs and other pets to survice you are free to use this code.
If you are a per hater or harmed any pet in any way you are also free to use this code yet you have to bear shame on you forever. *



GPSToggler 3 - the latest reincarnation of the old trusty GPSTogger
===================================================================

GPS toggle widget for Android rooted devices. It works even for those ROMs and kernels, other software failed.
It requires the 'su' privileges (no reboot is required anymore).

Utilizes parts of 'AndroidProcesses' library code courtesy Jared Rummler from
https://github.com/jaredrummler/AndroidProcesses
(Starting 1.0.640 the 'AndroidProcess' library is not used anymore).


ANTIVIRUS REPORTS MALICIOUS SOFTWARE
====================================

1. AVG Antivirus for Android reports the GPSToggler can steal your SMS. 
   Actually this software has nothing to do with SMS at all (see the sources).



Versions:
=========

1.1.685
-------
1. [WIP] Google Drive cloud support.


1.0.678
-------
1. Minor fixes.
2. Latest Android Studio 2.3 and Gradle 3.4.1 adopted.



1.0.664
-------
1. Two weird bugs fixed.


1.0.663
-------
1. WIP for a better GUI. 
2. WIP for more options.
3. 'su' support improved.
4. Bugs fixed.


1.0.640
-------
1. Pure native applictions recognition tool implemented.
2. WIP for a better interface.

1.0.630
-------
1. WIP. '/proc' activity has been developed.
2. WIP. Per-process fore- and background activation implemented.


1.0.532
-------
1. WIP version. Planned to move all the /proc activity into the native code.


1.0.409
-------
1. Accessibility processing improved.
2. Application reenumerated and the list updated only if required.


1.0.402
-------
1. Fixed various 'root' issues.


1.0.393
-------
1. Gradle project fixes.


1.0.384
-------
1. Android 7.0 Nougat WIP. 1st more or less working verison.
   Android 7.0 Nougat further hardened system access. Root is not enough now and prevents Automation working properly. Partially solved. 

2. Not using 'AndroidProcesses' anymore due to Nougat issues.

1.0.371
-------
1. WIP: Accessibility service automatic engage with root. 


1.0.367
-------
1. Implemented (WIP) Accessibility service to quickly react to any applications ons and offs. 
   Less CPU overhead, quicker GPS turns.

2. Bugs/spelling fixed.


1.0.348
-------
1. Asynchronous engine bug fixed. 
2. Textuals fixed.
3. Widget processing improved.
 

1.0.338
-------
1. Proper widget updates (removed duplicated calls).


1.0.334
-------
1. Settings processing.


1.0.333
-------
1. Class repositioning and some classes renamed.
2. Speller checker.
3. Minor bugs fixed.


1.0.326
-------
1. Bugs fixed.


1.0.323
-------
1. Settings continued.
2. Widget list timeout set to 15 seconds.


1.0.320
-------
1. Versioning restored.



1.0.309
-------
0. Actual 1.0.319.
1. Settings continued.
2. Warnings fixed.


1.0.308
-------
1. Boot initiation fixed partially. More work required. For no please start main activity for few seconds after every boot.
2. Spelling.
3. Settings continued.


1.0.304
-------
1. The 'Settings' activity continued.


1.0.302
-------
1. Maximum possible defence against application killing: 

  * Filtering press back' key' filtered to close Activities ASAP before Android kills the process itself.
  * A nested 'GPSToggler3Monitor' application tries to resurrect the main application if killed.

2. SettingsActivity placeholder.


1.0.300
-------
1. Stability improvements. Application may be killed only by means of a custom ROM 'long back pressed'.


1.0.279
-------
1. Routine code development
