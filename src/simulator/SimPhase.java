
/**
 * SimPhase.java
 * 
 * Created on 10-Mar-2005
 * City University
 * BSc Computing with Distributed Systems
 * Project title: Simulating Animal Learning
 * Project supervisor: Dr. Eduardo Alonso 
 * @author Dionysios Skordoulis
 *
 * Modified in October-2009
 * The Centre for Computational and Animal Learning Research 
 * @supervisor Dr. Esther Mondragon 
 * email: e.mondragon@cal-r.org
 * @author Rocio Garcia Duran
 *
 * Modified in July-2011
 * The Centre for Computational and Animal Learning Research 
 * @supervisor Dr. Esther Mondragon 
 * email: e.mondragon@cal-r.org
 * @author Dr. Alberto Fernandez
 * email: alberto.fernandez@urjc.es
 * 
 * Modified in December-2011
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 *
 */
package simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import simulator.configurables.ContextConfig;
import simulator.configurables.ContextConfig.Context;
import simulator.configurables.ITIConfig;
import simulator.configurables.TimingConfiguration;
import extra166y.Ops;
import extra166y.Ops.DoubleOp;
import extra166y.ParallelDoubleArray;

/**
 * SimPhases is the class which models and processes a phase from the 
 * experiment. It will process the sequence of stimuli and give results
 * as requested from the controller.
 */
public class SimPhase {
	
	/** Group this phase is for. **/
	private SimGroup group;
	/** Trial sequence. **/
	private List<String> orderedSeq;
	/** Stimuli mappings. **/
	protected Map<String,SimStimulus> stimuli; 
	//Modified by J Gray, replace a cue with a CSC and hence a list of cues
	/** CSC cue maps. **/
	private Map<String,CueList> cues;
	/** Results map. **/
	protected Map<String,CueList> results; 
	
	private String initialSeq;
	/** Number of trials to run. **/
	protected int trials;
	/** Whether to use a random ordering of trials. **/
	private boolean random;
	/** TD parameters. **/
	private Double lambdaPlus, lambdaMinus, betaPlus, betaMinus, gamma, delta;
	/** The current prediction of the US. **/
	protected double prediction;
	/** The previous prediction of the US. **/
	protected double lastPrediction;
	/** Counter for maximum trial duration. **/
	protected int maxMaxOnset;
	/** ITI configuration. **/
	protected ITIConfig itis;
	/** Salience of contextual stimulus **/
	protected double bgSalience;
	/** Timing configuration. **/
	private TimingConfiguration timingConfig;
	/** Context configuration. **/
	private ContextConfig contextCfg;
	 
	/** Operation for adding together two double arrays. **/
	final Ops.BinaryDoubleOp addWeights = new Ops.BinaryDoubleOp() {
  		public double op(final double first, final double second) {
  			return first + second;
  	}};
  	
	/** Message passing object to update progress in the GUI & fast-cancel the sim. **/
	private volatile ModelControl control;
	
	
	/**
	 * SimPhase's Constructor method
	 * @param seq the sequence as it has been given by the user.
	 * @param order the same sequence processed into an ArrayList.
	 * @param stimuli2 the stimuli that the sequence contains.
	 * @param sg the parent group that this phase is belong to.
	 * @param random if the phase must be executed in a random way
	 * @param onsetConfig onsets configuration object
	 */
	public SimPhase(String seq, List<String> order, Map<String, SimStimulus> stimuli2, SimGroup sg, boolean random,
			TimingConfiguration timing, ITIConfig iti, ContextConfig context) {
	    
	    results = new TreeMap<String,CueList>();
	    initialSeq = seq;
	    stimuli = stimuli2;
	    orderedSeq = order;
	    group = sg;
	    this.random = random;
	    this.trials = orderedSeq.size();
	    this.cues = group.getCuesMap();
	    for(Entry<String, CueList> entry : group.getCuesMap().entrySet()) {
	    	if(seq.contains(entry.getKey())) {
	    		cues.put(entry.getKey(), entry.getValue());
	    	}
	    }	    
	    lambdaPlus = 0.0;
	    betaPlus = 0.0;
	    lambdaMinus = 0.0;
	    betaMinus = 0.0;
	    delta = 0.0;
	    bgSalience = 0.0;
	    //Added to allow for variable distributions of onsets - J Gray
	    //Added to control the onset of fixed onset stimulu
	    timingConfig = timing;
	    timingConfig.setTrials(trials);
	    iti.setTrials(trials);
	    lastPrediction = prediction = 0.0f;
	    maxMaxOnset = 0;
	    //Added to allow ITI modelling.
	    itis = iti;
	    //progress = 0;
	    //Added to make use of contexts per phase/group
	    contextCfg = context;
	}
	  	
