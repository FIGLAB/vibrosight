#include <ADC.h>
#include <Wire.h>

// Laser on/off control
#define LASER_PIN 0

// Digital Pot
#define POT_ADDR 0x2E

// Stepper defines
#define IN1X  6
#define IN2X  20
#define IN3X  21
#define IN4X  5

#define IN1Y  2
#define IN2Y  14
#define IN3Y  7
#define IN4Y  8

int StepsX = 0;
boolean DirectionX = false;

int StepsY = 0;
boolean DirectionY = false;

uint16_t curAlpha = 0;
uint16_t curBeta = 0;

uint16_t stepX = 0;
uint16_t stepY = 0;

float increStepY = 0;
float increStepX = 0;

unsigned long last_time;
unsigned long currentMillis;

int stepTime = 3000;

int isMeasBrightness = 0;

// Serial commands
byte commands[] = {0x00, 0x00, 0x00, 0x00, 0x00}; 
byte positionReady[] = {0x00, 0x00, 0x00}; 

// COM parameters
static const int baudrate = 460800;
uint8_t sendbuf[4096]; // FFT_SIZE shorts
uint8_t delimiter[] = {0xff, 0xff, 0xff, 0xff};

// ADC parameters
ADC *adc = new ADC();
const int readPin = A3;
int adcCounter  = 0;
uint16_t input[2048];

void setup() {
  pinMode(readPin, INPUT);
  
  Serial.begin(baudrate);

  // Digipot setting
  Wire.begin(); 
  Wire.beginTransmission(POT_ADDR);
  Wire.send(0x0);
  Wire.send(0x7F); // AFE Gain 100
  Wire.endTransmission();

  pinMode(LASER_PIN, OUTPUT);
  digitalWrite(LASER_PIN, LOW);

  // Steppers
  pinMode(IN1X, OUTPUT); 
  pinMode(IN2X, OUTPUT); 
  pinMode(IN3X, OUTPUT); 
  pinMode(IN4X, OUTPUT);
  
  pinMode(IN1Y, OUTPUT); 
  pinMode(IN2Y, OUTPUT); 
  pinMode(IN3Y, OUTPUT); 
  pinMode(IN4Y, OUTPUT);

  // Recalibrate steppers
  initializeZero();
}

void loop() {
  if (Serial.available() > 0) {
    int readLen = Serial.readBytes((char*)commands,5);

    // Set stepper positions
    uint16_t beta = 0;
    uint16_t alpha = 0;

    if(readLen != 0){
      beta  = commands[0];
      beta = beta <<8;
      beta = beta | commands[1];

      alpha  = commands[2];
      alpha = alpha <<8;
      alpha = alpha | commands[3];

      if(commands[4] != 0x0){
        isMeasBrightness = 1;
      }else{
        isMeasBrightness = 0;
      }
    }

    if(alpha > curAlpha){
      stepX = alpha - curAlpha;
      DirectionX = 1;
    }else{
      stepX = curAlpha - alpha;
      DirectionX = 0;
    }

    if(beta > curBeta){
      stepY = beta - curBeta;
      DirectionY = 1;
      curBeta = beta;
    }else{
      stepY = curBeta - beta;
      DirectionY = 0;
    }

      // Move to next Y position
      adcCounter = 0;
      while(stepY>0){ 
        currentMillis = micros();
        if(currentMillis-last_time>=stepTime){
          stepperY(); 
          last_time=micros();
          stepY--;
        }
      }

      // Scan along X axis while measuring reflected light intensity
      while(stepX>0){ 
        currentMillis = micros();
        if(currentMillis-last_time>=stepTime){
          input[adcCounter] = analogRead(readPin);
          adcCounter ++;
          stepperX(); 
          last_time=micros();
          stepX--;
        }
      }
      
    // Send measurements 
    if(isMeasBrightness){
      sendBrightnessData(adcCounter);
    }

    delay(1);

    curAlpha = alpha;
    curBeta = beta;

    // Send ready signal
    sendReadySig();
  }
}

void initializeZero(){
  uint16_t count = 1024;
  while(count>0){
    currentMillis = micros();
    if(currentMillis-last_time>=stepTime){
      stepperX();  // one step
      stepperY();  // one step
      last_time=micros();
      count--;
    }
  }
}

void sendReadySig(){
  Serial.write(positionReady, sizeof(positionReady));
  Serial.write(delimiter, sizeof(delimiter));
}

void sendBrightnessData(int len) {

    for(int i=0; i<len; i++) {
        uint16_t v = clamp_u16(input[i]);
        sendbuf[2*i] = v & 0xff;
        sendbuf[2*i+1] = v >> 8;
    }

    Serial.write(sendbuf, len*2);
    Serial.write(delimiter, sizeof(delimiter));
}

