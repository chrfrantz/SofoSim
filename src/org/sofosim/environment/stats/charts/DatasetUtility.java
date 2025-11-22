package org.sofosim.environment.stats.charts;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FileUtils;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.sofosim.environment.stats.Statistics;
import org.sofosim.environment.stats.TimeUtility;
import org.sofosim.environment.stats.printer.GraphsPrinter;
import org.sofosim.environment.stats.printer.ZipWriter;

/**
 * The DataSetUtility allows to write datasets from charts produced by SofoSim's Statistics module 
 * to outfiles and reread them to regenerate the charts offline while allowing layout manipulations.
 * 
 * @author cfrantz
 *
 */
public class DatasetUtility {
	
	/**
	 * Delimiter used in serialised files
	 */
	public static final String DELIMITER = "|";
	/**
	 * Settings file of DataUtility
	 */
	public static final String SETTINGS_FILE = "DataUtility_Settings";
	/**
	 * Header for Time Series data files
	 */
	protected static final String HEADER_TIME_SERIES = "## DATA UTILITY ## TIME SERIES ## v1 ##";
	/**
	 * Number of tokens in time series dataset file per entry
	 */
	protected static final int TIME_SERIES_DATASET_TOKENS = 6;
	/**
	 * Header for Category dataset files
	 */
	protected static final String HEADER_CATEGORIES_DATASET = "## DATA UTILITY ## CATEGORY DATASET ## v1 ##";
	/**
	 * Number of tokens in categories dataset file per entry
	 */
	protected static final int CATEGORIES_DATASET_TOKENS = 4;
	/**
	 * Indicator for unknown dataset type
	 */
	protected static final String HEADER_UNKNOWN_DATASET_TYPE = "Unknown Dataset type";
	/**
	 * Indicator for empty file
	 */
	protected static final String EMPTY_OR_INVALID_FILE = "Empty or unknown file type";
	/**
	 * Indicator that file could not be found
	 */
	protected static final String FILE_NOT_FOUND = "File not found";
	/**
	 * RadarChart implementation used for visualisation of RadarChart
	 */
	protected static Class<? extends RadarChart> radarChartImplementation = DefaultRadarChart.class;
	/**
	 * Indicates if charts are already shown during selection in FileChooser.
	 */
	protected static boolean showChartDuringSelection = true;
	/**
	 * Method invoked to format Time Series charts. Ignored if null.
	 */
	protected static Method formatTimeSeriesMethod = null;
	/**
	 * Filename of last opened dataset.
	 */
	private static File lastFilename = null;
	/**
	 * Default extension for dataset files.
	 */
	private static final String datasetExtension = ".txt";
	/**
	 * Extension for ZIP files.
	 */
	private static final String zipFileExtension = ".zip";
	
	/**
	 * Sets a method to be invoked to format drawn Time Series charts. The method needs to take three 
	 * parameters: {@link TimeSeriesChartWrapper} instance, org.jfree.chart.plot.XYPlot instance, 
	 * org.sofosim.stats.Statistics instance.
	 * @param classContainingMethod Class containing the method
	 * @param methodName Method name (ensure that it can accept the aforementioned arguments)
	 */
	protected static void setFormatTimeSeriesChartsMethod(Class classContainingMethod, String methodName){
		try {
			//System.out.println(DataStructurePrettyPrinter.decomposeRecursively(classContainingMethod.getDeclaredMethods(), null));
			formatTimeSeriesMethod = classContainingMethod.getDeclaredMethod(methodName, TimeSeriesChartWrapper.class, XYPlot.class, Statistics.class);
			if(!Modifier.isStatic(formatTimeSeriesMethod.getModifiers())){
				System.err.println("Time series formatting method needs to be static. Ignoring it...");
				formatTimeSeriesMethod = null;
				return;
			}
			formatTimeSeriesMethod.setAccessible(true);
			
		} catch (NoSuchMethodException | SecurityException e) {
			JOptionPane.showMessageDialog(null, "Could not find static time series chart formatting method with name '" 
					+ methodName + "' in class '" + classContainingMethod.getSimpleName() + "' " + System.getProperty("line.separator")
					+ " taking instances of TimeSeriesChartWrapper, XYPlot and Statistics as parameters." + System.getProperty("line.separator")
					+ "Will ignore it when plotting time series charts.", "Error", JOptionPane.ERROR_MESSAGE);
			//e.printStackTrace();
		}
	}
	