  	/**
	 * The TD algorithm.
	 * @param sequence list of trial strings in order
	 * @param tempRes Map to populate with results
	 */
	
	protected void algorithm(List<String> sequence, Map<String, CueList> tempRes, boolean context) {
		//Map cues to iterators of cues.
		Map<Character, CueList> tempMap;
		HashSet<String> uniqSeq = new HashSet<String>();
		uniqSeq.addAll(sequence);
		if(context) {
			//Set the alpha on the context we're using here
			tempRes.get(contextCfg.getContext().toString()).setAlpha(contextCfg.getAlpha());
		}
		List<SimCue> activeList = new ArrayList<SimCue>();
		for (int i = 1; i <=trials && !control.isCancelled(); i++) {
			lastPrediction = 0d;//new Double(0);
			String curNameSt = (String) sequence.get(i-1);
			SimStimulus currentSt = stimuli.get(curNameSt);
			tempMap = new HashMap<Character, CueList>();
			
			//Get the stimuli present this trial, copy them to the temporary map
			//and collect their onsets.
			for (int y = 0; y < curNameSt.length() - 1; y ++) { 
				Character curCue = Character.valueOf(curNameSt.charAt(y));
				tempRes.get(curCue+"").restart();
				tempMap.put(curCue, tempRes.get(curCue+""));
			}

			int iti = (int) Math.round(itis.next()/Simulator.getController().getModel().getTimestepSize());
			
			
			//Produce actual timings for this trial
			Map<String, int[]> timings = timingConfig.makeTimings(tempMap.keySet());
			int trialLength = timings.get("Total")[1];
			
			if(i <= trials) {
				maxMaxOnset = Math.max(maxMaxOnset, timings.get("CS_Total")[1]);
			}
			
			
			
			//Run through all the timesteps, duration of the trial is the total period
			//returned by the timings generator.
			for(int j = 0; j < (trialLength + iti) && !control.isCancelled(); j++) {
				activeList.clear();
				//Ready to update prediction
				prediction = 0d;//new Double(0);
				if(context) {
					if(timings.get(Simulator.OMEGA+"")[1]-1 == tempMap.get(contextCfg.getSymbol().charAt(0)).getIndex()) {
						tempMap.get(contextCfg.getSymbol().charAt(0)).restart();
					}
				}
				//Update each stimulus
				for (int y = 0; y < curNameSt.length()-1; y ++) { 
					Character curCue = Character.valueOf(curNameSt.charAt(y));
					String timingKey = Context.isContext(curCue+"") ? Simulator.OMEGA + "" : curCue.toString();
					//Timing range for this CS
					int onset = timings.get(timingKey)[0];
					int offset = timings.get(timingKey)[1];
					//Check if this stimulus is present right now, only update it
					//and allow it to contribute to the prediction if it is.
					if(timingKey.equals(Simulator.OMEGA + "") || 
							(onset <= j && j < offset)) {
							
							//Lazily expand if we need to grow the CSC
							SimCue active = tempMap.get(curCue).nextCue();
							//Start eligibility trace for the active component
							active.setActive(true);
							activeList.add(active);
							//Update predictions
							//Prediction that the active components are making
							prediction += active.getLastAssocValue();
							//tempMap.get(curCue).restart();
							
					}
				}
				
				//Make sure predictions are at least 0
				prediction = Math.max(prediction, 0);
				lastPrediction = Math.max(lastPrediction, 0);
				
				//Construct beta error term
				double betaError = getGamma()*prediction - lastPrediction;
				//System.out.println(betaError);
				//Is the US active right now?
				boolean usOn = ((timings.get("US")[0]) <= j && j < (timings.get("US")[1]));
				//Reinforce during US duration
				betaError += usOn ? (currentSt.isReinforced() ? getLambdaPlus() : 0) : 0;
				
				//Multiply by learning rate
				betaError *= currentSt.isReinforced() ? getBetaPlus() : getBetaMinus();
				//Update the cues
				updateCues(betaError, tempRes, tempMap.keySet());
				//Merge if a compound is present
				if(curNameSt.length() > 2) {
					mergeCues(curNameSt.substring(0, curNameSt.length()-1), tempRes, activeList);
				}
				//Update prediction
				lastPrediction = prediction;
			}
			//Store new prediction at the end of each trial
			store(tempRes, currentSt);
		}
	}
	
