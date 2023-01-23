package org.sofosim.environment.stats.charts;

import java.awt.Color;
import sim.util.media.chart.TimeSeriesChartGenerator;

public class TimeSeriesChartWrapper extends TimeSeriesChartGenerator{

	private Boolean printLabels = null;
	private Boolean printLegend = null;
	private Boolean printDashedLines = null;
	private Boolean printSeriesInUnifiedColor = null;
	private Boolean printLabelsInUnifiedColor = null;
	private Color unifiedLabelAndOrSeriesColor = null;
	
	public TimeSeriesChartWrapper() {
		super();
	}

	public TimeSeriesChartWrapper(Boolean printLabels, Boolean printLegend,
			Boolean printDashedLines, Boolean printInUnifiedColor,
			Boolean printLabelsInUnifiedColor, Color unifiedLabelColor) {
		super();
		this.printLabels = printLabels;
		this.printLegend = printLegend;
		this.printDashedLines = printDashedLines;
		this.printSeriesInUnifiedColor = printInUnifiedColor;
		this.printLabelsInUnifiedColor = printLabelsInUnifiedColor;
		this.unifiedLabelAndOrSeriesColor = unifiedLabelColor;
	}

	public Boolean getPrintLabels() {
		return printLabels;
	}

	public Boolean getPrintLegend() {
		return printLegend;
	}

	public Boolean getPrintDashedLines() {
		return printDashedLines;
	}

	public Boolean getPrintSeriesInUnifiedColor() {
		return printSeriesInUnifiedColor;
	}

	public Boolean getPrintLabelsInUnifiedColor() {
		return printLabelsInUnifiedColor;
	}

	public Color getUnifiedLabelAndOrSeriesColor() {
		return unifiedLabelAndOrSeriesColor;
	}

	/**
	 * Indicates if some special output properties have been defined.
	 * @return
	 */
	public boolean someOutputPropertiesDefined(){
		return (printLabels != null 
				|| printLegend != null 
				|| printDashedLines != null 
				|| printSeriesInUnifiedColor != null
				|| printLabelsInUnifiedColor != null);
	}
	
	
}
