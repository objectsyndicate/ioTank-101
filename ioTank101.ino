#include <CurieBLE.h> // Curie BLE lib
#include <Wire.h> // SPI lib
#include <SparkFun_APDS9960.h> // APDS-9960 lib
//#include <dht.h> // DHT11 lib


// RGB Light sensor vars
SparkFun_APDS9960 apds = SparkFun_APDS9960();
uint16_t ambient_light = 0;
uint16_t red_light = 0;
uint16_t green_light = 0;
uint16_t blue_light = 0;
uint8_t proximity_data = 0;

//DHT 11
//#define DHT11_PIN 4



//https://www.bluetooth.com/specifications/gatt/services
BLEService sensorSvc("b643cc1c-533f-49d0-9899-ff91dc503d28"); 


//https://www.bluetooth.com/specifications/gatt/characteristics
BLECharacteristic sensorChar("3117f4bd-ee31-4253-a1ec-51b6bc8da174", BLERead,150);


int oldBatteryLevel = 0;  // last battery level reading from analog input
long previousMillis = 0;  // last time the battery level was checked, in ms

void setup() {
  Serial.begin(9600);    // initialize serial communication
  pinMode(13, OUTPUT);   // initialize the LED on pin 13 to indicate when a central is connected

  // Initialize APDS-9960 (configure I2C and initial values)
  if ( apds.init() ) {
    Serial.println(F("APDS-9960 initialization complete"));
  } else {
    Serial.println(F("Something went wrong during APDS-9960 init!"));
  }
  
  // Start running the APDS-9960 light sensor (no interrupts)
  if ( apds.enableLightSensor(false) ) {
    Serial.println(F("Light sensor is now running"));
  } else {
    Serial.println(F("Something went wrong during light sensor init!"));
  }


  // Adjust the Proximity sensor gain
  if ( !apds.setProximityGain(PGAIN_2X) ) {
    Serial.println(F("Something went wrong trying to set PGAIN"));
  }
  
  // Start running the APDS-9960 proximity sensor (no interrupts)
  if ( apds.enableProximitySensor(false) ) {
    Serial.println(F("Proximity sensor is now running"));
  } else {
    Serial.println(F("Something went wrong during sensor init!"));
  }

  
  // Wait for initialization and calibration to finish
  delay(500);

  // begin initialization
  BLE.begin();

  BLE.setLocalName("ioTank");

  BLE.setAdvertisedServiceUuid(sensorSvc.uuid());

  BLE.setAdvertisedService(sensorSvc);
  
  sensorSvc.addCharacteristic(sensorChar);
  BLE.addService(sensorSvc);

// sensorChar.setValue(sensorData, 5);

  BLE.advertise();

  Serial.println("Bluetooth device active, waiting for connections...");
}

void loop() {
  // listen for BLE peripherals to connect:
  BLEDevice central = BLE.central();

  // if a central is connected to peripheral:
  if (central) {
    Serial.print("Connected to central: ");
    // print the central's MAC address:
    Serial.println(central.address());
    // turn on the LED to indicate the connection:
    digitalWrite(13, HIGH);

    // check the battery level every 200ms
    // as long as the central is still connected:
    while (central.connected()) {
      long currentMillis = millis();
      // if 200ms have passed, check the battery level:
      if (currentMillis - previousMillis >= 200) {
        previousMillis = currentMillis;
        readSensors();
      }
    }
    // when the central disconnects, turn off the LED:
    digitalWrite(13, LOW);
    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
  }
}


int buffLen = 0;
#define THERMISTORNOMINAL 10000
// temp. for nominal resistance (almost always 25 C)
#define TEMPERATURENOMINAL 25
// how many samples to take and average, more takes longer
// but is more 'smooth'
#define NUMSAMPLES 3
// The beta coefficient of the thermistor (usually 3000-4000)
#define BCOEFFICIENT 3984
// the value of the 'other' resistor
#define SERIESRESISTOR 10000
int samples[NUMSAMPLES];
float steinhart = 0;

