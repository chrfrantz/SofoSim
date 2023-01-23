package org.sofosim.environment.stats.charts;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Set;
import javax.swing.JFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.util.Rotation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;


/**
 * Refined and customized RadarChart implementation with flexible assignment of
 * plot implementation and further utility methods (e.g. prespecifying categories, 
 * legend, etc.). Default implementation: DefaultRadarChart
 * 
 * @author Christopher Frantz
 *
 */
public abstract class RadarChart extends JFrame {

	/**
	 * SpiderWebPlot implementation used for instantiation.
	 */
	protected Class<? extends SpiderWebPlot> plotImplementation = SpiderWebPlot.class;
	
	/**
	 * Size of the ChartPanel showing the plot.
	 */
	protected Dimension plotSize = new Dimension(900, 700);
	
	/**
	 * Reference to dataset
	 */
	private DefaultCategoryDataset dataset = null;
	
	/**
	 * Reference to plot itself
	 */
	private SpiderWebPlot plot;
	
	/**
	 * Reference to chart
	 */
	private JFreeChart chart;
	
	/**
	 * Indicates if legend should be initialised after chart instantiation.
	 */
	private boolean addLegend = false;
	
	/**
	 * Indicates if RadarChart has been prepared
	 */
	private boolean prepared = false;
	
	/**
	 * Indicates if added categories should be kept, even 
	 * if the dataset is cleared.
	 */
	private boolean maintainFixedCategories = false;
	
	/**
	 * Automatically scales axes length to maximum value in dataset.
	 * Set using {@link #setAutoAdjustAxisRangeToMaxValue(boolean)}. 
	 */
	protected boolean autoAdjustAxisRangeToMaxValue = false;
	
	/**
	 * Defines whether plot axis lengths are scaled based on maxium 
	 * value in dataset.<br> 
	 * Expands and contracts (!) based on maximum value in dataset.<br>
	 * Is automatically deactivated if maximum axis length is 
	 * set using {@link #setMaxAxisValue(double)}.<br>
	 * If set to false and value greater than current max. value 
	 * is entered, range is automatically expanded (but not contracted).
	 * Default: false
	 * @param autoAdjust
	 */
	public void setAutoAdjustAxisRangeToMaxValue(boolean autoAdjust) {
		autoAdjustAxisRangeToMaxValue = autoAdjust;
	}
	
	/**
	 * Indicates whether plot axis lengths are scaled to maximum value 
	 * of dataset. Can be manipulated using {@link #setAutoAdjustAxisRangeToMaxValue(boolean)}.
	 */
	public boolean getAutoAdjustAxisRangeToMaxValue() {
		return autoAdjustAxisRangeToMaxValue;
	}
	
	/**
	 * Label font used per default for category and value labels.
	 */
	protected Font labelFont = new Font("SansSerif", Font.PLAIN, 12);

	/**
	 * Instantiates a Radar chart or Kiviat graph. After instantiation it needs
	 * to be filled with values using {@link #addValue(Number, String, String)}
	 * and prepared using {@link #prepareGui(double)}. Finally it needs to be
	 * made visible by calling setVisible() on it. Once visible, data can be
	 * added and the chart updates automatically. However,
	 * {@link #clearDataset()} will need to be called prior to updating the
	 * dataset.
	 * 
	 * @param title Title of Kiviat graph window
	 */
	public RadarChart(String title) {
		super(title);
		this.dataset = new DefaultCategoryDataset();
	}
	
	/**
	 * Fixes categories added to dataset. Even if cleared, 
	 * the datasets maintains those categories. 
	 * Good for unified radar charts when printed repeatedly.
	 * @param activated Boolean indicating if categories should 
	 * be kept once added
	 */
	public void maintainFixedCategories(boolean activated){
		this.maintainFixedCategories = activated;
	}
	
	/**
	 * Returns the dataset underlying this radar chart.
	 * @return
	 */
	public CategoryDataset getDataset(){
		return this.dataset;
	}
	
