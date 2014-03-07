/**
 * SimExport.java
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
package simulator.util.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import simulator.CueList;
import simulator.ModelControl;
import simulator.SimCue;
import simulator.SimGroup;
import simulator.SimModel;
import simulator.SimPhase;
import simulator.SimView;
import simulator.configurables.ContextConfig.Context;

/**
 * Exports the results from the simulator to a spreadsheet. It uses the HSSF
 * free library provided from apache jakarta project. The HSSF allows numeric, 
 * string, date or formula cell values to be written to or read from an XLS file. Also
 * we can do row and column sizing, cell styling (bold, italics, borders,etc), and 
 * support for both built-in and user defined data formats. It creates a workbook
 * that has a different sheet for every model's group. Every phase is represented
 * with a different table.
 */
public class SimExport implements Runnable {
    private Map<String, SimGroup> groups; 
    private Font groupFont, titleFont, tableTopFont, tableContFont;
    private Workbook wb;
    private Row row;
    private Cell cell;
    private CellStyle cs1,cs2,cs3,cs4,cs5;
    private final SimModel model;
    private SimView view;
    private String name;
	private File file;
	private boolean success;
	private ModelControl control;
        
    /**
     * SimExport's Constructor method.
     * @param view the application's view.
     * @param model the current model where that values will come from.
     * @param directory the last chosen directory
     */
    public SimExport(SimView view, SimModel model, String name, File file) {
    	this.view = view;
        this.model = model;
        this.file = file;
        this.name = name;
        success = true;
    }
  
    public void doExport() throws IOException {
        row = null;
        cell = null;
    	wb = new SXSSFWorkbook();
    	createStyles();        
    	FileOutputStream fileOut = new FileOutputStream(file);
                
    	groups = model.getGroups();
        Set<String> setGroups = groups.keySet();
        Iterator<String> iterGroups = setGroups.iterator();
        
        
        //Generate iteration between groups.
        while(iterGroups.hasNext() && !control.isCancelled()) {
        	long estimatedCycle = System.currentTimeMillis();
        	exportGroup(iterGroups.next(), name);
        	control.incrementProgress(1);
        	control.setEstimatedCycleTime(System.currentTimeMillis()-estimatedCycle);
        }
        if(!control.isCancelled()) {
        	wb.write(fileOut);
        }
        fileOut.close();
        control.setProgress(100);
    }
    
