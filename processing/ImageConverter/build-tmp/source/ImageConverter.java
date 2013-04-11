import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import controlP5.*; 
import javax.swing.*; 
import sojamo.drop.*; 
import processing.serial.*; 
import ddf.minim.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class ImageConverter extends PApplet {

/*
Image Converter for hacked Brother KH970.
2013 April
So Kanno
*/







ControlP5 cp5;
PImage dimg;  //for drag and drop function
PImage img;   //for displaying and sending image
PImage oimg;  //for displaying original image
PImage simg;  //keeping original size image
PImage title;
SDrop drop;
Serial port;
Minim minim;
AudioSample ready;
AudioSample sent;
AudioSample done;
AudioSample reset;

boolean resizeFlag = true;
boolean dimgConvert = true;
String getFile = null;
int threshold = 210;
PFont pfont;
boolean colorValue = true;
int strokeColor = 25;
int column = 64;
int row = 64;
int maxColumn = 200;
int maxRow = 200;
int[][] pixelBin = new int[row][column];
int[][] displayBin = new int[maxRow][maxColumn];
boolean [][] sendStatus = new boolean [maxRow][maxColumn];
int header = 0;
byte footer = 126;
int lime = color(25, 100, 90);
int pink = color(90, 100, 100);

public void setup() {
  size(1150, 690);
  colorMode(HSB, 100);
  pfont = loadFont("04b-03b-16.vlw");
  textFont(pfont, 16);
  ControlFont cfont = new ControlFont(pfont, 16); 
  simg = loadImage("default.gif");
  oimg = loadImage("default.gif");
  img = loadImage("default.gif");
  dimg = loadImage("default.gif");
  title = loadImage("title.gif");
  cp5 = new ControlP5(this);

  cp5.addSlider("threshold")
    .setPosition(840, 20)
      .setSize(200, 30)
        .setRange(0, 99)
          .setValue(25);

  cp5.addSlider("column")
    .setPosition(840, 70)
      .setSize(200, 30)
        .setRange(32, 200)
          .setValue(64)
            .setColorValue(color(25, 100, 90));        

  cp5.addSlider("row")
    .setPosition(840, 120)
      .setSize(200, 30)
        .setRange(32, 200)
          .setValue(64)
            .setColorValue(color(90, 100, 100));  

  cp5.addButton("Reset")
    .setPosition(840, 540)
      .setSize(100, 30);

  cp5.addButton("SendtoKnittingMachine")
    .setPosition(840, 590)
      .setSize(203, 30);

  cp5.getController("threshold")
    .getCaptionLabel()
      .setFont(cfont)
        .setSize(16);

  cp5.getController("column")
    .getCaptionLabel()
      .setColor(color(25, 100, 90))
        .setFont(cfont)
          .setSize(16);

  cp5.getController("row")
    .getCaptionLabel()
      .setColor(color(90, 100, 100))
        .setFont(cfont)
          .setSize(16);

  cp5.getController("SendtoKnittingMachine")
    .getCaptionLabel()
      .setFont(cfont)
        .setSize(16);

  cp5.getController("Reset")
    .getCaptionLabel()
      .setFont(cfont)
        .setSize(16);

  drop = new SDrop(this);
  String portName = Serial.list()[0];
  port = new Serial(this, portName, 57600);
  port.clear();
  // port.bufferUntil(footer);

  for (int i=0; i<maxRow; i++) {
    sendStatus[i][0] = false;
  }

  for(int i=0; i<maxColumn; i++){
    for(int j=0; j<maxRow; j++){
      displayBin[i][j] = 0;
    }
  }

  minim = new Minim(this);
  ready = minim.loadSample("ready.aif", 512);
  sent = minim.loadSample("sent.aif", 512);
  done = minim.loadSample("done.aif", 1024);
  reset = minim.loadSample("reset.aif", 1024);  
}

public void draw() {
  if(dimgConvert){
    img = simg;
    oimg = dimg;
    oimg.resize(285, 285);
    oimg.updatePixels();
    // simg = dimg;
    dimgConvert = false;
    println("image loaded");
  }
  background(15,5,15);

  if(oimg != null){
    image(oimg, 840, 235, 285, 285);
    image(title, 20, 640);
    fill(0,0,100);
    text("original", 840, 220);
  }

  if (img != null) {
    img = simg;  
    img.resize(column, row);
    // img.resize(200, 200);
    // img.updatePixels();
    img.loadPixels();

    //converting Image to black and white(1/0)array "pixelBin[][]"
    // pixelBin = new int[row][column];
    // float scaleRatioX = img.width / column;
    // float scaleRatioY = img.height / row; 
    // for(int i=0; i<row; i++){
    //   for(int j=0; j<column; j++){
    //     color c = img.pixels[int(i*column*scaleRatioY)+int(j*scaleRatioX)];
    //     int b = int(brightness(c));
    //     if(b > threshold){
    //       pixelBin[i][j] = 1;
    //     }
    //     else if(b <= threshold){
    //       pixelBin[i][j] = 0;
    //     }
    //   }
    // }

    pixelBin = new int[row][column];
    for(int i=0; i<row; i++){
      for(int j=0; j<column; j++){
        int c = img.pixels[(i*column)+j];
        int b = PApplet.parseInt(brightness(c));
        if(b > threshold){
          pixelBin[i][j] = 1;
        }
        else if(b <= threshold){
          pixelBin[i][j] = 0;
        }
      }
    }

    //converting "pixelBin[][]" to "displayBin[][]" for displaying
    for (int i=0; i<maxRow; i++) {
      for(int j=0; j<maxColumn; j++){
        int margin = (maxColumn - column)/2;
        if(i<row){
          if(j>=margin && j<column+margin){
            displayBin[i][j] = pixelBin[i][j-margin];
          }else{
            displayBin[i][j] = 2;
          }
        }else{  
          displayBin[i][j] = 2;
        }
      }
    }

    //displaying displayBin[][]
    for (int i=0; i<maxRow; i++) {
      for(int j=0; j<maxColumn; j++){
        float h = 0;
        float s = 0;
        float b = 0;        
        if(displayBin[i][j] == 1){
          if(sendStatus[i][0] == false){
            h = 0;
            s = 0;
            b = 100;//white
          }
          else {
            h = 17;
            s = 100;
            b = 100;//yellow
          }
        }else if(displayBin[i][j] == 0){
          if(sendStatus[i][0] == false){
            h = 0;
            s = 0;
            b = 0;//black
          }
          else {
            h = 55;
            s = 100;
            b = 90;//blue
          }
        }else if(displayBin[i][j] == 2){
            h = 0;
            s = 0;
            b = 20;//grey
          }
        stroke(0,0,strokeColor);
        fill(h, s, b);
        rect(20+j*4, 20+i*3, 4, 3);
      }
    }
  }

  //draw column line and row line
  stroke(25,100,90);//lime green
  line(20 + 100*4 - column*2 , 20, 20 + 100*4 - column*2, 20+200*3);
  line(20 + 100*4 + column*2 , 20, 20 + 100*4 + column*2, 20+200*3);
  stroke(90,100,100);//pink
  line(20, 20, 20+200*4, 20);
  line(20, 20 + row*3 ,20+200*4 ,20 + row*3);
}

public void Reset(int theValue){
  header = 0;
  for (int i=0; i<row; i++) {
    sendStatus[i][0] = false;
  }
  reset.trigger();
}

public void SendtoKnittingMachine(int theValue) {
  //sending pixelBin[][] to knitting Machine! 
  for (int i=0; i<maxColumn; i++) {
    if(displayBin[header][i] == 2){
      port.write(0);
    }else{
      port.write(displayBin[header][i]);      
    }
  }
  port.write(footer);
  print(header);
  println("sent");
  sendStatus[header][0] = true;
  header++;
  ready.trigger();
}

// void serialEvent(Serial p) {
//   int a = p.read();
//   println(a);
// }

public void serialEvent(Serial p){
  header = p.read();
  print(header);
  println("received");
  header = PApplet.parseInt(header);
  // if(header != 0) header++;
  print("next is ");
  println(header);
  if(header < row-1){
    for(int i=0; i<column; i++){
      port.write(pixelBin[header][i]);
    }
    port.write(footer);
    print(header);
    println("sent");
    sendStatus[header][0] = true;
    sent.trigger();
    }else if(header == row-1){
      println("completed!");
      done.trigger();
      for(int i=0; i<row; i++){
       sendStatus[i][0] = false;
       header = 0;
      }
    }
}

public void dropEvent(DropEvent theDropEvent) {
  println("");
  println("isFile()\t"+theDropEvent.isFile());
  println("isImage()\t"+theDropEvent.isImage());
  println("isURL()\t"+theDropEvent.isURL());

  // if the dropped object is an image, then 
  // load the image into our PImage.
  if (theDropEvent.isImage()) {
    println("### loading image ...");
    dimg = theDropEvent.loadImage();
    simg = theDropEvent.loadImage();
    dimgConvert = true;
  }
}

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "ImageConverter" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}