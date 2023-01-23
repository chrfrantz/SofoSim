package org.sofosim.environment.stats.charts;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.statistics.SimpleHistogramBin;
import org.jfree.data.statistics.SimpleHistogramDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.sofosim.environment.stats.StatsCalculator;

public class DistributionChart extends JFrame {

	private static final boolean debug = false;
	private String title = null;
	private String xAxisLabel = null;
	private String yAxisLabel = null;
	private boolean showLegend = false;
	private SimpleHistogramDataset dataset = null;
	private Float tolerance = 0.1f;

	/**
	 * Initializes a histogram for given StatsCalculator values. 
	 * @param title Title for frame and chart
	 */
	public DistributionChart(String title){
		this(title, null, null, null, false);
	}
	
	/**
	 * Initializes a histogram for given StatsCalculator values. 
	 * @param title Title for frame and chart
	 * @param showLegend Indicates if legend should be shown
	 */
	public DistributionChart(String title, boolean showLegend){
		this(title, null, null, null, showLegend);
	}
	
	/**
	 * Initializes a histogram for given StatsCalculator values.
	 * Note: If network distribution is printed, x will usually be the 
	 * number of network connections and y the number of vertices 
	 * that have the connection count. 
	 * @param title Title for frame and chart
	 * @param xAxisLabel Label for Histogram x axis
	 * @param yAxisLabel Label for Histogram y axis
	 */
	public DistributionChart(String title, String xAxisLabel, String yAxisLabel) {
		this(title, null, xAxisLabel, yAxisLabel, false);
	}
	
	/**
	 * Initializes a histogram for given StatsCalculator values.
	 * Note: If network distribution is printed, x will usually be the 
	 * number of network connections and y the number of vertices 
	 * that have the connection count. 
	 * @param title Title for frame and chart
	 * @param xAxisLabel Label for Histogram x axis
	 * @param yAxisLabel Label for Histogram y axis
	 * @param showLegend Indicates if legend should be shown
	 */
	public DistributionChart(String title, String xAxisLabel, String yAxisLabel, boolean showLegend) {
		this(title, null, xAxisLabel, yAxisLabel, showLegend);
	}
	
	/**
	 * Initializes a histogram for given StatsCalculator values.
	 * Optionally, the user can specify tolerance for outer bins 
	 * (as of rounding problems during automated calculation).
	 * @param title Title for frame and chart
	 * @param toleranceForOuterBins Tolerance for left outer and right outer bin
	 */
	public DistributionChart(String title, Float toleranceForOuterBins){
		this(title, toleranceForOuterBins, null, null, false);
	}
	
	/**
	 * Initializes a histogram for given StatsCalculator values.
	 * Optionally, the user can specify tolerance for outer bins 
	 * (as of rounding problems during automated calculation).
	 * @param title Title for frame and chart
	 * @param toleranceForOuterBins Tolerance for left outer and right outer bin
	 * @param showLegend Indicates if legend should be shown
	 */
	public DistributionChart(String title, Float toleranceForOuterBins, boolean showLegend){
		this(title, toleranceForOuterBins, null, null, showLegend);
	}
	
	/**
	 * Initializes a histogram for given StatsCalculator values.
	 * Optionally, the user can specify tolerance for outer bins 
	 * (as of rounding problems during automated calculation).
	 * @param title Title for frame and chart
	 * @param toleranceForOuterBins Tolerance for left outer and right outer bin
	 * @param xAxisLabel Label for Histogram x axis
	 * @param yAxisLabel Label for Histogram y axis
	 */
	public DistributionChart(String title, Float toleranceForOuterBins, String xAxisLabel, String yAxisLabel){
		this(title, toleranceForOuterBins, xAxisLabel, yAxisLabel, false);
	}
	
	/**
	 * Initializes a histogram for given StatsCalculator values.
	 * Optionally, the user can specify tolerance for outer bins 
	 * (as of rounding problems during automated calculation).
	 * @param title Title for frame and chart
	 * @param toleranceForOuterBins Tolerance for left outer and right outer bin
	 * @param xAxisLabel Label for Histogram x axis
	 * @param yAxisLabel Label for Histogram y axis
	 * @param showLegend Indicates if legend should be shown
	 */
	public DistributionChart(String title, Float toleranceForOuterBins, String xAxisLabel, String yAxisLabel, boolean showLegend){
		super(title);
		this.title = title;
		this.xAxisLabel = xAxisLabel;
		this.yAxisLabel = yAxisLabel;
		this.showLegend = showLegend;
		if(toleranceForOuterBins != null){
			this.tolerance = toleranceForOuterBins;
		}
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
	}
	
