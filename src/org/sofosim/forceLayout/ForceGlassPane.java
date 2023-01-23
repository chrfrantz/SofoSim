package org.sofosim.forceLayout;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.vecmath.Point3d;
import org.nzdis.micro.messaging.MTRuntime;
import org.sofosim.forceLayout.util.CircleShape;
import org.sofosim.forceLayout.util.LineShape;
import org.sofosim.forceLayout.util.OvalShape;
import org.sofosim.forceLayout.util.TextShape;


public class ForceGlassPane<V> extends JComponent implements WindowListener{

	private ArrayList<LineShape> lineList = new ArrayList<LineShape>();
	private ArrayList<CircleShape> circleList = new ArrayList<CircleShape>();
	private ArrayList<OvalShape> ovalList = new ArrayList<OvalShape>();
	private ArrayList<TextShape> textList = new ArrayList<TextShape>(); 
	private ForceDirectedLayout layout = null;
	private JFrame frame = null;
	private final float frameRateThreshold = 0.001F;
	private static final String CALCULATING = "Calculating ...";
	private static final String OUTPUT_PREFIX = "Frame Rate: ";
	private static boolean initialized = false;
	private static boolean registeredListener = false;
	private static boolean printRoundsOnUi = true;
	
	public ForceGlassPane(JFrame frame, ForceDirectedLayout layout) {
		this.frame = frame;
		this.layout = layout;
		calculateFrameRate = layout.sim.showSocialForcesGraphUiInSingleFrame();
		if(!registeredListener){
			registeredListener = true;
	        frame.addWindowListener(this);
		}
	}
	
	/**
	 * Initialization of frame rate
	 */
	private void initializeFrameRateCalculation(){
		previousTimes = new long[64];
	    previousTimes[0] = System.currentTimeMillis();
	    previousIndex = 1;
	    previousFilled = false;
	    initialized = true;
	}
	
	/**
	 * Resets frame rate calculation - relevant if frame is minimized
	 */
	private void resetFrameRateCalculation(){
		initialized = false;
		frameRate = frameRateThreshold;
	}

	private long oldLineRound = 0L;
	
	/**
	 * Adds a line to be painted during the next rendering. Lines 
	 * will only be printed once, so must be readded every round 
	 * to reflect updates.
	 * @param shape
	 */
	public void paintLine(LineShape shape){
		long currentRound = layout.sim.schedule.getSteps();
		if(currentRound != oldLineRound){
			oldLineRound = currentRound;
			lineList.clear();
		}
		lineList.add(shape);
	}
	
	private long oldTextRound = 0L;
	
	/**
	 * Adds a text to be painted during the next rendering. Text 
	 * will only be printed once, so must be readded every round 
	 * to reflect updates.
	 * @param shape
	 */
	public void paintText(TextShape shape){
		if(shape.text != null && !shape.text.isEmpty()){
			long currentRound = layout.sim.schedule.getSteps();
			if(currentRound != oldTextRound){
				oldTextRound = currentRound;
				textList.clear();
			}
			textList.add(shape);
		}
	}
	
	private long oldCircleRound = 0L;
	
	/**
	 * Adds a circle to be painted during the next rendering. Circles 
	 * will only be printed once, so must be readded every round 
	 * to reflect updates.
	 * @param shape
	 */
	public void paintCircle(CircleShape shape){
		long currentRound = layout.sim.schedule.getSteps();
		if(currentRound != oldCircleRound){
			oldCircleRound = currentRound;
			circleList.clear();
		}
		circleList.add(shape);
	}
	
	private long oldOvalRound = 0L;
	
	/**
	 * Adds a oval to be painted during the next rendering. Ovals 
	 * will only be printed once, so must be readded every round 
	 * to reflect updates.
	 * @param shape
	 */
	public void paintOval(OvalShape shape){
		long currentRound = layout.sim.schedule.getSteps();
		if(currentRound != oldOvalRound){
			oldOvalRound = currentRound;
			ovalList.clear();
		}
		ovalList.add(shape);
	}
	
	int xSectorSize = 0;
	int ySectorSize = 0;
	int maxSectorValue = 0;
	private int alpha = 0;
	private int stepSize = 15;
	private int stepDecrement = 1;
	private int minAlphaLevel = 20;
	private int stepDecrementThreshold = 235;
	private Double meanDensity = -1.0;
	private int numberOfVertices = 0;
	private final Color sectorColor = new Color(Color.RED.getRed(), Color.RED.getGreen(), Color.RED.getBlue(), alpha);
	private Font defaultFont = new Font("Arial", Font.BOLD, 10);
	private Stroke defaultStroke = null;
	
	/**
	 * De/Activates frame rate calculation for Ui
	 * @param activated
	 */
	public void activateFrameRateCalculation(boolean activated){
		calculateFrameRate = activated;
		if(!activated){
			resetFrameRateCalculation();
		}
	}
	