	/**
	 * Method invoked to configure the statistics instance (and thus charts) globally. Ignored if null.
	 */
	protected static Method globalStatsConfigurationMethod = null;
	
	/**
	 * Sets a method to be invoked to configure the Statistics instance before plotting charts. 
	 * The method needs to take an object of type org.sofosim.stats.Statistics as parameter.
	 * @param classContainingMethod Class containing the method
	 * @param methodName Method name (ensure that it can accept the aforementioned arguments)
	 */
	protected static void setGlobalStatsConfigurationMethod(Class classContainingMethod, String methodName){
		try {
			//System.out.println(DataStructurePrettyPrinter.decomposeRecursively(classContainingMethod.getDeclaredMethods(), null));
			globalStatsConfigurationMethod = classContainingMethod.getDeclaredMethod(methodName, Statistics.class);
			if(!Modifier.isStatic(globalStatsConfigurationMethod.getModifiers())){
				System.err.println("Stats configuration method needs to be static. Ignoring it...");
				globalStatsConfigurationMethod = null;
				return;
			}
			globalStatsConfigurationMethod.setAccessible(true);
			
		} catch (NoSuchMethodException | SecurityException e) {
			JOptionPane.showMessageDialog(null, "Could not find static stats configuration method with name '" 
					+ methodName + "' in class '" + classContainingMethod.getSimpleName() + "' " + System.getProperty("line.separator")
					+ " taking an instance of Statistics as parameter." + System.getProperty("line.separator")
					+ "Will ignore it when plotting time series charts.", "Error", JOptionPane.ERROR_MESSAGE);
			//e.printStackTrace();
		}
	}
	
