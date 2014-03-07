package simulator.configurables;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import simulator.Simulator;
import simulator.configurables.ContextConfig.Context;
import simulator.configurables.USConfiguration.Relationship;

/**
 * TimingConfiguration.java
 * 
 * Created on Jan-2012
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 *
 */

public class TimingConfiguration implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6434454881295769569L;
	/** mapping from cues to us configuration.**/
	private Map<String, USConfiguration> relations;
	/** Default cue -> us configs. **/
	private static Map<String, USConfiguration> defaultRelations = new HashMap<String, USConfiguration>();
	/** Onset config **/
	private OnsetConfig durations;
	/** Default onsets. **/
	private static OnsetConfig defaultDurations = new OnsetConfig();
	/** US Duration. **/
	private double usDuration;
	private boolean configured;
	private boolean isReinforced;
	
	
	public TimingConfiguration(double usDuration) {
		this.usDuration = usDuration;
		relations = new HashMap<String, USConfiguration>();
		durations = new OnsetConfig();
		configured = false;
		isReinforced = true;
	}
	
	public TimingConfiguration() {
		this(1);
	}

	public void addOnset(String stimulus, float mean, float sd, long seed) {
		durations.set(stimulus, mean, sd, seed);
	}
	
	public void addOnset(String stimulus, int onset) {
		durations.set(stimulus, onset);
	}
	
	/**
	 * 
	 * @param cue name to get relationship for.
	 * @return the us relationship for this cue.
	 */
	
	public USConfiguration getRelation(String cue) {
		return relations.get(cue);
	}
	
	/**
	 * Add a US timing relation.
	 * @param cue
	 * @param relation
	 */
	
	public void addUSConfig(String cue, USConfiguration relation) {
		relations.put(cue, relation);
	}
	
	public void addUSConfig(String cue,Relationship type, int offset) {
		relations.put(cue, new USConfiguration(type, offset));
	}
	
	/**
	 * @return the relations
	 */
	public Map<String, USConfiguration> getRelations() {
		return relations;
	}
	/**
	 * @param relations the relations to set
	 */
	public void setRelations(Map<String, USConfiguration> relations) {
		this.relations = relations;
		for(Entry<String, USConfiguration> entry : relations.entrySet()) {
			if(!defaultRelations.containsKey(entry.getKey())) {
				defaultRelations.put(entry.getKey(), entry.getValue());
			}
		}
	}
	/**
	 * @return the durations
	 */
	public OnsetConfig getDurations() {
		return durations;
	}
	/**
	 * @param durations the durations to set
	 */
	public void setDurations(OnsetConfig durations) {
		this.durations = durations;
	}

	
	public double getUsDuration() {
		return usDuration;
	}
	
	/**
	 * @param usDuration the usDuration to set
	 */
	public void setUsDuration(double usDuration) {
		this.usDuration = usDuration;
	}

	public void setTrials(int trials) {
		durations.setTrials(trials);
	}

	public Double next(String cue) {
		return durations.next(cue);
	}

	public void reset() {
		durations.reset();
	}
	
	/**
	 * Produce a map of timings for stimuli & US given the set of constraints on
	 * CS relationships to US.
	 * @param set Cues to produce timings for.
	 * @return a map of cues -> start/end timestep.
	 */
	
	public Map<String, int[]> makeTimings(Set<Character> set) {
		Map<String, int[]> timings = new HashMap<String, int[]>();
		
		double multiplier = Simulator.getController().getModel().getTimestepSize();
		
		//Relative positions about t0
		double start;
		double end;
		double minStart = 0;
		double maxStart = Double.NEGATIVE_INFINITY;
		double maxEnd = Double.NEGATIVE_INFINITY;
		double minEnd = Double.POSITIVE_INFINITY;
		for(Character cue : set) {
			//Non-configurals only
			if(Character.isUpperCase(cue) && !Context.isContext(cue+"")) {
				USConfiguration relation = relations.get(cue + "");
				switch(relation.getType()) {
				case FORWARD:
					end = durations.getMean(cue +"") - relation.getOffset();
					start = end - durations.next(cue +"");
					break;
				case BACKWARD:
					start = relation.getOffset() + getUsDuration();
					end = start + durations.next(cue +"");
					break;
				case SIMULTANEOUS:
				default:
					start = -relation.getOffset();
					end = start + durations.next(cue +"");
					break;
				}
				start = Math.round(start/multiplier);
				end = Math.round(end/multiplier);
				timings.put(cue + "", new int[]{(int) start, (int) end});
				minStart = Math.min(start, minStart);
				maxStart = Math.max(start, maxStart);
				maxEnd = Math.max(maxEnd, end);
				minEnd = Math.min(minEnd, end);
			}
		}
		double meanOfMeans = 0;
		int count = 0;
		for(Character lookup : set) {
			try {
				meanOfMeans += durations.getMean(lookup + "");
				count++;
			} catch (NullPointerException e) {
				//Ignore omega having no duration.
			}
		}
		meanOfMeans /= count;
		meanOfMeans = Math.round(meanOfMeans/multiplier);
		//Configurals
		for(Character cue : set) {
			if (Character.isLowerCase(cue)) {
				timings.put(cue + "", new int[]{(int) maxStart, (int) minEnd});
			}
		}
				
		//Shift so to start from t0
		for(int[] timing : timings.values()) {
			timing[0] += -minStart;
			timing[1] += -minStart;
		}
		//Add total cs length
		timings.put("CS_Total", new int[]{0,(int) (-minStart+maxEnd)});
		maxEnd = Math.max(getUsDuration(), maxEnd);
		timings.put(Simulator.OMEGA+"", new int[]{0, (int) meanOfMeans});
		//Add US timings
		timings.put("US", new int[]{(int) -minStart, (int) Math.round(-minStart+(getUsDuration()/multiplier))});
		//Add total length
		timings.put("Total", new int[]{0,(int) (-minStart+maxEnd)});
		
		return timings;
	}

	/**
	 * Add list of stimuli with default configs, removes any stimuli
	 * not in the list.
	 * @param stimuli
	 */
	
	public void setStimuli(Collection<String> stimuli) {
		Map<String, USConfiguration> tmpRelation = new HashMap<String, USConfiguration>(stimuli.size());
		for(String stimulus : stimuli) {
			if(relations.containsKey(stimulus)) {
				tmpRelation.put(stimulus, relations.get(stimulus));
			} else {
				USConfiguration relation = new USConfiguration();
				if(defaultRelations.containsKey(stimulus)) {
					relation.setType(defaultRelations.get(stimulus).getType());
					relation.setOffset(defaultRelations.get(stimulus).getOffset());
				}
				tmpRelation.put(stimulus,relation);
			}
		}
		durations.setStimuli(stimuli);
		relations = tmpRelation;
		configured = true;
	}

	public boolean isConfigured() {
		return configured;
	}
	
	public boolean hasZeroDurations() {
		return durations.hasZeroDurations();
	}

	public void setConfigured(boolean b) {
		configured = b;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		if(usDuration > 0) {
			sb.append("US(");
			sb.append(usDuration);
			sb.append(") ");
		}
		for(Entry<String, StimulusOnsetConfig> entry : durations.getMap().entrySet()) {
			sb.append(entry.getKey());
			sb.append(':');
			sb.append(entry.getValue());
			sb.append(',');
			sb.append(relations.get(entry.getKey()));
			sb.append(' ');
		}
		return sb.toString();
	}

	/**
	 * Takes a phase string and checks to see if this configuration covers
	 * all the cues in it. Sets it as unconfigured if the check fails.
	 * 
	 * @param Phase string to check against
	 */
	public void checkFilled(String trials) {
		for(char c : trials.toCharArray()) {
			if(Character.isLetter(c) && !relations.containsKey(c+"")) {
				configured = false;
				break;
			}
		}
	}
	
	/**
	 * Clear accumulated default settings.
	 */
	
	public static void clearDefaults() {
		defaultRelations.clear();
		OnsetConfig.clearDefaults();
	}
	
	public boolean isReinforced() {
		return isReinforced;
	}
	
	public void setReinforced(boolean reinforced) {
		if(reinforced && !isReinforced) {
			usDuration = Simulator.getController().getModel().getTimestepSize();
		}
		isReinforced = reinforced;
		usDuration = isReinforced ? usDuration : 0;
	}
	
	/**
	 * Regenerate the timing configurations, producing new random duration sequences
	 * for variable cues.
	 */
	
	public void regenerate() {
		durations.regenerate();
		reset();
	}

	/**
	 * @return
	 */
	public boolean hasVariableDurations() {
		return durations.hasRandomDurations();
	}
}
