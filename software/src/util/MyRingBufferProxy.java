package util;

import java.util.ArrayList;
import java.util.Date;

import ddf.minim.analysis.FFT;
import ml.DataInstance;
import serial.BufferCallBack;

/*
 * Proxy between main UI and serial data
 */
public class MyRingBufferProxy implements BufferCallBack{
    public float[] elements = null;

    private int capacity  = 0; // length of the buffer 
    
    public FPSTracker fps;
    public float[] rawMeasures;
    public float[][] featureRowStats;

    public int numNew = 0; // number of new datapoints in the buffer
    boolean firstFill = false;
    private int writePos  = 0; // current write position
    
    public boolean positionReady = false;
    
    // keep track of labels of all active appliances
	public ArrayList<String> trainingLabels = new ArrayList<String>();
    
    // pause
    public boolean onHold = false;
    
    // for machine learning data capture
    public boolean isCaptureInstance = false;
    
    // for machine learning data classification
    public boolean isClassify = false;
    
    // true when dot stablizes after switching to a new position
    public boolean isReadyToCollect = false;
    
    // to indicate which direction the laser is currently pointing
	public int curDirection = 0;
    
	public ArrayList<DirectionalEntries> deList = new ArrayList<DirectionalEntries>();
    
    FFT fft;
    
    // FFT windows and raw measurements that will be saved offline 
    ArrayList<float[]> fftWindow;
    ArrayList<float[]> rawMeasBuffer; 
    
    int fftSize;
    float[] fftResult;
    
    public String stage = "training";

    public MyRingBufferProxy(int capacity, FFT fft, int fftSize, float[] fftResult) {
        this.capacity = capacity; 
        this.fftWindow = new ArrayList<float[]>();
        this.rawMeasBuffer = new ArrayList<float[]>();
        this.fftSize = fftSize;
        this.fftResult = fftResult;
        this.fft = fft;

        this.elements = new float[capacity];
        this.numNew = 0;
        this.writePos = 0;
        fps = new FPSTracker();
    }
    
    public void clear(){
    	fftWindow.clear();
    	rawMeasBuffer.clear();
    	trainingLabels.clear();
    }
    
    public MyRingBufferProxy(int capacity) {
        this.capacity = capacity; 
        this.elements = new float[capacity];
        this.numNew = 0;
        this.writePos = 0;
        fps = new FPSTracker();
    }
    
    public void reset(){
    	this.numNew = 0;
        this.writePos = 0;
    }
    
    public void pause(){
    	onHold = !onHold;
    	reset();
    }
    
	@Override
	public void onBuffer(float[] buffer) { 
		
		if(onHold){
			return;
		}
		
		fps.update();
		
		this.put(buffer);
		
		// for save offline
		if(isReadyToCollect){
			rawMeasBuffer.add(buffer);
		}
		
		if(this.fft != null){
			rawMeasures = this.getRaw(fft.timeSize());
			if(this.fft != null && fftSize != 0 && firstFill == true && isReadyToCollect){
				
				// for save offline
				updateFFT(rawMeasures.clone());
			}
		}else{
			rawMeasures = this.getRaw(buffer.length);
		}
	}
	
	// compute FFT features when stepper moves to the next point
	public void captureOneInstance(){
		
		// reset current on appliance list 
		trainingLabels.clear();
		
		// loop all deList see what are the current active appliances 
		for(int i = 0; i < deList.size(); i++){
			if(deList.get(i).curStatusOn){
				trainingLabels.add(deList.get(i).activityName);
				System.out.print(deList.get(i).activityName);
				System.out.print(" ");
			}
			
		}
		System.out.println();
		
		// compute features
		int collectedLength = fftWindow.size();
		 
		float[][] fftWindow2D = new float[collectedLength][fftSize];
			  
		for(int i = 0; i < collectedLength; i++){
			System.arraycopy(fftWindow.get(i), 0, fftWindow2D[i], 0, fftSize); 
		}
			  
		featureRowStats = getStatsRows(fftWindow2D);
		
		// store or classify the captured instance based on program stage 
		switch(stage){
			case "training":
				if(isCaptureInstance){
					
					if(deList.get(curDirection).curStatusOn){
						deList.get(curDirection).addPositive(captureInstance("Positive"));
					}else{
						deList.get(curDirection).addNegative(captureInstance("Negative"));
					}
					
					// save the current fftWindow to the direction Entry for offline analysis
					deList.get(curDirection).addFFTWindowBuffer_RawMeas_LabelList_Feature(rawMeasBuffer ,fftWindow, trainingLabels, featureRowStats);
					
				}
			break;
			
			case "testing":
				if(isClassify){
					// update classification result at the curDirection
					deList.get(curDirection).classify(captureInstance(null));
				}
			break;
		}
		
		// clear FFT window
		fftWindow.clear();
	}
	
