#define SIZE 4
int ledPins[SIZE] = {0, 1, 3, 11};
int index = 0;
//boolean check[SIZE] = {false, false, false, false};
int check[SIZE] = {-1, -1, -1, -1};
char cmd[SIZE + 1];

void setup(){
  for(int i = 0; i < SIZE;i++){
    pinMode(ledPins[i], OUTPUT);
  }
  Serial.begin(115200);
}

void loop(){
  while(Serial.available() > 0){
    for(int i = 0; i < SIZE + 1;i++){
      char c = Serial.read();
      cmd[i] = c;
      if(c == '\n'){
        index = i;
        break;
      }
      
    }
    int len = 0;
    for(int i = 0;i < index; i++){
      int motor = cmd[i] - '1';
      check[len++] = motor;
    }
    for(int i = 0;i < SIZE; i++){
      boolean hi = false;
      for(int k = 0; k < len;k++){
        if(check[k] == i){
          hi = true;
          break;
        }
      }
      if(hi){
        digitalWrite(ledPins[i], HIGH);
      }else{
        digitalWrite(ledPins[i], LOW);
      }
    }
    
    String back = "";
    for(int j = 0;j < index + 1;j++){
      back += cmd[j];
    }
    Serial.print(back);
  }
//  Serial.println("www");
}
