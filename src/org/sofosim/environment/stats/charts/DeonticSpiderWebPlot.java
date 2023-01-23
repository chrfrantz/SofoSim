package org.sofosim.environment.stats.charts;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.HashMap;

import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.util.TableOrder;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtils;


public class DeonticSpiderWebPlot extends SpiderWebPlot{

	String deonticLabel = "Extremes";
	Stroke deonticLineStroke = new BasicStroke(3.0f);
	Color deonticLinePaint = Color.BLACK;
	Double maxAngleBetweenExtremes = 240.0;
	Double defaultStartAngle = 180.0 + ((maxAngleBetweenExtremes - 180.0) / 2);
	Integer pointRadius = 3;

	/**
	 * Determines label positioning relative from centre of chart
	 * for selected labels (extremes, but customised in 
	 * {@link #draw(Graphics2D, Rectangle2D, Point2D, PlotState, PlotRenderingInfo)}).
	 * Default: 1.4; 
	 */
	Double scaleFactorForLabelPositioning = 1.4;
	
	/**
	 * Determines the scale of the dashed line that spread/separate the extreme deontics
	 * on the radar chart.
	 * Default: 1.5
	 */
	Float lineSpreadScaleForExtremeCategories = 1.5f;
	
	float gapHorizontal = 0.5f;
	float gapVertical = 0.2f;
	
	/**
	 * Indicates if tick markers should be drawn in axes.
	 */
	public boolean drawTicks = false;
	
	/**
	 * Tick granularity as fractions of axis length (e.g. [0.5f, 1f] -> tick at half of axis length and full length).
	 */
	public double[] tickAxisFractions = {0.5d, 1d};
	
	/**
	 * Indicates if series values should be drawn.
	 */
	public boolean drawValues = true;
	
	/**
	 * Draws values in percent as opposed to absolute values. 
	 */
	public boolean drawValuesInPercent = true;
	
	/**
	 * Threshold to consider values to be printed (to avoid 
	 * cluttering of small values in the plot center). 
	 * Requires {@link #drawValues} to be enabled.
	 */
	public float thresholdForDrawingValues = 0f;
	
	/**
	 * Max. value in dataset.
	 */
    private double maxAbsValue = 0.0;
    
    /**
     * Holds sums for individual series.
     */
    private HashMap<Integer, Double> seriesSums = new HashMap<>();
	
	public DeonticSpiderWebPlot(CategoryDataset dataset){
		super(dataset);
		setStartAngle(defaultStartAngle);
	}
	
	/**
	 * Determines max. value in dataset and saves it to {@link #maxAbsValue}. Can alternatively 
	 * assign it to plot max. value (length of leg). Also determines sum of items per series.
	 * @param seriesCount Number of series to iterate through
	 * @param catCount Number of categories to iterate through
	 * @param setMaxValue Defines whether max. value is automatically assigned to chart.
	 */
	private void calculateMaxAndSumValues(int seriesCount, int catCount, boolean setMaxValue) {
        double v = 0;
        Number nV = null;

        for (int seriesIndex = 0; seriesIndex < seriesCount; seriesIndex++) {
            double seriesSum = 0;
        	for (int catIndex = 0; catIndex < catCount; catIndex++) {
                nV = getPlotValue(seriesIndex, catIndex);
                if (nV != null) {
                	seriesSum += nV.doubleValue();
                    v = nV.doubleValue();
                    if (v > this.maxAbsValue) {
                    	this.maxAbsValue = v;
                    }
                    if (v > this.getMaxValue() && setMaxValue) {
                    	this.setMaxValue(v);
                    }
                }
            }
            seriesSums.put(seriesIndex, seriesSum);
        }
    }
	
