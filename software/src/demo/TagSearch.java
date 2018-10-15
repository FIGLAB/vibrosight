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
 */
public class TagSearch extends PApplet{
	
	// Tag search thresholds
	float vThresh = 50;
	int lenCompare = 10;
	int lenWindow = 10;
	int threshXY = 15;
	int searchStep = 5;
	
	// for saving result coordinates
	Table table;
	
	// motor serial
	int centerAlpha = 800;
	int centerBeta = 560;
	
	int alphaRange [] = {-700, 700}; //120 degree
	int betaRange [] = {-270, 70}; //60 degree (= 30 * 2)
	
	// for rendering 
	PGraphics canvas;
   	ColorMap cm = new ColorMap();
	ScrollingSpectrogramLayer vizLayer;
	
   	// for serial communication
	SerialReader reader;
	MyRingBufferProxy myDataProxy;
	
	// length of intensity measurement buffer
	int bufferSize = alphaRange[1] - alphaRange[0]; 
	int windowSize = bufferSize; 
	
	// Thread to search tag
	Thread tagSearcher;
		
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
	
	@Override
	public void settings() {
		size(1024, 700, P3D);
		//fullScreen();
	}

	@Override
	public void setup() {
		frameRate(60);
		background(0);
		
		table = new Table();
		table.addColumn("x");
		table.addColumn("y");
		
		myDataProxy = new MyRingBufferProxy(1024*5+1);
		reader = new SerialReader(this, myDataProxy, bufferSize/2);

		tagSearcher = new Thread(new SearchThread());
		tagSearcher.setDaemon(true);
		tagSearcher.start();
	}

	@Override
	public void draw() {
		background(0);
		
		myDataProxy.fps.tick();
		
		fill(255);
		
		if(myDataProxy.getNewCount() >= bufferSize){ // at least there is a buffer of unseen data 
			myDataProxy.numNew = 0;
		}
		
		// draw raw signal
		stroke(0xff, 0x99, 0);
		
		if(myDataProxy.rawMeasures != null){
		
				for(int i = 0; i < windowSize; i++) {
					float xPos = map(i, 0, windowSize, 0, width);
					rect(xPos, height/2, 1, -myDataProxy.rawMeasures[i] / 5);
				}
			
		}
	}

	public static void main(String args[]) {
		PApplet.main(new String[] { "--location=0,0", TagSearch.class.getName() });
	}
	
	public class SearchThread implements Runnable{

		@Override
		public void run() {
			
			ArrayList<int []> tapeXYs = new ArrayList<int []>();

			while(true){
				// tape search
				tapeXYs.clear();
				
				// remove backlash
				moveTo(alphaRange[0] + centerAlpha - 50, betaRange[0] + centerBeta - 50, false);
				
				while(!myDataProxy.positionReady){
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				// start searching
				for(int j = betaRange[0] + centerBeta; j <= betaRange[1]+ centerBeta; j+=searchStep){
					
					moveTo(alphaRange[0] + centerAlpha, j, false);
					
					// brightness measurements are received before sig ready is received
					while(!myDataProxy.positionReady){
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					// move to right, start collecting brightness measurements
					moveTo(alphaRange[1] + centerAlpha, j, true);
					
					while(!myDataProxy.positionReady){
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					// find tape Xs in this path
					ArrayList<Integer> results = findXs();
					
					// assemble coordinates 
					for(int x : results ){
						int [] temp = {x, j};
						print(x);
						print(" ");
						print(j);
						println();
						tapeXYs.add(temp);
					}
				}
				
				// cluster Xs and Ys to find meanXs and meanYs
				ArrayList<ArrayList<int[]>> toFilterTapeXYs = new ArrayList<ArrayList<int[]>>();
				
				Set<Integer> set = new HashSet<Integer>();
				
				ArrayList<int []> filtertapeXYs = new ArrayList<int []>();
				
				if(tapeXYs.size() >= 2){
					for(int i = 0; i < tapeXYs.size(); i++){
						if(set.contains(i)){
							continue;
						}
						
						ArrayList<int[]> temp = new ArrayList<int[]>();
						temp.add(tapeXYs.get(i));
						int[] xy = tapeXYs.get(i);
						
						for(int j = i+1; j < tapeXYs.size(); j++){
							int[] newXY = tapeXYs.get(j);
							
							if(Math.abs(newXY[0] - xy[0]) < threshXY && Math.abs(newXY[1] - xy[1]) < threshXY){
								temp.add(newXY.clone());
								set.add(j);
							}
						}
						
						toFilterTapeXYs.add(temp);	
					}
					
					// calculate meanXs and meanYs
					for(int i = 0; i < toFilterTapeXYs.size(); i++){
						ArrayList<int[]> sameCluster = toFilterTapeXYs.get(i);
						int x = 0;
						int y = 0;
						for(int j = 0; j < sameCluster.size(); j++){
							x = x + sameCluster.get(j)[0];
							y = y + sameCluster.get(j)[1];
						}
						
						x = x/sameCluster.size();
						y = y/sameCluster.size();
						int xy[] = {x,y};
						print(x);
						print(" ");
						print(y);
						println();
						filtertapeXYs.add(xy);
					}
				}else if (tapeXYs.size() == 1){
					filtertapeXYs.add(tapeXYs.get(0));
				}
				
				// save coordinates to csv table
				for(int[] e : filtertapeXYs){
					TableRow row = table.addRow(); 
					row.setInt("x", e[0]);
					row.setInt("y", e[1]);
				}
				
				long curT = System.currentTimeMillis();
				String fn = "./data/tag/" + curT+"coordinates.csv";
				saveTable(table, fn);
				
				println(fn + "  saved!");
			}
		}
	}
	
	ArrayList<Integer>findXs(){
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
							int middleX = cluster.get(cluster.size()/2);
							
							middleX = centerAlpha + alphaRange[0] + middleX;
							rst.add(middleX);
							
							cluster = new ArrayList<Integer>();
						}
					}
				}
		}
		return rst;
	}
}
