package demo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.data.Table;
import processing.data.TableRow;
import serial.SerialReader;
import util.ColorMap;
import util.MyRingBufferProxy;
import util.ScrollingSpectrogramLayer;

/*
 * Code for Tag search
 * Before running it, make sure the sensor board is flushed with the TagSearch firmware
 */
public class TagSearch extends PApplet{
	
	// Tag search thresholds
	float vThresh = 50;
	int lenCompare = 10;
	int lenWindow = 10;
	int threshXY = 15;
	int searchStep = 5;
	
	// Table for saving detected coordinates
	Table table;
	
	// Search angle control
	int centerAlpha = 800;
	int centerBeta = 560;
	
	int alphaRange [] = {-700, 700}; //120°
	int betaRange [] = {-270, 70}; //60° 
	
	// For visualization 
	PGraphics canvas;
   	ColorMap cm = new ColorMap();
	ScrollingSpectrogramLayer vizLayer;
	
   	// For serial communication
	SerialReader reader;
	
	// Data proxy
	MyRingBufferProxy myDataProxy;
	
	// Length of intensity measurement buffer
	int bufferSize = alphaRange[1] - alphaRange[0]; 
	int windowSize = bufferSize; 
	
	// Thread to search tag
	Thread tagSearcher;
	
	public static void main(String args[]) {
		PApplet.main(new String[] { "--location=0,0", TagSearch.class.getName() });
	}
	
	@Override
	public void settings() {
		size(1024, 700, P3D);
	}

	@Override
	public void setup() {
		frameRate(60);
		background(0);
		
		// Initialize the coordinates table
		table = new Table();
		table.addColumn("alpha");
		table.addColumn("beta");
		
		// Initialize the data proxy and the serial reader
		myDataProxy = new MyRingBufferProxy(1024*5+1);
		reader = new SerialReader(this, myDataProxy, bufferSize/2);
		
		// Start the tag searching thread 
		tagSearcher = new Thread(new SearchThread());
		tagSearcher.setDaemon(true);
		tagSearcher.start();
	}

	@Override
	public void draw() {
		background(0);
		
		fill(255);
		
		if(myDataProxy.getNewCount() >= bufferSize){ 
			myDataProxy.numNew = 0;
		}
		
		// Draw raw signal
		stroke(0xff, 0x99, 0);
		
		if(myDataProxy.rawMeasures != null){
			for(int i = 0; i < windowSize; i++) {
				float xPos = map(i, 0, windowSize, 0, width);
				rect(xPos, height/2, 1, -myDataProxy.rawMeasures[i] / 5);
			}
		}
	}
	
	// Motor control function
		public void moveTo(int alpha, int beta, boolean measBrightness){
			myDataProxy.positionReady = false;
			byte send_buf[] = new byte [5];
			send_buf[0] = (byte)((beta >> 8) & 0xff);
			send_buf[1] = (byte)((beta) & 0xff);
			            
			send_buf[2] = (byte)((alpha >> 8) & 0xff);
			send_buf[3] = (byte)((alpha) & 0xff);
			          
			if(measBrightness)
				send_buf[4] = 0x1;
			else
				send_buf[4] = 0x0;
			reader.writeToPort(send_buf);
		}

	public class SearchThread implements Runnable{

