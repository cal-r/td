/**
 * SimGraph.java
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
package simulator.graph;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import simulator.CueList;
import simulator.SimGroup;
import simulator.SimModel;
import simulator.SimPhase;

/**
 * SimGraph produces a new frame with the experiments graph line presentation. 
 * It retrieves the values from the results from all the phases in total and draws 
 * them on a panel having the option to choose another group or disable some of
 * cues. The class imports the free Java class library 'JFreeChart' for generating 
 * charts. It has the ability to zoom in and out on a specific area, save or print
 * the image and finally u can modify any available properties before output the 
 * graph. Window may still be open while the user chooses to do a new experiment so 
 * to compare values.  
 * 
 */

public class SimGraph extends JFrame implements ActionListener{

	private static final int LINE_WIDTH = 2;    	
	private static final String CUE_SEP = " - ";

    private Container cp;
    private SimGraphPanel gp;
    private JPanel groupPanel, cuePanel;
    private JCheckBox groupButtons[], cueButtons[];
    
    private XYSeriesCollection xyseriescollection;
    
    private TreeSet<String>                          cueMap;     
    private TreeSet<String>          selectedGroups, selectedCues;
    Object                           groupNames[],   cueNames[];
    protected int phase;
    protected double threshold;
    /** Mapping from series name to colours & shapes. **/
    private static Map<String, SeriesImage> seriesImageMap = new HashMap<String, SeriesImage>();

    /**
     * Class holding a shape-colour combination.
     * City University
     * BSc Computing with Artificial Intelligence
     * Project title: Building a TD Simulator for Real-Time Classical Conditioning
     * @supervisor Dr. Eduardo Alonso 
     * @author Jonathan Gray
    *
     */
    
    private class SeriesImage {
    	/** Colour of this combination. **/
    	private Paint colour;
    	/** Shape of the combination. **/
    	private Shape shape;
    	
    	public SeriesImage(Shape shape, Paint colour) {
    		this.colour = colour;
    		this.shape = shape;
    	}

		/**
		 * @return the shape
		 */
		public Shape getShape() {
			return shape;
		}

		/**
		 * @param shape the shape to set
		 */
		public void setShape(Shape shape) {
			this.shape = shape;
		}

		/**
		 * @return the colour
		 */
		public Paint getColour() {
			return colour;
		}

		/**
		 * @param colour the colour to set
		 */
		public void setColour(Paint colour) {
			this.colour = colour;
		}
		/**
		 * Check if this is the same as another combination (shape colour & shape)
		 */
		
		public boolean equals(Object other) {
			if(other instanceof SeriesImage) {
				SeriesImage tmp = (SeriesImage) other;
				if(tmp.getColour().equals(colour) && tmp.getShape().equals(shape)) {
					return true;
				}
			}
			return false;
		}
		
		public int hashCode() {
			return colour.hashCode();
		}
    }
    
    /**
     * Clear the colour mapping.
     */
    public static void clearDefaults() {
    	seriesImageMap.clear();
    }
    
