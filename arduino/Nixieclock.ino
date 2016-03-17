#include <Wire.h>
#include <DS1307RTC.h>
#include <Time.h>

#define TIME_MSG_LEN  11   // time sync to PC is HEADER followed by Unix time_t as ten ASCII digits
#define TIME_HEADER  'T'   // Header tag for serial time sync message
#define TIME_REQUEST  7    // ASCII bell character requests a time sync message 

//const int pinsBus [8] = { 2, 3, 4, 5, 6, 7, 8, 9 };
const int pinsBus [8] = {9, 8, 7, 6, 5, 4, 3, 2};
const int pinCounterClock = 10;
const int pinCounterReset = 11;

byte numbers [3];
boolean points [3][4];
time_t time;

void setup() {
  noInterrupts();
  Serial.begin(9600);
  interrupts();
  for (int i = 0; i < 8; i++) {
    pinMode(pinsBus[i], OUTPUT);
  }
  pinMode(pinCounterClock, OUTPUT);
  pinMode(pinCounterReset, OUTPUT);
  
  // Reset the display
  pulseCounterReset();
  writeDisplay();
}

void loop() {
  if (Serial.available()) {
    processSyncMessage();
  }
  time = RTC.get();
  
  if (time != 0) {
    boolean dst = summertime(year(time), month(time), day(time), hour(time), 0);
    time += 3600; // UTC+1
    if (dst) time += 3600; //dst
    
    numbers[0] = hour(time);
    numbers[1] = minute(time);
    numbers[2] = second(time);
  } else {
    if (RTC.chipPresent()) {
      Serial.println("The DS1307 is stopped.  Please run the SetTime");
      Serial.println("example to initialize the time and begin running.");
      Serial.println();
    } else {
      Serial.println("DS1307 read error!  Please check the circuitry.");
      Serial.println();
    }
  }
  writeDisplay();
}

boolean summertime(int year, byte month, byte day, byte hour, byte tzHours) {
// European Daylight Savings Time calculation by "jurs" for German Arduino Forum
// input parameters: "normal time" for year, month, day, hour and tzHours (0=UTC, 1=MEZ)
// return value: returns true during Daylight Saving Time, false otherwise
  if (month<3 || month>10) return false; // keine Sommerzeit in Jan, Feb, Nov, Dez
  if (month>3 && month<10) return true; // Sommerzeit in Apr, Mai, Jun, Jul, Aug, Sep
  if (month==3 && (hour + 24 * day)>=(1 + tzHours + 24*(31 - (5 * year /4 + 4) % 7)) || month==10 && (hour + 24 * day)<(1 + tzHours + 24*(31 - (5 * year /4 + 1) % 7)))
    return true;
  else
    return false;
}

void processSyncMessage() {
  // if time sync available from serial port, update time and return true
  while(Serial.available() >=  TIME_MSG_LEN ) {  // time message consists of header & 10 ASCII digits
    char c = Serial.read() ;
    Serial.print(c);  
    if( c == TIME_HEADER ) {      
      time_t pctime = 0;
      for(int i=0; i < TIME_MSG_LEN -1; i++) {  
        c = Serial.read();          
        if( c >= '0' && c <= '9') {  
          pctime = (10 * pctime) + (c - '0') ; // convert digits to a number    
        }
      }  
      RTC.set(pctime);   // Sync Arduino clock to the time received on the serial port
    }
  }
}

void writeDisplay() {
  pulseCounterReset();
  for (byte d = 0; d < sizeof(numbers); d++) {
    byte latches [3] = {0, 0, 0};
    // first digit
    byte value = numbers[d] / 10;
    switch (value) {
      case 1:
      latches[0] |= 64;
      break;
      case 2:
      latches[0] |= 32;
      break;
      case 3:
      latches[0] |= 16;
      break;
      case 4:
      latches[0] |= 8;
      break;
      case 5:
      latches[0] |= 4;
      break;
      case 6:
      latches[0] |= 2;
      break;
      case 7:
      latches[0] |= 1;
      break;
      case 8:
      latches[1] |= 128;
      break;
      case 9:
      latches[1] |= 64;
      break;
      case 0:
      latches[1] |= 32;
      break;
    }
    // second digit
    value = numbers[d] % 10;
    switch (value) {
      case 1:
      latches[1] |= 4;
      break;
      case 2:
      latches[1] |= 2;
      break;
      case 3:
      latches[1] |= 1;
      break;
      case 4:
      latches[2] |= 128;
      break;
      case 5:
      latches[2] |= 64;
      break;
      case 6:
      latches[2] |= 32;
      break;
      case 7:
      latches[2] |= 16;
      break;
      case 8:
      latches[2] |= 8;
      break;
      case 9:
      latches[2] |= 4;
      break;
      case 0:
      latches[2] |= 2;
      break;
    }
    // set points
    if (points[d][0]) latches[0] |= 128;
    if (points[d][1]) latches[1] |= 16;
    if (points[d][2]) latches[1] |= 8;
    if (points[d][3]) latches[2] |= 1;
    
    setBus(latches[0]);
    pulseCounterClock();
    setBus(latches[1]);
    pulseCounterClock();
    setBus(latches[2]);
    pulseCounterClock();
  }
}

void pulseCounterClock() {
  digitalWrite(pinCounterClock, HIGH);
  digitalWrite(pinCounterClock, LOW);
}
void pulseCounterReset() {
  digitalWrite(pinCounterReset, HIGH);
  digitalWrite(pinCounterReset, LOW);
}

void setBus(byte value) {
  for (int i = 0; i < 8; i++) {
    int bit = value & 1;
    if (bit == 1) {
      digitalWrite(pinsBus[i], HIGH);
    } else {
      digitalWrite(pinsBus[i], LOW);
    }
    value >>= 1;
  }
}