	/**
	 * Generates a dataset for the histogram. Overwrites the existing dataset of the chart. 
	 * Once done, call prepareGui() to display/update the histogram with the current dataset.
	 * @param calculator StatsCalculator instance
	 * @param keyOfConcern Key of StatsCalculator holding the distribution
	 * @param numberOfBins Number of histogram bins (will be automatically reduced if not sufficient information).
	 * @return
	 */
	public IntervalXYDataset createDataset(StatsCalculator<Number> calculator,
			String keyOfConcern, Integer numberOfBins) {
		if(!calculator.hasKey(keyOfConcern) || calculator.getValues(keyOfConcern).isEmpty() || numberOfBins == null || numberOfBins <= 0){
			System.err.println("Histogram for key '" + keyOfConcern + "' cannot be generated as no entries or no valid number of bins specified.");
			dataset = null;
			return dataset;
		}
		ArrayList<Double> entries = new ArrayList<Double>();
		ArrayList<Number> numberEntries = calculator.getValues(keyOfConcern);
		for(int i = 0; i < numberEntries.size(); i++) {
			entries.add(numberEntries.get(i).doubleValue());
		}
		//initialize dataset with name
		dataset = new SimpleHistogramDataset(keyOfConcern);
		Double highest = calculator.getMaxValue(keyOfConcern);
		Double lowest = calculator.getMinValue(keyOfConcern);
		return generateDataSet(entries, highest, lowest, numberOfBins, true);
	}
	
	/**
	 * Creates dataset from input data in which the index represents 
	 * the bin and the value represents the count for that bin, i.e. 
	 * precalculated distribution. The value of index 0 represents 
	 * the count for that value, for index 3 represents the count of 
	 * elements that have the value 3.
	 * Core purpose of this method is the use with GraphStream 
	 * degree distribution data.
	 * @param inputData Input data array
	 * @param keyOfConcern Title and identifier for histogram
	 * @param numberOfBins Number of bins
	 * @return
	 */
	public IntervalXYDataset createDataset(int[] inputData,
			String keyOfConcern) {
		if(inputData == null || inputData.length == 0){// || numberOfBins == null || numberOfBins <= 0){
			System.err.println("Histogram for key '" + keyOfConcern + "' cannot be generated as no entries or no valid number of bins specified.");
			dataset = null;
			return dataset;
		}
		ArrayList<Double> dblList = new ArrayList<Double>();
		//initialize dataset with name
		dataset = new SimpleHistogramDataset(keyOfConcern);
		Double min = 0.0;
		Double max = Double.parseDouble(String.valueOf(inputData.length));//Double.MAX_VALUE * -1;
		for(int i = 0; i < inputData.length; i++){
			Double value = Double.parseDouble(String.valueOf(inputData[i]));
			//if value is zero, then there is no entry for that count, no need to enter
			for(int j = 0; j < value; j++){
				//enter the same value (index) as often as value indicates (decompose original raw data set)
				dblList.add(new Double(String.valueOf(i)));
			}
			//min = Math.min(min, i);
		}
		return generateDataSet(dblList, max, min, inputData.length, false);
	}
	
	/**
	 * Creates dataset for histogram
	 * @param entries Entries as doubles
	 * @param title Title of histogram (and name of dataset)
	 * @param highestValue Highest value in dataset
	 * @param lowestValue Lowest value in dataset
	 * @param numberOfBins Number of bins in dataset
	 * @param tolerance Should there be some tolerance to the left and right outer bin (for rounding errors)?
	 * @return
	 */
	public IntervalXYDataset createDataset(ArrayList<Double> entries, String title, Integer highestValue, Integer lowestValue, int numberOfBins, boolean tolerance){
		dataset = new SimpleHistogramDataset(title);
		return generateDataSet(entries, highestValue.doubleValue(), lowestValue.doubleValue(), numberOfBins, tolerance);
	}
	
	/**
	 * Creates dataset for histogram
	 * @param entries Entries as doubles
	 * @param title Title of histogram (and name of dataset)
	 * @param highestValue Highest value in dataset
	 * @param lowestValue Lowest value in dataset
	 * @param numberOfBins Number of bins in dataset
	 * @param tolerance Should there be some tolerance to the left and right outer bin (for rounding errors)?
	 * @return
	 */
	public IntervalXYDataset createDataset(ArrayList<Double> entries, String title, Double highestValue, Double lowestValue, int numberOfBins, boolean tolerance){
		dataset = new SimpleHistogramDataset(title);
		return generateDataSet(entries, highestValue, lowestValue, numberOfBins, tolerance);
	}
	