    /**
     * SimGraph's Constructor method.
     * @param s frame's title.
     * @param m the experiment's model to use.
     * @param phase
     * @param compound
     * @param configuralCompoundsMapping
     */
    public SimGraph(String s, SimModel m, int phase, boolean compound) {
        super(s);
        threshold = m.getThreshold();
        this.phase = phase;
        Map<String,SimGroup> groupMap = m.getGroups(); // to not lose the actual model and not update with a new one
                       
        // selectedGroups = new TreeSet<String>(groupMap.keySet()); // AF Oct-2011
        selectedGroups = new TreeSet<String>();
        selectedCues   = new TreeSet<String>();
        
        xyseriescollection = new XYSeriesCollection();
        cueMap = new TreeSet<String>(); // all the cues in all the groups in all the phases

        // Alberto Fernandez Nov-2011: iterate first cues, then groups, so as to keep the same colors along different groups
        
    	List<String> cues = m.getListAllCues();
    	for (String cueName: cues) {

	        Iterator<SimGroup> itergroup = groupMap.values().iterator();
	        while (itergroup.hasNext()) {
	   	     	// for all groups
	        	SimGroup group = itergroup.next();
	        	
	        	if (!group.getPhases().get(this.phase).intialSequence().equals("0")) { // AF Oct-2011
	        		
	        		selectedGroups.add(group.getNameOfGroup()); // AF Oct-2011
	        		
	        		// Alberto Fernandez Nov-2011:
	        		// 
		        	// TreeMap<String,SimCue> cues = group.getCuesMap();
	        		
		        	//ArrayList<String> cues = m.getListAllCues();
	        		
		        	//Iterator itercues = cues.iterator();
		        	
		        	// Alberto Fernandez Nov-2011
		        	//Iterator itercues = cues.values().iterator();
		            //while (itercues.hasNext()) {
		            //for (String cueName: cues) {
		            	// for all cues of the group (to keep the colors similar for each group and cue in every phase)
	        		
		            	// Alberto Fernandez Nov-2011
		            	//SimCue tempCue = (SimCue)itercues.next();
		            	
		            	// Alberto Fernandez Nov-2011
		            	// Alberto Fern·ndez August-2011
		            	//String cueName = tempCue.getSymbol();
		            	
		            	//if (!Character.isLowerCase(cueName.charAt(0))) { // don't draw configural cues 
			            	String interfaceName = m.cueName2InterfaceName(cueName);
	    	            	
			            	//String seriesName = group.getNameOfGroup() + CUE_SEP + tempCue.getSymbol() + "";
			            	String seriesName = group.getNameOfGroup() + CUE_SEP + interfaceName + "";
			            	
			            	XYSeries tempXYSeries = new XYSeries(seriesName);
			                   
			            	// Alberto Fernandez Nov-2011
			            	//if (((SimPhase)group.getPhases().get(this.phase)).isCueInStimuli(tempCue.getSymbol())) {                		
				            //if (((SimPhase)group.getPhases().get(this.phase)).isCueInStimuli(cueName)) {                		
			            		// looking for the data in results of the phase
			            		// Alberto Fernandez Nov-2011
			            		//SimCue alfa = (SimCue) ((SimPhase)group.getPhases().get(this.phase)).getResults().get(tempCue.getSymbol());
				            	//Modified for CSC cues. J Gray
				            	SimPhase thisPhase = (SimPhase) group.getPhases().get(this.phase);
			            		CueList alfa = thisPhase.getResults().get(cueName);
			                 	//if (compound || (!compound && alfa.get(0).getSymbol().length()==1)){
			                 		// Add values from the experiments results on the canvas in X, Y coordinates.
			                 		/*for (int y = 1; y < alfa.size() && y < thisPhase.getMaxDuration(); y ++) { 
			                 			SimCue cue = alfa.get(y-1);
			                 			//tempXYSeries.add(y, cue.getLastAssocValue().floatValue());	
			                 			tempXYSeries.add(y, cue.getLastAssocValue().floatValue()/*response(0.035));
			                 		}*/
			                 		
			                 	//}
			            	//}
			            		//if(alfa != null) {
			            		if (thisPhase.isCueInStimuli(cueName)) {
			            			if (drawStimulus(compound, cueName)) {
			            				xyseriescollection.addSeries(plot(seriesName, alfa, thisPhase.getMaxDuration()));
			            			}
			            		
			            	// Alberto Fernandez Nov-2011
			            	//cueMap.add(tempCue.getSymbol()+"");
			            			cueMap.add(cueName+"");
			            		}
			            		//}
		            	//}
		            }   
	        	}
	        }

        // selectedCues only with the cues that are taking part in the phase
        Iterator<SimGroup> itergr = groupMap.values().iterator();
        while (itergr.hasNext()) {
   	     	// for all groups
        	SimGroup group = (SimGroup)itergr.next();
        	SimPhase pha = (SimPhase)group.getPhases().get(this.phase);
        
        	for (String cue: cueMap){
        		if (pha.isCueInStimuli(cue) ) {
        		 	if (drawStimulus(compound, cue))
                        //selectedCues.add(cue); 
        		 		selectedCues.add(m.cueName2InterfaceName(cue)); 
                }
        	}
        }
                
        // for buttons
        groupNames = selectedGroups.toArray();
        cueNames = selectedCues.toArray();
        
        gp = new SimGraphPanel(xyseriescollection,selectedGroups,selectedCues, compound); // para que tenga todas en cuenta en el constructor
        
        
        cp = this.getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(gp, BorderLayout.CENTER);
        
        cp.add(createGroupPanel(), BorderLayout.WEST);
        cp.add(createCuePanel(), BorderLayout.SOUTH);

        groupMap = null;
    }

    
    public void dispose() {
    	super.dispose();
    	cp.remove(gp);
        gp = null;
        groupPanel=null;
        cuePanel = null;
        groupButtons = null;
        cueButtons = null;
        
        xyseriescollection = null;
        
        cueMap = null;     
        selectedGroups = null;
        selectedCues = null;
        groupNames = null;
        cueNames = null;
    	
    }
    
