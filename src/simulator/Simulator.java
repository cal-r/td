package simulator;

import jsr166y.ForkJoinPool;

/**
 * Simulator creates phase (model), view and controller. They are created
 * once here and passed to the other parts that need them so there
 * is only on copy of each. The MVC structure.
 */
public class Simulator {
	private static SimModel model = new SimModel();
	private SimView view;
	private static SimController controller;
	public static final double VERSION = 0.96; 
	public static ForkJoinPool fjPool = new ForkJoinPool();
	public static final char OMEGA = '\u03A9';
		
	/**
	 * Simulator's Constructor method
	 */
	public Simulator() {
		view = new SimView(model);
		setController(new SimController(model, view));
		 
		view.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		view.setVisible(true);		 
	}
	
	/**
	 * The main method of the application. It creates a new Simulator object.
	 * This is the method that is needed to start up the program.
	 * @param args by default this parameter is needed if any arguments inserted
	 * from the command prompt.
	 */	
	// Modified by E. Mondragon. July 29, 2011 
	//public static void main(String[] args) {
	//javax.swing.JFrame.setDefaultLookAndFeelDecorated(true);
	//new Simulator();
	public static void main(String[] args) {
		//javax.swing.JFrame.setDefaultLookAndFeelDecorated(false);
		new Simulator();
	}

	/**
	 * @return the controller
	 */
	public static SimController getController() {
		return controller;
	}

	/**
	 * @param controller the controller to set
	 */
	public void setController(SimController controller) {
		Simulator.controller = controller;
		view.updatePhasesColumnsWidth();
	}
}
