/**
 * SimGroup.java
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
 */
package simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import simulator.configurables.ContextConfig;
import simulator.configurables.ContextConfig.Context;
import simulator.configurables.ITIConfig;
import simulator.configurables.TimingConfiguration;
import extra166y.ParallelArray;

/**
 * SimGroup is the class which models a group from the experiment. It will process 
 * any new sequences of stimuli and adds them to its ArrayList of phases. It is an
 * intermediate between the Controller and the Phase itself. It contains all the necessary
 * variables need to run a simulation and it keeps a record of the results but in a global
 * view, meaning that the results will be an extract from all phases together.
 */
public class SimGroup implements Runnable{
     
    /**
     * Adds a new phase in the group's arraylist. The stimuli sequence of the given 
     * is being processed mainly so it could be added as a new SimPhase object and
     * secondary it might produce new cues which weren't on previous phases. This 
     * new cues are added on the group's cue list as well. 
     * @param seqOfStimulus the stimuli sequence of the given phase.
     * @param boolean to know if the phase is going to be randomly executed
     * @param int the number of the current phase
     * @param boolean to know if the phase is going to use configural cues
     * @param mapping with configural compounds from their "virtual" name
     * @return true if the method completed without any errors and false otherwise.
     */
	public static String getKeyByValue(Map<String, String> configCuesNames, String value) {
	     String key = null;
	     int count = 0;
	     for (Map.Entry<String,String> entry : configCuesNames.entrySet()) {
	         if (entry.getValue().equals(value)) {
	             key = entry.getKey();
	             count++;
	         }
	     }
	     
	     return key;
	}
	
	// Alberto Fernández August-2011
    // Added boolean parameters isConfiguralCompounds and configuralCompoundsMapping.
    protected ArrayList<SimPhase> phases;      
    private Map<String, CueList> cues;
    private String nameOfGroup;
	private int noOfPhases, noOfCombinations, count;
	/** Threaded array. **/
	private ParallelArray<SimPhase> phasePool;
	/** At least one random phase indicator. **/
	private boolean hasRandom;

 
    /** The model this group belongs to. **/
	private SimModel model;
    
    /**
     * Create a group
     * @param n name of the Group
     * @param np number of phases
     * @param rn number of combinations
     * @return true if the method completed without any errors and false otherwise.
     */    
    public SimGroup(String n, int np, int rn, SimModel model) {
        nameOfGroup = n;
        noOfPhases = np;
        noOfCombinations = rn;
        count = 1;        
        cues = new TreeMap<String,CueList>();
        phases = new ArrayList<SimPhase>(noOfPhases);
        this.setModel(model);
    }
  	