	@Override
	public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
            PlotState parentState, PlotRenderingInfo info) {

        // adjust for insets...
        RectangleInsets insets = getInsets();
        insets.trim(area);

        if (info != null) {
            info.setPlotArea(area);
            info.setDataArea(area);
        }

        drawBackground(g2, area);
        drawOutline(g2, area);

        Shape savedClip = g2.getClip();

        g2.clip(area);
        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                getForegroundAlpha()));

        if (!DatasetUtils.isEmptyOrNull(this.getDataset())) {
            int seriesCount = 0, catCount = 0;

            if (this.getDataExtractOrder() == TableOrder.BY_ROW) {
                seriesCount = getDataset().getRowCount();
                catCount = getDataset().getColumnCount();
            }
            else {
                seriesCount = getDataset().getColumnCount();
                catCount = getDataset().getRowCount();
            }
            
            if (drawValuesInPercent) {
            	// Calculate but don't assign max value to allow calculation of fractions
            	calculateMaxAndSumValues(seriesCount, catCount, false);
            }

            // only scale axes to 1.0 if no explicit max value has been specified 
            if (this.getMaxValue() == DEFAULT_MAX_VALUE) {
            	if (drawValuesInPercent) {
            		// Set max value on scale to 1.0 if nothing else specified
                	setMaxValue(1.0);
            	} else {
            		// Calculate and assign max. value
            		calculateMaxAndSumValues(seriesCount, catCount, true);
            	}
            }

            // Next, setup the plot area

            // adjust the plot area by the interior spacing value

            double gapHorizontal = area.getWidth() * this.gapHorizontal; //getInteriorGap();
            double gapVertical = area.getHeight() * this.gapVertical; //getInteriorGap();

            double X = area.getX() + gapHorizontal / 2;
            double Y = area.getY() + gapVertical / 2;
            double W = area.getWidth() - gapHorizontal;
            double H = area.getHeight() - gapVertical;

            double headW = area.getWidth() * this.headPercent;
            double headH = area.getHeight() * this.headPercent;

            // make the chart area a square
            double min = Math.min(W, H) / 2;
            X = (X + X + W) / 2 - min;
            Y = (Y + Y + H) / 2 - min;
            W = 2 * min;
            H = 2 * min;

            Point2D  centre = new Point2D.Double(X + W / 2, Y + H / 2);
            Rectangle2D radarArea = new Rectangle2D.Double(X, Y, W, H);

            //CF modifications start here
            
            //CF: reduce category count by one
            int correctedCategoryCount = catCount - 1;
            
            // draw the axis and category label
            for (int cat = 0; cat < catCount; cat++) {
                double angle = getStartAngle()
                        + (getDirection().getFactor() * cat * maxAngleBetweenExtremes / correctedCategoryCount);

                Point2D endPoint = getWebPoint(radarArea, angle, 1);
                                                     // 1 = end of axis
                Line2D line = new Line2D.Double(centre, endPoint);
                
                //prepare scaledArea for overriding if labels need to be positioned to extremes
                Rectangle2D scaledArea = radarArea;
                
                //set default paint
                g2.setPaint(this.getAxisLinePaint());
                //print fat end points
                g2.setStroke(new BasicStroke(5f));
            	g2.fillOval(new Double(endPoint.getX()).intValue() - pointRadius, new Double(endPoint.getY()).intValue() - pointRadius, pointRadius * 2, pointRadius * 2);
                //do all special stuff only done for deontic extremes
            	if(		//left deontic extreme
            			cat == 0 ||
            			//second from left extreme
            			//cat == 1 ||
            			//second from right extreme
            			//cat == correctedCategoryCount - 1 ||
            			//right deontic extreme
            			cat == correctedCategoryCount){
                	//recalculate line for extremes - scale factor determines length of dashed line (for extremes: 1.5)
                	Point2D extremePoint = getWebPoint(radarArea, angle, lineSpreadScaleForExtremeCategories);
                	Line2D extremeline = new Line2D.Double(endPoint, extremePoint);
                	
                	//and draw dashed
                	BasicStroke dashStroke = new BasicStroke(
        					1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 
        	                1.0f, new float[] {8.0f, 8.0f}, 0.0f
        	            );
                	g2.setStroke(dashStroke);
                	g2.draw(extremeline);
                	//scale plot area before drawing the label
                    Rectangle2D extremeLabelArea = new Rectangle2D.Double(radarArea.getCenterX() - (radarArea.getWidth() * scaleFactorForLabelPositioning / 2), 
                    		radarArea.getCenterY() - (radarArea.getHeight() * scaleFactorForLabelPositioning / 2), 
                    		radarArea.getWidth() * scaleFactorForLabelPositioning, radarArea.getHeight() * scaleFactorForLabelPositioning);
                    scaledArea = extremeLabelArea;
                }
                //now continue with conventional line (from SpiderWebPlot)
                //reset to default stroke
                g2.setStroke(this.getAxisLineStroke());
                g2.draw(line);
                
                drawLabel(g2, scaledArea, 0.0, cat, angle, maxAngleBetweenExtremes / correctedCategoryCount);
                
                if(drawTicks){
	                //draw ticks
	                drawTicks(g2, radarArea, angle, cat);
                }
            }
            
            //CF: added drawing of custom line
            drawAdditionalDeonticChartElements(g2, radarArea, centre);

            // Now actually plot each of the series polygons..
            for (int series = 0; series < seriesCount; series++) {
                drawRadarPoly(g2, radarArea, centre, info, series, catCount,
                        headH, headW);
            }
        }
        else {
            drawNoDataMessage(g2, area);
        }
        g2.setClip(savedClip);
        g2.setComposite(originalComposite);
        drawOutline(g2, area);
    }
	
	 /**
     * Draws a radar plot polygon. Overrides original method. Only reason is to adjust maximum angle ({@link #maxAngleBetweenExtremes}).
     *
     * @param g2 the graphics device.
     * @param plotArea the area we are plotting in (already adjusted).
     * @param centre the centre point of the radar axes
     * @param info chart rendering info.
     * @param series the series within the dataset we are plotting
     * @param catCount the number of categories per radar plot
     * @param headH the data point height
     * @param headW the data point width
     */
	@Override
    protected void drawRadarPoly(Graphics2D g2,
                                 Rectangle2D plotArea,
                                 Point2D centre,
                                 PlotRenderingInfo info,
                                 int series, int catCount,
                                 double headH, double headW) {

        Polygon polygon = new Polygon();

        EntityCollection entities = null;
        if (info != null) {
            entities = info.getOwner().getEntityCollection();
        }

        // plot the data...
        for (int cat = 0; cat < catCount; cat++) {

        	Number dataValue = null;
        	try{
        		dataValue = getPlotValue(series, cat);
        	} catch(IndexOutOfBoundsException e){
        		dataValue = null;
        	}

            if (dataValue != null) {
                double value = dataValue.doubleValue();
                
                if (drawValuesInPercent) {
                	value = roundTwoDecimals(value / seriesSums.get(series));
                }

                if (value >= 0) { // draw the polygon series...

                    // Finds our starting angle from the centre for this axis

                    double angle = getStartAngle()	//reduced category count for angle, else used all categories
                        + (getDirection().getFactor() * cat * this.maxAngleBetweenExtremes / (catCount - 1));

                    // The following angle calc will ensure there isn't a top
                    // vertical axis - this may be useful if you don't want any
                    // given criteria to 'appear' move important than the
                    // others..
                    //  + (getDirection().getFactor()
                    //        * (cat + 0.5) * 360 / catCount);

                    // find the point at the appropriate distance end point
                    // along the axis/angle identified above and add it to the
                    // polygon

                    Point2D point = getWebPoint(plotArea, angle,
                            value / getMaxValue());
                    polygon.addPoint((int) point.getX(), (int) point.getY());

                    // put an ellipse at the point being plotted..

                    Paint paint = getSeriesPaint(series);
                    Paint outlinePaint = getSeriesOutlinePaint(series);
                    Stroke outlineStroke = getSeriesOutlineStroke(series);

                    Ellipse2D head = new Ellipse2D.Double(point.getX()
                            - headW / 2, point.getY() - headH / 2, headW,
                            headH);
                    g2.setPaint(paint);
                    g2.fill(head);
                    g2.setStroke(outlineStroke);
                    g2.setPaint(outlinePaint);
                    g2.draw(head);
                    
                    //CF: print value at slight point offset - and only print if above threshold
                    if(drawValues && value > thresholdForDrawingValues){
	                    //check if value is natural number
	                    String stringValue;
	                    if(value % 1 == 0){
	                    	//if already natural number, cut off decimal places
	                    	stringValue = String.valueOf(new Double(value).intValue());
	                    } else {
	                    	stringValue = String.valueOf(value);
	                    }
	                    if (drawValuesInPercent) {
	                    	//Cut off leading 0 if percent
	                    	stringValue = stringValue.replaceFirst("0.", ".");
	                    }
	                    //fix left-aligned positioning
	                    int stringWidth = g2.getFontMetrics(g2.getFont()).stringWidth(stringValue);
	                    int stringHeight = g2.getFontMetrics(g2.getFont()).getHeight();
	                    Float labelCompartmentAngle = 30f;
	                    Float labelAngle;
	                    // Distance of label from point
	                    int dist = 20;
	                    // Somewhat arbitrary positioning
	                    if((series + cat) % 2 == 0){
	                    	//turn to left
	                    	labelAngle = new Double((angle - 90) - labelCompartmentAngle * (series + cat + 0)).floatValue();
	                    } else {
	                    	//turn to right
	                    	labelAngle = new Double((angle + 90) - labelCompartmentAngle * (series + 0)).floatValue();
	                    }
	                    Point2D valuePoint = getWebPoint(plotArea, angle,
	                            value / getMaxValue());
	                    Point2D valueLabelPoint = new Point2D.Double(valuePoint.getX() + (Math.sin(labelAngle) * dist), 
                    			valuePoint.getY() - (Math.cos(labelAngle) * dist)); 
	                    //shift printing point a bit to the outside (* <proportional shift> + <absolute shift>)
	                    //Point2D valueLabelPoint = getWebPoint(plotArea, angle,
	                            //value / getMaxValue() * 1.02f + 0.08);
	                    //draw actual value
	                    g2.drawString(stringValue, 
	                    		new Float(valueLabelPoint.getX() - stringWidth / 2), 
	                    		new Float(valueLabelPoint.getY() + stringHeight / 4));
                    }
                    //END of value printing
                    
                    if (entities != null) {
                        int row = 0; int col = 0;
                        if (getDataExtractOrder() == TableOrder.BY_ROW) {
                            row = series;
                            col = cat;
                        }
                        else {
                            row = cat;
                            col = series;
                        }
                        String tip = null;
                        Number valueTest = null;
                        try {
                        	valueTest = getDataset().getValue(row, col);
                        } catch(IndexOutOfBoundsException e) {
                        	valueTest = null;
                        }
                        if (getToolTipGenerator() != null && valueTest != null) {
                            tip = getToolTipGenerator().generateToolTip(
                                    getDataset(), row, col);
                        }

                        String url = null;
                        if (getURLGenerator() != null) {
                            url = getURLGenerator().generateURL(getDataset(),
                                   row, col);
                        }

                        Shape area = new Rectangle(
                                (int) (point.getX() - headW),
                                (int) (point.getY() - headH),
                                (int) (headW * 2), (int) (headH * 2));
                        CategoryItemEntity entity = new CategoryItemEntity(
                                area, tip, url, getDataset(),
                                getDataset().getRowKey(row),
                                getDataset().getColumnKey(col));
                        entities.add(entity);
                    }

                }
            }
        }
        // Plot the polygon

        Paint paint = getSeriesPaint(series);
        g2.setPaint(paint);
        g2.setStroke(getSeriesOutlineStroke(series));
        g2.draw(polygon);

        // Lastly, fill the web polygon if this is required

        if (this.isWebFilled()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    0.1f));
            g2.fill(polygon);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    getForegroundAlpha()));
        }
    }

	//ADDITIONS
	
	private void drawAdditionalDeonticChartElements(Graphics2D g2, Rectangle2D radarArea, Point2D centre){
		//draw additional line
		Point2D endPoint = getWebPoint(radarArea, 270, 1);
		Line2D  line = new Line2D.Double(centre, endPoint);
		g2.setPaint(deonticLinePaint);
		g2.setStroke(deonticLineStroke);
        g2.draw(line);
	}

	//taken from https://github.com/mkrauskopf/jfreechart-patches/blob/jfreechart-1.0.x/source/org/jfree/chart/plot/SpiderWebPlot.java
	
	private static final double TICK_MARK_LENGTH = 6d;
	
	private void drawTicks(Graphics2D g2, Rectangle2D radarArea, double axisAngle, int cat) {
        double[] ticks = this.tickAxisFractions;
        for (int i = 0; i < ticks.length; i++) {
            double tick = ticks[i];
            Point2D middlePoint = getWebPoint(radarArea, axisAngle, tick);
            double xt = middlePoint.getX();
            double yt = middlePoint.getY();
            double angrad = Math.toRadians(-axisAngle);
            g2.translate(xt, yt);
            g2.rotate(angrad);
            g2.drawLine(0, (int) -TICK_MARK_LENGTH / 2, 0, (int) TICK_MARK_LENGTH / 2);
            g2.rotate(-angrad);
            g2.translate(-xt, -yt);
            //drawTickLabel(g2, radarArea, middlePoint, axisAngle, cat, tick);
        }
    }
	
	/**
	 * Rounds a double value to two decimals.
	 * @param d double value to be rounded
	 * @return
	 */
	public Double roundTwoDecimals(double d) {
    	DecimalFormat twoDForm = new DecimalFormat("#.##");
    	Double result = null;
    	
		try	{
			result = Double.valueOf(twoDForm.format(d));
		}catch(Exception e){
			System.out.println("Value causing trouble in Stats: " + e.getCause());
			e.printStackTrace();
			return null;
		}
		return result;
	}


}
