package demo;

import ddf.minim.Minim;
import ddf.minim.analysis.FFT;
import ddf.minim.analysis.FourierTransform;
import processing.core.PApplet;
import processing.core.PGraphics;
import serial.SerialReader;
import util.ColorMap;
import util.DirectionalEntries;
import util.MyRingBufferProxy;
import util.ScrollingSpectrogramLayer;

public class DataVisualizer extends PApplet{
	
	// FFT
	Minim minim;
	FFT fft;
	int samplingRate = 1000000/200;
	int fftSize = -1; 
	
	// Motor controls
	int centerAlpha = 800;
	int centerBeta = 560;
	
	// Rendering 
	PGraphics canvas;
   	ColorMap cm = new ColorMap();
   	boolean invalidateCanvas = false;
	ScrollingSpectrogramLayer vizLayer;
	
   	// Serial communication
	SerialReader reader;
	MyRingBufferProxy myDataProxy;
	
	// Signal processing 
	int bufferSize = 256; // data length of each frame of raw measurements
	int windowSize = 512; // data length of visualization (50% overlap)
	float fftResult[];
	
	public static void main(String args[]) {
		PApplet.main(new String[] { "--location=0,0", DataVisualizer.class.getName() });
	}
	
	@Override
	public void settings() {
		size(1024, 700, P3D);
	}

	@Override
	public void setup() {
		frameRate(60);
		background(0);
		
		// Graphics
		canvas = createGraphics(2048,height);
		canvas.beginDraw();
		canvas.background(255);
		canvas.endDraw();
		
		// FFT
		minim = new Minim(this);
		fft = new FFT(windowSize, samplingRate);
		fft.window(FourierTransform.HAMMING);
		fftSize = fft.specSize();;
		fftResult = new float[fftSize];
		
		// Initialize data proxy 
		myDataProxy = new MyRingBufferProxy(1024*5+1, fft, fftSize, fftResult);
		
		// Add tag coordinates and event names
		DirectionalEntries de = new DirectionalEntries(0,0, "dummy") ;
		myDataProxy.deList.add(de);
		
		// Initialize serial reader
		reader = new SerialReader(this, myDataProxy, bufferSize/2);
		
		// Initialize scrolling spectrogram
		vizLayer = new ScrollingSpectrogramLayer(this, createGraphics(width,height));
		vizLayer.preventBuffering = true;
		vizLayer.ScaleY = 1.0f;
		
		// Move to the the dummy tag's position and start collecting data
		moveTo(centerAlpha, centerBeta);
		myDataProxy.isReadyToCollect = true;
	}

	@Override
	public void draw() {
		
		background(0);
		fill(255);
		
		// Text labels 
		textSize(50);
		text("Data Visualizer", 20, 60);
		
		textSize(30);
		text(String.format("Serial FPS: %.1f", myDataProxy.fps), 600, 60);
		
		textSize(30);
		text("FFT Spectrum:", 20, height - fftSize - 50);
		
		textSize(30);
		text("Raw Measurements:", 20, height - fftSize - 150 - 100);
		
		// Get data from data proxy
		if(myDataProxy.getNewCount() >= bufferSize){
			myDataProxy.numNew = 0;
			vizLayer.setSensorConnected(true);
			vizLayer.spectrogram = fftResult.clone();
		}
		
		// Draw fft waterfall
		if (invalidateCanvas || vizLayer.preventBuffering) {
			pushMatrix();
			translate(0, height - fftSize - 50);
			VizView();
			popMatrix();
			invalidateCanvas = false;
		}
		
		// Draw raw signal
		stroke(0xff, 0x99, 0);
		for(int i = 0; i < windowSize; i++) {
			float xPos = map(i, 0, windowSize, 0, width);
			rect(xPos, height- fftSize - 150, 1, -myDataProxy.rawMeasures[i] / 30);
		}
		
	}
	
	// Draw visualizer layer
	public void VizView()
	{
		vizLayer.draw();
		image(vizLayer.canvas(),0,0);
	}

	// Motor control 
	public void moveTo(int alpha, int beta){
		
		// For removing backlash
		myDataProxy.positionReady = false;
		
		byte send_buf[] = new byte [5];
        send_buf[0] = (byte)(((beta - 50) >> 8) & 0xff);
        send_buf[1] = (byte)(((beta-50)) & 0xff);
        
        send_buf[2] = (byte)(((alpha-50) >> 8) & 0xff);
        send_buf[3] = (byte)(((alpha-50)) & 0xff);
        
        send_buf[4] = 0x0; // turn laser off
        
        reader.writeToPort(send_buf);
        
		while(!myDataProxy.positionReady){
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Move to target 
		myDataProxy.positionReady = false;
		send_buf[0] = (byte)((beta >> 8) & 0xff);
		send_buf[1] = (byte)((beta) & 0xff);
	            
		send_buf[2] = (byte)((alpha >> 8) & 0xff);
		send_buf[3] = (byte)((alpha) & 0xff);
	            
		send_buf[4] = 0x1; // turn laser on     
		reader.writeToPort(send_buf);
	}
}
	

