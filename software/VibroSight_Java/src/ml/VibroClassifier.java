package ml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/*
 * Class that defines training and classification functions
 */

public class VibroClassifier {
    VibroFeatureCalc featureCalc = null;
    public weka.classifiers.trees.RandomForest classifier = null;
    
    public Instances dataset;
    Attribute classAttribute;
    
    public void train(Map<String, List<DataInstance>> instances) {
    	
    	// Add labels
    	featureCalc = new VibroFeatureCalc(new ArrayList<>(instances.keySet()));
    	
    	// Add data
    	 List<DataInstance> trainingData = new ArrayList<>();
         for(List<DataInstance> v : instances.values()) {
             trainingData.addAll(v);
         }
         
         // Prepare training dataset
         dataset = featureCalc.calcFeatures(trainingData);
         
         // Initialize classifier
         classifier = new weka.classifiers.trees.RandomForest();
         
         try {
        	String[] options = weka.core.Utils.splitOptions("-P 100 -I 100 -num-slots 1 -K 0 -M 1.0 -V 0.001 -S 1");
        	classifier.setOptions(options);
			classifier.buildClassifier(dataset);
			this.classAttribute = dataset.classAttribute();
			System.out.println("Training is done!");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public String classify(DataInstance data) {
    	
        if( classAttribute == null || classifier == null) {
            return "Unknown";
        }
    	
        Instance instance = featureCalc.calcInstanceFeatures(data);
        
        try {
        	int result = (int) classifier.classifyInstance(instance);
            return classAttribute.value(result);
        } catch(Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
}