	/**
	 * Merge cues together to form a compound cue.
	 * @param compoundName name of the compound
	 * @param tempRes working results map
	 * @param active list of active components
	 */
	
	protected void mergeCues(String compoundName, Map<String, CueList> tempRes, List<SimCue> active) {
		CueList compound = tempRes.containsKey(compoundName) ? tempRes.get(compoundName) : 
			new CompoundCueList(compoundName, 0d, group.getModel().getThreshold());
		
		if(active.size() == compoundName.length()) {
		SimCue cue = compound.nextCue();
		double totalAssoc = 0;
		for(SimCue tmp : active) {
			totalAssoc += tmp.getLastAssocValue();
		}
		cue.setAssocValue(totalAssoc);
		
		tempRes.put(compoundName, compound);
		}
	}
	
	

	/**
	 * Update cues according to TD algorithm.
	 * @param betaError beta error term
	 * @param tempRes results map
	 * @param cues set of cues to update
	 */
	
	protected void updateCues(double betaError, Map<String, CueList> tempRes,
			Set<Character> cues) {
		//For each stimulus in the trial
		for(Character updating : cues) {		
			//For each component of the stimulus
			tempRes.get(updating.toString()).update(betaError, delta, gamma);
		}
	}
	
	/**
	 * Store the cues' values for this trial.
	 * @param tempRes
	 * @param current
	 */
	
	protected void store(Map<String, CueList> tempRes, SimStimulus current) {
		for(CueList cue : tempRes.values()) {
			if(cue.getSymbol().length() > 1 && current.getCueNames().equals(cue.getSymbol())) {
				cue.store();
				cue.restart();
			}
			else if(cue.getSymbol().length() == 1 && current.getCueNames().contains(cue.getSymbol())) {
				cue.store();
				cue.restart();
			}
		}
	}
	
	/**
	 * Returns an exact TreeMap copy from the TreeMap that is been given.
	 * It iterates through it's keys and puts their values into a new object.
	 * Modified Dec-2011 to use CSC cues. J Gray
	 * @param cues2 the original TreeMap object which to copy from.
	 * @return
	 */
	protected TreeMap<String,CueList> copyKeysMapToTreeMap(Map<String, CueList> cues2) { 
	    TreeMap<String,CueList> reqMap = new TreeMap<String,CueList>(); 
	    
	    //Iterating over the elements in the set
	    Iterator<Entry<String, CueList>> it = cues2.entrySet().iterator();
	    while (it.hasNext()) {
	        // Get element
	        Entry<String, CueList> element = it.next();
	        //Skip compounds
	        if(element.getKey().length() == 1) {
	        	
	        // Alberto Fernández July-2011: removed if
	        //if (this.isCueInStimuli(element)){ // only the cues in the phase
	        	CueList currentCsc = element.getValue();
	        	CueList cscValues = currentCsc.copy();
	        	reqMap.put(element.getKey(), cscValues);
	        //}
	        }
	    }
	    return reqMap;
	}

	
	/**
	 * Empty the HashMap with the results inside. This happens in case that 
	 * the user chooses to keep the same information on the phase table but 
	 * wishes to update their value.
	 *
	 */
	public void emptyResults() {
	    results.clear();
	}
	