void readSensors() {

// send buffer

// RGB sensor
  if (  !apds.readProximity(proximity_data) ||
        !apds.readAmbientLight(ambient_light) ||
        !apds.readRedLight(red_light) ||
        !apds.readGreenLight(green_light) ||
        !apds.readBlueLight(blue_light) ) {
    Serial.println("Error reading light values");
  } else {
   /* 
    Serial.print("Ambient: ");
    Serial.print(ambient_light);
    Serial.print(" Red: ");
    Serial.print(red_light);
    Serial.print(" Green: ");
    Serial.print(green_light);
    Serial.print(" Blue: ");
    Serial.println(blue_light);
    Serial.print(" IR: ");
    Serial.println(prox_light);
  */
  }

// Read UVI
int uvi = analogRead(A0);

// Read soil humidity
int soil = analogRead(A2);

// Calculate temp
uint8_t i;
float average;

// take N samples in a row, with a slight delay
for (i = 0; i < NUMSAMPLES; i++) {
  samples[i] = analogRead(A1);
  delay(10);
}

// average all the samples out
average = 0;
for (i = 0; i < NUMSAMPLES; i++) {
  average += samples[i];
}
average /= NUMSAMPLES;

// Serial.print("Average analog reading ");
// Serial.println(average);
// convert the value to resistance
average = 1023 / average - 1;
average = SERIESRESISTOR / average;
// Serial.print("Thermistor resistance ");
// Serial.println(average);

steinhart = average / THERMISTORNOMINAL;     // (R/Ro)
steinhart = log(steinhart);                  // ln(R/Ro)
steinhart /= BCOEFFICIENT;                   // 1/B * ln(R/Ro)
steinhart += 1.0 / (TEMPERATURENOMINAL + 273.15); // + (1/To)
steinhart = 1.0 / steinhart;                 // Invert
steinhart -= 273.15;                         // convert to C


// calculate UV Index
float uviVoltage= uvi * (3.3 / 1023.0);
float uvIntensity = mapfloat(uviVoltage, 0.96, 2.8, 0.0, 15.0); //Convert the voltage to a UV intensity level




String LS = String(ambient_light);
String GS = String(green_light);
String BS = String(blue_light);
String RS = String(red_light);

String UVS = String(uvIntensity);
String SoilS = String(soil);
String T1S = String(steinhart);

String PS = String(proximity_data);

String CK = String(calculateColorTemperature(red_light,green_light, blue_light));
String LUX = String(calculateLux(red_light,green_light, blue_light));

//dht DHT;
//int chk = DHT.read11(DHT11_PIN);
//String H = String(DHT.humidity, 1);
//String T2 = String(DHT.temperature, 1);


// Send string
String BTString = "{\"A\":"+LS+", \"R\":"+RS+", \"G\":"+GS+", \"B\":"+BS+", \"L\":"+LUX+", \"K\":"+CK+", \"U\":"+UVS+", \"S\":"+SoilS+", \"T\":"+T1S+", \"P\":"+PS+"}";
Serial.println(BTString);


// everything wants to know how long the buf should be
buffLen = BTString.length()+1;
delay(10);
//Serial.println(buffLen);


// char buf
uint8_t buf[buffLen];

// Convert send string to char array
BTString.getBytes(buf, buffLen);

// Send the char array via BLE
sensorChar.setValue(buf, buffLen);     

// if one enables the DHT 11 this value must be +500ms.
delay(100);
}




/**************************************************************************/
/*!
    @brief  Converts the raw R/G/B values to color temperature in degrees
            Kelvin 
            https://github.com/adafruit/Adafruit_TCS34725/
            https://cdn.sparkfun.com/assets/learn_tutorials/3/2/1/Avago-APDS-9960-datasheet.pdf
            https://cdn-shop.adafruit.com/datasheets/TCS34725.pdf
            https://www.usna.edu/Users/oceano/raylee/papers/RLee_AO_CCTpaper.pdf
*/
/**************************************************************************/
uint16_t calculateColorTemperature(uint16_t r, uint16_t g, uint16_t b)
{
  float X, Y, Z;      /* RGB to XYZ correlation      */
  float xc, yc;       /* Chromaticity co-ordinates   */
  float n;            /* McCamy's formula            */
  float cct;

  /* 1. Map RGB values to their XYZ counterparts.    */
  /* Based on 6500K fluorescent, 3000K fluorescent   */
  /* and 60W incandescent values for a wide range.   */
  /* Note: Y = Illuminance or lux                    */
  X = (-0.14282F * r) + (1.54924F * g) + (-0.95641F * b);
  Y = (-0.32466F * r) + (1.57837F * g) + (-0.73191F * b);
  Z = (-0.68202F * r) + (0.77073F * g) + ( 0.56332F * b);

  /* 2. Calculate the chromaticity co-ordinates      */
  xc = (X) / (X + Y + Z);
  yc = (Y) / (X + Y + Z);

  /* 3. Use McCamy's formula to determine the CCT    */
  n = (xc - 0.3320F) / (0.1858F - yc);

  /* Calculate the final CCT */
  cct = (449.0F * powf(n, 3)) + (3525.0F * powf(n, 2)) + (6823.3F * n) + 5520.33F;

  /* Return the results in degrees Kelvin */
  return (uint16_t)cct;
}



uint16_t calculateLux(uint16_t r, uint16_t g, uint16_t b)
{
  float illuminance;

  /* This only uses RGB ... how can we integrate clear or calculate lux */
  /* based exclusively on clear since this might be more reliable?      */
  illuminance = (-0.32466F * r) + (1.57837F * g) + (-0.73191F * b);

  return (uint16_t)illuminance;
}


//The Arduino Map function but for floats
//From: http://forum.arduino.cc/index.php?topic=3922.0
float mapfloat(float x, float in_min, float in_max, float out_min, float out_max)
{
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}