	@Override
	public void onPositionalBuffer(){ // stepper settles
		positionReady = true;
		System.out.println("position ready!");
	}
		
	
	void updateFFT(float[] inputBuffer){
		float[] beforeFFT = new float[inputBuffer.length];
		System.arraycopy(inputBuffer,0, beforeFFT,0, inputBuffer.length);
		
		  fft.forward(beforeFFT);
		  
		  for(int i = 0; i < fftSize; i++){
			  if(fft.avgSize() > 300){
				  fftResult[i] =  fft.getAvg(i);
			  }else{
				  fftResult[i] = fft.getBand(i);
			  }
		  }
		  
		  // add to fft window buffer for ML
		  fftWindow.add(fftResult.clone());
	}
    
	private void put(float[] buffer){ 
		int length = buffer.length;
	
		if(length + writePos <= capacity){
			System.arraycopy(buffer, 0, this.elements, writePos, length);
			writePos = (writePos + length)%capacity;
		}else{
			System.arraycopy(buffer, 0, this.elements, writePos, capacity - writePos);
			System.arraycopy(buffer, capacity - writePos, this.elements, 0, length - (capacity - writePos));
			writePos = writePos + length - capacity;
			firstFill = true; 
		}
		
		this.numNew = numNew + length;
	}
	
	public float[] getRaw(int askedLength){ // return askedLength's latest raw value
		if(askedLength > capacity){
			System.out.println("Error: asked length is larger than capacity !");
			return null;
		}
		
		float [] rst = new float [askedLength];
		
		if(askedLength <= writePos){
			System.arraycopy(elements, writePos - askedLength, rst, 0, askedLength);
		}else{
			System.arraycopy(elements, 0, rst, askedLength - writePos, writePos);
			System.arraycopy(elements, capacity - (askedLength - writePos), rst, 0, askedLength - writePos);
		}
		return rst;
	}

	public int getNewCount(){
		return this.numNew;
	}
	
	private float[][] getStatsRows(float[][] fftResultWindow){ 
		
		int nRows = fftResultWindow.length;
		int nCols = fftResultWindow[0].length;
		
		float[][] rstC = new float[3][nCols];
		
		ArrayList<float[][]> rst = new ArrayList<float[][]>();
		
		for(int j = 0; j < nCols; j++){
			for(int i = 0; i<nRows; i++){
				
				// find row-wise maximums
				if(fftResultWindow[i][j] > rstC[0][j]){
					rstC[0][j] = fftResultWindow[i][j];
				}
				
				// calculate means
				rstC[1][j] = rstC[1][j] + fftResultWindow[i][j]/nRows;
			}
		}
		
		// calculate stds
		for(int j = 0; j < nCols; j++){
			for(int i = 0; i<nRows; i++){
				rstC[2][j] = rstC[2][j] + (fftResultWindow[i][j] - rstC[1][j])*(fftResultWindow[i][j] - rstC[1][j])/nRows;
			}
			rstC[2][j] = (float)Math.sqrt(rstC[2][j]);
		}
		
		// result dimension is [3][n]  [0 max, 1 mean, 2 std]
		return rstC;
	}
	
	DataInstance captureInstance (String label){
		DataInstance res = new DataInstance();
		res.date = new Date();
		res.label = label;
		
		float[][] tempR = featureRowStats.clone();
		
		int g = tempR.length;
		int k = tempR[0].length;
		
		float[] features = new float[g*k];
		
		int index = 0;
		
		for(int i = 0; i < g; i++){
			System.arraycopy(tempR[i], 0, features, index + i*k, k);
		}

		res.measurements = features;
		return res;
	}
  
}