    /**
     * 
     * @param compound
     * @param cue
     * @return
     */
    
    protected boolean drawStimulus(boolean compound, String cue) {
    	return (compound || (!compound && cue.length()==1));
    }
    
    /** Returns an ImageIcon, or null if the path was invalid. */
    private ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL =  this.getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
    
    /**
     * Plot the average weight of a cue against trials.
     * @param name
     * @param alfa
     * @return
     */
    
    public XYSeries plot(String name, CueList alfa, int maxDuration) {
    	XYSeries tempXYSeries = new XYSeries(name);
    	try {
    			int vectorSize = alfa.getTrialCount();
    			for(int y = 0; y < vectorSize; y++) {
    				tempXYSeries.add(y+1, alfa.averageAssoc(y));
    			}
    	} catch (NullPointerException e) {
    		
    	}
    	return tempXYSeries;
    }

    
    /**
     * Creates JCheckbox on the left of the frame so the user will
     * have the flexibility to choose between the groups. The only thing 
     * that he has to do is to click on them. The graph will refresh 
     * automatically.
     * @return a JPanel.
     */
    public JPanel createGroupPanel() {
    	groupPanel = new JPanel(new GridLayout(0,1));  // rows,columns      
        groupButtons = new JCheckBox[groupNames.length];
        
        // Iterate through groups and create JRadioButtons.
        for(int i = 0; i < groupNames.length; i++) {
            groupButtons[i] = new JCheckBox((String)groupNames[i]);
            groupButtons[i].setActionCommand((String)groupNames[i]);
            groupButtons[i].addActionListener(this);
            groupButtons[i].setSelected(true);
            groupPanel.add(groupButtons[i]);
        }
        return groupPanel;
    }
    
    /**
     * Creates JCheckbox on the bottom of the frame so the user will
     * have the flexibility to choose between the cues. The only thing 
     * that he has to do is to click on them. The graph will refresh 
     * automatically.
     * @return a JPanel.
     */
    public JPanel createCuePanel() {
        cuePanel = new JPanel();      
        cueButtons = new JCheckBox[cueNames.length]; 
        
        // Iterate through groups and create JRadioButtons.
        for(int i = 0; i < cueNames.length; i++) {
            cueButtons[i] = new JCheckBox((String)cueNames[i]);
            cueButtons[i].setActionCommand((String)cueNames[i]);
            cueButtons[i].addActionListener(this);
            // Alberto Fernandez August-2011
            //if (cueNames[i].toString().charAt(0)=='(') {
            if (cueNames[i].toString().contains("(")) {
                cueButtons[i].setSelected(false);            	
            }
            else {
                cueButtons[i].setSelected(true);
            }
            cuePanel.add(cueButtons[i],BorderLayout.CENTER);
        }
        return cuePanel;
    }
      