	/**
     * Returns the phase's 'beta' value which represents the non-reinforced
     * stimuli.
     * @return a 'beta' value for the non-reinforced stimuli.
     */
	public Double getBetaMinus() {
		return betaMinus;
	}

	/**
     * Returns the phase's 'beta' value which represents the reinforced
     * stimuli.
     * @return a 'beta' value for the reinforced stimuli.
     */
	public Double getBetaPlus() {
		return betaPlus;
	}
	
	public double getContextSalience() {
		return bgSalience;
	}
	
	/**
	 * @return the delta
	 */
	public Double getDelta() {
		return delta;
	}
	
	public Double getGamma() {
		return gamma;
	}
	
	/**
	 * @return the itis
	 */
	public ITIConfig getITI() {
		return itis;
	}
	
	/**
     * Returns the phase's 'lambda' value which represents the non-reinforced
     * stimuli.
     * @return a 'lambda' value for the non-reinforced stimuli.
     */
	public Double getLambdaMinus() {
		return lambdaMinus;
	}
	
	/**
     * Returns the phase's 'lambda' value which represents the reinforced
     * stimuli.
     * @return a 'lambda' value for the reinforced stimuli.
     */
	public Double getLambdaPlus() {
		return lambdaPlus;
	}

	/**
	 * Get the longest duration of all the trials in this phase.
	 * @return an integer giving the maximum duration of the trials.
	 */
	
	public int getMaxDuration() {
		return maxMaxOnset;
	}

	/**
	 * Returns the total number of trials that this phase contains.
	 * @return the number of trials.
	 */
	public int getNoTrials() {
	    return trials;
	}
	
	/**
	 * Returns the results into a HashMap containing the cues that are
	 * participate in this phase or in the other group's phase's (their value
	 * remain the same) with their associative strengths.
	 * Modified to return a CSC cue-list. J Gray
	 * @return the results from the algorithms process.
	 */
	public Map<String, CueList> getResults() { 
	    return results;
	}

	/**
	 * Returns the results into a HashMap containing the stimuli that are
	 * participate in this phase or in the other group's phase's (their value
	 * remain the same) with their associative strengths.
	 * @return the stimuli of the phase.
	 */
	public Map<String,SimStimulus> getStimuli() {
	    return stimuli;
	}

	/**
	 * Returns the initial sequence that was entered by the user on the 
	 * phases table.
	 * @return the initial sequence.
	 */
	public String intialSequence() {
	    return initialSeq;
	}
	
	/**
	 * Returns true if the cue is in the stimuli of the phase.
	 * (Returns true if the cue is taking part in the phase)
     * @param cue the cue looked for
 	 * @return if the cue is taking part in the current phase
	 */
	public boolean isCueInStimuli(String cue){
	 	   boolean is = false;
	 	   // looking for the cue in the stimuli
	 	   Set<String> keys = stimuli.keySet();
	 	   for (String key: keys) {
	 		   	if (cue.length()>1){ //cue is a compound --> check with the complete name
	 		   		if(cue.startsWith(Simulator.OMEGA+"")) {
	 		   			cue = cue.substring(1);
	 		   		}
	 		   		if (stimuli.get(key).getCueNames().equals(cue)) {
	 		   			is = true; 
	 		   			break;
	 		   		}
	 		   	}else if (key.indexOf(cue)!=-1) { // cue is a simple one --> check if cue is inside the stimuli
	    				is = true; 
	    				break;
	    		}
	 	   }
	 	   if(cue.equals(Simulator.OMEGA+"")) is = true;
	 	   return is;
	}
	
