/**
 * SimCue.java
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cue class represents a model for every cue in the simulator's stimulus.
 * It stores the symbol of the cue, the 'alpha' value and also an ArrayList with all
 * the associative strength values that are changed on every timestep.
 * Additionally modified to act as part of a CSC in temporal difference by adding an
 * eligibility trace that is updated on every timestep.
 */
public class SimCue {
	
	/**
	 * City University
	 * BSc Computing with Artificial Intelligence
	 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
	 * @supervisor Dr. Eduardo Alonso 
	 * @author Jonathan Gray
	 **/
	public enum Trace implements Serializable{
		BOUNDED("Bounded accumulating"), REPLACING("Replacing"), 
		ACCUMULATING("Accumulating");
		private String nameStr;
		private Trace(String nameStr) {
			this.nameStr = nameStr;
		}
		public String toString() {
			return nameStr;
		}
	}

	private String symbol;
	private Double alpha;
	/** Historic weights. **/
	private List<Double> assocValue;
	/** Eligibility trace for this cue. **/
	private List<Double> trace;
	/** Recently active indicator. **/
	private boolean active;
	/** Current eligibility trace. **/
	private double traceVal;
	/** Working weight. **/
	private double assoc;
	/** Whether bounded eligibility traces are in use. **/
	private Trace traceType;
	/** Historic responses. **/
	private List<Double> responses;
	private List<Double> thresholds;

	/**
	 * Cue's Constructor method.
	 * @param symbol is the character of the cue (e.g. A, B, .. ,Z).
	 * @param alpha a Double value for the alpha of the specified cue.
	 */
	public SimCue(String symbol, Double alpha) {
		
		assocValue = new ArrayList<Double>(50);
		setAssocValue(new Double(0));
		this.symbol = symbol;
		this.alpha = alpha;
		this.trace = new ArrayList<Double>();
		traceVal = 0;
		assoc = 0;
		active = false;
		traceType = Trace.REPLACING;
		responses = new ArrayList<Double>();
		thresholds = new ArrayList<Double>(60);
	}
	
	public SimCue(String symbol, Double alpha, Trace trace) {
		this(symbol, alpha);
		traceType = trace;
	}

	
	/**
	 * Returns the cue's symbol. It should be a character from the alphabet.
	 * @return  a character which represents cue's symbol (A, B, .. ,Z).
	 */
	public String getSymbol() {
		return symbol;
	}
	
	/**
	 * Changes the Double variable of cue's alpha value
	 * @param alpha a double value of the alpha for the specified cue.
	 */
	public void setAlpha(Double alpha) {
	    this.alpha = alpha;
	}
	
	/**
	 * Returns the cue's 'alpha' value, a Double type variable.
	 * @return a Double value of the alpha for the specified cue.
	 */
	public Double getAlpha() {
		return alpha;
	}
	
	/**
	 * Sets the working V value for this cue.
	 * @param av a Double value of the associative strength.
	 */
	public void setAssocValue(Double av) {
		//assocValue.add(av);
		assoc = av;
	}

	// Added Alberto Fern�ndez July-2011
	/**
	 * Changes the the ArrayList assocValue
	 * which represents the associative values of the specified cue.
	 * @param av an ArrayList<Double> value of the associative strength.
	 */
	public void setAssocValueVector(List<Double> av) {
		//assocValue.clear();
		//assocValue.addAll(av);
		assocValue = av;
		assoc = av.size() > 0 ? av.get(av.size()-1) : 0;
	}

	// Added Alberto Fern�ndez July-2011
	/**
	 * Returns the the ArrayList assocValue
	 * which represents the associative values of the specified cue.
	 * @return the ArrayList<Double> value of the associative strength.
	 */
	public List<Double> getAssocValueVector() {
		return assocValue;
	}


	/**
	 * Returns the current working V of this cue.
	 * 
	 * @return a Double value of the last associative strength.
	 */
	public Double getLastAssocValue() {
		//return assocValue.isEmpty() ? 0 : (Double) assocValue.get(assocValue.size() - 1);
		return assoc;
	}
	
	/**
	 * Returns the current size of the associative value. This will tell us how many stages
	 * the associative value has gone through.
	 * @return the number of stages the associative value has gone through.
	 */
	public int getAssocValueSize() {
		return assocValue.size();
	}
	
	/**
	 * Returns the associative value that the experiment had on a specific
	 * trial. The method will get as an argument the number of the phase.
	 * @param trial the experiments trial.
	 * @return the Double value of the associative value on the requested trial.
	 */
	public Double getAssocValueAt(int trial) {
		double result = 0;
		try {
			result = (Double) assocValue.get(trial);
		} catch (ArrayIndexOutOfBoundsException e) {
			result = 0;
		}
		return result;
	}
	
	/**
	 * Updates the associative value on the ArrayList. But do succeed such an update,
	 * we will first need to remove the value from the requested position and then we
	 * add the new one again on the requested position.
	 * @param n the requested position.
	 * @param av the new associative value.
	 */
	public void setAssocValueAt(int n, Double av) {
		assocValue.remove(n);
		assocValue.add(n, av);
	}
	
	/**
	 * Resets the object's variables into their initial values. This is useful if the user
	 * chooses to restart the experiment with new values.
	 */
	public void reset() {
	    assocValue.clear();
	    setAssocValue(new Double(0));
	    alpha = new Double(0);
	    trace.clear();
	    traceVal = 0d;
	}

