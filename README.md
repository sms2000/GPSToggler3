==========
THIS CODE IS 'UNRESTRICTED PUBLIC DOMAIN UNGER GPLL' LICENCED except 3rd party code of 'AndroidProcesses' library with Apache license.

Let me know if you consider your rights violated.

* GPLL is the 'General Pet Lovers Licence' - the non-restricting license promoting the self control for humans.
If you a pet lover, pet adopter, veterinary personel or in any other way helping cats, dogs and other pets to survice you are free to use this code.
If you are a per hater or harmed any pet in any way you are also free to use this code yet you have to bear shame on you forever. *



GPSToggler 3 - the latest reincarnation of the old trusty GPSTogger
===================================================================

GPS toggle widget for Android rooted devices. It works even for those ROMs and kernels, other software failed.
It requires the 'su' privilegies only during the very first run (reboot is still required). 

Utilizes parts of 'AndroidProcesses' library code courtesy Jared Rummler from
https://github.com/jaredrummler/AndroidProcesses


ANTIVIRUS REPORTS MALICIOUS SOFTWARE
====================================

1. AVG Antivirus for Android reports the GPSToggler can steal your SMS. 
   Actually this software has nothing to do with SMS at all (see the sources).



Versions:
=========
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
