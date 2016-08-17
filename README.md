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