  	public boolean addPhase(String seqOfStimulus, boolean isRandom, int phaseNum, 
    		                boolean isConfiguralCompounds, TreeMap<String, String> configuralCompoundsMapping,
    		               TimingConfiguration timings, ITIConfig iti, ContextConfig context) {
        

    	seqOfStimulus = seqOfStimulus.toUpperCase(); // Sequence is always stored in upper case. Alberto Fernández August-2011
    	
        List<String> order = new ArrayList<String>(50);       
        String sep = "/";
        String[] listedStimuli = seqOfStimulus.toUpperCase().split(sep);
        //CSC of cues. J Gray
        CueList cscCues;
        
        if (!cues.containsKey(context.getSymbol()) && !context.getContext().equals(Context.EMPTY)) {
	    	//Modified by J Gray to add CSC cues.
	    	cscCues = new CueList(context.getSymbol(), 0, model.getThreshold(),model.getTraceType());
			cues.put(context.getSymbol(), cscCues);
		}
        
        
        // Added by Alberto Fernández
        // Introduce "virtual" cues (lower case letters) in case of configural compounds. 
        
		int noStimuli = listedStimuli.length;
		Map<String, SimStimulus> stimuli = new HashMap<String, SimStimulus>();
		
		for (int i = 0; i < noStimuli; i++) {
		    String selStim = listedStimuli[i], repStim = "", cuesName = "", stimName = "";
		    boolean reinforced = false;
		    boolean oktrials = false, okcues = false, okreinforced = false;
		    
		    if(model.isUseContext()) {
		    	cuesName = context.getSymbol();
		    }
		    
		    String compound = "";
		    int noStimRep = 1;
		    for (int n = 0; n < selStim.length(); n++) {
		        char selChar = selStim.charAt(n);
		        
				if (Character.isDigit(selChar) && !oktrials) {
					repStim += selChar;
				}
				else if (Character.isLetter(selChar) && !okcues) {
					oktrials = true;
				    cuesName += selChar;
				    if (!cues.containsKey(selChar+"")) {
				    	//Modified by J Gray to add CSC cues.
				    	cscCues = new CueList(selChar+"", 0, model.getThreshold(),model.getTraceType());
						cues.put(selChar+"", cscCues);
					}
					compound += selChar;
				}
				else if ((selChar == '-' || selChar == '+') && !okreinforced) {
					oktrials = true; 
					okcues = true;
					reinforced = (selChar == '+');
					okreinforced = true;
					
					// Added by Alberto Fernández August-2011
					if ((model.isUseContext() || compound.length() > 1) && isConfiguralCompounds) {
						// Add configural cue as a "virtual" cue (lower case letter)
						compound = model.isUseContext() ? context.getSymbol()+compound : compound;
						String virtualCueName = getKeyByValue(configuralCompoundsMapping,compound);
						if (virtualCueName == null) {
							if (configuralCompoundsMapping.isEmpty()) {
								virtualCueName = "a";
							}
							else {
								char c = configuralCompoundsMapping.lastKey().charAt(0);
								c = (char) ((int)c + 1);
								virtualCueName = ""+c;
							}
							configuralCompoundsMapping.put(virtualCueName, compound);
						}
					    cuesName += virtualCueName;
						compound += virtualCueName;
					    if (!cues.containsKey(virtualCueName+"")) {
					    	//Modified to use CSCs. J Gray
					    	cscCues = new CueList(virtualCueName+"", 0, model.getThreshold(),model.getTraceType());
							cues.put(virtualCueName+"", cscCues);
						}
					}
				}
				else return false;
		    }
		    // Add to the cues the compound of the stimuli
		    // Alberto Fernandez Oct-2011
		    //if (!cues.containsKey(compound)) {
		    if (!compound.equals("") && !cues.containsKey(compound) && configuralCue(compound)) {
		    	//Modified to use CSCs
		    	cscCues = new CueList(compound, 0, model.getThreshold(),model.getTraceType());
		    	//cues.put(compound, cscCues);
		    }
			
		    stimName = cuesName + (reinforced ? '+' : '-');
			
			if (repStim.length() > 0) noStimRep = Integer.parseInt(repStim);
					
			if (stimuli.containsKey(stimName)) ((SimStimulus) stimuli.get(stimName)).addTrials(noStimRep);
			//Set fixed/variable after this has been parsed.
			else stimuli.put(stimName,new SimStimulus (stimName, noStimRep, cuesName, reinforced));
					
			for (int or = 0; or < noStimRep; or++) order.add(stimName);
		}	
		//Set indicator that this group has at least one randomised phase
		hasRandom = hasRandom ? hasRandom : isRandom;
		return addPhaseToList(seqOfStimulus, order, stimuli, isRandom, timings, iti, context);		
	}
  	
  	/**
  	 * Add a phase to this groups list of phases.
  	 * @param seqOfStimulus
  	 * @param order
  	 * @param stimuli
  	 * @param isRandom
  	 * @param timings
  	 * @param iti
  	 * @return
  	 */
  	
  	protected boolean addPhaseToList(String seqOfStimulus, List<String> order, Map<String, SimStimulus> stimuli,
  			boolean isRandom, TimingConfiguration timings, ITIConfig iti, ContextConfig context) {
  		return phases.add(new SimPhase(seqOfStimulus, order, stimuli, this, isRandom, timings, iti, context));
  	}
    
