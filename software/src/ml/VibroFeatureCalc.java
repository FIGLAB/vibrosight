package ml;

import java.util.List;

/*
 * Helper functions to assemble instances
 */
public class VibroFeatureCalc extends FeatureCalcBase {

	public VibroFeatureCalc(List<String> classLabels) {
		super(classLabels);
	}
	
	private static void addRawMeasurements(String prefix, float[] v, ValueAdder out) {
		for(int i=0; i<v.length; i++) {
			out.add(prefix + i, v[i]);
		}
	}

	@Override
	protected void calcFeatures(DataInstance data, ValueAdder out) {
		addRawMeasurements("measurements", data.measurements, out);
	}
}
