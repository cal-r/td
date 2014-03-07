package simulator.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import simulator.Simulator;

/**
 * VariableDistribution.java
 * 
 * Class representing a variable distribution which produces
 * sequences of random numbers with a given mean and SD.
 * 
 * Created on 01-Dec-2011
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 *
 */

public class VariableDistribution implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4932520581988365938L;
	/** Lazily initialized random seed for variable distributions. **/
	private static long randomSeed = 0;
	/** Mean for this distribution. **/
	private double mean;
	/** Standard deviation for this distribution. **/
	private float sd;
	/** List of onsets. **/
	private List<Double> onsets;
	/** Random generator for variable onsets. **/
	protected Random random;
	/** Random seed. **/
	private long seed;
	/** Trial index. **/
	private int index;
	/** Flag set to indicate this distribution must be rebuilt. **/
	private boolean isChanged;
	
	/** True if we are using geometric mean. **/
	private boolean meanType;
	
	/** Total number of trials. **/
	private int trials;
	
	/**
	 * Creates a new variable distribution with the specified mean and standard deviation
	 * and a default random seed.
	 * @param mean The mean for the distribution
	 * @param sd The standard deviation for the distribution
	 */
	
	public VariableDistribution(final double mean, final float sd, int trials) {
		this.setSd(sd);
		this.mean = mean;
		seed = randomSeed();
		random = new Random(randomSeed());
		index = 0;
		this.trials = trials;
		meanType = false;
		onsets = new ArrayList<Double>();
		isChanged = false;
	}
	
	/**
	 *  Creates a new variable distribution with a specified random seed, supplying the same
	 *  sd, mean and seed as a previous VariableDistribution will give an idential distribution.
	 * @param mean2
	 * @param sd
	 * @param seed The seed to be used for the random number generator.
	 */
	
	public VariableDistribution(final double mean2, final float sd, long seed, int trials, boolean meanType) {
		this(mean2, sd, trials);
		random.setSeed(seed);
		this.meanType = meanType;
	}
	
	/**
	 * Get the onset for the specified trial number.
	 * @param trial the trial number to get the onset for.
	 * @return an integer indicating the timestep that onset occurs at.
	 */
	
	public double getOnset(final int trial) {
		return onsets.get(trial);
	}

	/**
	 * 
	 * @return the mean for the distribution.
	 */
	
	public double getMean() {
		return mean;
	}

	/**
	 * 
	 * @param meanVar the new mean for the distribution.
	 */
	
	public void setMean(double meanVar) {
		this.mean = meanVar;
	}

	/**
	 * 
	 * @return the standard deviation for the distribution.
	 */
	
	public float getSd() {
		return sd;
	}

	/**
	 * 
	 * @param sd new standard deviation.
	 */
	
	public void setSd(float sd) {
		this.sd = sd;
	}
	
	public long getSeed() {
		return seed;
	}
	
	/**
	 * 
	 * @return the seed for the random number generator.
	 */
	
	public static long randomSeed() {
		if (randomSeed == 0) {
			randomSeed = System.currentTimeMillis();
		}
		return randomSeed;
	}
	
	public static void newRandomSeed() {
		randomSeed = System.currentTimeMillis();
	}
	
	/**
	 * Transform a list of random numbers into one standardized about
	 * an exact mean.
	 * @return a standardized list of numbers.
	 */
	
	private List<Double> standardize() {
		List<Double> standardized = new ArrayList<Double>();
		
		double actualMean = meanType ? geometricMean(onsets) : arithmeticMean(onsets);
		
		for(double onset : onsets) {
			double shifted = onset - actualMean;
			shifted += mean;
			shifted = Math.max(shifted, Simulator.getController().getModel().getTimestepSize()); //Make lower bound 1 timestep
			standardized.add(shifted);
		}
		
		
		return standardized;
	}
	
	/**
	 * Get the next onset in this variable distribution.
	 * Onsets are selected from an exponential distribution with the given mean.
	 * Variable distributions configured with the same mean & sd will
	 * give the same sequence of onsets.
	 * @return an integer indicating the next duration.
	 */
	
	protected double nextRandom() {
		double next = -1*mean*Math.log(random.nextDouble());
		return Math.max(next, Simulator.getController().getModel().getTimestepSize());
	}
	
	/**
	 * Build the standardized list of onsets.
	 */
	
	public void build() {
		index = 0;
		if(isChanged) {
			onsets.clear();
			random = new Random(randomSeed());
			for(int i = 0; i < trials; i++) {
				onsets.add(nextRandom());
			}
			onsets = standardize();
			isChanged = false;
		}
	}
	
	public double next() {
		double next = onsets.get(index);
		index++;
		return next;
	}
	
	public void setIndex(int num) {
		index = num;
	}

	public void setTrials(int numTrials) {
		trials = numTrials;
		isChanged = true;
	}
	
	/**
	 * @param onsets2 a list of integers.
	 * @return the arithmetic mean of the list of numbers given.
	 */
	
	private double arithmeticMean(List<Double> onsets2) {
		double actualMean = 0;
		for(double onset : onsets2) {
			actualMean += onset;
		}
		actualMean /= onsets2.size();
		return actualMean;
	}
	
	/**
	 * 
	 * @param onsets2 a list of integers.
	 * @return the geometric mean of the list of numbers
	 */
	
	private double geometricMean(List<Double> onsets2) {
		double actualMean = 1;
		for(double onset : onsets2) {
			actualMean *= onset;
		}
		actualMean = Math.pow(actualMean, 1d/onsets2.size());
		return actualMean;
	}

	/**
	 * @return false if this distribution is using the arithmetic mean, true if geometric.
	 */
	public boolean isArithmetic() {
		return meanType;
	}

	/**
	 * @param meanType set to false to use arithmetic, true for geometric.
	 */
	public void setMeanType(boolean meanType) {
		this.meanType = meanType;
		isChanged = true;
	}

	/**
	 * Reshuffle the onsets list.
	 */
	public void regenerate() {
		Collections.shuffle(onsets);
	}

}