	/**
	 * Return if the phase will be randomly executed
	 * @return
	 */
	public boolean isRandom() {
		return random;
	}
	
	/**
	 * Run a shuffled set of variable distributions.
	 * @param sequence
	 * @param res
	 * @param context
	 */
	
	public void runRandom(List<String> sequence, Map<String, CueList> res, boolean context) {
		Map<String, Integer> trialCounts = new HashMap<String, Integer>();
		Map<String, List<Integer>> maxCues = new HashMap<String,List<Integer>>();
    	// Alberto Fernández July-2011
    	//J Gray Dec-2011
		Ops.DoubleOp divide = new Ops.DoubleOp() {
			
			@Override
			public double op(double value) {
				return value/group.getModel().getVariableCombinationNo();
			}
		};    	
        // Shuffle process
        TreeMap<String, List<ParallelDoubleArray[]>> avgResult = new TreeMap<String, List<ParallelDoubleArray[]>>();
        TreeMap<String, CueList> tempRes = copyKeysMapToTreeMap(cues);
        for (int i = 0; i < group.getModel().getVariableCombinationNo() && !control.isCancelled(); i++) {
        	// Copies an exact copy of the result treemap and 
        	// runs the algorithm using this temporarily copy.
        	tempRes = copyKeysMapToTreeMap(cues);
        	//Time one cycle
        	long count = System.currentTimeMillis();
        	//Run the algorithm for this sequence
        	algorithm(sequence, tempRes, context);
        	control.setEstimatedCycleTime(System.currentTimeMillis()-count);
        	//Reshuffle onset sequence
        	timingConfig.regenerate();
        	itis.reset();
        	
        	//Add the results to the averaged equivalent weights
        	runningRandomTotal(tempRes, avgResult, trialCounts, divide,maxCues);
        	control.incrementProgress(1);//(100d/(group.noOfCombin()+1))/group.getNoOfPhases());
        }	
        if(control.isCancelled()) {
        	return;
        }
        //Reconstitute the lists of weights to an appropriate cue object
        reconstitute(res, avgResult, trialCounts,maxCues);
	}
	
	/**
	 * This starts the execution of the algorithm. The method first checks
	 * if the sequence has to be executed in random order and then executes
	 * the same algorithm but in different execution style. If the sequence
	 * is random, creates a tempSequence from the original and runs a simple
	 * shuffle method. The shuffle methods use a random generator which 
	 * provides a number from 0 to the end of the sequences length. Then swaps
	 * position with the previous number - position. Finally it calls the 
	 * algorithm. The previous task is running iterative depending the
	 * number of combinations that the user has chosen. If the sequence
	 * is not supposed to run in random order it skips this step and goes
	 * straight to the algorithm.
	 *
	 */
	public void runSimulator() {
	    results = copyKeysMapToTreeMap(cues); 
	    boolean context = group.getModel().isUseContext();
	    // Sequence is running randomly
	    if (isRandom()) {
	    	Map<String, Integer> trialCounts = new HashMap<String, Integer>();
	    	Map<String, List<Integer>> maxCues = new HashMap<String,List<Integer>>();
        	// Alberto Fernández July-2011
	    	//J Gray Dec-2011
        	
        	// end changes July-2011
        	
	    	Ops.DoubleOp divide = new Ops.DoubleOp() {
				
				@Override
				public double op(double value) {
					return value/group.noOfCombin();
				}
			}; 
	    	
	    	
	        // Shuffle process
	        Random generator = new Random();
	        TreeMap<String, List<ParallelDoubleArray[]>> avgResult = new TreeMap<String, List<ParallelDoubleArray[]>>();
	        TreeMap<String, CueList> tempRes = copyKeysMapToTreeMap(cues);
	        for (int i = 0; i < group.noOfCombin() && !control.isCancelled(); i++) {
	        	List<String> tempSeq = orderedSeq;
	        	int n;
	        	for (int x = 0; x < trials && orderedSeq.size()>1; x++) {
	        		n = generator.nextInt(orderedSeq.size()-1);
	        		String swap = (String)tempSeq.get(x);
	        		tempSeq.remove(x);
	        		tempSeq.add(n, swap);
	        	}
	        	// Copies an exact copy of the result treemap and 
	        	// runs the algorithm using this temporarily copy.
	        	tempRes = copyKeysMapToTreeMap(cues);
	        	
	        	//Run the algorithm for this sequence
	        	if(timingConfig.hasVariableDurations()) {
		    		runRandom(tempSeq,tempRes, context);
		    	} else {
		    		long count = System.currentTimeMillis();
		    		algorithm(tempSeq,tempRes, context); // Alberto Fern·ndez July-2011
		    		control.incrementProgress(1);
		    		control.setEstimatedCycleTime(System.currentTimeMillis()-count);
		    	}
	        	
	        	
	        	//Reset onset sequence
	        	timingConfig.reset();
	        	itis.reset();
	        	
	        	//Add the results to the averaged equivalent weights
	        	runningRandomTotal(tempRes, avgResult, trialCounts, divide,maxCues);
	        }	
	        if(control.isCancelled()) {
	        	return;
	        }
	        //Reconstitute the lists of weights to an appropriate cue object
	        results = new TreeMap<String, CueList>();
	        reconstitute(results, avgResult, trialCounts, maxCues);
	        //control.incrementProgress(1);//(100d/(group.noOfCombin()+1))/group.getNoOfPhases());	        
	    }
	    // A standard sequence
	    else {
	    	if(timingConfig.hasVariableDurations()) {
	    		runRandom(orderedSeq,results, context);
	    	} else {
	    		long count = System.currentTimeMillis();
	    		algorithm(orderedSeq,results, context); // Alberto Fern·ndez July-2011
	    		control.setEstimatedCycleTime(System.currentTimeMillis()-count);
	    	}
	    	control.incrementProgress(1);//00d/group.getNoOfPhases());
	    }
	    cues.putAll(results);
	}
	
