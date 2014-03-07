package simulator.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import simulator.Simulator;
import simulator.configurables.OnsetConfig;
import simulator.configurables.StimulusOnsetConfig;
import simulator.configurables.TimingConfiguration;
import simulator.configurables.USConfiguration;
import simulator.editor.OnsetsEditor;
import simulator.editor.USEditor;
import simulator.util.ValuesTableModel;

public class TimingDialog extends JDialog {
	private final JPanel contentPanel = new JPanel();
	private JTable table;
	/**  configuration storage. **/
	private TimingConfiguration timings;
	/** Boolean indicating that the US should be hidden. **/
	private boolean usHidden;
	private TableColumn usCol;


	/**
	 * Create the dialog.
	 */
	public TimingDialog(ActionListener listener) {
		usHidden = false;
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			TimingValuesTableModel tm = new TimingValuesTableModel();
			table = new JTable(tm) {
				
				//Working around a nullpointer thrown because we return an interface
				//for the onsets column.
				public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
				    if (columnClass == null) {
				      return null;
				    }
				    else {
				      Object renderer = defaultRenderersByColumnClass.get(columnClass);
				      if (renderer != null) {
				        return (TableCellRenderer)renderer;
				      }
				      else {
				        Class<?> superclass = columnClass.getSuperclass(); 
				        if (superclass==null) {
				          return getDefaultRenderer(Object.class);
				        }
				        return getDefaultRenderer(superclass);
				      }
				    }
				  }
			};
			//Make window close on second enter press
			InputMap map = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).getParent();
			map.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
			tm.setValuesTable(true);
			DefaultCellEditor singleClickEditor = new DefaultCellEditor(new JTextField());
		    singleClickEditor.setClickCountToStart(1);
		    table.setDefaultEditor(Object.class, singleClickEditor);
		    table.setCellSelectionEnabled(false);
			//Add custom editors for onset and us relation objects
			table.setDefaultEditor(StimulusOnsetConfig.class,
	                new OnsetsEditor());
			table.setDefaultEditor(USConfiguration.class,
	                new USEditor());
			table.setCellSelectionEnabled(false);
			table.getColumnModel().getColumn(2).setPreferredWidth(20);
			table.doLayout();
			table.requestFocus();
			
			JScrollPane scrollPane = new JScrollPane(table);
			contentPanel.add(scrollPane);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(listener);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
	}

	/**
	 * Produces a duration configuration from the table model by
	 * merging together all the cells.
	 * @return a new OnsetConfig holding the input duration config.
	 */
	
	private OnsetConfig getOnsetConfig() {
		TableModel model = table.getModel();
		OnsetConfig onsets = new OnsetConfig();
		for (int i = 0; i < model.getRowCount(); i++) {
			onsets.set((String) model.getValueAt(i, 1), (StimulusOnsetConfig) model.getValueAt(i, 2));
		}
		onsets.setConfigured(true);
		return onsets;
	}
	
	/**
	 * Produces a mapping from CS names to US relationships.
	 * @return a new OnsetConfig holding the input duration config.
	 */
	
	private Map<String, USConfiguration> getUSConfig() {
		TableModel model = table.getModel();
		Map<String, USConfiguration> relationships = new HashMap<String, USConfiguration>();
		for (int i = 0; i < model.getRowCount(); i++) {
			relationships.put((String) model.getValueAt(i, 1), (USConfiguration) model.getValueAt(i, 3));
		}
		return relationships;
	}
	
	/**
	 * Set individual us configs in the model.
	 * @param currentConfig
	 */
	
	private void setUSConfig(Map<String, USConfiguration> currentConfig) {
		TableModel model = table.getModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			String cue = (String) model.getValueAt(i, 1);
			USConfiguration tmp = currentConfig.get(cue);
			model.setValueAt(tmp, i, 3);
		}
	}

	/**
	 * Set individual onset configs in the model.
	 * @param currentConfig
	 */
	
	private void setOnsetConfig(OnsetConfig currentConfig) {
		TableModel model = table.getModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			String cue = (String) model.getValueAt(i, 1);
			StimulusOnsetConfig tmp = currentConfig.getMap().get(cue);
			model.setValueAt(tmp, i, 2);
		}
	}
	
	/**
	 * Table model for timings.
	 * 
	 * @author J Gray
	 *
	 */
	private class TimingValuesTableModel extends ValuesTableModel {
        
	    /**
         * OnsetValuesTableModel Constructor method.
         */
        public TimingValuesTableModel() {
        	super();
        	col = 4;
    		columnNames = new String[]{"US Length","CS", "CS Temporal Properties", "Conditioning"};
        }
        
        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        public void setValueAt(Object value, int row, int col) {           
            super.setValueAt(value, row, col);
        	if(col == 2) {
        		USConfiguration neighbour = (USConfiguration) getValueAt(row, 3);
        		neighbour.setForwardDefault(((StimulusOnsetConfig)value).getMean());
        	}
        }
        
        /**
         * CS names uneditable, only first cell editable for US length.
         */
        public boolean isCellEditable(int row, int col) {
            return col  == 1 || col == 0 && row > 0 ? false : true;
        }
        
        /**
         * Return an interface for the onsets column.
         */
        
        public Class getColumnClass(int column) {
        	Class c = super.getColumnClass(column);
        	if(column == 2) {
        		c = StimulusOnsetConfig.class;
        	}
        	return c;
        }
	  
        /**
         * Initialize the table with default values, or those from the
         * provided onset configuration of available.
         * @param iniValues
         */
        public void setValuesTable(boolean iniValues) {   
        	Vector data1 = new Vector();        	          
        	
        	try {	
        		row = 0;
        		col = 4;
        		columnNames = new String[]{"US Length","CS", "CS Temporal Properties", "Conditioning"};
        		
        		if(timings == null) {
        	        timings = new TimingConfiguration();
        		}
        		
        		for(Entry<String, StimulusOnsetConfig> stim : timings.getDurations().getMap().entrySet()) {
        			Object record[] = new Object[col];
        			record[0] = "";
        			record[1] = stim.getKey();
        			record[2] = stim.getValue();
        			record[3] = timings.getRelation(stim.getKey());
        			data1.add(record);
        		}
        		if(!data1.isEmpty()) {
        			((Object[])data1.get(0))[0] = timings.getUsDuration() + "";
        		}
            	setData(data1);
            	fireTableChanged(null); // notify everyone that we have a new table.
        	}
        	catch(Exception e) {
        		setData(new Vector()); // blank it out and keep going. 
        		e.printStackTrace();                
        	}
        	if(!timings.isReinforced() && !data1.isEmpty()) {
        		table.setValueAt("0",0, 0);
    			usCol = table.getColumnModel().getColumn(0);
    			table.getColumnModel().removeColumn(usCol);
    		}
        }
	}

	public void setConfig(TimingConfiguration currentConfig) {
		timings = currentConfig;
		((TimingValuesTableModel)table.getModel()).setValuesTable(true);
	}

	public TimingConfiguration getConfig() {
		TableModel model = table.getModel();
		if(model.getRowCount() > 0) {
			String inputUsLength = (String) model.getValueAt(0, 0);
			inputUsLength = inputUsLength.isEmpty() ? "0" : inputUsLength;
			timings = new TimingConfiguration();
			timings.setUsDuration(Double.parseDouble(inputUsLength));
			timings.setDurations(getOnsetConfig());
			timings.setRelations(getUSConfig());
			timings.setConfigured(true);
		}
		return timings;
	}

	/**
	 * @param contains
	 */
	public void setUSHidden(boolean hide) {
		usHidden = hide;
		if(hide) {
			timings.setUsDuration(0);
		} else if(!hide && timings.getUsDuration() == 0) {
			timings.setUsDuration(1*Simulator.getController().getModel().getTimestepSize());
		}
	}
}
