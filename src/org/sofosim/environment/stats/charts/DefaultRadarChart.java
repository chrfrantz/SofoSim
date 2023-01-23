package org.sofosim.environment.stats.charts;

import java.util.LinkedHashSet;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.SpiderWebPlot;

/**
 * Default implementation of RadarChart.
 * 
 * @author Christopher Frantz
 *
 */
public class DefaultRadarChart extends RadarChart {

	public DefaultRadarChart(final String title) {
		super(title);
	}
	
	/**
	 * Instantiates a DefaultRadarChart with a given title, plot implementation class, max. default value for individual legs, 
	 * and option to maintain categories even after clearing (so the configuration remains stable even when new values are 
	 * filled). Additionally categories can already be predefined if known ex ante. 
	 * Further a legend can be activated.
	 * @param title
	 * @param plotImplementation
	 * @param maxValue
	 * @param maintainCategoriesAfterClearing
	 * @param presetCategories
	 * @param addLegend
	 */
	public DefaultRadarChart(final String title, final Class<? extends SpiderWebPlot> plotImplementation, final Double maxValue, final boolean maintainCategoriesAfterClearing, final LinkedHashSet<String> presetCategories, final boolean addLegend) {
		super(title);
		this.plotImplementation = plotImplementation;
		getPlot().setMaxValue(maxValue);
		maintainFixedCategories(maintainCategoriesAfterClearing);
		presetCategories(presetCategories);
		addLegend();
	}

	@Override
	public void formatChart(JFreeChart chart, SpiderWebPlot plot) {
		//do nothing
	}

}