	private Boolean calculateFrameRate = null;
	private final boolean printOnScreenElseOnConsole = true;
	
	@Override
	protected void paintComponent(Graphics g){
		long currentRound = layout.sim.schedule.getSteps();
		//clear lines if deactivated (effectively timeout)
		if(!lineList.isEmpty() && oldLineRound < currentRound - 1){
			lineList.clear();
			oldLineRound = currentRound;
		}
		//clear text if deactivated in UI (effectively timeout)
		if(!textList.isEmpty() && oldTextRound < currentRound - 1){
			textList.clear();
			oldTextRound = currentRound;
		}
		//clear circles if deactivated in UI (effectively timeout)
		if(!circleList.isEmpty() && oldCircleRound < currentRound - 1){
			circleList.clear();
			oldCircleRound = currentRound;
		}
		//clear ovals if deactivated in UI (effectively timeout)
		if(!ovalList.isEmpty() && oldOvalRound < currentRound - 1){
			ovalList.clear();
			oldOvalRound = currentRound;
		}
		//System.out.println("Painting.");
		
		if(calculateFrameRate){
			calculateFrameRate();
			if(printOnScreenElseOnConsole){
				String rate;
				if(frameRate <= frameRateThreshold){
					rate = CALCULATING;
				} else {
					rate = roundThreeDecimals(frameRate).toString();
				}
				g.drawString(new StringBuilder(OUTPUT_PREFIX).append(rate).toString(), 5, layout.sim.GRID_HEIGHT + 25);
			} else {
				System.out.println(OUTPUT_PREFIX + frameRate);
			}
		}
		g.drawRect(0, 0, layout.sim.GRID_WIDTH, layout.sim.GRID_HEIGHT);
		if(ForceDirectedLayout.drawAndManageSectors){
			if(xSectorSize == 0){
				xSectorSize = layout.xSizeOfSector;
				ySectorSize = layout.ySizeOfSector;
			}
			//draw density of sectors
			if(ForceDirectedLayout.drawSectorDensity){
				try{
					for(Entry<Point3d, HashSet<V>> entry: ((HashMap<Point3d, HashSet<V>>)layout.getSectors()).entrySet()){
						int numberOfEntitiesInSector = entry.getValue().size();
						if(meanDensity == -1){
							numberOfVertices += entry.getValue().size();
						}
						Color actualColor = sectorColor;
						//only paint sectors if number of vertices higher than mean
						if(numberOfEntitiesInSector > meanDensity){
							try{
								actualColor = new Color(actualColor.getRed(), actualColor.getGreen(), actualColor.getBlue(), actualColor.getAlpha() + stepSize * (numberOfEntitiesInSector - meanDensity.intValue()) + minAlphaLevel);
								//if alpha is getting too high, reduce step size for density
								if(actualColor.getAlpha() > stepDecrementThreshold){
									stepSize -= stepDecrement;
								}
							} catch(IllegalArgumentException e){
								actualColor = new Color(actualColor.getRed(), actualColor.getGreen(), actualColor.getBlue(), stepDecrementThreshold);
							}
						}
						g.setColor(actualColor);
						g.fillRect(new Double(entry.getKey().x * xSectorSize).intValue(), new Double(entry.getKey().y * ySectorSize).intValue(), xSectorSize, ySectorSize);
						//draw number of entities in sector
						g.setColor(Color.BLACK);
						g.setFont(new Font("Serif", Font.PLAIN, 24));
						//Note: If 3D is activated, densities for all z axis sectors will be printed on top of each other, thus render the feature somewhat unusable
						g.drawString(String.valueOf(entry.getValue().size()), new Double(entry.getKey().x * xSectorSize + xSectorSize * 2 / (float)5).intValue(), new Double(entry.getKey().y * ySectorSize + ySectorSize * 3 / (float)5).intValue());
						//g.drawString(String.valueOf(entry.getValue().size()), new Double(entry.getKey().x * xSectorSize + entry.getKey().z /(float)(xSectorSize/layout.sim.GRID_DEPTH)).intValue(), new Double((entry.getKey().y * ySectorSize + ySectorSize * 1.5 / (float)5) /*+ ySectorSize * entry.getKey().z /(float)(xSectorSize/layout.sim.GRID_DEPTH)*/).intValue());
					}
					if(meanDensity == -1){
						//calculate mean vertices per sector
						meanDensity = new Double(new Double(numberOfVertices)/layout.getSectorCount());
						System.out.println("Number of sectors: " + layout.getSectorCount());
						System.out.println("Mean density in grid (vertices per sector): " + meanDensity);
					}
				} catch(ConcurrentModificationException e) {
					
				}
			}
			
			//draw sector grid
			for(int x=1; x<(layout.xGridSize/xSectorSize) + 1; x++){
				g.drawLine(x * xSectorSize, 0, x * xSectorSize, layout.yGridSize);
			}
			for(int y=1; y<(layout.yGridSize/ySectorSize) + 1; y++){
				g.drawLine(0, y * ySectorSize, layout.xGridSize, y * ySectorSize);
			}
		}
		
		//printing lines on UI
		for(int i=0; i<lineList.size(); i++){
			LineShape shape = lineList.get(i);
			if(shape != null && shape.color != null){
				if(shape.stroke != null){
					if(this.defaultStroke == null){
						this.defaultStroke = ((Graphics2D)g).getStroke();
					}
					((Graphics2D)g).setStroke(shape.stroke);
				}
				g.setColor(shape.color);
				g.drawLine((int)shape.startingPoint.getX(), (int)shape.startingPoint.getY()
						, (int)shape.endPoint.getX(), (int)shape.endPoint.getY());
				if(shape.stroke != null){
					((Graphics2D)g).setStroke(this.defaultStroke);
				}
			}
		}
		
		//printing text on UI
		for(int i=0; i<textList.size(); i++){
			TextShape shape = textList.get(i);
			if(shape != null && shape.text != null 
					&& !shape.text.isEmpty() && shape.color != null){
				g.setColor(shape.color);
				if(shape.font == null){
					g.setFont(defaultFont);
				} else {
					g.setFont(shape.font);
				}
				//draw to multiline printing method (which will handle text alignment on per-line basis)
				drawMultilineString(g, shape.text, (int)(shape.center.getX()), (int)shape.center.getY());
			}
		}
		
		g.setColor(Color.BLACK);
		
		//painting circle on UI
		for(int i=0; i<circleList.size(); i++){
			CircleShape shape = circleList.get(i);
			if(shape != null){
				if(shape.filled){
					//filled
					g.setColor(shape.color);
					g.fillOval((int)shape.center.getX(), (int)shape.center.getY(), shape.diameter, shape.diameter);
				} else {
					//unfilled
					g.setColor(Color.black);
					g.drawOval((int)shape.center.getX(), (int)shape.center.getY(), shape.diameter, shape.diameter);
				}
			}
		}
		
		//painting ovals on UI
		for(int i=0; i<ovalList.size(); i++){
			OvalShape shape = ovalList.get(i);
			if(shape != null){
				g.drawOval((int)shape.center.getX(), (int)shape.center.getY(), shape.xLength, shape.yLength);
			}
		}
		
		//printing round information on UI
		if(printRoundsOnUi){
			g.setFont(new Font("Serif", Font.PLAIN, 24));
			g.drawString(layout.sim.schedule.getSteps() + " rounds", 5, layout.yGridSize - 10);
		}
	}
	