static inline uint16_t clamp_u16(int v) {
    // leave 65535 (0xffff) for the terminator
    if(v > 65534) return 65534; 
    if(v < 0) return 0;
    return (uint16_t)v;
}

// Stepper helper functions
void stepperX(){
    switch(StepsX){
       case 0:
         digitalWrite(IN1X, LOW); 
         digitalWrite(IN2X, LOW);
         digitalWrite(IN3X, LOW);
         digitalWrite(IN4X, HIGH);
       break; 
       case 1:
         digitalWrite(IN1X, LOW); 
         digitalWrite(IN2X, LOW);
         digitalWrite(IN3X, HIGH);
         digitalWrite(IN4X, HIGH);
       break; 
       case 2:
         digitalWrite(IN1X, LOW); 
         digitalWrite(IN2X, LOW);
         digitalWrite(IN3X, HIGH);
         digitalWrite(IN4X, LOW);
       break; 
       case 3:
         digitalWrite(IN1X, LOW); 
         digitalWrite(IN2X, HIGH);
         digitalWrite(IN3X, HIGH);
         digitalWrite(IN4X, LOW);
       break; 
       case 4:
         digitalWrite(IN1X, LOW); 
         digitalWrite(IN2X, HIGH);
         digitalWrite(IN3X, LOW);
         digitalWrite(IN4X, LOW);
       break; 
       case 5:
         digitalWrite(IN1X, HIGH); 
         digitalWrite(IN2X, HIGH);
         digitalWrite(IN3X, LOW);
         digitalWrite(IN4X, LOW);
       break; 
         case 6:
         digitalWrite(IN1X, HIGH); 
         digitalWrite(IN2X, LOW);
         digitalWrite(IN3X, LOW);
         digitalWrite(IN4X, LOW);
       break; 
       case 7:
         digitalWrite(IN1X, HIGH); 
         digitalWrite(IN2X, LOW);
         digitalWrite(IN3X, LOW);
         digitalWrite(IN4X, HIGH);
       break; 
       default:
         digitalWrite(IN1X, LOW); 
         digitalWrite(IN2X, LOW);
         digitalWrite(IN3X, LOW);
         digitalWrite(IN4X, LOW);
       break; 
    }
    SetDirectionX();
} 

void stepperY(){
    switch(StepsY){
       case 0:
         digitalWrite(IN1Y, LOW); 
         digitalWrite(IN2Y, LOW);
         digitalWrite(IN3Y, LOW);
         digitalWrite(IN4Y, HIGH);
       break; 
       case 1:
         digitalWrite(IN1Y, LOW); 
         digitalWrite(IN2Y, LOW);
         digitalWrite(IN3Y, HIGH);
         digitalWrite(IN4Y, HIGH);
       break; 
       case 2:
         digitalWrite(IN1Y, LOW); 
         digitalWrite(IN2Y, LOW);
         digitalWrite(IN3Y, HIGH);
         digitalWrite(IN4Y, LOW);
       break; 
       case 3:
         digitalWrite(IN1Y, LOW); 
         digitalWrite(IN2Y, HIGH);
         digitalWrite(IN3Y, HIGH);
         digitalWrite(IN4Y, LOW);
       break; 
       case 4:
         digitalWrite(IN1Y, LOW); 
         digitalWrite(IN2Y, HIGH);
         digitalWrite(IN3Y, LOW);
         digitalWrite(IN4Y, LOW);
       break; 
       case 5:
         digitalWrite(IN1Y, HIGH); 
         digitalWrite(IN2Y, HIGH);
         digitalWrite(IN3Y, LOW);
         digitalWrite(IN4Y, LOW);
       break; 
         case 6:
         digitalWrite(IN1Y, HIGH); 
         digitalWrite(IN2Y, LOW);
         digitalWrite(IN3Y, LOW);
         digitalWrite(IN4Y, LOW);
       break; 
       case 7:
         digitalWrite(IN1Y, HIGH); 
         digitalWrite(IN2Y, LOW);
         digitalWrite(IN3Y, LOW);
         digitalWrite(IN4Y, HIGH);
       break; 
       default:
         digitalWrite(IN1Y, LOW); 
         digitalWrite(IN2Y, LOW);
         digitalWrite(IN3Y, LOW);
         digitalWrite(IN4Y, LOW);
       break; 
    }
    SetDirectionY();
}

void SetDirectionX(){
if(DirectionX==1){StepsX++;}
if(DirectionX==0){StepsX--;}
if(StepsX>7){StepsX=0;}
if(StepsX<0){StepsX=7; }
}

void SetDirectionY(){
if(DirectionY==1){StepsY++;}
if(DirectionY==0){StepsY--;}
if(StepsY>7){StepsY=0;}
if(StepsY<0){StepsY=7; }
}