	/**
	 * Sets the dataset for a radar chart. Should be called 
	 * before calling {@link #prepareGui()}. Will maintain 
	 * fixed categories if activated ({@link #maintainFixedCategories(boolean)}
	 * and previously added ({@link #presetCategories(Set)}). 
	 * @param dataset Dataset to be set for RadarChart
	 */
	public void setDataset(final DefaultCategoryDataset dataset){
		
		if(maintainFixedCategories && this.dataset != null){
			//clear but maintain categories
			clearDataset();
			//Add new data manually
			for(int i = 0; i < dataset.getColumnCount(); i++){
				//series
				for(int j = 0; j < dataset.getRowCount(); j++){
					//check value for null before adding
					Number val = dataset.getValue(dataset.getRowKey(j), dataset.getColumnKey(i));
					if(val != null){
						addValue(val, 
							dataset.getRowKey(j).toString(), 
							dataset.getColumnKey(i).toString());
					}
				}
			}
		} else {
			//replace entirely if categories are not to be maintained
			this.dataset = dataset;
		}
		if(this.plot != null){
			this.plot.setDataset(this.dataset);
		}
		/*if(this.plot == null){
			this.plot = new SpiderWebPlot(dataset);
		}
		//iterate through dataset and add values one by one in order to ensure expansion of max. value
		//categories
		for(int i = 0; i < this.dataset.getColumnCount(); i++){
			//series
			for(int j = 0; j < this.dataset.getRowCount(); j++){
				addValue(dataset.getValue(dataset.getRowKey(j), dataset.getColumnKey(i)), 
						dataset.getRowKey(j).toString(), 
						dataset.getColumnKey(i).toString());
			}
		}*/
	}

	/**
	 * Clears the existing dataset. Should be called before entering updated
	 * values of previously entered data. 
	 * If {@link #maintainFixedCategories(boolean)} has been activated, already 
	 * entered categories will be kept.
	 */
	public void clearDataset() {
		if(maintainFixedCategories){
			//categories
			for(int i = 0; i < this.dataset.getColumnCount(); i++){
				//series
				for(int j = 0; j < this.dataset.getRowCount(); j++){
					//remove all series - but leave categories
					this.dataset.removeRow(j);
				}
			}
		} else {
			this.dataset.clear();
		}
		//reset axis range to small value
		if(autoAdjustAxisRangeToMaxValue){
			getPlot().setMaxValue(0.1);
		}
	}
	
