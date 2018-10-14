package ml;

import java.util.List;

/*
 * Class to assemble features
 */

public class VibroFeatureCalc extends FeatureCalcBase {

	public VibroFeatureCalc(List<String> classLabels) {
		super(classLabels);
	}
	
	private static void addRawMeasurements(String columnHeader, float[] v, ValueAdder out) {
		for(int i=0; i<v.length; i++) {
			out.add(columnHeader + i, v[i]);
		}
	}

	@Override
	protected void calcFeatures(DataInstance data, ValueAdder out) {
		addRawMeasurements("measurements", data.measurements, out);
	}
	
}