	/**
	 * Draws multiline Strings to replace the legacy drawString method
	 * @param g
	 * @param text
	 * @param x
	 * @param y
	 */
	private void drawMultilineString(Graphics g, String text, int x, int y) {
        for (String line : text.split(MTRuntime.LINE_DELIMITER)){
        	int stringLength = g.getFontMetrics().stringWidth(line);
        	int tempX =  (int)(x - (stringLength / (float)2));
            g.drawString(line, tempX, y += g.getFontMetrics().getHeight());
        }
    }

	
	//frame rate measurement taken from: http://www.java2s.com/Code/Java/2D-Graphics-GUI/Textanimation.htm
	
	private long[] previousTimes; // milliseconds

	private int previousIndex;

	private boolean previousFilled;
	
	private double frameRate; // frames per second
	
	protected void calculateFrameRate() {
		if(!initialized){
			initializeFrameRateCalculation();
		}
	    // Measure the frame rate
	    long now = System.currentTimeMillis();
	    int numberOfFrames = previousTimes.length;
	    double newRate;
	    // Use the more stable method if a history is available.
	    if (previousFilled){
	      newRate = (double) numberOfFrames
	          / (double) (now - previousTimes[previousIndex]) * 1000.0;
	    } else{ 
	      newRate = 1000.0 / (double) (now - previousTimes[numberOfFrames - 1]);
	    }
	    firePropertyChange("frameRate", frameRate, newRate);
	    frameRate = newRate;
	    // Update the history.
	    previousTimes[previousIndex] = now;
	    previousIndex++;
	    if (previousIndex >= numberOfFrames) {
	      previousIndex = 0;
	      previousFilled = true;
	    }
	  }
	
	/**
	 * Rounds a double value to three decimals.
	 * @param d double value to be rounded
	 * @return
	 */
	public Double roundThreeDecimals(double d) {
    	DecimalFormat threeDForm = new DecimalFormat("#.###");
    	Double result = null;
    	
		try	{
			result = Double.valueOf(threeDForm.format(d));
		}catch(Exception e){
			System.out.println("Value causing trouble in Stats: " + e.getCause());
			e.printStackTrace();
			return null;
		}
		return result;
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		resetFrameRateCalculation();
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}


}