    private void exportGroup(String groupName, String name) {

    	int rowPos = 0;
    	int colPos = 0;
        final SimGroup group = groups.get(groupName);
        final Sheet sheet = wb.createSheet(group.getNameOfGroup());
        
    	// Modified by Alberto Fern�ndez July-2011
       
        // Title = file name
                       	
        row = sheet.createRow(rowPos);
        cell = row.createCell(colPos);
        
        // Alberto Fernandez Oct-2011
        // sheet.addMergedRegion(new Region(rowPos, colPos, rowPos, (short)(colPos + 3)));
        // Deprecated: Region(int rowFrom, short colFrom, int rowTo, short colTo) 
        // Current:    CellRangeAddress(int firstRow, int lastRow, int firstCol, int lastCol) 
        
        sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos, colPos, (colPos + 3)));
        
        
        cell.setCellValue(name); // write file name
        cell.setCellStyle(cs1);

       
        //Show procedural settings
        
        rowPos += 2;
        row = sheet.createRow(rowPos);
    	cell = row.createCell(colPos);
    	cell.setCellValue("Random trial combinations:");
    	cell.setCellStyle(cs2);
    	sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,  (short)colPos,  (short)(colPos + 2)));
    	cell = row.createCell(colPos+3);
    	cell.setCellValue(model.getCombinationNo());
    	cell.setCellStyle(cs2);
    	rowPos++;
    	row = sheet.createRow(rowPos);
     	cell = row.createCell(colPos);
     	cell.setCellValue("Random CS length distributions:");
     	cell.setCellStyle(cs2);
     	sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,  (short)colPos,  (short)(colPos + 2)));
    	cell = row.createCell(colPos+3);
     	cell.setCellValue(model.getVariableCombinationNo());
     	cell.setCellStyle(cs2);
     	rowPos++;
     	row = sheet.createRow(rowPos);
     	cell = row.createCell(colPos);
     	cell.setCellValue("Timestep length:");
     	cell.setCellStyle(cs2);
     	sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,  (short)colPos,  (short)(colPos + 2)));
    	cell = row.createCell(colPos+3);
     	cell.setCellValue(model.getTimestepSize());
     	cell.setCellStyle(cs2);
     	rowPos++;
     	row = sheet.createRow(rowPos);
     	cell = row.createCell(colPos);
     	cell.setCellValue("Trace type:");
     	cell.setCellStyle(cs2);
     	sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,  (short)colPos,  (short)(colPos + 2)));
    	cell = row.createCell(colPos+3);
     	cell.setCellValue(model.getTraceType().toString());
     	cell.setCellStyle(cs2);
     	rowPos++;
     	row = sheet.createRow(rowPos);
     	cell = row.createCell(colPos);
     	cell.setCellValue("Mean type:");
     	cell.setCellStyle(cs2);
     	sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,  (short)colPos,  (short)(colPos + 2)));
    	cell = row.createCell(colPos+3);
     	cell.setCellValue(model.isGeometricMean() ? "Geometric" : "Arithmetic");
     	cell.setCellStyle(cs2);
     	rowPos++;
     	row = sheet.createRow(rowPos);
     	cell = row.createCell(colPos);
     	cell.setCellValue("Distribution type:");
     	cell.setCellStyle(cs2);
     	sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,  (short)colPos,  (short)(colPos + 2)));
    	cell = row.createCell(colPos+3);
     	cell.setCellValue(model.isExponential() ? "Exponential" : " Uniform");
     	cell.setCellStyle(cs2);
     	rowPos++;
        
    	// Show Parameters

       // CS model

    	rowPos += 2;
    	
    	row = sheet.createRow(rowPos);
    	cell = row.createCell(colPos);
    	cell.setCellValue("CS alpha:");
    	cell.setCellStyle(cs2);
    	//sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos, (short)(colPos + 1)));
    	
    	rowPos++;
    	
        AbstractTableModel cstmv = view.getCSValuesTableModel();
        for (int r = 0; r < cstmv.getRowCount(); r++){
        	//if (group.getCuesMap().containsKey(cstmv.getValueAt(r,0))){
        	if (group.getCuesMap().containsKey(model.interfaceName2cueName((String)cstmv.getValueAt(r,0)))){
           		double value = new Double((String) cstmv.getValueAt(r, 1));
            	row = sheet.createRow(rowPos);
            	cell = row.createCell(colPos);
            	cell.setCellValue((String)cstmv.getValueAt(r,0));
            	cell.setCellStyle(cs3);
            	cell = row.createCell((colPos + 1));
            	cell.setCellValue(value);
            	cell.setCellStyle(cs4);
            	rowPos++;
        	}
        }
        if(model.isUseContext() && !model.contextAcrossPhase()) {
        	row = sheet.createRow(rowPos);
        	cell = row.createCell(colPos);
        	cell.setCellValue(Context.PHI.toString());
        	cell.setCellStyle(cs3);
        	cell = row.createCell((colPos + 1));
            cell.setCellValue(model.getContextAlpha());		                		
        	cell.setCellStyle(cs4);
        	rowPos++;
        }

        // US model

    	rowPos += 2;
    	
    	row = sheet.createRow(rowPos);
    	cell = row.createCell(colPos);
    	cell.setCellValue("US: ");
    	cell.setCellStyle(cs2);
    	//sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos, (short)(colPos + 1)));
    	
    	rowPos++;
    	
	    AbstractTableModel ustmv = view.getUSValuesTableModel();
        for (int r = 0; r < ustmv.getRowCount(); r++){
        	Object ovalue = ustmv.getValueAt(r, 1);
        	row = sheet.createRow(rowPos);
        	cell = row.createCell(colPos);
        	cell.setCellValue((String)ustmv.getValueAt(r,0));
        	cell.setCellStyle(cs3);
        	//cell = row.createCell((short)(colPos + 1));
        	cell = row.createCell((colPos + 1));
        	if (!((String) ovalue).equals("")){
           		double value = new Double((String) ovalue);
            	cell.setCellValue(value);		                		
        	}
        	cell.setCellStyle(cs4);
        	rowPos++;
        }
        //Other values
        row = sheet.createRow(rowPos);
    	cell = row.createCell(colPos);
    	cell.setCellValue("Other: ");
    	cell.setCellStyle(cs2);
    	//sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos, (short)(colPos + 1)));
    	
    	rowPos++;
        AbstractTableModel otmv = view.getOtherValuesTableModel();
        for (int r = 0; r < otmv.getRowCount(); r++){
        	Object ovalue = otmv.getValueAt(r, 1);
        	row = sheet.createRow(rowPos);
        	cell = row.createCell(colPos);
        	cell.setCellValue((String)otmv.getValueAt(r,0));
        	cell.setCellStyle(cs3);
        	//cell = row.createCell((short)(colPos + 1));
        	cell = row.createCell((colPos + 1));
        	if (!((String) ovalue).equals("")){
           		double value = new Double((String) ovalue);
            	cell.setCellValue(value);		                		
        	}
        	cell.setCellStyle(cs4);
        	rowPos++;
        }
        if(model.showResponse()) {
        	row = sheet.createRow(rowPos);
        	cell = row.createCell(colPos);
        	cell.setCellValue("Response Threshold:");
        	cell.setCellStyle(cs3);
        	cell = row.createCell((colPos + 1));
            cell.setCellValue(model.getThreshold());		                		
        	cell.setCellStyle(cs4);
        	rowPos++;
        }
        //Context alphas

        if(model.isUseContext()) {
        	rowPos += 2;
        	
        	row = sheet.createRow(rowPos);
        	cell = row.createCell(colPos);
        	cell.setCellValue("Context Alphas: ");
        	cell.setCellStyle(cs2);
        	//sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos, (short)(colPos + 1)));
        	
        	rowPos++;
        	row = sheet.createRow(rowPos);
        	for(int c = 1; c < group.getNoOfPhases() + 1; c++) {
        		cell = row.createCell(colPos+c);
        		cell.setCellValue("P"+c);
        		cell.setCellStyle(cs3);
        	}
        	rowPos++;
        	for(String cue : group.getCuesMap().keySet()) {
        		if(Context.isContext(cue)) {
        			row = sheet.createRow(rowPos);
        			cell = row.createCell(colPos);
        			cell.setCellValue(cue);		                		
        			cell.setCellStyle(cs4);
        			for(int c = 1; c < group.getNoOfPhases() + 1; c++) {
        				cell = row.createCell(colPos+c);
        				try {
        					double alpha = group.getPhases().get(c-1).isCueInStimuli(cue) ? group.getPhases().get(c-1).getResults().get(cue).getAlpha() : null;
        					cell.setCellValue(alpha);
        				} catch (NullPointerException e) {
        					//Ignore that, this context isn't here now.
        				}
                		cell.setCellStyle(cs3);
        			}
        			rowPos++;
        		}
        	}
        }
        rowPos++;
    	

        // Create group title on top of the page
        row = sheet.createRow(rowPos);
        cell = row.createCell(colPos);
        
        //sheet.addMergedRegion(new Region(rowPos, colPos, rowPos, (short)(colPos + 3)));
        sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos, colPos, (short)(colPos + 3)));
        
        
        cell.setCellValue(group.getNameOfGroup());
        cell.setCellStyle(cs1);
        
        // Generate iteration between phases
        for (int i = 0; i < group.getPhases().size(); i++) {
            rowPos += 2;
            SimPhase curPhase = group.getPhases().get(i);
            
         	row = sheet.createRow(rowPos);
         	cell = row.createCell(colPos);
         	cell.setCellValue("Phase " + (i+1));
         	cell.setCellStyle(cs2);
         	// sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos, (short)(colPos + 1)));
         	sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,  (short)colPos,  (short)(colPos + 1)));
         	
         	cell = row.createCell((colPos + 2));
         	cell.setCellValue("Random : " + curPhase.isRandom()); 
         	cell.setCellStyle(cs2);
         	sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos, (short)(colPos + 2), (short)(colPos + 3)));

         	cell = row.createCell((colPos + 4));
         	cell.setCellValue("Sequence : " + curPhase.intialSequence());
         	cell.setCellStyle(cs2);
         	sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos, (short)(colPos + 4), (short)(colPos + 10)));                   		

         	Map<String, CueList> results = curPhase.getResults();
         	String cueNames[] = {};
         	cueNames = results.keySet().toArray(cueNames);
         	
         	rowPos++;
         	row = sheet.createRow(rowPos);
         	cell = row.createCell(colPos);
         	cell.setCellValue("Stimuli Temporal Parameters: ");
         	cell.setCellStyle(cs2);
         	// sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos, (short)(colPos + 1)));
         	sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,  (short)colPos,  (short)(colPos + 2)));
         	rowPos++;
         	for (String cue : cueNames){
            	if(curPhase.isCueInStimuli(cue) && !Context.isContext(cue) && 
            			cue.length() == 1 && Character.isUpperCase(cue.charAt(0))) {
            	row = sheet.createRow(rowPos);
            	cell = row.createCell(colPos);
            	cell.setCellValue(cue);
            	cell.setCellStyle(cs3);
            	cell = row.createCell((colPos + 1));
                cell.setCellValue(curPhase.getTimingConfig().getDurations().getMap().get(cue)+"");		                		
            	cell.setCellStyle(cs4);
            	cell = row.createCell((colPos + 2));
                cell.setCellValue(curPhase.getTimingConfig().getRelation(cue)+"");		                		
            	cell.setCellStyle(cs4);
            	rowPos++;
            	}
            }
         	//US timing
         	row = sheet.createRow(rowPos);
        	cell = row.createCell(colPos);
        	cell.setCellValue("US");
        	cell.setCellStyle(cs3);
        	cell = row.createCell((colPos + 1));
            cell.setCellValue(curPhase.getTimingConfig().getUsDuration()+"");		                		
        	cell.setCellStyle(cs4);
         	
         	rowPos += 2;
          	
         	// max = maximum number of trial of all the cues in the phase
         	int max = 0;
         	for (int j=0; j<cueNames.length; j++){
         		if(curPhase.isCueInStimuli(cueNames[j])) {
         			CueList curCscCue = results.get(cueNames[j]);
         		
         			max = Math.max(curCscCue.getTrialCount(), max);
         		}
         	}

	            // Alberto Fernandez August-2011: export (1) cues, (2) compounds, (3) configural cues

         	//Realtime
         	row = sheet.createRow(rowPos);
         	cell = row.createCell(colPos);
         	cell.setCellValue("Realtime V");
         	cell.setCellStyle(cs2);
         	rowPos++;
         	
         	// first row: Trial names                         	
	            row = sheet.createRow(rowPos);
	            trialRow(max, colPos, row);
    		
         	rowPos++;

         // export Cues
     		for (int y = 1; y < cueNames.length + 1; y++) {
     			String cueName = (String)cueNames[y-1];
     			if (cueName.length() == 1 && 
     					(Character.isUpperCase(cueName.charAt(cueName.length()-1))) 
     				&& curPhase.isCueInStimuli(cueName)) {
     				rowPos = exportComponents(cueName, curPhase, sheet, rowPos, colPos, results);
     			}
     		}
     		rowPos++;
     		// export compound Cues
     		for (int y = 1; view.getIsSetCompound() && y < cueNames.length + 1; y++) {
     			String cueName = (String)cueNames[y-1];
     			if (cueName.length() > 1 && curPhase.isCueInStimuli(cueName)) {
     				rowPos = exportComponents(cueName, curPhase, sheet, rowPos, colPos, results);
     			}
     		}
         	rowPos++;
         	
         	
         	//Trial average
         	row = sheet.createRow(rowPos);
         	cell = row.createCell(colPos);
         	cell.setCellValue("V Per Trial");
         	cell.setCellStyle(cs2);
         	
         	rowPos++;
         	
         // first row: Trial names                         	
	            row = sheet.createRow(rowPos);
	           trialRow(max, colPos, row);
    		
    		
         	rowPos++;

         	// export Cues
     		for (int y = 1; y < cueNames.length + 1; y++) {
     			String cueName = (String)cueNames[y-1];
     			if (cueName.length() == 1 && 
     					(Character.isUpperCase(cueName.charAt(cueName.length()-1))) 
     				&& curPhase.isCueInStimuli(cueName)) {
     				rowPos = exportTrial(cueName, curPhase, sheet, rowPos, colPos, results);
     			}
     		}
     		rowPos++;
     		// export compound Cues
     		for (int y = 1; view.getIsSetCompound() && y < cueNames.length + 1; y++) {
     			String cueName = (String)cueNames[y-1];
     			if (cueName.length() > 1 && curPhase.isCueInStimuli(cueName)) {
     				rowPos = exportTrial(cueName, curPhase, sheet, rowPos, colPos, results);
     			}
     		}
         	rowPos++;
    		
         	
         	if(true) {
         		//Response
         		row = sheet.createRow(rowPos);
         		cell = row.createCell(colPos);
         		cell.setCellValue("Realtime Response");
         		cell.setCellStyle(cs2);
         	
         		rowPos++;
         		
         		// 	first row: Trial names                         	
         		row = sheet.createRow(rowPos);
         		trialRow(max, colPos, row);
    		
         		rowPos++;

         		// export Cues
         		for (int y = 1; y < cueNames.length + 1; y++) {
         			String cueName = (String)cueNames[y-1];
         			if (cueName.length() == 1 && 
         					(Character.isUpperCase(cueName.charAt(cueName.length()-1))) 
         				&& curPhase.isCueInStimuli(cueName)) {
         				rowPos = exportResponse(cueName, curPhase, sheet, rowPos, colPos, results);
         			}
         		}
         	// export compound Cues
         		for (int y = 1; view.getIsSetCompound() && y < cueNames.length + 1; y++) {
         			String cueName = (String)cueNames[y-1];
         			if (cueName.length() > 1 && curPhase.isCueInStimuli(cueName)) {
         				rowPos = exportResponse(cueName, curPhase, sheet, rowPos, colPos, results);
         			}
         		}
             	rowPos++;
         		
         		rowPos++;
         		//Response
         		row = sheet.createRow(rowPos);
         		cell = row.createCell(colPos);
         		cell.setCellValue("Mean Response Per Trial");
         		cell.setCellStyle(cs2);
         	
         		rowPos++;
         		
         		// 	first row: Trial names                         	
         		row = sheet.createRow(rowPos);
         		trialRow(max, colPos, row);
    		
         		rowPos++;
         		
         	// export Cues
         		for (int y = 1; y < cueNames.length + 1; y++) {
         			String cueName = (String)cueNames[y-1];
         			if (cueName.length() == 1 && 
         					(Character.isUpperCase(cueName.charAt(cueName.length()-1))) 
         				&& curPhase.isCueInStimuli(cueName)) {
         				rowPos = exportAverageResponse(cueName, curPhase, sheet, rowPos, colPos, results);
         			}
         		}
         	// export compound Cues
         		for (int y = 1; view.getIsSetCompound() && y < cueNames.length + 1; y++) {
         			String cueName = (String)cueNames[y-1];
         			if (cueName.length() > 1 && curPhase.isCueInStimuli(cueName)) {
         				rowPos = exportAverageResponse(cueName, curPhase, sheet, rowPos, colPos, results);
         			}
         		}
             	rowPos++;
         	}
     		
    		// export configural cues (if any)
        	rowPos++;

        	boolean configural = false;
        	int maxConfigural = 0;
        	for(int p = 0; p < cueNames.length; p++) {
        		if(Character.isLowerCase(cueNames[p].charAt(0)) && curPhase.isCueInStimuli(cueNames[p].charAt(0) + "")) {
        			configural = true;
        			maxConfigural = Math.max(maxConfigural, 
        					results.get(cueNames[p]).getTrialCount());
        		}
        	}
        	
        	if(configural) {
        		//Realtime
        		row = sheet.createRow(rowPos);
        		cell = row.createCell(colPos);
        		cell.setCellValue("Realtime V");
        		cell.setCellStyle(cs2);
        		rowPos++;
         	
        		// first row: Trial names                         	
        		row = sheet.createRow(rowPos);
        		trialRow(maxConfigural, colPos, row);
    		
        		rowPos++;
        	
        		for (int y = 1; y < cueNames.length + 1; y++) {
        			String cueName = (String)cueNames[y-1];
        			String interfaceName;
        			if (cueName.length() == 1 && Character.isLowerCase(cueName.charAt(cueName.length()-1))) {
        				// export compound Cues
        				rowPos = exportComponents(cueName, curPhase, sheet, rowPos, colPos, results);
             			}
        		}
        		
        		//Trial average
             	row = sheet.createRow(rowPos);
             	cell = row.createCell(colPos);
             	cell.setCellValue("V Per Trial");
             	cell.setCellStyle(cs2);
             	
             	rowPos++;
             	
             // first row: Trial names                         	
 	            row = sheet.createRow(rowPos);
 	            trialRow(maxConfigural, colPos, row);
        		
        		
             	rowPos++;

             	// export Cues
         		for (int y = 1; y < cueNames.length + 1; y++) {
         			String cueName = (String)cueNames[y-1];
         			if (cueName.length() == 1 && 
         					(Character.isLowerCase(cueName.charAt(cueName.length()-1))) 
         				&& curPhase.isCueInStimuli(cueName)) {
         				rowPos = exportTrial(cueName, curPhase, sheet, rowPos, colPos, results);
         			}
         		}
         		rowPos++;
        	}
        	sheet.setColumnWidth(0, (short) ((50 * 5) / ((double) 1 / 20)));
        }
    }
    
     
    /**
     * Create some cell styles for the workbooks's table.
     * This could be borders, font style and size or even the 
     * background color.  
     *
     */
    private void createStyles() {
        
        // First style
        cs1 = wb.createCellStyle();

        groupFont = wb.createFont();
        groupFont.setFontHeightInPoints((short)24);
        groupFont.setFontName("Courier New");
        groupFont.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
        cs1.setFont(groupFont);
        cs1.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        cs1.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cs1.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cs1.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cs1.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cs1.setFillBackgroundColor(HSSFColor.PALE_BLUE.index);
        
        // Second style
        cs2 = wb.createCellStyle();
        
        titleFont = wb.createFont();
        titleFont.setFontHeightInPoints((short)14);
        titleFont.setFontName("Courier New");
        cs2.setFont(titleFont);
        
        // Third style
        cs3 = wb.createCellStyle();
        
        tableTopFont = wb.createFont();
        tableTopFont.setFontHeightInPoints((short)12);
        tableTopFont.setFontName("Courier New");
        tableTopFont.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
        cs3.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        cs3.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cs3.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cs3.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cs3.setBorderTop(XSSFCellStyle.BORDER_THIN);
//        cs3.setFillBackgroundColor(HSSFColor.GREY_25_PERCENT.index);
        cs3.setFillBackgroundColor(HSSFColor.RED.index);
        cs3.setFont(tableTopFont);
        
        // Fourth style
        cs4 = wb.createCellStyle();
        
        tableContFont = wb.createFont();
        tableContFont.setFontHeightInPoints((short)12);
        tableContFont.setFontName("Courier New");
        cs4.setFont(tableContFont);
        cs4.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        cs4.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cs4.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cs4.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cs4.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cs4.setFillBackgroundColor(HSSFColor.GREY_25_PERCENT.index);
        
        // Fifth(highlight) style
        cs5 = wb.createCellStyle();
        cs5.setFont(tableContFont);
        cs5.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        cs5.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cs5.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cs5.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cs5.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cs5.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
        cs5.setFillPattern(CellStyle.SOLID_FOREGROUND);
    }
    
    /**
     * Helper function for dumping a trial average for a stimulus to
     * an excel sheet.
     * @param cueName
     * @param curPhase
     * @param sheet
     * @param rowPos
     * @param colPos
     * @param results
     * @return
     */
    
    protected int exportTrial(String cueName, SimPhase curPhase, Sheet sheet, 
    		int rowPos, int colPos, Map<String, CueList> results) {
    	// export Cues
        	String interfaceName = getInterfaceName(cueName);
     		if (curPhase.isCueInStimuli(cueName)) {
 	   			CueList cues = results.get(cueName);
 	   			for (int x = 0; x < 1; x++){
 	   				row = sheet.createRow(rowPos);
 	   				for(int z = 0; z <= cues.getTrialCount(); z++) {
 	   					if (z == 0) {
 	   						cell = row.createCell((colPos + z));
 	   						cell.setCellValue(interfaceName + "");
 	   						cell.setCellStyle(cs3);
 	   					} else {
 	   						
 	   						cell = row.createCell((colPos + z));
 	   						cell.setCellValue(cues.averageAssoc(z-1));
 	   						cell.setCellStyle(cs4);
 	   						//sheet.setColumnWidth(x, (short) ((50 * 4) / ((double) 1 / 20)));
 	   					}
 	   				}
 	   				rowPos++;
        		//}
 	   			}
     		}
     	return rowPos;
    }
    
    /**
     * Helper function to correctly format the print name of a cue
     * @param cueName
     * @return
     */
    
    protected String getInterfaceName(String cueName) {
    	String interfaceName;
    	if (cueName.length() > 1) {
 			if (Character.isUpperCase(cueName.charAt(cueName.length()-1))) { //compound
        		interfaceName = cueName; // no change
 			}
 			else { // configural compound
           		String compoundName = cueName.substring(0,cueName.length()-1);
					//interfaceName = "[" + compoundName + "�]";
					interfaceName = "[" + compoundName + "]";
 			} 
		} else if (Character.isLowerCase(cueName.charAt(0)) && !cueName.endsWith("ω")) {
			interfaceName = model.cueName2InterfaceName(cueName);
		}	else {
				interfaceName = cueName; // no change
		}
    	return interfaceName;
    }
    
    /**
     * Export the components of a cue to excel.
     * @param cueName
     * @param curPhase
     * @param sheet
     * @param rowPos
     * @param colPos
     * @param results
     * @return
     */
    
    protected int exportComponents(String cueName, SimPhase curPhase, Sheet sheet, 
    		int rowPos, int colPos, Map<String, CueList> results) {
        	String interfaceName;
     		if (curPhase.isCueInStimuli(cueName)) {
        		interfaceName = getInterfaceName(cueName);
 	   			CueList cues = results.get(cueName);
 	   			for (int x = 0; x < cues.size()  && x < curPhase.getMaxDuration(); x++) {
 	   				row = sheet.createRow(rowPos);
 	   				SimCue curcue = cues.get(x);
 	   				for (int z = 0; z <= cues.getTrialCount(); z++) { 
 	   					if (z == 0) {
 	   						cell = row.createCell((colPos));
 	   						cell.setCellValue(interfaceName + " Component "+(x+1));
 	   						cell.setCellStyle(cs3);
 	   					} else {
 	   						
 	   						cell = row.createCell((colPos + z));
 	   						cell.setCellValue(curcue.getAssocValueVector().get(z-1).doubleValue());
 	   						cell.setCellStyle(cs4);
 	   						
 	   					}
 	   				}
 	   				rowPos++;
 	   			}
     		} 
     		return rowPos;
    }
    
    /**
     * Export the response of a cue to excel.
     * @param cueName
     * @param curPhase
     * @param sheet
     * @param rowPos
     * @param colPos
     * @param results
     * @return
     */
    
    protected int exportResponse(String cueName, SimPhase curPhase, Sheet sheet, 
    		int rowPos, int colPos, Map<String, CueList> results) {
        	String interfaceName;
     		if (curPhase.isCueInStimuli(cueName)) {
        		interfaceName = getInterfaceName(cueName);
 	   			CueList cues = results.get(cueName);
 	   			for (int x = 0; x < cues.size() && x < curPhase.getMaxDuration(); x++) {
 	   				row = sheet.createRow(rowPos);
 	   				SimCue curcue = cues.get(x);
 	   				for (int z = 0; z <= cues.getTrialCount(); z++) { 
 	   					if (z == 0) {
 	   						cell = row.createCell((colPos));
 	   						cell.setCellValue(interfaceName + " Component "+(x+1));
 	   						cell.setCellStyle(cs3);
 	   					} else {
 	   						
 	   						cell = row.createCell((colPos + z));
 	   						cell.setCellValue(curcue.response(z-1));
 	   						cell.setCellStyle(cs4);
 	   						if(cues.getMaxCueList().get(z-1) < x) {
 	   							//cell.setCellStyle(cs5);
 	   						}
 	   						//sheet.setColumnWidth(z, (short) ((50 * 4) / ((double) 1 / 20)));
 	   					}
 	   				}
 	   				rowPos++;
 	   			}
     		} 
     		return rowPos;
    }
    
    /**
     * Export the average response of a cue to excel.
     * @param cueName
     * @param curPhase
     * @param sheet
     * @param rowPos
     * @param colPos
     * @param results
     * @return
     */
    
    protected int exportAverageResponse(String cueName, SimPhase curPhase, Sheet sheet, 
    		int rowPos, int colPos, Map<String, CueList> results) {
        	String interfaceName;
     		if (curPhase.isCueInStimuli(cueName)) {
        		interfaceName = getInterfaceName(cueName);
 	   			CueList cues = results.get(cueName);
 	   		row = sheet.createRow(rowPos);	
 	   		for (int z = 0; z <= cues.getTrialCount(); z++) { 
					if (z == 0) {
						cell = row.createCell((colPos));
						cell.setCellValue("Mean response "+interfaceName);
						cell.setCellStyle(cs3);
					} else {
						
						cell = row.createCell((colPos + z));
						cell.setCellValue(cues.averageResponse(z-1));
						cell.setCellStyle(cs4);
						//sheet.setColumnWidth(z, (short) ((50 * 4) / ((double) 1 / 20)));
					}
				}
 	   				rowPos++;
     		} 
     		return rowPos;
    }
    
    /**
     * Print a row of trial headers.
     * @param num
     * @param colPos
     * @param row2
     */
    
    private void trialRow(int num, int colPos, Row row2) {
    	for (int x = 1; x <= num; x++) {
			cell = row2.createCell(colPos + x);
			cell.setCellValue("Trial " + x);
			cell.setCellStyle(cs3);
			//row2.getSheet().setColumnWidth(x, (short) ((50 * 4) / ((double) 1 / 20)));
		}
    }
    

	/**
	 * @return the model being exported.
	 */
	public SimModel getModel() {
		return model;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			doExport();
		} catch (IOException e) {
			success = false;
		}
	}

	/**
	 * @return true if the export succeeded
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * @param control
	 */
	public void setControl(ModelControl control) {
		this.control = control;
	}
	
}
