package ml;

import java.io.Serializable;
/*
 * One instance for machine learning
 */

public class DataInstance implements Serializable {

	private static final long serialVersionUID = -1L;
	public String label;
	public float[] measurements;
}
