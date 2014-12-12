#include <Servo.h>

#define MAX_SPEED 2000
#define MED_SPEED 1350
#define MIN_SPEED 700

Servo motor;
int pin = 4;
char num[2];
String speedOfMotor = "";
int i, j; 
int motorSpeed = MIN_SPEED;

boolean isNum = false;

void setup(){
  Serial.begin(115200);
  i = 0;
  j = 0;
  motor.attach(pin);
}

void loop(){
  while(Serial.available() > 0){
    char c = Serial.read();
    if(c == '\n'){
       motorSpeed = speedOfMotor.toInt();
       speedOfMotor = ""; 
    }else{
      if(isDigit((int) c)){
        speedOfMotor += c;
      }
    }
    Serial.print(c);
  }
  motor.writeMicroseconds(motorSpeed);
//  Serial.println("No signal");
}