	/**
	 * Returns the wrapped plot instance. Creates new instance if not 
	 * instantiated yet.
	 * @return
	 */
	public SpiderWebPlot getPlot(){
		if(this.plot == null){
			Constructor ctor;
			try {
				ctor = plotImplementation.getDeclaredConstructor(CategoryDataset.class);
				ctor.setAccessible(true);
			    this.plot = (SpiderWebPlot) ctor.newInstance(dataset);
			} catch (NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		return this.plot;
	}
	
	/**
	 * Returns the wrapped chart. Returns null if chart 
	 * is not yet initialized via {@link #prepareGui()}.
	 * @return
	 */
	public JFreeChart getChart(){
		return chart;
	}

	/**
	 * Sets the axis maximum value and switches 
	 * off auto-adjustment of scale.
	 * This setter should be used instead of 
	 * getPlot().setMaxValue() as the latter 
	 * does not deactivate auto-adjustment if
	 * it was previously activated 
	 * (Default: deactivated).
	 * @param value Max axis range value
	 */
	public void setMaxAxisValue(double value){
		getPlot().setMaxValue(value);
		autoAdjustAxisRangeToMaxValue = false;
	}
	
	/**
	 * Adds a value for a given series and category on the Radar chart. Once the
	 * Radar chart has been prepared and set visible, new data can be added and
	 * the chart is automatically updated. Ensure to call
	 * {@link #clearDataset()} before updating the dataset entries. If the
	 * entered value is greater than the max. plot value, the plot range is
	 * extended to this value if {@link #autoAdjustAxisRangeToMaxValue} is 
	 * activated.
	 * 
	 * @param value
	 *            Numeric value for a given series and category
	 * @param series
	 *            Series the value belongs to (multiple series appear in
	 *            overlay)
	 * @param categories
	 *            Category the value refers to (i.e. 'arm' on graph)
	 */
	public void addValue(Number value, String series, String categories) {
		if (this.plot != null
				&& new BigDecimal(value.toString()).compareTo(new BigDecimal(
						this.plot.getMaxValue())) >= 1 && 
						autoAdjustAxisRangeToMaxValue) {
			this.plot.setMaxValue(value.doubleValue());
		}
		this.dataset.addValue(value, series, categories);
	}
	
	/**
	 * Allows the specification of preset fixed categories (useful 
	 * in combination with {@link #maintainFixedCategories(boolean)}) 
	 * to assure static and predefined category associations.
	 * @param presetCategories Ordered set of category names.
	 */
	public void presetCategories(Set<String> presetCategories){
		if(presetCategories != null){
			for(String value: presetCategories){
				addValue(0, "PRESET CATEGORIES", value);
			}
		} else {
			System.err.println("RadarChart: Predefined categories have been null.");
		}
	}

	/**
	 * Indicates if the RadarChart has been prepared for making it visible.
	 * 
	 * @return
	 */
	public boolean isPrepared() {
		return this.prepared;
	}

	/**
	 * Prepares the GUI (without making it visible). 
	 * You will still need to call setVisible() after execution. See
	 * {@link #prepareGui(double)} for customized scale ranges.
	 */
	public void prepareGui() {
		prepareGui(null);
	}

	/**
	 * Prepares GUI based on dataset and specified maximum value without making
	 * it visible. You will still need to call setVisible() after execution.
	 * Ensure to fill dataset before calling prepareGui(). Once prepared, new
	 * data can be added and the view is automatically updated. Ensure to call
	 * {@link #clearDataset()} before updating existing dataset entries. Note:
	 * The plot automatically expands the maximum value based on input dataset
	 * values.
	 * 
	 * @param maxValue Maximum value on scale, or null if automatic expansion desired
	 */
	public void prepareGui(Double maxValue) {
		if (!prepared) {
			//creates plot if not existent yet
			getPlot();
			//this.plot.setStartAngle(54);
			this.plot.setDirection(Rotation.CLOCKWISE);
			this.plot.setInteriorGap(0.40);
			if(maxValue != null){
				this.plot.setMaxValue(maxValue);
			}
			this.plot.setToolTipGenerator(new StandardCategoryToolTipGenerator());
			this.chart = new JFreeChart(super.getTitle(),
					TextTitle.DEFAULT_FONT, plot, false);
			ChartUtils.applyCurrentTheme(this.chart);
			if(addLegend){
				//if previously activated, executed addLegend now
				addLegend();
			}
			final ChartPanel chartPanel = new ChartPanel(this.chart);
			this.plot.setLabelFont(labelFont);
			//allow developer to fine-tune plot
			formatChart(this.chart, (SpiderWebPlot) this.chart.getPlot());
			// this.plot = (SpiderWebPlot) chartPanel.getChart().getPlot();
			// this.dataset = (DefaultCategoryDataset) plot.getDataset();
			chartPanel.setPreferredSize(plotSize);
			setContentPane(chartPanel);
			this.pack();
			this.prepared = true;
		}
	}
	
	/**
	 * Performs check for current state before 
	 * calling parent {@link #setVisible(boolean)} 
	 * method.
	 */
	@Override
	public void setVisible(boolean visible){
		if(!isVisible() && visible == true){
			super.setVisible(visible);
		} else if(isVisible() && visible == false){
			super.setVisible(visible);
		}
	}
	
	/**
	 * Adds a legend for the radar chart.
	 */
	public void addLegend(){
		if(chart != null){
			chart.addLegend(new LegendTitle(chart.getPlot()));
	    	//create border around legend
	    	chart.getLegend().setFrame(new BlockBorder(Color.BLACK));
		} else {
			addLegend = true;
		}
	}
	
	@Override
	public String toString(){
		return this.plotImplementation.getSimpleName() + " - " + this.getTitle();
	}
	
	/**
	 * Allows developers to fine-tune the appearance of the plot.
	 * @param plot
	 */
	public abstract void formatChart(JFreeChart chart, SpiderWebPlot plot);

}