	/**
	 * Helper function for random runs. Reconstitutes a map of lists of weights and a map of trial counts
	 * into a map of averaged cues.
	 * @param tempRes Map to return results into
	 * @param avgResult  Map of cue names -> lists of weights
	 * @param trialCounts Map of trial counts for cscs.
	 */
	
	private void reconstitute(Map<String, CueList> tempRes, TreeMap<String, List<ParallelDoubleArray[]>> avgResult,
			Map<String, Integer> trialCounts,Map<String, List<Integer>> maxCues) {
		for(Entry<String, List<ParallelDoubleArray[]>> entry : avgResult.entrySet()) {
        	CueList tmpCueList = entry.getKey().length() == 1 ? new CueList(entry.getKey(),cues.get(entry.getKey()).getAlpha(), group.getModel().getThreshold()) :
        		new CompoundCueList(entry.getKey(), 0d,group.getModel().getThreshold());
        	if(Context.isContext(tmpCueList.getSymbol())) {
        		tmpCueList.setAlpha(contextCfg.getAlpha());
        	}
        	tmpCueList.setAverageWeights(entry.getValue().get(0)[0].asList());
        	tmpCueList.setAverageResponse(entry.getValue().get(0)[1].asList());
        	tmpCueList.setTrialCount(trialCounts.get(entry.getKey()));
        	tmpCueList.setMaxCueList(maxCues.get(entry.getKey()));
        	for(int i = 2; i < entry.getValue().size(); i++) {
        		SimCue tmpCue;
        		tmpCue = entry.getKey().length() == 1 ? new SimCue(entry.getKey(), cues.get(entry.getKey()).getAlpha()) : new SimCue(entry.getKey(), 0d);
        		tmpCue.setAssocValueVector(entry.getValue().get(i)[0].asList());
        		tmpCue.setResponses(entry.getValue().get(i)[1].asList());
        		tmpCueList.add(tmpCue);
        		tmpCueList.setSymbol(entry.getKey());
        	}
        	if(!tmpCueList.isEmpty()) {
        		tempRes.put(entry.getKey(), tmpCueList);
        	}
        	//entry.getValue().clear();
		}
		//avgResult.clear();
	}
	
