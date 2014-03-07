/**
 * 
 */
package simulator;

import simulator.SimCue.Trace;

/**
 * A CSC specifically for storing compound results.
 * 
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 **/
public class CompoundCueList extends CueList {

	/**
	 * @param symbol
	 * @param alpha
	 */
	public CompoundCueList(String symbol, double alpha,double threshold) {
		super(symbol, alpha,threshold);
		getAverageWeights().clear();
	}

	/**
	 * @param symbol
	 * @param alpha
	 * @param trace
	 */
	public CompoundCueList(String symbol, double alpha, double threshold, Trace trace) {
		super(symbol, alpha, threshold,trace);
	}
	
	/**
	 * Removes a 0 from the associative weights vector when generating
	 * a new cue as weights are calculated externally.
	 */
	
	public SimCue nextCue() {
		SimCue thisCue;
		if(!cueIt.hasNext()) {
			thisCue = super.nextCue();
			thisCue.getAssocValueVector().remove(0);
			thisCue.getResponses().remove(0);
		} else {
			thisCue = super.nextCue();
		}
		return thisCue;
	}

}
