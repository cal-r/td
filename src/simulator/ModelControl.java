/**
 * 
 */
package simulator;

/**
 * A message passing object for tracking progress in long
 * running tasks and instructing them to cancel if required.
 * 
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 **/

public class ModelControl {
	/** Task progress. **/
	private volatile double progress;
	/** Cancelled switch. **/
	private volatile boolean isCancelled;
	/** Estimated time taken for one run of the basic section of the task. **/
	private volatile long estimatedCycleTime = 0;
	/** Number of times the cycletime has been updated. **/
	private volatile int modCount;
	private volatile boolean isComplete;
	
	public ModelControl() {
		progress = 0;
		isCancelled = false;
		modCount = 1;
		isComplete = false;
	}

	/**
	 * @return the progress
	 */
	public double getProgress() {
		return progress;
	}
	
	/**
	 * 
	 * @return the estimated time for a single section of the underlying task.
	 */
	
	public long getEstimatedCycleTime() {
		return estimatedCycleTime/modCount;
	}
	
	/**
	 * 
	 * @param time recorded run time for a subsection of the main task.
	 */
	
	public void setEstimatedCycleTime(long time) {
		modCount = modCount +1;
		estimatedCycleTime+=time;// = Math.max(estimatedCycleTime,time);
	}

	/**
	 * @param progress the progress to set
	 */
	public void setProgress(double progress) {
		this.progress = progress;
	}
	
	/**
	 * 
	 * @param increment to apply to progress.
	 */
	
	public void incrementProgress(double increment) {
		progress += increment;
	}

	/**
	 * @return true if the associated task has been cancelled.
	 */
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * @param isCancelled true to cancel the task this passes messages for.
	 */
	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}

	/**
	 * @return the isComplete
	 */
	public boolean isComplete() {
		return isComplete;
	}

	/**
	 * @param isComplete the isComplete to set
	 */
	public void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}
}