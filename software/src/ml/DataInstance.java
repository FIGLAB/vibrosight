package ml;

import java.io.Serializable;
import java.util.Date;
/*
 * To store one instance for ML
 */
public class DataInstance implements Serializable {

	private static final long serialVersionUID = -1L;
	public String label;
	public Date date;
	public float[] measurements;
}
