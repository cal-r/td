package simulator.configurables;

import java.io.Serializable;

import simulator.util.Distributions;
import simulator.util.VariableDistribution;

/**
 * VariableOnsetConfig.java
 * 
 * Class holding a configuration for a variable duration.
 * 
 * Created on 01-Dec-2011
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 *
 */

public class VariableOnsetConfig implements StimulusOnsetConfig,Serializable  {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7336395455119352484L;
	/** Variable distribution for generating durations. **/
	private VariableDistribution varDist;
	/** Mean used by the distribution. **/
	private double mean = 0;
	/** Standard deviation for the distribution. **/
	private float sd = 0;
	/** Number of trials. **/
	private int trials;
	/** Distribution type. **/
	private int type;
	/** Mean type. **/
	private boolean isGeometric;

	public VariableOnsetConfig(double mean, long seed, int trials, int type, boolean geometric) {
		this.sd = 0;
		this.mean = mean;
		this.trials = trials;
		isGeometric = geometric;
		this.type = type;
		varDist = Distributions.getDistribution(type, mean, seed, trials, geometric);
	}
	
	/**
	 * Get the next onset from the distribution.
	 */
	
	public double getNextOnset() {
		return varDist.next();
	}

	@Override
	public boolean isFixed() {
		return false;
	}

	/**
	 * @return the mean
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * @param mean the mean to set
	 */
	public void setMean(double mean) {
		this.mean = mean;
	}

	/**
	 * @return the standard deviation
	 */
	public float getSd() {
		return sd;
	}

	/**
	 * @param sd the standard deviation to set
	 */
	public void setSd(float sd) {
		this.sd = sd;
	}

	public long getSeed() {
		return varDist.getSeed();
	}
	
	public String toString() {
		return "V(μ" + mean+ ")";
	}

	@Override
	public void reset() {
		varDist.build();
		varDist.setIndex(0);
	}
	
	public int getTrials() {
		return trials;
	}
	
	public void setTrials(int num) {
		trials = num;
		varDist.setTrials(trials);
	}

	/**
	 * @return the isGeometric
	 */
	public boolean isGeometric() {
		return isGeometric;
	}

	/**
	 * @param isGeometric the isGeometric to set
	 */
	public void setGeometric(boolean isGeometric) {
		this.isGeometric = isGeometric;
		varDist.setMeanType(isGeometric);
	}

	/**
	 * @return the type of distribution used.
	 */
	public int getType() {
		return type;
	}

	/**
	 * @param type the type to use.
	 */
	public void setType(int type) {
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see simulator.configurables.StimulusOnsetConfig#regenerate()
	 */
	@Override
	public void regenerate() {
		varDist.regenerate();
	}

}