	/**
	 * Helper function for random runs. Maintains a running total of weights and trial counts, averaged at
	 * each step.
	 * @param tempRes
	 * @param avgResult
	 * @param trialCounts
	 * @param divide 
	 */
	
	private void runningRandomTotal(TreeMap<String, CueList> tempRes, TreeMap<String, List<ParallelDoubleArray[]>> avgResult,
			Map<String, Integer> trialCounts, DoubleOp divide, Map<String, List<Integer>> maxCues) {
		ParallelDoubleArray[] avg;
		//Add the results to the averaged equivalent weights
    	for(Entry<String, CueList> cues : tempRes.entrySet()) {
    		List<ParallelDoubleArray[]> avgList = avgResult.get(cues.getKey());
    		//Create a list for this cue if it is missing
			if(avgList == null) {
				avgList = new ArrayList<ParallelDoubleArray[]>();
				avgResult.put(cues.getKey(), avgList);
			}
			ParallelDoubleArray weights;
			weights = ParallelDoubleArray.createEmpty(cues.getValue().getAverageWeights().size(),Simulator.fjPool);
			//Average weights first
			weights.asList().addAll(cues.getValue().getAverageWeights());
			try {
				avg = avgList.get(0);
			} catch (IndexOutOfBoundsException e) {
				avg = new ParallelDoubleArray[2];
				avg[0] = ParallelDoubleArray.createEmpty(cues.getValue().getAverageWeights().size(),Simulator.fjPool);
				avgResult.get(cues.getValue().getSymbol()).add(avg);
			}
			avg[0].setLimit(weights.asList().size());
			avg[0].replaceWithMapping(addWeights, weights.replaceWithMapping(divide));
			//Average responses next
			weights.asList().clear();
			weights.asList().addAll(cues.getValue().getAverageResponse());
			try {
				avg = avgList.get(1);
			} catch (IndexOutOfBoundsException e) {
				avg[1] = ParallelDoubleArray.createEmpty(cues.getValue().getAverageWeights().size(),Simulator.fjPool);
				avgResult.get(cues.getValue().getSymbol()).add(avg);
			}
			avg[1].setLimit(weights.asList().size());
			avg[1].replaceWithMapping(addWeights, weights.replaceWithMapping(divide));
			//Trial counts
			trialCounts.put(cues.getKey(), cues.getValue().getTrialCount());
			//Max components for each trial.
			List<Integer> totalMax = new ArrayList<Integer>();
			if(maxCues.containsKey(cues.getKey())) {
				for(int i = 0; i < cues.getValue().getMaxCueList().size(); i++) {
					totalMax.add(
						Math.max(maxCues.get(cues.getKey()).get(i), cues.getValue().getMaxCueList().get(i)));
				}
				maxCues.put(cues.getKey(), totalMax);
			} else {
				maxCues.put(cues.getKey(), cues.getValue().getMaxCueList());
			}
			
			
			//Iterate through all the components of the cue and add their weight vectors
			//to the average vector, expanding it if necessary
    		for (int p = 0; p < cues.getValue().size() && !cues.getValue().get(p).getAssocValueVector().isEmpty(); p++) {
    			SimCue cue = cues.getValue().get(p);
    			
    			try {
    				avg = avgList.get(p+2);
    			} catch (IndexOutOfBoundsException e) {
    				avg = new ParallelDoubleArray[2];
    				avg[0] = ParallelDoubleArray.createEmpty(cue.getAssocValueVector().size(),Simulator.fjPool);
    				avg[1] = ParallelDoubleArray.createEmpty(cue.getResponses().size(),Simulator.fjPool);
    				avgResult.get(cue.getSymbol()).add(avg);
    			}
    			//Weights first
    			weights = ParallelDoubleArray.createEmpty(cue.getAssocValueVector().size(),Simulator.fjPool);
    			weights.asList().addAll(cue.getAssocValueVector());
    			avg[0].setLimit(weights.asList().size());
    			avg[0].replaceWithMapping(addWeights, weights.replaceWithMapping(divide));
    			//Responses second
    			weights = ParallelDoubleArray.createEmpty(cue.getResponses().size(),Simulator.fjPool);
    			weights.asList().addAll(cue.getResponses());
    			avg[1].setLimit(weights.asList().size());
    			avg[1].replaceWithMapping(addWeights, weights.replaceWithMapping(divide));	
    		}
    	}
	}
	