    /**
     * Empties every phase's results. It iterates through the phases
     * and calls the SimPhase.emptyResults() method. This method
     * cleans up the results variable.
     *
     */
    public void clearResults() {
        for (int i = 0; i < noOfPhases; i++) {
            SimPhase sp = phases.get(i);
            sp.emptyResults();
        }
        count = 1;
    }
    
    /**
	 * Returns the number of trials that have been produced so far.
	 * @return the number of trials so far.
	 */
	public int getCount() {
	    return count;
	}
    
    /**
     * Returns the TreeMap which contains the cues and their values.
     * An important object on overall group result processing.
     * @return the group's cues.
     */
    public Map<String, CueList> getCuesMap() { 
        return cues;
    }
    
    /**
	 * @return the model
	 */
	public SimModel getModel() {
		return model;
	}
    
	/**
	 * Returns the group's current name. By default shall be "Group n" where
	 * n is the position that has on the table.
	 * @return the name of the group.
	 */
	public String getNameOfGroup() {
	    return nameOfGroup;
	}
	
	/**
	 * Returns the ArrayList which contains the SimPhases, the phases that 
	 * run on this specific group.
	 * @return the group's phases.
	 */
	public List<SimPhase> getPhases() {
	    return phases;
	}
	
	/**
	 * 
	 * @return a boolean indicating that this group has at least one random phase.
	 */
	
	public boolean hasRandom() {
		return hasRandom;
	}

	/**
	 * Adds one more value to the count variable which represents
	 * the trials so far.
	 */
	public void nextCount() {
	    count++;
	}
	
	/**
     * Returns the number of combinations that shall be run if the
     * user has chosen a random sequence.
     * @return the number of combinations.
     */
    public int noOfCombin() {
		return noOfCombinations;
	}
  	
	/**
     * Returns the results from the simulation. They are included into
     * a String object. The method iterates through each phase and returns
     * the phase's results.
     * @return the group's results represented in a string.
     */
    public String phasesOutput(boolean compound, Map<String, String> configCuesNames) {
        StringBuffer result = new StringBuffer();
     
        // For all phases
        for (int i = 0; i < noOfPhases; i++) {                       
            SimPhase sp = phases.get(i);
            result.append("(Phase ").append(i+1).append(" , Seq: ").append(sp.intialSequence())
            .append(" Rand: ").append(sp.isRandom()).append(")").append("\n\n");
            
            Map<String, CueList> results = sp.getResults();

        	// Alberto Fern·ndez August-2011
            
            // Output  Cues
            Iterator<Entry<String, CueList>> iterCue = results.entrySet().iterator();
            while (iterCue.hasNext()) {
                Entry<String, CueList> pairCue = iterCue.next();
                CueList tempCscCue = pairCue.getValue();
                String cueName = tempCscCue.getSymbol();
                if(sp.isCueInStimuli(cueName) && (compound || cueName.length() == 1)) { //Don't output cues not in this phase
                	if (!configuralCue(cueName) && (cueName.startsWith(Simulator.OMEGA+"") || cueName.equals(cueName.toUpperCase()))) {
                		result.append("Cue : ").append(cueName);
                	} 	else if (cueName.length() == 1) {
                		String compoundName, interfaceName;
                		// configural cue
                		compoundName = configCuesNames.get(cueName);
                		interfaceName = "c(" + compoundName + ")";
                		result.append("Cue : ").append(interfaceName);
                	}	else {
                			String compoundName, interfaceName;
                			// configural compound
                			compoundName = cueName.substring(0,cueName.length()-1);
                			interfaceName = "[" + compoundName + "]";
                			result.append("Cue : ").append(interfaceName);
                	}
                	if(!cueName.contains(Simulator.OMEGA+"")) {
                		result.append("\n\n").append("Realtime.").append("\n\n");
                	
                		for (int z = 0; z < tempCscCue.size() && z < sp.getMaxDuration(); z++) {
                			SimCue tempCue = tempCscCue.get(z);
                			//Last-but-one V value (i.e. the predicted for next time.)
                			result.append("Component ").append(z+1).append(" V = ").append(tempCue.getAssocValueAt(tempCscCue.getTrialCount()-1));
                			result.append('\n');
                		}
                	}
                	result.append('\n').append("Trial\n\n");
                	
                	for (int z = 0; z < tempCscCue.getTrialCount(); z++) {
                		//Last-but-one V value (i.e. the predicted for next time.)
                		result.append("Trial ").append(z+1).append(" V = ").append(tempCscCue.averageAssoc(z));
                		result.append('\n');
                	}
                	if(model.showResponse() && !cueName.contains(Simulator.OMEGA+"")) {
                		result.append('\n').append("Simulated Response\n\n");
                		for (int z = 0; z < tempCscCue.size() && z < sp.getMaxDuration(); z++) {
                    		SimCue tempCue = tempCscCue.get(z);
                    		result.append("Response t").append(z+1).append(" = ");
                    		result.append(tempCue.response(tempCue.getAssocValueSize() - 1));
                    		result.append('\n');
                    	}
                	}
                	result.append('\n');
                }
            }
          
        }
        return result.toString();
    }

