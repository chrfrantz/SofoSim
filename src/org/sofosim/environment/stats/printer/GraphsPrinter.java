package org.sofosim.environment.stats.printer;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.jfree.chart.JFreeChart;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;
import org.nzdis.micro.util.StackTracePrinter;
import org.sofosim.environment.stats.Statistics;
import org.sofosim.environment.stats.printer.ZipWriter.StringOutputStream;
import org.w3c.dom.DOMImplementation;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

public class GraphsPrinter {

	private Statistics stats = null;
	
	/**
	 * Scale factor for printed images. 
	 */
	private float scaleFactor = 1f;
	
	/** Global disabler for writing graphs to disk. */
	public static boolean enabled = true;
	
	public GraphsPrinter(Statistics stats){
		this.stats = stats;
	}
	
	/**
	 * Sets the scale factor for printed images. 
	 * @param factor
	 */
	public void setScaleFactor(float factor){
		if(factor <= 0){
			System.err.println("GraphPrinter: Invalid scake factor provided. Ignored. (Value: " + factor + ")");
			return;
		}
		this.scaleFactor = factor;
	}
		
	/**
	 * Prints a JFrame to a PDF file.
	 * @param componentToPrint
	 * @param filename
	 */
	public synchronized void printFrameToPDF(final JFrame componentToPrint, final String filename)  {
		if(enabled){
			if(!performNullCheck(false, componentToPrint, filename)){
				System.err.println("GraphPrinter: Aborted printing of frame to PDF");
				return;
			}
			try {
				EventQueue.invokeAndWait(new Runnable(){

					@Override
					public void run() {
						try{
							boolean printAsImage = true;
					    	int width = componentToPrint.getWidth();
					        int height = componentToPrint.getHeight();
					
					        if(printAsImage){
						        //creates image from frame first before printing
						        Document document = new Document(PageSize.A4);
						    	try {
						    	    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream (filename));
						    	    document.open();
						        
							    	BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
							    	Graphics2D img2D = image.createGraphics();
							    	componentToPrint.print(img2D);
							    	float viewWidth = writer.getPageSize().getWidth() - document.leftMargin() -
									document.rightMargin();
									double scaleX = viewWidth / width;
									
									//didn't work:
									//img2D.scale(scaleX, scaleX);  // keep aspect ratio
									
									//transform image match page size - works!
									AffineTransform at = new AffineTransform();
							        at.scale(scaleX, scaleX);
							        AffineTransformOp scaleOp =
							            new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
							        BufferedImage newImage = scaleOp.filter(image, null);  
							    	    
									Paragraph p = new Paragraph();
									
									Image itextImg = Image.getInstance(newImage, Color.BLUE, false);
									p.add(itextImg);
									document.add(p);
									
									Paragraph fileP = new Paragraph("Filename: " + filename);
									document.add(fileP);
									
									document.close();
									
						    	}catch(Exception e){
									
								} finally {
									if(document.isOpen()){
						    	        document.close();
						    	    }
								}
					        } else {
					        	//directly print from component (loses frame in output)
						    	Document document = new Document();
						    	try {
						    	    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream (filename));
						    	    document.open();
						    	    PdfContentByte contentByte = writer.getDirectContent();
						    	    PdfTemplate template = contentByte.createTemplate(500, 500);
						    	    Graphics2D g2 = template.createGraphics(500, 500);
						    	    componentToPrint.print(g2);
						    	    g2.dispose();
						    	    contentByte.addTemplate(template, 30, 300);
						    	} catch (Exception e) {
						    	    e.printStackTrace();
						    	}
						    	finally{
						    	    if(document.isOpen()){
						    	        document.close();
						    	    }
						    	}
					        }
					        System.out.println("Printed Frame '" + componentToPrint.getTitle() + "' to PDF file '" + filename + "'.");
						} catch(NullPointerException e){
							System.err.println("Could not print frame as input parameter is null.");
						}
					}
					
				});
			} catch (InvocationTargetException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Prints a JFreeChart object to a PDF file. Prints filename on chart itself to 
	 * allow easier identification.
	 * @param chart
	 * @param width
	 * @param height
	 * @param fileName
	 */
	public void printChartToPdf(JFreeChart chart, int width, int height, String fileName){
		printChartToPdf(chart, width, height, fileName, true);
	}
	
	/**
	 * Prints a JFreeChart object to a PDF file. Allows selection whether filename should be 
	 * printed on chart as well.
	 * @param chart
	 * @param width
	 * @param height
	 * @param filename
	 * @param printFileNameOnChart
	 */
	private synchronized void printChartToPdf(final JFreeChart chart, final int width, final int height, final String filename, final boolean printFileNameOnChart){
		if(enabled){
			if(!performNullCheck(false, chart, filename)){
				System.err.println("GraphPrinter: Aborted printing of chart to PDF");
				return;
			}
			try {
				EventQueue.invokeAndWait(new Runnable(){

					@Override
					public void run() {
						try {
						    Document document = new Document(new com.itextpdf.text.Rectangle(width,height));
						    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
						    document.addAuthor("SofoSim");
						    document.open();
						    PdfContentByte cb = writer.getDirectContent();
						    PdfTemplate tp = cb.createTemplate(width, height);
						    Graphics2D g2 = tp.createGraphics(width, height, new DefaultFontMapper());
						    Rectangle2D rectangle2D = new Rectangle2D.Double(0, 0, width, height); 
						    //synchronization prevents concurrent modification for fast-running simulations
						    synchronized(chart){
						    	chart.draw(g2, rectangle2D);
						    }
						    g2.dispose();
						    if(printFileNameOnChart){
							    tp = addFileName(tp, filename);
							    cb.addTemplate(tp, 0, 0);
							}
						    document.close();
						    System.out.println("Printed Chart '" + chart.getTitle().getText() + "' to PDF file '" + filename + "'.");
					    } catch( Exception e ){
					    		e.printStackTrace();
					    }
					}
					
				});
			} catch (InvocationTargetException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Adds a given filename (or any other text) to a PDF template.
	 * @param tp
	 * @param filename
	 * @return
	 */
	private PdfTemplate addFileName(PdfTemplate tp, String filename){
		if(filename != null && !filename.isEmpty()){
			tp.beginText();
		    try {
				tp.setFontAndSize(BaseFont.createFont(), 8);
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    tp.moveText(5, 3);
		    tp.showText("Filename: " + filename);
		    tp.endText();
		}
		return tp;
	}
	
	public static final String IMAGE_FORMAT_BMP = "BMP";
	public static final String IMAGE_FORMAT_PNG = "PNG";
	public static final String IMAGE_FORMAT_GIF = "GIF";
	public static final String IMAGE_FORMAT_JPG = "JPG";
	public static final String IMAGE_FORMAT_SVG = "SVG";
	
	/**
	 * Default image format.
	 */
	public static String IMAGE_FORMAT_DEFAULT = IMAGE_FORMAT_PNG;
	
	/**
	 * Indicates whether SVG output is produced using Batik 
	 * or JFreeSVG.<br>
	 * Note: Batik offers more generic support for rendering 
	 * Swing UI elements, but will produce images of elements 
	 * it can't render (e.g. JFreecharts), unless those override 
	 * the paint()/paintComponents() method. However, this is 
	 * solved in SofoSim. JFreeSVG, on the other hand, cannot 
	 * render UI components and produces output that cannot be 
	 * easily post-processed using Inkscape.
	 * Recommended setting: true
	 */
	public static boolean useBatikForSVG = true;
	
	/**
	 * Indicates if SVG files should be zipped in order to reduce size.
	 */
	public static boolean zipSvgOutput = false;
	
	/**
	 * Returns image format ending for given format (constants in GraphsPrinter). 
	 * Returns the ending for the default format if a given image format is not found
	 * @param imageFormat
	 * @return
	 */
	public static String getEndingForImageFormat(String imageFormat){
		String format = imageFormatMap.get(imageFormat);
		if(format == null){
			System.out.println("GraphsPrinter: The specified format '" + imageFormat 
					+ "' is not supported. Resorting to default format '" +  IMAGE_FORMAT_DEFAULT + "'.");
			return imageFormatMap.get(IMAGE_FORMAT_DEFAULT);
		}
		return format;
	}
	
	/**
	 * Indicates if a given image format (constant from GraphsPrinter) 
	 * is supported.
	 * @param imageFormat
	 * @return
	 */
	public static boolean imageFormatSupported(String imageFormat){
		return imageFormatMap.containsKey(imageFormat);
	}
	
	/**
	 * Map structure to map from format to ending
	 */
	private static HashMap<String, String> imageFormatMap = new HashMap<>();
	
	static{
		imageFormatMap.put(IMAGE_FORMAT_BMP, ".bmp");
		imageFormatMap.put(IMAGE_FORMAT_PNG, ".png");
		imageFormatMap.put(IMAGE_FORMAT_GIF, ".gif");
		imageFormatMap.put(IMAGE_FORMAT_JPG, ".jpg");
		imageFormatMap.put(IMAGE_FORMAT_SVG, ".svg");
	}
	
	/**
	 * Prints JFrame to image file (same as other method but without printing of 
	 * additional text).
	 * @param frame Frame to print
	 * @param filename Output filename (Note: Consider using {@link Statistics#buildFileName(String, boolean, String)} methods to include simulation-related information)
	 * @param imageFormat Image format as specified in constants IMAGE_FORMAT_PNG, IMAGE_FORMAT_GIF, IMAGE_FORMAT_JPG
	 */
	public void printFrameToImage(JFrame frame, String filename, String imageFormat){
		printFrameToImage(frame, filename, imageFormat, null);
	}
		
	/**
	 * Prints a JFrame to an image file of choice, e.g. jpg, bmp (see all constants in GraphPrinter and below).
	 * @param frame Frame to print
	 * @param filename Output filename (Note: Consider using {@link Statistics#buildFileName(String, boolean, String)} methods to include simulation-related information)
	 * @param imageFormat Image format as specified in constants in {@link #GraphsPrinter}.
	 * @param additionalText optional additional text to be put above the file name
	 */
	public synchronized void printFrameToImage(final JFrame frame, String filename, String imageFormat, final String additionalText){
		if(enabled){
			if(!performNullCheck(false, frame, filename, imageFormat)){
				System.err.println("GraphPrinter: Aborted printing of frame to image");
				return;
			}
			//activated, automatically switches to PNG if SVG is selected
			//checkAndAbortSvgProcessing(imageFormat, frame.getTitle(), filename);
			
			if(scaleFactor != 1f){
				final Dimension originalSize = frame.getPreferredSize();
				frame.setSize(new Dimension(new Float(originalSize.width * scaleFactor).intValue(), new Float(originalSize.height * scaleFactor).intValue()));
				frame.repaint(100);
				try {
					Thread.sleep(120);
				} catch (InterruptedException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
			
			final int width = frame.getSize().width;
			final int height = frame.getSize().height;
			final String format = imageFormat;
			final String name = filename;
			
			//invoke EDT thread
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						EventQueue.invokeAndWait(new Runnable(){
		
							@Override
							public void run() {
								BufferedImage bi = null;
								Graphics2D g = null;
								//leave SVG option in case we want to revive at later stage
								if(format.equals(IMAGE_FORMAT_SVG)){
									g = startSvgGraphics(g, width, height);
								} else {
									bi = new BufferedImage(width, height + extraOffsetForFileName, BufferedImage.TYPE_INT_RGB); 
									g = prepareImage(bi, width, height);
								}
								
								//print actual frame content to graphics object
								frame.paint(g);
								
								//additional text
								if(additionalText != null && !additionalText.isEmpty()){
									g = addTextElement(g, width, 5, height + 5, additionalText, null, null);
								}
								//check and perhaps replace filename
								final String actualFilename = checkFilenameFormatConsistency(format, name, true);
								//add filename below image
								g = addFileName(g, width, 5, height + (extraOffsetForFileName / 2), actualFilename);
			
								try{
									//special treament for SVG files
									if(format.equals(IMAGE_FORMAT_SVG)){
										finishSvgGraphics(g, actualFilename);
									} else {
										//dispose g before writing (can also be afterwards)
										g.dispose();
										ImageIO.write(bi/*.getScaledInstance(bi.getWidth(), bi.getHeight(), java.awt.Image.SCALE_SMOOTH)*/, format, new File(actualFilename));
									}
									//frame.setSize(originalSize);
									System.out.println("Printed Frame '" + frame.getTitle() + "' to " + format + " file '" + actualFilename + "'.");
								}catch (Exception e) {
									System.err.println("Printing of Frame '" + frame.getTitle() + "' failed.");
									e.printStackTrace();
								}
							}
							
						});
					} catch (InvocationTargetException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}).start();
		}

	}
	
	/**
	 * Switch indicating if D3D warning should be thrown if Java 8 is run and 
	 * {@link #printFrameToImageUsingJRobot(JFrame, String, String)} is used.
	 * Potential issue: http://www.javaprogrammingforums.com/awt-java-swing/29583-gui-freeze-d3d-screen-updater-thread-blocked.html
	 */
	public static boolean printD3DWarningIfRunningJava8 = true;
	
	/**
	 * Internal switch indicating if running Java 8 in fact.
	 */
	private static boolean warnD3DJava8Issue = false;
	
	static {
		if(System.getProperty("java.version").startsWith("1.8.")){
			warnD3DJava8Issue = true;
		}
	}
	
	/**
	 * Reference to JRobot for image printing
	 */
	private Robot robot = null;
	
	/**
	 * Prints JFrame to image file using JRobot (without further manipulation).
	 * Ensures it is on top when printing and resets it afterwards.
	 * @param frame Frame to print
	 * @param filename Output filename (Note: Consider using {@link Statistics#buildFileName(String, boolean, String)} methods to include simulation-related information)
	 * @param imageFormat Image format as specified in constants in {@link #GraphsPrinter}.
	 */
	public void printFrameToImageUsingJRobot(final JFrame frame, String filename, String imageFormat){
		if(!performNullCheck(false, frame, filename, imageFormat)){
			System.err.println("GraphPrinter: Aborted image printing of frame '" + frame.getTitle() + "' to file '" + filename + "' (Format: " + imageFormat + ") using JRobot.");
			return;
		}
		//defaulting to PNG if SVG is chosen format
		/*if(imageFormat.equals(IMAGE_FORMAT_SVG)){
			StringBuilder builder = new StringBuilder("Frame '");
			builder.append(frame.getTitle());
			builder.append("': Image format '");
			builder.append(imageFormat);
			builder.append("' is not supported for frame printing with JRobot (Hint: Use alternative methods in GraphsPrinter!). Resorting to '");
			//reset image format to be used
			imageFormat = IMAGE_FORMAT_DEFAULT.equals(IMAGE_FORMAT_SVG) ? IMAGE_FORMAT_PNG : IMAGE_FORMAT_DEFAULT;
			builder.append(imageFormat);
			builder.append("'.");
			System.err.println(builder.toString());
			//fix file ending as well
			filename = filename.replaceAll(getEndingForImageFormat(IMAGE_FORMAT_SVG) + "$", getEndingForImageFormat(imageFormat));
		}*/
		imageFormat = checkAndAbortSvgProcessing(imageFormat, frame.getTitle(), filename);
	
		
		final String format = imageFormat;
		final String name = checkFilenameFormatConsistency(imageFormat, filename, true);
		try {
			SwingUtilities.invokeAndWait(new Runnable(){

				@Override
				public void run() {
					boolean onTopSetting = false;
					if(Toolkit.getDefaultToolkit().isAlwaysOnTopSupported()){
						if(warnD3DJava8Issue && printD3DWarningIfRunningJava8 && !System.getProperty("sun.java2d.d3d").equals("false")){
							System.out.println("GraphPrinter: Using frame.isAlwaysOnTop() on Java 8. Might lead to deadlock. Disable D3D support to play safe (Property: sun.java2d.d3d=false)");
						}
						onTopSetting = frame.isAlwaysOnTop();
						//ensure it is printed on top
						if(frame.isVisible()){
							frame.setAlwaysOnTop(true);
						} else {
							System.err.println("Frame '" + frame.getTitle() + "' is not visible. Cannot be printed!");
							return;
						}
						//while frame is not in foreground, wait....
						while(!frame.isForegroundSet()){
							try {
								System.out.println("GraphPrinter - waiting for frame '" + frame.getTitle() + "' to be in foreground.");
								Thread.sleep(300);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					try {
						if(robot == null){
							robot = new Robot();
						}
						final Rectangle rect = frame.getBounds();
						final BufferedImage image = robot.createScreenCapture(rect);
						ImageIO.write(image, format, new File(name));
						System.out.println("Printed Frame '" + frame.getTitle() + "' to " + format + " file '" + name + "'.");
						if(Toolkit.getDefaultToolkit().isAlwaysOnTopSupported()){
							//reset old setting
							frame.setAlwaysOnTop(onTopSetting);
						}
					} catch (AWTException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				
			});
		} catch (InvocationTargetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
	
	private final int extraOffsetForFileName = 45;
	private int lineBreakOffset = 15;
	private static final String backslash = "\\";
	
	/**
	 * Prints JFreeChart to image file. Prints filename on chart.
	 * @param chart Chart to print
	 * @param width Width of chart
	 * @param height Height of chart
	 * @param filename Output filename (Note: Consider using {@link Statistics#buildFileName(String, boolean, String)} methods to include simulation-related information)
	 * @param imageFormat Image format as specified in constants in {@link #GraphsPrinter}
	 */
	public void printChartToImage(JFreeChart chart, int width, int height, String filename, String imageFormat){
		printChartToImage(chart, width, height, filename, imageFormat, null, true);
	}
	
	/**
	 * Prints JFreeChart to image file.
	 * @param chart Chart to print
	 * @param width Width of chart
	 * @param height Height of chart
	 * @param filename Output filename (Note: Consider using {@link Statistics#buildFileName(String, boolean, String)} methods to include simulation-related information)
	 * @param imageFormat Image format as specified in constants in {@link #GraphsPrinter}.
	 * @param additionalText Additional text to be added to output
	 * @param printFileNameOnChart Prints filename on chart if set to true
	 */
	public synchronized void printChartToImage(final JFreeChart chart, final int width, final int height, final String filename, final String imageFormat, final String additionalText, final boolean printFileNameOnChart){
		if(enabled){
			if(!performNullCheck(false, chart, filename, imageFormat)){
				System.err.println("GraphPrinter: Aborted printing of chart to image");
				return;
			}
			//try {
				new Thread(new Runnable(){

					@Override
					public void run() {
						BufferedImage bi = null;
						try{
							bi = new BufferedImage(width, height + extraOffsetForFileName, BufferedImage.TYPE_INT_RGB);
						} catch(IllegalArgumentException e){
							System.err.println("Statistics: Error when attempting to print chart '" + chart.getTitle().getText() + "'. Printing aborted." + System.getProperty("line.separator")
									+ e.getMessage());
							return;
						}
						Graphics2D g = null;
						if(imageFormat.equals(IMAGE_FORMAT_SVG)){
							g = startSvgGraphics(g, width, height);
						} else {
							g = prepareImage(bi, width, height);
						}
						//do actual printing of content to graphics object
						Rectangle2D rectangle2D = new Rectangle2D.Double(0, 0, width, height);
						//synchronization avoid concurrent modifications for fast-running simulations
						synchronized(chart){
							chart.draw(g, rectangle2D);
						}
						//additional text
						if(additionalText != null && !additionalText.isEmpty()){
							g = addTextElement(g, width, 5, height, additionalText, null, null);
						}
						//check and perhaps replace filename
						final String adjustedFilename = checkFilenameFormatConsistency(imageFormat, filename, true);
						//add file name to chart
						if(printFileNameOnChart){
							g = addFileName(g, width, 5, height + (extraOffsetForFileName / 2), adjustedFilename);
						}
						if(!imageFormat.equals(IMAGE_FORMAT_SVG)){
							//don't dispose quite yet if writing SVG files
							g.dispose();
						}
						
						try{
							//special treatment for SVG files
							if(imageFormat.equals(IMAGE_FORMAT_SVG)){
								finishSvgGraphics(g, adjustedFilename);
							} else {
								//else regular writing
								ImageIO.write(bi, imageFormat, new File(adjustedFilename));
							}
							System.out.println("Printed Chart '" + chart.getTitle().getText() + "' to " + imageFormat + " file '" + adjustedFilename + "'" + 
									(imageFormat.equals(IMAGE_FORMAT_SVG) && zipSvgOutput ? " (saved as zip file)": "") + ".");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
				}).start();
			/*} catch (InvocationTargetException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/
			
		}
	}
	
	/**
	 * Starts preparation of SVG drawing graphics component.
	 * Depending on setting of {@link #useBatikForSVG} it either 
	 * uses Apache Batik or JFreeSVG for producing output.
	 * @param g Graphics reference. May be null as it is overwritten anyway.
	 * @param width Width of source image/component (may be changed during process)
	 * @param height Height of source image/component (may be changed during process)
	 * @return Graphics2D instance ready for drawing/painting
	 */
	private Graphics2D startSvgGraphics(Graphics2D g, int width, int height){
		if(useBatikForSVG){
			//Batik version (renders JComponents but makes images of charts)
			// Get a DOMImplementation.
			DOMImplementation domImpl =
				GenericDOMImplementation.getDOMImplementation();
	
			// Create an instance of org.w3c.dom.Document.
			String svgNS = null;//"http://www.w3.org/2000/svg";
			org.w3c.dom.Document document = domImpl.createDocument(svgNS, "svg", null);
	
			// Create an instance of the SVG Generator.
			g = new org.apache.batik.svggen.SVGGraphics2D(document);
		} else {
			//JFreeSVG version
			g = new SVGGraphics2D(width, height + extraOffsetForFileName);
		}
		return g;
	}
	
	/**
	 * Finishes SVG graphic prepared with {@link #startSvgGraphics(Graphics2D, int, int)} and
	 * drawn/painted on.
	 * @param g Reference to SVG-printing {@link Graphics2D} instance.
	 * @param filename Filename for output file
	 */
	private void finishSvgGraphics(Graphics2D g, String filename){
		try{
			if(useBatikForSVG){
				//Batik version
				OutputStream outputStream = null;
				if(zipSvgOutput) {
					outputStream = new ZipWriter().new StringOutputStream();
				} else {
					outputStream = new FileOutputStream(new File(filename));
				}
				Writer out = new OutputStreamWriter(outputStream, "UTF-8");
				((org.apache.batik.svggen.SVGGraphics2D)g).stream(out, true /* use css */);						
				outputStream.flush();
				outputStream.close();
				if(zipSvgOutput) {
					ZipWriter.writeDataToZipFile(filename, null, ((StringOutputStream)outputStream).getString(), imageFormatMap.get(IMAGE_FORMAT_SVG), null);
				}
			} else {
				if(zipSvgOutput) {
					System.err.println("GraphsPrinter: Zipping of JFreeSVG output is not yet implemented (only Batik). Output is done in svg instead.");
				}
				//JFreeSVG version
				SVGUtils.writeToSVG(new File(filename), ((SVGGraphics2D)g).getSVGElement());
			}
			g.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Transcodes SVG string to PNG. Uses Batik libraries, so may not be working with 
	 * SVGs produced by other libraries, especially if using proprietary extensions. 
	 * @param svgString Stringified SVG
	 * @param filename Target filename for produced PNG
	 */
	private void transcodeSvgToPng(String svgString, String filename){
		Reader stringReader = new StringReader(svgString);
		TranscoderInput transcoderInput = new TranscoderInput(stringReader);
		TranscoderOutput transcoderOutput = null;
		try {
			transcoderOutput = new TranscoderOutput(new FileOutputStream(new File(filename)));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		PNGTranscoder pngTranscoder = new PNGTranscoder();
		try {
			pngTranscoder.transcode(transcoderInput, transcoderOutput);
		} catch (TranscoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks whether image format SVG is selected. 
	 * If so, prints message and selects {@link #IMAGE_FORMAT_DEFAULT} 
	 * before returning for further processing.
	 * Amends filename if switching file formats. 
	 * @return Image format switched to as SVG alternative 
	 */
	private String checkAndAbortSvgProcessing(String imageFormat, String frameTitle, String filename){
		if(imageFormat.equals(IMAGE_FORMAT_SVG)){
			StringBuilder builder = new StringBuilder("Frame '");
			builder.append(frameTitle);
			builder.append("': Image format '");
			builder.append(imageFormat);
			builder.append("' is not supported for frame printing. Resorting to '");
			//reset image format to be used
			imageFormat = IMAGE_FORMAT_DEFAULT.equals(IMAGE_FORMAT_SVG) ? IMAGE_FORMAT_PNG : IMAGE_FORMAT_DEFAULT;
			builder.append(imageFormat);
			builder.append("'.");
			System.err.println(builder.toString());
			//fix file ending as well
			//filename = filename.replaceAll(getEndingForImageFormat(IMAGE_FORMAT_SVG) + "$", getEndingForImageFormat(imageFormat));
		}
		return imageFormat;
	}
	
	/**
	 * Checks if specified filename ending is consistent with specified file format.
	 * Optionally creates full file path if not existent.
	 * @param imageFormat
	 * @param filename
	 * @param createPathIfNotExisting
	 * @return Returns potentially adjusted filename (replacement of illegal symbols) to be used for actual saving on file system.
	 */
	private String checkFilenameFormatConsistency(String imageFormat, String filename, boolean createPathIfNotExisting){
		if(filename != null){
			String ending = filename.substring(filename.lastIndexOf(".")+1).toUpperCase().trim();
			if(!ending.equals(imageFormat)){
				System.err.println("Image format '" + imageFormat + "' does not match filename ending '." + ending + "'. Be mindful when opening file '" + filename + "'.");
			}
			if(createPathIfNotExisting){
				File newFile = new File(filename);
				if(!newFile.exists() && newFile.getParentFile() != null){
					newFile.getParentFile().mkdirs();
				}
			}
			//check for illegal symbols
			filename = filename.replaceAll(":", "-");
			return filename;
		} else {
			throw new RuntimeException("Passed empty or null filename to GraphPrinter!");
		}
	}
	
	/**
	 * Prepares buffered image. 
	 * Should only be used in EDT thread.
	 * @param bi
	 * @param width
	 * @param height
	 * @return
	 */
	private Graphics2D prepareImage(BufferedImage bi, int width, int height){ 
		Graphics2D g = bi.createGraphics();
		g.setBackground(Color.WHITE);  
		g.clearRect(0, 0, width, height + extraOffsetForFileName);
		return g;
	}
	
	/**
	 * Adds filename to graphics object g. Automatically introduces line breaks if 
	 * filename is longer than width of g. Takes position and actual filename as 
	 * further parameters and prints directly on g and returns it afterwards 
	 * for further processing.<br>
	 * Should only be called from EDT thread.
	 * @param g graphics object
	 * @param gWidth width of graphics object
	 * @param xPos x start position of filename
	 * @param yPos y start position of filename
	 * @param filename 
	 * @return
	 */
	private Graphics2D addFileName(Graphics2D g, int gWidth, int xPos, int yPos, String filename){
		return addTextElement(g, gWidth, xPos, yPos, filename, "Filename: ", null);
	}
	
	/**
	 * Adds text element with parameter in equivalence to addFileName, but more generic. 
	 * Additional parameters include the prefix for the text (e.g. "Filename: ") and the 
	 * indication of the desired line break indicator (e.g. backslash: "\\").<br>
	 * Should only be called from EDT thread.
	 * @param g
	 * @param gWidth
	 * @param xPos
	 * @param yPos
	 * @param text
	 * @param prefix
	 * @param desiredLinebreakIndicator
	 * @return
	 */
	private Graphics2D addTextElement(Graphics2D g, int gWidth, int xPos, int yPos, String text, String prefix, String desiredLinebreakIndicator){
		g.setFont(new Font("Serif", Font.PLAIN, 12));
		g.setPaint(Color.BLACK);
		String newText = null;
		if(prefix != null && !prefix.isEmpty()){
			newText = prefix + text;
		} else {
			newText = text;
		}
		int strWidth = g.getFontMetrics(g.getFont()).stringWidth(newText) - xPos;
		if(strWidth > gWidth){
			int bs = -1;
			if(desiredLinebreakIndicator != null && !desiredLinebreakIndicator.isEmpty()){
				bs = newText.lastIndexOf(desiredLinebreakIndicator);
			}
			//aesthetics
			if(bs != -1){
				//looking for specified symbols to break more esthetically
				while(newText.length() > 0){
					String bsString = newText.substring(0, bs+1);
					if(g.getFontMetrics(g.getFont()).stringWidth(bsString) > gWidth){
						if(bsString.lastIndexOf(desiredLinebreakIndicator) != -1){
							bs = bsString.lastIndexOf(desiredLinebreakIndicator);
						} else {
							bs = -1;
							break;
						}
					} else {
						g.drawString(bsString, xPos, yPos);
						yPos += lineBreakOffset;
						newText = newText.substring(bs+1);
						bs = newText.lastIndexOf(desiredLinebreakIndicator);
						if(bs == -1){
							break;
						}
					}
				}
			}
			//purely space-oriented
			if(bs == -1 && newText.length() > 0){
				//if no backslash in filename, break when enough space
				while(newText.length() > 0){
					int cutIndex = newText.length();
					while(g.getFontMetrics(g.getFont()).stringWidth(newText.substring(0, cutIndex)) > gWidth){
						cutIndex--;
					}
					//one additional cut to ensure everything is readable
					//cutIndex--;
					g.drawString(newText.substring(0, cutIndex), xPos, yPos);
					yPos += lineBreakOffset;
					newText = newText.substring(cutIndex);
				}
			}
			return g;
		}
		g.drawString(newText, xPos, yPos);
		return g;
	}
	
	/**
	 * Checks if passed objects are not null. Prints object that is null 
	 * and return false. If all objects are not null, returns true.
	 * @param throwException throw RuntimeException in addition to printing message
	 * @param objects Objects to check
	 * @return boolean indicating if null object passed (false)
	 */
	private boolean performNullCheck(boolean throwException, Object... objects){
		if(objects != null){
			for(int i = 0 ; i < objects.length; i++){
				if(objects[i] == null){
					String message = "GraphPrinter: Method parameter is null. Trace: " + StackTracePrinter.getStackTrace();
					if(throwException){
						throw new RuntimeException(message);
					} else {
						System.err.println(message);
						return false;
					}
				}
			}
			return true;
		}
		String message = "Invalid parameter set passed for null check: " + StackTracePrinter.getStackTrace();
		if(throwException){
			throw new RuntimeException(message);
		} else {
			System.err.println(message);
			return false;
		}
	}
	
}
