package util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ml.DataInstance;
import ml.VibroClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;

/*
 * The data structure to save all information from one tag
 * including raw measurements, fft result windows, and the ML classifier
 */
public class DirectionalEntries implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	public int alpha; 
	public int beta; 
	public String activityName = "";
	VibroClassifier classifier;
	public boolean curResult = false;
	public boolean curStatusOn = false;
	
	String[] classNames = {"Negative", "Positive"};
	
	// for saving data offline 
	public ArrayList<ArrayList<float[]>> rawMeasBuffer = new ArrayList<ArrayList<float[]>>();
	public ArrayList<ArrayList<float[]>> fftWindowBuffer = new ArrayList<ArrayList<float[]>>();
	public ArrayList<ArrayList<String>> currentOnAppList = new ArrayList<ArrayList<String>>(); 
	public ArrayList<float[][]> featureBuffer = new ArrayList<float[][]>();
	
	public Map<String, List<DataInstance>> trainingData = new HashMap<>();
	
	{for (String className : classNames){
		trainingData.put(className, new ArrayList<DataInstance>());
	}}
	
	public DirectionalEntries(int alpha, int beta, String activityName){
		this.alpha = alpha;
		this.beta = beta;
		this.activityName = activityName;
	}
	
	public void addNegative(DataInstance dataInstance){
		trainingData.get("Negative").add(dataInstance);
	}
	
	public void addPositive(DataInstance dataInstance){
		trainingData.get("Positive").add(dataInstance);
	}
	
	// save data for offline analysis
	public void addFFTWindowBuffer_RawMeas_LabelList_Feature(ArrayList<float[]> rawMeas, ArrayList<float[]> fftWindow, ArrayList<String> onAppList, float[][] features){
		
		// add raw measurements
		rawMeasBuffer.add(rawMeas);
		
		// add fft window buffer 
		fftWindowBuffer.add(cloneListFloat(fftWindow));
		
		// add current on appliance list
		currentOnAppList.add(cloneListString(onAppList));
		
		// add the current one feature to the buffer  
		featureBuffer.add(features);
	}
	
	public static ArrayList<float[]> cloneListFloat(ArrayList<float[]> list) {
		ArrayList<float[]> clone = new ArrayList<float[]>(list.size());
	    for (float[] item : list) clone.add(item.clone());
	    return clone;
	}
	
	public static ArrayList<String> cloneListString(ArrayList<String> list) {
		ArrayList<String> clone = new ArrayList<String>(list.size());
	    for (String item : list) clone.add(item);
	    return clone;
	}
	
	public boolean classify(DataInstance dataInstance){
		boolean rst = false;
		
		String label = classifier.classify(dataInstance);
		if(label == "Positive"){
			curResult = true;
			return true;
		}
		curResult = false;
		return rst;
	}
	
	public void train(){
		this.classifier = new VibroClassifier();
		this.classifier.train(trainingData);
	}
	
	public void reTrain(){
		this.classifier = null;
	}
	
	
	public void evaluate(String foldpath) throws Exception{ // save evaluation result to a folder
		 Evaluation eval = new Evaluation(this.classifier.dataset);
		 Random rand = new Random(1);  // using seed = 1
		 int folds = 10;
		 eval.crossValidateModel(this.classifier.classifier, this.classifier.dataset, folds, rand);
		 System.out.println(eval.toSummaryString());
	}
	
}