	/**
	 * @return the list of eligibility traces
	 */
	public List<Double> getTraceList() {
		return trace;
	}

	/**
	 * @param trace the eligibility trace list to set
	 */
	public void setTraceList(List<Double> trace) {
		this.trace = trace;
	}
	
	/**
	 * Set the current trace.
	 * @param d eligibility trace to add.
	 */
	
	public void setTrace(double d) {
		traceVal = d;
	}
	
	/**
	 * Get the last eligibility trace
	 * @return the value of the last trace.
	 */
	
	public double getLastTrace() {
		return traceVal;
	}


	/**
	 * Get a copy of this cue with empty associative lists.
	 * @return a copy.
	 */
	
	public SimCue copy() {
		SimCue copy = new SimCue(symbol, alpha);
		copy.setTraceType(traceType);
		return copy;
	}

	/**
	 * Set whether a cue has just been activated.
	 * @param b true is just active, otherwise false
	 */
	
	public void setActive(boolean b) {
		active = b;
	}
	
	/**
	 * Check whether this cue was just activated.
	 * @return true if the cue was just activated.
	 */
	
	public boolean isActive() {
		return active;
	}
	
	/**
	 * Get the simulated response rate for this component.
	 * @param threshold the B threshold to use for the distribution of b.
	 * @return an integer count of simulated responses per minute.
	 */
	
	public double response(int trial) {
		return responses.get(trial);
	}
	
	public void makeResponse(double threshold) {
		Random random = new Random();
		
		thresholds.clear();
		for(int i = 0; i < 60; i++) {
			thresholds.add(random.nextDouble());
		}
		responses.add(response(threshold));
	}

	
	/**
	 * Push the current weight records to storage to separate
	 * results by trial.
	 */
	
	public void store() {
		assocValue.add(assoc);
	}
	
	/**
	 * 
	 * @return the number of trials this stimulus appeared in.
	 */
	
	public int numTrials() {
		return assocValue.size();
	}
	
	public String toString() {
		return symbol+" α("+alpha+"), V("+assoc+")";
	}
	
	/**
	 * Update this cues trace & weights
	 * @param betaError beta error term
	 * @param delta 
	 * @param gamma 
	 */
	
	public void update(double betaError, double delta, double gamma) {
		//Update trace & weight				
		//Multiply by stimulus' alpha
		double deltaWeight = alpha*betaError*traceVal;
		
		//Update weight
		assoc += deltaWeight;
	}
	
	public void updateTrace(double delta, double gamma) {
		double newTrace; 
		switch(traceType) {
		case BOUNDED:
			newTrace = getBoundedTrace(delta);
			break;
		case REPLACING:
			newTrace = getReplacingTrace(delta, gamma);
			break;
		default:
			newTrace = getAccumulatingTrace(delta, gamma);
		}
		traceVal = newTrace;
		active = false; //Components active for only single time-step
	}
	
	/**
	 * Calculate a new trace using Sutton's TDLambda
	 * formulation. x(t+1) = x(t)*delta*gamma + y(t)
	 * 
	 * @param delta trace decay
	 * @param gamma discount factor
	 * @return a double holding the new trace value
	 */
	
	public double getAccumulatingTrace(double delta, double gamma) {
		//If the cue was activated recently, start the trace.
		double newTrace = isActive() ? 1 : 0;
		newTrace += getLastTrace()*delta*gamma;
		return newTrace;
	}
	
	/**
	 * Calculate a new accumulating trace bounded to 1.
	 * @param delta
	 * @param gamma
	 * @return a double holding the new trace value
	 */
	
	public double getReplacingTrace(double delta, double gamma) {
		return Math.min(getAccumulatingTrace(delta, gamma), 1);
	}
	
	/**
	 * Calculate a new trace using Sutton & Barto's original
	 * formulation.
	 * x(t+1) = x(t) + delta*(y(t) - x(t))
	 * 
	 * @param delta decay factor
	 * @return a double holding the new trace value
	 */
	
	public double getBoundedTrace(double delta) {
		//If the cue was activated recently, start the trace.
		double newTrace = isActive() ? 1 : 0;
		newTrace -= getLastTrace();
		newTrace *= delta;
		newTrace += getLastTrace();
		return newTrace;
	}
	
	/**
	 * 
	 * @return the trace type used.
	 */
	
	public Trace getTraceType() {
		return traceType;
	}
	
	/**
	 * 
	 * @param trace the type of trace to use.
	 */
	
	public void setTraceType(Trace type) {
		traceType = type;
	}

	/**
	 * @param threshold
	 * @return
	 */
	public double response(double threshold) {
		int responses = 0;
		try {
			for(double boundary : thresholds) {
				int response = assoc > boundary*threshold ? 1 : 0;
				response = boundary < 4/60 ? 1 : response;
				responses += response;
			}
		} catch (ArrayIndexOutOfBoundsException e) {//swallow this, response is zero.
			System.out.println(e.getStackTrace());
		}
		catch (IndexOutOfBoundsException e) {//swallow this, response is zero.
			System.out.println(e.getStackTrace());
		}
		return responses;
	}

	/**
	 * @return the responses
	 */
	public List<Double> getResponses() {
		return responses;
	}

	/**
	 * @param responses the responses to set
	 */
	public void setResponses(List<Double> responses) {
		this.responses = responses;
	}
	
}