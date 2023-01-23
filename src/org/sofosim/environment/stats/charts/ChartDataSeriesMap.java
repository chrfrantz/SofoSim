package org.sofosim.environment.stats.charts;

import java.util.LinkedHashMap;
import javax.swing.JFrame;
import org.jfree.data.xy.XYSeries;

public class ChartDataSeriesMap {

	private TimeSeriesChartWrapper chart = null;
	private LinkedHashMap<String,XYSeries> dataSeries = null;
	private JFrame frame = null;
	
	public ChartDataSeriesMap(TimeSeriesChartWrapper chart,
			LinkedHashMap<String, XYSeries> dataSeries) {
		this.chart = chart;
		this.dataSeries = dataSeries;
	}
	
	public TimeSeriesChartWrapper getChart() {
		return chart;
	}
	
	public void setChart(TimeSeriesChartWrapper chart) {
		this.chart = chart;
	}
	
	public LinkedHashMap<String, XYSeries> getDataSeries() {
		return dataSeries;
	}
	
	public void setDataSeries(LinkedHashMap<String, XYSeries> dataSeries) {
		this.dataSeries = dataSeries;
	}
	
	public void clearDataSeries(){
		this.dataSeries.clear();
	}
	
	public void setJFrame(JFrame frame){
		this.frame = frame;
	}
	
	public JFrame getJFrame(){
		return this.frame;
	}
	
	public void clearResources(){
		this.frame.setVisible(false);
		this.frame.dispose();
		this.chart.removeAllSeries();
		this.chart.removeAll();
		clearDataSeries();
	}
	
}