    /**
     * Checks if this is the name of a configural cue (i.e. contains lowercase characters)
     * @param cueName the cue to check
     * @return true if this is a configural cue
     */
    
	protected boolean configuralCue(String cueName) {
		if(cueName.length() == 1) { return false; }
		return !cueName.equals(cueName.toUpperCase());
	}

	/**
     * The Runnable's run method. This starts a new Thread. It 
     * actually runs every SimPhases.runSimulator() method which is the
     * method that uses the formula on the phases.
     */
    public void run() {
    	//Add to phasepool so we can still cancel them quickly if required
    	phasePool = ParallelArray.createEmpty(noOfPhases, SimPhase.class, Simulator.fjPool);
        phasePool.asList().addAll(phases);
        for (int i = 0; i < noOfPhases; i++) {
        	if(model.contextAcrossPhase()) {
            	//Deal with different omega per phase
            	for(Entry<String, CueList> entry : cues.entrySet()) {
            		String realName = model.getConfigCuesNames().get(entry.getKey());
            		realName = realName == null ? "" : realName;
            	}
            }
            phases.get(i).runSimulator();
        }
    }

	/**
	 * @param model the model to set
	 */
	public void setModel(SimModel model) {
		this.model = model;
	}

	/**
	 * @return the noOfPhases
	 */
	public int getNoOfPhases() {
		return noOfPhases;
	}

	/**
	 * @param noOfPhases the noOfPhases to set
	 */
	public void setNoOfPhases(int noOfPhases) {
		this.noOfPhases = noOfPhases;
	}
	
	/**
	 * 
	 * @return a map of context names to their configurations.
	 */
	
	public Map<String, ContextConfig> getContexts() {
		Map<String, ContextConfig> contexts = new HashMap<String, ContextConfig>();
		
		for(SimPhase phase : phases) {
			contexts.put(phase.getContextConfig().getSymbol(), phase.getContextConfig());
		}
		
		return contexts;
	}

	/**
	 * @param control the message passing object to use
	 */
	public void setControl(ModelControl control) {
		for(SimPhase phase : phases) {
			phase.setControl(control);
		}
	}
	
	/**
	 * 
	 * @return a count of how many phases are random in this group
	 */
	
	public int countRandom() {
		int count = 0;
		for(SimPhase phase : phases) {
			if(phase.isRandom() || phase.getTimingConfig().hasVariableDurations()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * @return the number of runs of the algorithm in this group
	 */
	public int numRandom() {
		int count = 0;
		for(SimPhase phase : phases) {
			int increment = phase.isRandom() ? model.getCombinationNo() : 0;
			increment = phase.getTimingConfig().hasVariableDurations() ? Math.max(increment, 1)*model.getVariableCombinationNo() : increment;
			count += increment;
		}
		return count;
	}
}