		@Override
		public void run() {
			ArrayList<int []> tapeABs = new ArrayList<int []>();
			while(true){
				
				tapeABs.clear();
				
				// For removing backlash
				moveTo(alphaRange[0] + centerAlpha - 50, betaRange[0] + centerBeta - 50, false);
				
				while(!myDataProxy.positionReady){
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				// Start searching
				for(int j = betaRange[0] + centerBeta; j <= betaRange[1]+ centerBeta; j+=searchStep){
					
					moveTo(alphaRange[0] + centerAlpha, j, false);
					
					while(!myDataProxy.positionReady){
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					// Scan while measuring reflectance
					moveTo(alphaRange[1] + centerAlpha, j, true);
					
					while(!myDataProxy.positionReady){
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					// Find tag Alphas in this path
					ArrayList<Integer> results = findAlphas();
					
					for(int alpha : results ){
						int [] temp = {alpha, j};
						print(alpha);
						print(" ");
						print(j);
						println();
						tapeABs.add(temp);
					}
				}
				
				// cluster Alphas and Betas to find cluster cemters
				ArrayList<ArrayList<int[]>> toFilterTapeABs = new ArrayList<ArrayList<int[]>>();
				
				Set<Integer> set = new HashSet<Integer>();
				
				ArrayList<int []> filtertapeABs = new ArrayList<int []>();
				
				if(tapeABs.size() >= 2){
					for(int i = 0; i < tapeABs.size(); i++){
						if(set.contains(i)){
							continue;
						}
						
						ArrayList<int[]> temp = new ArrayList<int[]>();
						temp.add(tapeABs.get(i));
						int[] xy = tapeABs.get(i);
						
						for(int j = i+1; j < tapeABs.size(); j++){
							int[] newAB = tapeABs.get(j);
							
							if(Math.abs(newAB[0] - xy[0]) < threshXY && Math.abs(newAB[1] - xy[1]) < threshXY){
								temp.add(newAB.clone());
								set.add(j);
							}
						}
						
						toFilterTapeABs.add(temp);	
					}
					
					// Calculate meanXs and meanYs
					for(int i = 0; i < toFilterTapeABs.size(); i++){
						ArrayList<int[]> sameCluster = toFilterTapeABs.get(i);
						int a = 0;
						int b = 0;
						for(int j = 0; j < sameCluster.size(); j++){
							a = a + sameCluster.get(j)[0];
							b = b + sameCluster.get(j)[1];
						}
						
						a = a/sameCluster.size();
						b = b/sameCluster.size();
						int ab[] = {a,b};
						print(a);
						print(" ");
						print(b);
						println();
						filtertapeABs.add(ab);
					}
				}else if (tapeABs.size() == 1){
					filtertapeABs.add(tapeABs.get(0));
				}
				
				// Save coordinates to csv table
				for(int[] e : filtertapeABs){
					TableRow row = table.addRow(); 
					row.setInt("alpha", e[0]);
					row.setInt("beta", e[1]);
				}
				
				long curT = System.currentTimeMillis();
				String fn = "./data/tag/" + curT + "coordinates.csv";
				saveTable(table, fn);
				
				println(fn + " saved!");
			}
		}
	}
	
	// Find cluster centers' alpha values
	ArrayList<Integer>findAlphas(){
		ArrayList<Integer> rst = new ArrayList<Integer>();
		if(myDataProxy.rawMeasures == null){
			return rst;
		}
		
		float[] temp = myDataProxy.rawMeasures.clone();
		int len = temp.length;
		float mean = 0;
		float std = 0;
		
		// calculate mean
		for(int i = 0; i < temp.length; i++){
			mean = mean + temp[i]/len;
		}
		
		// calculate std
		for(int i = 0; i < temp.length; i++){
			std = std + (temp[i] - mean)*(temp[i] - mean);
		}
		
		std = (float) Math.sqrt(std/(len-1));
		
		ArrayList<Integer> cluster = new ArrayList<Integer>();
		
		//find tapes
		for(int i = 0; i < temp.length - 1; i++){
				if(i >= lenCompare + lenWindow && i < temp.length - 1 - lenCompare - lenWindow){
					
					float leftT = 0;
					float rightT = 0;
					
					for(int t = 0; t < lenWindow; t++){
						leftT = leftT + temp[i-lenCompare - t]/lenWindow;
						rightT = rightT + temp[i+lenCompare + t]/lenWindow;
					}
				
					if((temp[i] - leftT > vThresh) && (temp[i] - rightT > vThresh)){
						println(temp[i] - leftT);
						cluster.add(i);
						
						if((temp[i+1] - leftT < vThresh) || (temp[i+1] - rightT < vThresh)){
							int middleA = cluster.get(cluster.size()/2);
							
							middleA = centerAlpha + alphaRange[0] + middleA;
							rst.add(middleA);
							
							cluster = new ArrayList<Integer>();
						}
					}
				}
		}
		return rst;
	}
}
