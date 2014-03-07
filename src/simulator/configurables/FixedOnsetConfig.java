package simulator.configurables;

import java.io.Serializable;

/**
 * FixedOnsetConfig.java
 * 
 * Class to store a fixed duration.
 * 
 * Created on 01-Dec-2011
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 *
 */

public class FixedOnsetConfig implements StimulusOnsetConfig, Serializable  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9146869895424869352L;
	/** The duration in s. **/
	private double onset;
	
	public FixedOnsetConfig(double onset) {
		this.onset = onset;
	}

	/**
	 * @return a boolean indicating that this is, indeed, a fixed duration.
	 */
	public boolean isFixed() {
		return true;
	}

	/**
	 * @return the next duration.
	 */
	public double getNextOnset() {
		return onset;
	}

	/**
	 * @param onset the duration in s.
	 */
	public void setOnset(double onset) {
		this.onset = onset;
	}
	
	public String toString() {
		return "F("+onset+")";
	}

	@Override
	public void reset() {
	}
	
	@Override
	public void setTrials(int t) {}

	@Override
	public double getMean() {
		return onset;
	}

	/* (non-Javadoc)
	 * @see simulator.configurables.StimulusOnsetConfig#regenerate()
	 */
	@Override
	public void regenerate() {
		// Does nothing here
		
	}
}
