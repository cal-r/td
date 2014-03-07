package simulator.configurables;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import simulator.util.Distributions;

/**
 * OnsetConfig.java
 * 
 * Class to store a collection of duration configurations for stimuli.
 * 
 * Created on 01-Dec-2011
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 *
 */

public class OnsetConfig implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4792950062566036011L;
	/** Mapping from stimulus name to a duration configuration. **/
	private Map<String, StimulusOnsetConfig> onsets;
	/** Defaults mapping. **/
	private static Map<String, StimulusOnsetConfig> defaultOnsets = new HashMap<String, StimulusOnsetConfig>();
	/** Indicator that this has been configured. **/
	private boolean set;
	/** Mean type. **/
	private boolean isGeo;
	/** Distribution type. **/
	private int type;
	private boolean hasRandom;
	
	public OnsetConfig() {
		onsets = new TreeMap<String, StimulusOnsetConfig>();
		set = false;
		isGeo = false;
		type = Distributions.EXPONENTIAL;
	}
	
	/**
	 * Set a new duration configuration for a stimulus.
	 * @param stimulus The stimulus this configuration applies to.
	 * @param config the configuration object.
	 */
	
	public void set(String stimulus, StimulusOnsetConfig config) {
		onsets.put(stimulus, config);
		if(!defaultOnsets.containsKey(stimulus) && config.getMean() > 0) {
			defaultOnsets.put(stimulus, config);
		}
	}
	
	/**
	 * Set a new fixed duration configuration for a stimulus.
	 * @param stimulus The stimulus this configuration applies to.
	 * @param onset Duration in seconds.
	 */
	
	public void set(String stimulus, int onset) {
		onsets.put(stimulus, new FixedOnsetConfig(onset));
	}
	
	/**
	 * Set a new variable duration configuration for a stimulus.
	 * @param stimulus The stimulus this configuration applies to.
	 * @param mean Mean for the variable distribution.
	 * @param sd Standard deviation for the variable distribution.
	 * @param seed Seed for the random number generator.
	 */
	
	public void set(String stimulus, float mean, float sd, long seed) {
		onsets.put(stimulus, new VariableOnsetConfig(mean, seed, 0, type, isGeo));
	}
	
	/**
	 * Check whether a particular stimulus is fixed or variable.
	 * @param stimulus
	 * @return true is the stimulus is fixed, false if variable.
	 */
	
	public boolean isFixed(String stimulus) {
		StimulusOnsetConfig onset = onsets.get(stimulus);
		return onset == null ? false : onset.isFixed();
	}
	
	/**
	 * 
	 * @return the mapping from stimulus name to duration configs.
	 */
	
	public Map<String, StimulusOnsetConfig> getMap() {
		return onsets;
	}
	
	/**
	 * Ensure that only the stimuli in the provided list have duration
	 * configurations.
	 * @param stimuli the list of stimuli to store duration configs for.
	 */
	
	public void setStimuli(Collection<String> stimuli) {
		//Prune onsets for stimuli that don't exist.
		Iterator<Entry<String, StimulusOnsetConfig>> it = onsets.entrySet().iterator();
		while(it.hasNext()) {
			if (!stimuli.contains(it.next().getKey())) {
				it.remove();
			}
		}
		for(String stimulus : stimuli) {
			if(!onsets.containsKey(stimulus)) {
				if(defaultOnsets.containsKey(stimulus)) {
					set(stimulus, defaultOnsets.get(stimulus));
				} else {
					set(stimulus, 0);
				}
			}
		}
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(String str : onsets.keySet()) {
			sb.append(str);
			sb.append(": ");
			sb.append(onsets.get(str));
			sb.append(" \n");
		}
		return sb.toString();
	}
	
	public double next(String stimulus) {
		return onsets.get(stimulus).getNextOnset();
	}

	public boolean isConfigured() {
		return set;
	}
	
	public void setConfigured(boolean set) {
		this.set = set;
	}
	
	/**
	 * Reset all onset configurations to their starting state.
	 */
	
	public void reset() {
		for(StimulusOnsetConfig onset : onsets.values()) {
			onset.reset();
		}
	}

	/**
	 * 
	 * @param trials Number of random onsets required.
	 */
	
	public void setTrials(int trials) {
		for(StimulusOnsetConfig onset : onsets.values()) {
			onset.setTrials(trials);
			onset.reset();
		}
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
		for(StimulusOnsetConfig onset : onsets.values()) {
			if(!onset.isFixed()) {
				((VariableOnsetConfig)onset).setGeometric(isGeo);
			}
		}
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
		for(Entry<String, StimulusOnsetConfig> onset : onsets.entrySet()) {
			if(!onset.getValue().isFixed()) {
				VariableOnsetConfig old = (VariableOnsetConfig) onset.getValue();
				onsets.put(onset.getKey(), new VariableOnsetConfig(old.getMean(), old.getSeed(), old.getTrials(), type, old.isGeometric()));
			}
		}
	}
	
	/**
	 * Merge another onset configuration into this one. 
	 * The type and geometric mean settings are those of this config.
	 * @param other
	 */
	
	public void merge(OnsetConfig other) {
		onsets.putAll(other.getMap());
		setType(type);
		setGeo(isGeo);
	}

	public double getMean(String cue) {
		StimulusOnsetConfig onset =  onsets.get(cue);
		return onset.isFixed() ? onset.getNextOnset() :onset.getMean();
	}

	/**
	 * @return true if there is at least one duration set to 0
	 */
	public boolean hasZeroDurations() {
		for(StimulusOnsetConfig onset : onsets.values()) {
			if(onset.getMean() == 0) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Clear accumulated default onsets.
	 */
	
	public static void clearDefaults() {
		defaultOnsets.clear();
	}

	/**
	 * 
	 */
	public void regenerate() {
		for(StimulusOnsetConfig onset : onsets.values()) {
			onset.regenerate();
		}
	}

	/**
	 * @return
	 */
	public boolean hasRandomDurations() {
		for(StimulusOnsetConfig onset : onsets.values()) {
			if(onset instanceof VariableOnsetConfig) {
				return true;
			}
		}
		return false;
	}
}