	/**
	 * Saves dataset of time series charts to disk. Input format is <Chart name><<Series name><List of XYDataItems>>.
	 * @param filename Filename used to store data. 
	 * @param map data to be saved
	 * @param zipFile Indicates if a ZIP file should be generated instead of a uncompressed TXT file.
	 */
	public static void saveTimeSeriesDataToDisk(String filename, Map<String, LabelledDataset> map, boolean zipFile){
		if(map != null){
			synchronized (map) {
				StringBuilder outString = new StringBuilder();
				//write header
				outString.append(HEADER_TIME_SERIES).append(System.getProperty("line.separator"));
				//iterate through <ID>/<Series name/Series data> structure
				for(Entry<String, LabelledDataset> entry: map.entrySet()){
					for(Entry<String, XYSeries> entry2: entry.getValue().entrySet()){
						for(int i = 0; i < entry2.getValue().getItemCount(); i++){
							XYDataItem item = entry2.getValue().getDataItem(i);
							//add chart name
							outString.append(entry.getKey()).append(DELIMITER);
							//x label
							outString.append(entry.getValue().xLabel).append(DELIMITER);
							//y label
							outString.append(entry.getValue().yLabel).append(DELIMITER);
							//add series name
							outString.append(entry2.getKey()).append(DELIMITER);
							//add data item x and y value
							outString.append(item.getX()).append(DELIMITER);
							outString.append(item.getY()).append(DELIMITER);
							outString.append(System.getProperty("line.separator"));
						}
					}
				}
				//and write out
				if(zipFile) {
					//write to zip file
					ZipWriter.writeDataToZipFile(filename, null, outString, datasetExtension, zipFileExtension);
				} else {
					//write to conventional txt file
					try {
						FileUtils.write(new File(filename), outString.toString());
						System.out.println("Wrote datasets for time series charts to outfile '" + filename + "'.");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Reads datasets written to disk and prepares them as Map with structure
	 * <Chart name><<Series name><List of XYDataItems>>
	 * @param filename Filename to be read
	 * @return
	 */
	public static Map<String, LabelledDataset> readTimeSeriesDataFromDisk(String filename){
		List<String> inStrings = readData(filename);
		
		LinkedHashMap<String, LabelledDataset> map = new LinkedHashMap<>();
		StringTokenizer tokenizer = null;
		if(inStrings == null){
			System.err.println("Chosen file '" + filename + "' empty. Processing aborted...");
			return null;
		}
		if(!inStrings.isEmpty()){
			//check for header
			if(!inStrings.get(0).equals(HEADER_TIME_SERIES)){
				System.err.println("Chosen file '" + filename + "' does not contain Time Series Dataset. Processing aborted...");
				return null;
			}
		}
		for(int i = 1; i< inStrings.size(); i++){
			tokenizer = new StringTokenizer(inStrings.get(i), DELIMITER);
			if(tokenizer.countTokens() != TIME_SERIES_DATASET_TOKENS){
				System.err.println("Invalid number of tokens for entry '" + inStrings.get(i) + "'. Processing aborted...");
				return null;
			}
			String chartName = tokenizer.nextToken();
			String xLabel = tokenizer.nextToken();
			String yLabel = tokenizer.nextToken();
			String seriesName = tokenizer.nextToken();
			String x = tokenizer.nextToken();
			String y = tokenizer.nextToken();
			if(map.containsKey(chartName)){
				if(map.get(chartName).containsKey(seriesName)){
					//adding value for existing series
					map.get(chartName).get(seriesName).add(Double.parseDouble(x), Double.parseDouble(y));
				} else {
					//adding new series to chart
					XYSeries series = new XYSeries(seriesName);
					series.add(Double.parseDouble(x), Double.parseDouble(y));
					map.get(chartName).put(seriesName, series);
				}
			} else {
				//adding new chart
				XYSeries series = new XYSeries(seriesName);
				series.add(Double.parseDouble(x), Double.parseDouble(y));
				LabelledDataset nestedMap = new DatasetUtility().new LabelledDataset(xLabel, yLabel);
				nestedMap.put(seriesName, series);
				map.put(chartName, nestedMap);
			}
		}
		return map;
	}
	
	/**
	 * Saves RadarChart dataset to outfile in order to recreate radar chart.
	 * @param filename Filename to save it to
	 * @param radarChart RadarChart instance whose dataset is to be saved
	 */
	public static void saveCategoryDataToDisk(String filename, RadarChart radarChart, boolean zipFile){
		
		CategoryDataset dataset = radarChart.getDataset();
		//System.out.println("Categories: " + dataset.getColumnCount());
		//System.out.println("Series: " + dataset.getRowCount());
		StringBuilder outString = new StringBuilder();
		//write header
		outString.append(HEADER_CATEGORIES_DATASET).append(System.getProperty("line.separator"));
		synchronized(dataset){
			//per series
			for(int i = 0; i < dataset.getRowCount(); i++){
				//per category
				for(int j = 0; j < dataset.getColumnCount(); j++){
					//only save values that are not null
					if(dataset.getValue(i, j) != null){
						//print title of chart
						outString.append(radarChart.getTitle()).append(DELIMITER);
						//name of series
						outString.append(dataset.getRowKey(i)).append(DELIMITER);
						//name of category
						outString.append(dataset.getColumnKey(j)).append(DELIMITER);
						//values
						outString.append(dataset.getValue(i, j)).append(DELIMITER);
						//line break
						outString.append(System.getProperty("line.separator"));
					}
				}
			}
			//and write out to file
			if(zipFile) {
				//write to zip file
				ZipWriter.writeDataToZipFile(filename, radarChart.getTitle(), outString, datasetExtension, zipFileExtension);
			} else {
				//write to regular txt file
				try {
					FileUtils.write(new File(filename), outString.toString());
					System.out.println("Wrote datasets for radar chart '" 
							+ radarChart.getTitle() + "' to outfile '" + filename + "'.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Reads category dataset from disk.
	 * @param filename Filename of file to be read
	 * @return CategoryDataset that additionally maintains title information
	 */
	public static TitledCategoryDataset readCategoryDataFromDisk(String filename){
		TitledCategoryDataset dataset = null;
		List<String> inStrings = readData(filename);
		
		StringTokenizer tokenizer = null;
		if(inStrings == null){
			System.err.println("Chosen file '" + filename + "' empty. Processing aborted...");
			return null;
		}
		if(!inStrings.isEmpty()){
			if(!inStrings.get(0).equals(HEADER_CATEGORIES_DATASET)){
				System.err.println("Chosen file '" + filename + "' does not contain Category Dataset. Processing aborted...");
				return null;
			}
		}
		for(int i = 1; i< inStrings.size(); i++){
			tokenizer = new StringTokenizer(inStrings.get(i), DELIMITER);
			if(tokenizer.countTokens() != CATEGORIES_DATASET_TOKENS){
				System.err.println("Invalid number of tokens for entry '" + inStrings.get(i) + "'. Processing aborted...");
				return null;
			}
			String chartName = tokenizer.nextToken();
			String seriesName = tokenizer.nextToken();
			String category = tokenizer.nextToken();
			String value = tokenizer.nextToken();
			if(dataset == null && !value.equals("null")){
				dataset = new TitledCategoryDataset(chartName);
			}
			if(!value.equals("null")){
				//only add value to dataset if not null
				dataset.addValue(Double.parseDouble(value), seriesName, category);
			}
		}
		return dataset;
	}
	
	private static void modifyCharts(Stats stats){
		stats.printFileNamesOnCharts = true;
	}
	
	private static List<String> readData(String filename) {
		List<String> inStrings = null;
		if(filename.endsWith(zipFileExtension)) {
			inStrings = ZipWriter.readDataFromZipFile(filename);
		} else {
			//else assume conventional file
			try {
				inStrings = FileUtils.readLines(new File(filename));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return inStrings;
	}
	
	/**
	 * Returns the type of dataset in given file based 
	 * on the file header.
	 * See constants in DataSetUtility for possible 
	 * responses.
	 * @param filename Filename of file to be checked
	 * @return
	 */
	public static String getDatasetType(String filename){
		File file = new File(filename);
		if(file.exists()){
			List<String> content = readData(filename);
			
			if(content != null && !content.isEmpty()){
				switch(content.get(0)){
					case HEADER_CATEGORIES_DATASET:
						return HEADER_CATEGORIES_DATASET;
					case HEADER_TIME_SERIES:
						return HEADER_TIME_SERIES;
				}
				return HEADER_UNKNOWN_DATASET_TYPE;
			} else {
				return EMPTY_OR_INVALID_FILE;
			}
		} else {
			return FILE_NOT_FOUND;
		}
	}
	
	/**
	 * Reads the datasets from a given file and recreates the charts.
	 * @param filename
	 */
	public static String readDataFromDiskAndRebuildChart(String filename){
		String datasetType = getDatasetType(filename);
		switch(datasetType){
			case HEADER_TIME_SERIES:
				Map<String, LabelledDataset> chartSet = readTimeSeriesDataFromDisk(filename);
				if(chartSet != null){
					DatasetUtility utility = new DatasetUtility();
					stats = utility.new Stats();
					modifyCharts(stats);
					for(Entry<String, LabelledDataset> entry: chartSet.entrySet()){
						stats.setupChart(entry.getKey(), 
								entry.getValue().xLabel == null ? Stats.ROUNDS : entry.getValue().xLabel, 
								entry.getValue().yLabel == null ? entry.getKey() : entry.getValue().yLabel);
						ChartDataSeriesMap map = stats.getChartDataMap().get(entry.getKey());
						map.setDataSeries(entry.getValue());
					}
					stats.createCharts();
				}
				return null;
			case HEADER_CATEGORIES_DATASET:
				TitledCategoryDataset dataset = readCategoryDataFromDisk(filename);
				if(dataset != null){
					DatasetUtility utility = new DatasetUtility();
					stats = utility.new Stats();
					stats.printRadarCharts = true;
					modifyCharts(stats);
					RadarChart radarChart = stats.getRadarChart(dataset.title);
					radarChart.setDataset(dataset);
					radarChart.prepareGui();
					radarChart.setVisible(true);
					stats.registerForPrinting(radarChart);
				}
				return null;
			case HEADER_UNKNOWN_DATASET_TYPE:
				return "Unsupported dataset type in file." + System.getProperty("line.separator") 
						+"File: '" + filename + "'.";
			case EMPTY_OR_INVALID_FILE:
				return "Cannot generate charts from invalid or empty dataset file." + System.getProperty("line.separator") 
						+"File: '" + filename + "'.";
			case FILE_NOT_FOUND:
				return "File could not be found." + System.getProperty("line.separator") 
						+"File: '" + filename + "'.";
		}
		return "Could not detect dataset type in file." + System.getProperty("line.separator") 
						+"File: '" + filename + "'.";
		
	}
	
	/**
	 * Reference to Statistics helper implementation
	 */
	private static Stats stats = null;
	/**
	 * Frame holding refresh button that activates a renewed choice of datasets.
	 */
	private static JFrame refreshFrame = null;
	/**
	 * JFileChooser instance used to pick dataset file to be visualised.
	 */
	private static JFileChooser ui = null;
	
	/**
	 * Lets the user choose file used for the recreation of charts.
	 * @param args
	 */
	public static void main(String args[]){
		ui = new JFileChooser(startDirectory());
		ui.setFileSelectionMode(JFileChooser.FILES_ONLY);
		ui.setFileFilter(new FileFilter(){

			@Override
			public boolean accept(File f) {
				if(f.isDirectory() || 
						f.getName().endsWith(datasetExtension) || 
						f.getName().endsWith(zipFileExtension)){
					return true;
				}
				return false;
			}

			@Override
			public String getDescription() {
				return "Directories, .txt, .zip files";
			}
			
		});
		// derived from http://www.exampledepot.8waytrips.com/egs/javax.swing.filechooser/ChgEvent.html
		ui.addPropertyChangeListener(new PropertyChangeListener() {
		    public void propertyChange(PropertyChangeEvent evt) {
		        if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY
		                .equals(evt.getPropertyName())) {
		            JFileChooser chooser = (JFileChooser)evt.getSource();
		            File oldFile = (File)evt.getOldValue();
		            File newFile = (File)evt.getNewValue();

		            // The selected file should always be the same as newFile
		            File curFile = chooser.getSelectedFile();
		            if((oldFile == null || !oldFile.equals(newFile)) && curFile != null){
		            	if(showChartDuringSelection){
		            		if(stats != null){
								stats.resetStats();
							}
		            		String response = readDataFromDiskAndRebuildChart(curFile.getAbsolutePath());
		    				printErrorMessageWithGivenResponse(null, response);
		            	}
		            }
		        } else if (JFileChooser.SELECTED_FILES_CHANGED_PROPERTY.equals(
		                evt.getPropertyName())) {
		            JFileChooser chooser = (JFileChooser)evt.getSource();
		            File[] oldFiles = (File[])evt.getOldValue();
		            File[] newFiles = (File[])evt.getNewValue();

		            // Get list of selected files
		            // The selected files should always be the same as newFiles
		            File[] files = chooser.getSelectedFiles();
		        }
		    }
		}) ;
		
		int ret = ui.showOpenDialog(null);
		if(ret == JFileChooser.APPROVE_OPTION){
			if(ui.getSelectedFile() != null){
				try {
					FileUtils.write(new File(SETTINGS_FILE), ui.getSelectedFile().getAbsolutePath());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String response = readDataFromDiskAndRebuildChart(ui.getSelectedFile().getAbsolutePath());
				lastFilename = ui.getSelectedFile();
				printErrorMessageWithGivenResponse(null, response);
			} else {
				JOptionPane.showMessageDialog(ui, "You picked an invalid file.");
			}
		} else {
			System.err.println("User cancelled DatasetFile selection.");
		}
		//open refresh button window
		if(refreshFrame == null){
			refreshFrame = new JFrame("Choose different file and redraw");
			refreshFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			JPanel panel = new JPanel();
			//File Chooser
			JButton btnChoose = new JButton("Choose different file");
			btnChoose.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if(ui != null){
						ui.showOpenDialog(null);
						if(stats != null){
							stats.resetStats();
						}
						String response = readDataFromDiskAndRebuildChart(ui.getSelectedFile().getAbsolutePath());
						printErrorMessageWithGivenResponse(refreshFrame, response);
					}
				}
			});
			panel.add(btnChoose);
			//Checkbox for auto-opening
			final JCheckBox chkAutoOpen = new JCheckBox("Auto-open charts");
			chkAutoOpen.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if(chkAutoOpen.isSelected()){
						showChartDuringSelection = true;
					} else {
						showChartDuringSelection = false;
					}
				}
			});
			panel.add(chkAutoOpen);
			//Print button
			JButton btnPrint = new JButton("Print chart...");
			btnPrint.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					final JDialog dialog = new JDialog(refreshFrame, true);
					JPanel dialogPanel = new JPanel();
					//can contain different types
					ArrayList framesAndCharts = new ArrayList();
					framesAndCharts.addAll(stats.getChartDataMap().keySet());
					framesAndCharts.addAll(stats.getFramesRegisteredForPrinting());
					//register in combobox
					final JComboBox cmbCharts = new JComboBox(framesAndCharts.toArray());
					dialogPanel.add(cmbCharts);
					//Button for printing
					JButton btnPrintChart = new JButton("Print selected chart");
					btnPrintChart.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent arg0) {
							final Object item = cmbCharts.getSelectedItem();
							if(item != null){
								new Thread(new Runnable(){

									@Override
									public void run() {
										if(item.getClass().equals(String.class)){
											stats.printChart(item.toString(), lastFilename.getName() + "_" + item.toString());
										} else if (item.getClass().equals(radarChartImplementation)){
											stats.printFrame((JFrame) item, lastFilename.getName() + "_" + item.toString());
										} else {
											JOptionPane.showMessageDialog(dialog, "Could not identify type of chosen chart " + item + " (Type: " + item.getClass().getSimpleName() + ")", "Error", JOptionPane.ERROR_MESSAGE);
										}
									}
									
								}).start();
								
							}
						}
					});
					dialogPanel.add(btnPrintChart);
					dialog.setContentPane(dialogPanel);
					dialog.pack();
					dialog.setVisible(true);
				}
			});
			panel.add(btnPrint);
			refreshFrame.setContentPane(panel);
			refreshFrame.pack();
			refreshFrame.setVisible(true);
		}
	}
	
