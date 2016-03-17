# NixieClock
Resources for my Nixie Clock

Currently the Arduino code only supports the timesetting functionality of the sync software. It also does only show the time, nothing more.
All other options, also the ones which are already present in the sync software (setting DST, timezones, etc.), still need to be implemented in the Arduino code. Feel free to do so.
The sync software needs the (RXTX)[https://github.com/rxtx/rxtx] native library. This was the first and last time I used this library, its aweful.