	/**
     * Sets the phase's 'beta' value which represents the non-reinforced
     * stimuli.
     * @param l 'beta' value for the non-reinforced stimuli.
     */
	public void setBetaMinus(Double l) {
		betaMinus = l;
	}
	
	/**
     * Sets the phase's 'beta' value which represents the reinforced
     * stimuli.
     * @param l 'beta' value for the reinforced stimuli.
     */
	public void setBetaPlus(Double l) {
		betaPlus = l;
	}

	
	public void setContextSalience(double salience) {
		bgSalience = salience;
	}


	/**
	 * @param delta the delta to set
	 */
	public void setDelta(Double delta) {
		this.delta = delta;
	}


	/**
     * Sets the phase's 'gamma' value which represents the discount factor.
     * @param
     */
	public void setGamma(Double g) {
		gamma = g;
	}

	/**
	 * @param itis the itis to set
	 */
	public void setITI(ITIConfig itis) {
		this.itis = itis;
	}


	/**
     * Sets the phase's 'lambda' value which represents the non-reinforced
     * stimuli.
     * @param l 'lambda' value for the non-reinforced stimuli.
     */
	public void setLambdaMinus(Double l) {
		lambdaMinus = l;
	}
	
	/**
     * Sets the phase's 'lambda' value which represents the reinforced
     * stimuli.
     * @param l 'lambda' value for the reinforced stimuli.
     */
	public void setLambdaPlus(Double l) {
		lambdaPlus = l;
	}
	
	/**
	 * Set the random attribute for this phase
	 * @param random
	 */
	public void setRandom(boolean random) {
		this.random = random;
	}
	
	/**
	 * @return the timing configuration for this phase
	 */
	public TimingConfiguration getTimingConfiguration() {
		return timingConfig;
	}

	/**
	 * @param timings set the timing configuration for this phase
	 */
	public void setUsLength(TimingConfiguration timings) {
		timingConfig = timings;
	}

	/**
	 * @return the timingConfig
	 */
	public TimingConfiguration getTimingConfig() {
		return timingConfig;
	}

	/**
	 * @param timingConfig the timingConfig to set
	 */
	public void setTimingConfig(TimingConfiguration timingConfig) {
		this.timingConfig = timingConfig;
	}

	/**
	 * @return the cues
	 */
	public Map<String,CueList> getCues() {
		return cues;
	}

	/**
	 * @param cues the cues to set
	 */
	public void setCues(Map<String,CueList> cues) {
		this.cues = cues;
	}

	/**
	 * @return the group
	 */
	public SimGroup getGroup() {
		return group;
	}

	/**
	 * @param group the group to set
	 */
	public void setGroup(SimGroup group) {
		this.group = group;
	}

	/**
	 * @return the orderedSeq
	 */
	public List<String> getOrderedSeq() {
		return orderedSeq;
	}

	/**
	 * @param orderedSeq the orderedSeq to set
	 */
	public void setOrderedSeq(List<String> orderedSeq) {
		this.orderedSeq = orderedSeq;
	}

	/**
	 * @return the contextCfg
	 */
	public ContextConfig getContextConfig() {
		return contextCfg;
	}

	/**
	 * @param contextCfg the contextCfg to set
	 */
	public void setContextConfig(ContextConfig contextCfg) {
		this.contextCfg = contextCfg;
	}

	/**
	 * @param control
	 */
	public void setControl(ModelControl control) {
		this.control =  control;
	}
	
	
}