	/**
	 * Shows an error dialog with a given content (response). Only opens if response is != null.
	 * @param parentComponent Component that is parent of dialog
	 * @param response Message to be shown
	 */
	private static void printErrorMessageWithGivenResponse(Component parentComponent, String response){
		if(response != null){
			JOptionPane.showMessageDialog(parentComponent, response, "DataUtility", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Returns the start directory for the DataUtility file chooser. If 
	 * no settings file ({@link #SETTINGS_FILE}) is present, it uses the 
	 * project directory.
	 * @return
	 */
	private static String startDirectory(){
		File file = new File(SETTINGS_FILE);
		if(file.exists()){
			List<String> settings;
			try {
				settings = FileUtils.readLines(file);
				if(settings != null && settings.size() > 0){
					return settings.get(0);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//else default to project directory
		return System.getProperty("user.dir");
	}
	
	/**
	 * Specialisation of LinkedHashMap that also holds x and y axis label.
	 * @author cfrantz
	 *
	 */
	public class LabelledDataset extends LinkedHashMap<String, XYSeries>{
		
		public final String xLabel;
		public final String yLabel;
		
		public LabelledDataset(String xLabel, String yLabel){
			this.xLabel = xLabel;
			this.yLabel = yLabel;
		}
		
		public LabelledDataset(String xLabel, String yLabel, LinkedHashMap<String, XYSeries> entries){
			this.xLabel = xLabel;
			this.yLabel = yLabel;
			this.putAll(entries);
		}
		
	}
	
	/**
	 * Special Statistics implementation only used for the recreation of charts.
	 * @author cfrantz
	 *
	 */
	private class Stats extends Statistics{

		public Stats() {
			super(false, false, -1);
			manageWindowsUsingPositionSaver = false;
			radarChartImplementation = DatasetUtility.this.radarChartImplementation;
			if(DatasetUtility.globalStatsConfigurationMethod != null){
				try{
					DatasetUtility.globalStatsConfigurationMethod.invoke(null, this);
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//set up time prefix
			//derive current time
			String timeAndPerhapsSimClass = TimeUtility.getCurrentTimeString(TimeUtility.DATE_FORMAT_SEMI_CONCATENATED);
			//save time only (for preparation of outfile - not subfolder - prefix)
			String timeOnly = timeAndPerhapsSimClass;
			this.outFilePrefix = timeOnly;
			
			//deactivate printing of rounds information in filenames added to output
			this.considerRoundsInFilenameGeneration = false;
			
			//set up outfolder
			setGlobalSubfolderForOutput("DatasetPrints");
			
			//initialise printer
			printer = new GraphsPrinter(this);
		}
		
		/**
		 * Show charts.
		 */
		public void createCharts(){
			setupChartFrames();
			checkForChangedDataSeries();
		}

		@Override
		public void singleStep() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void customMethodThatIsRunPeriodically() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void reset() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setupCharts() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void printGraphs() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void formatCharts(TimeSeriesChartWrapper chart, XYPlot plot, Statistics stats) {
			if(DatasetUtility.formatTimeSeriesMethod != null){
				try{
					DatasetUtility.formatTimeSeriesMethod.invoke(null, chart, plot, stats);
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		@Override
		public void runAtEndOfEachRound() {
			// TODO Auto-generated method stub
			
		}
		
	}
	



}