	/**
	 * Creates dataset for histogram while determining highest and lowest value itself.
	 * @param entries Entries as doubles
	 * @param title Title of histogram (and name of dataset)
	 * @param numberOfBins Number of bins in dataset
	 * @param tolerance Should there be some tolerance to the left and right outer bin (for rounding errors)?
	 * @return
	 */
	public IntervalXYDataset createDataset(ArrayList<Double> entries, String title, int numberOfBins, boolean tolerance){
		dataset = new SimpleHistogramDataset(title);
		Double minValue = Double.MAX_VALUE;
		Double maxValue = Double.MAX_VALUE * -1;
		for(int i = 0; i < entries.size(); i++){
			minValue = Math.min(entries.get(i), minValue);
			maxValue = Math.min(entries.get(i), maxValue);
		}
		return generateDataSet(entries, maxValue, minValue, numberOfBins, tolerance);
	}
	
	private IntervalXYDataset generateDataSet(ArrayList<Double> entries, Double highestValue, Double lowestValue, int numberOfBins, boolean tolerance){
		
		Float tempTolerance = 0f;
		if(tolerance){
			tempTolerance = this.tolerance;
		}
		if(debug){
			System.out.println("Lowest entry: " + lowestValue + ", highest entry: " + highestValue);
		}
		
		if(entries == null){
			return null;
		}
		//reduce bin size if not enough entries
		if(entries.size() < numberOfBins){
			if(debug){
				System.out.println("Reduced number of bins to " + entries.size() + " instead of requested " + numberOfBins + " bins.");
			}
			numberOfBins = entries.size();
		}
		if(numberOfBins == 0){
			//just take integer difference between highest and lowest as number of bins if bins not specified
			numberOfBins = new Double(highestValue - lowestValue).intValue();
		}
		if(debug){
			System.out.println("Entries: " + entries);
		}
		//calculate bin size
		Double binSize = (highestValue - lowestValue) / numberOfBins;
		if(debug){
			System.out.println("Calculated bin size (before correction if zero): " + binSize);
		}
		//assign default bin size if only one bin (i.e. binSize = 0)
		if(binSize == 0){
			binSize = 0.01;
		}
		Double lowerEnd = lowestValue;
		if(debug){
			System.out.println("Creating histogram '" + title + "' with " + numberOfBins + " bins, bin size: " + binSize);
		}
		if(numberOfBins == 0){
			if(!entries.isEmpty()){
				//set number of bins to one as default
				numberOfBins = 1;
			} else {
				//entries must be empty, then don't continue processing
				System.err.println("No entries, aborting generation of Distribution chart.");
				return null;
			}
			//System.out.println("Number of bins is zero. Some context: Highest " + highestValue + ", lowest " + lowestValue + ", bin size " + binSize + ", entries: " + entries);
		}
		//setup bins
		for (int i = 0; i < numberOfBins; i++) {
			if(debug){
				System.out.println("Trying to add bin " + ((i == 0) ? lowerEnd - (binSize * tempTolerance) : lowerEnd) + ", " 
						+ ((i == numberOfBins - 1) ? (lowerEnd + binSize) + (binSize * tempTolerance) : lowerEnd + binSize) 
						+ ", incl. lower: " + true + ", incl. upper: " + ((i == numberOfBins - 1) ? true : false));
			}
			dataset.addBin(new SimpleHistogramBin(((i == 0) ? lowerEnd - (binSize * tempTolerance) : lowerEnd), 
					((i == numberOfBins - 1) ? (lowerEnd + binSize) + (binSize * tempTolerance) : lowerEnd + binSize),
					true, ((i == numberOfBins - 1) ? true : false)));
			lowerEnd += binSize;
		}
		//add observations
		double[] observations = new double[entries.size()];
		for (int i = 0; i < entries.size(); i++) {
			observations[i] = entries.get(i).doubleValue();
		}
		try{
			dataset.addObservations(observations);
		} catch (RuntimeException e){
			System.err.println("Error when attempting to enter observations " + entries);
			System.err.println("Number of bins: " + numberOfBins);
			e.printStackTrace();
		}
		return dataset;
	}
	
	/**
	 * Prepares the histogram using the dataset provided with createDataset().
	 * Call setVisible() on frame to show histogram.
	 */
	public void prepareGui(){
		prepareGui(false);
	}
	
	/**
	 * Prepares the histogram using the dataset provided with createDataset().
	 * Call setVisible() on frame to show histogram.
	 * @param setYAxisIntegerTicks Plots the y axis with integer ticks (instead of fractions).
	 */
	public void prepareGui(boolean setYAxisIntegerTicks){
		if(dataset == null || dataset.getSeriesCount() == 0){
			System.err.println("No histogram will be created as dataset is empty. Call createDataset() first before calling prepareGui().");
			return;
		}
		JFreeChart chart = ChartFactory.createHistogram(
                this.title, xAxisLabel, yAxisLabel, dataset,
                PlotOrientation.VERTICAL, showLegend, true, false);
		if (setYAxisIntegerTicks) {
			XYPlot plot = (XYPlot) chart.getPlot();
			NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
	        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		}
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(600, 500));
        setContentPane(chartPanel);
        this.pack();
	}

}
