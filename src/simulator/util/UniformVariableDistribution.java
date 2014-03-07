/**
 * 
 */
package simulator.util;

/**
 * Class representing a uniform distribution.
 * 
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 **/
public class UniformVariableDistribution extends VariableDistribution {

	/**
	 * @param mean
	 * @param sd
	 * @param trials
	 */
	public UniformVariableDistribution(float mean, float sd, int trials) {
		super(mean, sd, trials);
	}

	/**
	 * @param mean
	 * @param sd
	 * @param seed
	 * @param trials
	 */
	public UniformVariableDistribution(double mean, float sd, long seed,
			int trials, boolean meanType) {
		super(mean, sd, seed, trials, meanType);
	}
	
	/**
	 * Get the next onset in this variable distribution.
	 * Onsets are selected from an exponential distribution with the given mean.
	 * Variable distributions configured with the same mean & sd will
	 * give the same sequence of onsets.
	 * @return an integer indicating the next duration.
	 */
	
	protected double nextRandom() {
		double next = (random.nextDouble()+0.5) * getMean();
		return next > 0 ? next : 1;//(int) /*Math.round(-1*mean*Math.log(random.nextDouble()));*/Math.round(mean+random.nextGaussian()*sd);
	}

}