    /**
     * The ActionListeners assigned to every JRadioButton. 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent actionevent) {
    	selectedGroups = new TreeSet<String>();
        selectedCues   = new TreeSet<String>();
        
    	for (JCheckBox button: groupButtons) {
    		if (button.isSelected()) {
    			selectedGroups.add(button.getActionCommand());
    		}
    	}
    	
    	for (JCheckBox cuebutton: cueButtons) {
    		if (cuebutton.isSelected()) {
    			selectedCues.add(cuebutton.getActionCommand());
    		}
    	}

    	gp.modify(selectedGroups,selectedCues);
        
        this.doLayout();
        this.validate();
        this.repaint(); 
    }

    /**
     * SimGraphPanel produces a line graph from a specific group and display it in
     * a JPanel. By default the graph contains all group's cues. The user is able to
     * select and deselect the ones that he needs by clicking the appropriate checkbox
     * on the bottom of the panel.
     */
    class SimGraphPanel extends JPanel implements ActionListener {
 
    	private static final int LINE_WIDTH = 2;    	
		private XYLineAndShapeRenderer renderer;
        private XYSeriesCollection xyseriescollection;

        /**
         * SimGraphPanel's Constructor method.
         * @param groups the groups that was selected to be displayed.
         */
        public SimGraphPanel(XYSeriesCollection xy, TreeSet<String> groups, TreeSet<String> cues, boolean compound) {
            super(new BorderLayout());
            xyseriescollection = xy;
            JFreeChart jfreechart = createChart(xyseriescollection);  
            setColorShapeCombination(jfreechart);
            
            
            // X = domainsAxis
            XYPlot plot = jfreechart.getXYPlot();
            NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
            domainAxis.setAutoRange(false);
            //domainAxis.setTickUnit(new NumberTickUnit(1.0));
            domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits()); // AF Sept-2011
            plot.setDomainAxis(domainAxis);
            
            plot.setBackgroundPaint(null);
            plot.setDomainGridlinePaint(Color.black);
            plot.setRangeGridlinePaint(Color.black);
            
            
            // Removing the series that are not taking part in the phase from the legend
            // With this solution, the Legend will not be reupdated according to the buttons
            LegendItemCollection lcol = plot.getLegendItems();
            LegendItemCollection newlcol = new LegendItemCollection();
           	for (int l=0; l< lcol.getItemCount(); l++) {
               	LegendItem litem = lcol.get(l);
               	String label = litem.getLabel();
               	String groupSerie = label.substring(0,label.indexOf(SimGraph.CUE_SEP));
               	String cueSerie   = label.substring(label.indexOf(SimGraph.CUE_SEP)+SimGraph.CUE_SEP.length(),label.length());
               	if (groups.contains(groupSerie) && cues.contains(cueSerie)) {
               		// Alberto Fernandez August-2011
               		//if (compound || (!compound && cueSerie.length()==1)){
                   	if (compound || (!compound && (cueSerie.length()==1 || cueSerie.contains("(")))) {
                   		// Alberto Fernandez Nov-2011
               			//newlcol.add(litem);	
                   		// new constructor with the goal of setting shapeVisible=true
                   		LegendItem newLegend = new LegendItem(litem.getLabel(),
								                   	          litem.getDescription(),
								                   	          litem.getToolTipText(),
								                   	          litem.getURLText(),
								                   	          true, // shapeVisible
								                   	          litem.getShape(),
								                   	          litem.isShapeFilled(),
								                   	          litem.getFillPaint(),
								                   	          litem.isShapeOutlineVisible(),
								                   	          litem.getOutlinePaint(),
								                   	          litem.getOutlineStroke(),
								                   	          litem.isLineVisible(),
								                   	          litem.getLine(),
								                   	          litem.getLineStroke(),
								                   	          litem.getLinePaint());
               			newlcol.add(newLegend);	
               		}
               	}
           	}
           	plot.setFixedLegendItems(newlcol);

            //Making wider and markable
            BasicStroke stro = new BasicStroke(LINE_WIDTH);
            for (int i=0; i<xyseriescollection.getSeriesCount(); i++) {
            	renderer.setSeriesStroke(i, stro); 
            	renderer.setSeriesShapesVisible(i, true); 
            	
            	// Alberto Fernandez Sept-2011
            	//String seriesName = xyseriescollection.getSeries(i).getName();
            	//if (xyseriescollection.getSeries(i).getName().contains("(")) {
               	if ( ((String)xyseriescollection.getSeries(i).getKey()).contains("(")) {
                	renderer.setSeriesVisible(i, false);             	
            	}
            	else {
                	renderer.setSeriesVisible(i, true);             	
            	}
            }                              
            ChartPanel chartpanel = new ChartPanel(jfreechart);                         
            add(chartpanel);
            chartpanel.setPreferredSize(new Dimension(500, 270));  
            
