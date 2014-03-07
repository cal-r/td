package simulator.configurables;

/**
 * StimulusOnsetConfig.java
 * 
 * Interface for duration configuration objects.
 * 
 * Created on 01-Dec-2011
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 *
 */

public interface StimulusOnsetConfig {

	/**
	 * @return whether this config is fixed (true) or variable (false).
	 */
	public boolean isFixed();

	/**
	 * @return get the next stimulus duration.
	 */
	public double getNextOnset();

	/**
	 * Reset this configuration to its start state.
	 */
	public void reset();

	/** Set the number of onsets required. **/
	
	public void setTrials(int trials);

	/** Return the mean for the config. **/
	
	public double getMean();

	/**
	 * Reshuffle the durations.
	 */
	public void regenerate();
}
