package simulator.configurables;

import java.io.Serializable;

import simulator.util.Distributions;
import simulator.util.VariableDistribution;

/**
 * Class to store ITI configurations.
 * 
 * @author J Gray
 *
 */

public class ITIConfig implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7893808552297881175L;
	/** Minimum ITI duration **/
	private double minimum;
	/** Variable timing generator for variable period ITIs. **/
	private VariableDistribution varied;
	/** Configured indicator. **/
	private boolean configured;
	/** True if geometric mean is in use. **/
	private boolean isGeo;
	/** Type of variable distribution. **/
	private int type;
	/** Number of trials this is for. **/
	private int trials;
	/** Default mean. **/
	private static double mean = 0;
	/** Default minimum. **/
	private static double defaultMinimum = 300;
	/** Defaults modified. **/
	private static boolean minModified = false;
	private static boolean meanModified = false;
	
	/**
	 * Default constructor, initializes unconfigured with a minimum duration of 300s
	 * with an additional variable duration of 300s. 
	 */
	
	public ITIConfig(int trials) {
		minimum = defaultMinimum;
		varied = Distributions.getDistribution(0, mean, 0, trials, false);
		configured = false;
	}
	
	/**
	 * 
	 * @param min minimum duration of ITI
	 * @param meanVar mean variable duration
	 */
	
	public ITIConfig(final double min, final float meanVar, int trials, boolean geometric, int type) {
		minimum = min;
		varied = Distributions.getDistribution(type, meanVar, trials, geometric);
		configured = true;
		this.type = type;
		if(!minModified) {
			defaultMinimum = min;
			minModified = true;
		}
		if(!meanModified) {
			mean = meanVar;
			meanModified = true;
		}
	}
	
	/**
	 * Get the next ITI duration in seconds.
	 * @return
	 */
	
	public double next() {
		return varied.next()+minimum;
	}
	
	/**
	 * Output to string.
	 */
	
	public String toString() {
		return minimum+"s + μ("+varied.getMean()+")s";
	}

	/**
	 * 
	 * @return the mean for the variable duration.
	 */
	
	public double getMean() {
		return varied.getMean();
	}

	/**
	 * 
	 * @return the fixed minimum duration.
	 */
	
	public double getMinimum() {
		return minimum;
	}
	
	/**
	 * 
	 * @param min the new minimum duration.
	 */
	
	public void setMinimum(double min) {
		minimum = min;
		if(!minModified) {
			defaultMinimum = min;
			minModified = true;
		}
	}
	
	/**
	 * 
	 * @param mean new mean variable duration.
	 */
	
	public void setMean(double meanVar) {
		varied.setMean(meanVar);
		if(!meanModified) {
			mean = meanVar;
			meanModified = true;
		}
	}

	/**
	 * @return a boolean indicating whether this has been checked by the user.
	 */
	public boolean isConfigured() {
		return configured;
	}

	/**
	 * @param configured sets this config as checked or unchecked.
	 */
	public void setConfigured(boolean configured) {
		this.configured = configured;
	}
	
	public void setTrials(int trials) {
		varied.setTrials(trials);
		varied.build();
	}

	public void reset() {
		varied.build();
		varied.setIndex(0);
	}
	
	/**
	 * @return the type of mean in use.
	 */
	public boolean isGeo() {
		return isGeo;
	}

	/**
	 * @param isGeo the type of mean to use.
	 */
	public void setGeo(boolean isGeo) {
		this.isGeo = isGeo;
		varied.setMeanType(isGeo);
	}

	/**
	 * @return the type of distribution in use.
	 */
	public int getType() {
		return type;
	}

	/**
	 * @param type the type to use.
	 */
	public void setType(int type) {
		this.type = type;
		varied = Distributions.getDistribution(type, varied.getMean(), trials, isGeo);
	}
	
	/**
	 * Clear accumulated default settings.
	 */
	
	public static void resetDefaults() {
		mean = 0;
		defaultMinimum = 300;
		meanModified = false;
		minModified = false;
	}
}