            chartpanel.setDefaultDirectoryForSaveAs(new File("."));
        }

        /**
         * Modify the SimGraphPanel showing only the data of the selected groups and cues
         * @param groups that was selected to be displayed.
         * @param cues   that was selected to be displayed.
         * 
         */
        public void modify(TreeSet<String> groups, TreeSet<String> cues) {
        	
        	for (int i=0; i<xyseriescollection.getSeriesCount(); i++) {
            	//String serie = xyseriescollection.getSeries(i).getName();
            	String serie = (String)xyseriescollection.getSeries(i).getKey();
            	String groupSerie = serie.substring(0, serie.indexOf(SimGraph.CUE_SEP));
            	String cueSerie   = serie.substring(serie.indexOf(SimGraph.CUE_SEP)+SimGraph.CUE_SEP.length(),serie.length());
            	
            	// the group and the cue is selected
        		if (groups.contains(groupSerie) && cues.contains(cueSerie)) { 
        			renderer.setSeriesVisible(i,true);
        		}
        		else {
        			renderer.setSeriesVisible(i,false);
        		}
        	}
        }

        
        /**
         * Creates a chart from the coordinates that has been given. When the data has 
         * been processed and put on the chart, it returns it.
         * @param xydataset the data with the coordinates.
         * @return the chart with the group's graph presentation.
         */
        private JFreeChart createChart(XYDataset xydataset) {
			// the parameter after orientation specifies whether or not the legend is shown
        	JFreeChart jfreechart = ChartFactory.createXYLineChart("", getLabel().get(1),getLabel().get(0),
        			xydataset, PlotOrientation.VERTICAL, true, true, false);
        	XYPlot xyplot = jfreechart.getXYPlot();
            renderer = (XYLineAndShapeRenderer) xyplot.getRenderer(); 
            return jfreechart;
        }
        
        /**
         * The ActionListeners assigned to every JCheckBox. 
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent actionevent) {
        	
        }

    
    }
    
    /**
     * Helper function to make sure that each series has a unique colour-shape combination across
     * graphs and forcibly darken very light yellow to something visible.
     * @param chart
     */
    
    private void setColorShapeCombination(JFreeChart chart) {
    	int count = chart.getXYPlot().getDataset().getSeriesCount();
    	String name;
    	for(int i = 0; i < count; i++) {
    		//Confiigural compounds & regular compounds should use the same colour-shape combo
    		name = chart.getXYPlot().getDataset().getSeriesKey(i).toString().replaceAll("\\[|\\]", "");
    		if (!seriesImageMap.containsKey(name)){
    			Color color;
    			Shape shape;
    			SeriesImage combo;
    			do {
    				color = (Color) chart.getXYPlot().getDrawingSupplier().getNextPaint();
    				shape = chart.getXYPlot().getDrawingSupplier().getNextShape();
    				if((int)Math.sqrt(
    					      color.getRed() * color.getRed() * .241 + 
    					      color.getGreen() * color.getGreen() * .691 + 
    					      color.getBlue() * color.getBlue() * .068) > 160) {
        				color = color.darker();
        			}
    				combo = new SeriesImage(shape, color);
    			} while(seriesImageMap.containsValue(combo));
    			seriesImageMap.put(name, combo);
    		}
    		chart.getXYPlot().getRenderer().setSeriesShape(i, seriesImageMap.get(name).getShape());
    		chart.getXYPlot().getRenderer().setSeriesPaint(i, seriesImageMap.get(name).getColour());
    	}
    }

	public List<String> getLabel() {
		List<String> labels = new ArrayList<String>();
		labels.add("Mean Associative Strength");
		labels.add("Trial");
		return labels;
	}
}
