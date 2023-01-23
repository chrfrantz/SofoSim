package org.sofosim.environment.stats;

import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.media.chart.MinGapDataCuller;
import sim.util.media.chart.TimeSeriesChartGenerator;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.frogberry.windowPositionSaver.PositionSaver;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.nzdis.micro.constants.AgentConsoleOutputLevels;
import org.nzdis.micro.messaging.MTRuntime;
import org.nzdis.micro.util.DataStructurePrettyPrinter;
import org.nzdis.micro.util.SimpleSemaphore;
import org.sofosim.environment.annotations.SimulationParam;
import org.sofosim.environment.stats.charts.ChartDataSeriesMap;
import org.sofosim.environment.stats.charts.CustomLabelGenerator;
import org.sofosim.environment.stats.charts.DatasetUtility;
import org.sofosim.environment.stats.charts.DefaultRadarChart;
import org.sofosim.environment.stats.charts.RadarChart;
import org.sofosim.environment.stats.charts.TimeSeriesChartWrapper;
import org.sofosim.environment.stats.printer.GraphsPrinter;
import org.sofosim.environment.stats.printer.StatsDataWriter;
import org.sofosim.environment.stats.printer.StatsParamWriter;
import org.sofosim.environment.stats.printer.StatsWriter;
import org.sofosim.environment.stats.spaceChecker.SpaceChecker;
import org.sofosim.environment.stats.spaceChecker.actions.LowSpaceActionInterruptExecution;
import org.sofosim.environment.GridSim;



public abstract class Statistics implements Steppable {

    private static final long serialVersionUID = -8209353235228331855L;

    public GridSim sim = null;
    
    //controller for graph output/stopping/resetting, ..
    public StatsGraphController graphController = null;
    //printer for stats information
    public GraphsPrinter printer = null;
    //calculator for statistics
    public static StatsCalculator<Double> statsCalculator = null;
    
    /**
     * Indicator if radar charts should be printed as part of graph printing operations.
     */
    public boolean printRadarCharts = false;
    
    /**
     * Indicates if Jung graphs should be printed as part of graph printing operations.
     */
    public boolean printJungGraphs = true;
    
    /**
     * Indicates if the radar charts have been registered for printing.
     */
    private boolean radarChartsRegisteredForPrinting = false;
    
    /**
     * Map containing radar chart instances.
     */
    private HashMap<String, RadarChart> radarCharts = new HashMap<>();
    
    /**
     * Radar chart implementation used for instantiation a new RadarChart. 
     * If not specified by user, will use DefaultRadarChart.
     */
    public Class<? extends RadarChart> radarChartImplementation = DefaultRadarChart.class;
    
    /**
     * Returns a radar chart instance using the given title as ID. 
     * Uses the currently associated {@link #radarChartImplementation} to create the instance.
     * @param title Title under which chart will be registered.
     * @return instance of radar chart using currently registered radar implementation 
     */
    public RadarChart getRadarChart(String title){
        return getRadarChart(title, radarChartImplementation);
    }
    
    /**
     * Returns a radar chart instance of the given type and associates it with the given key (used as 
     * title).
     * @param title Title (and ID) of chart
     * @param chartImplementation RadarChart specialisation to be used for chart
     * @return instance of radar chart
     */
    public RadarChart getRadarChart(String title, Class<? extends RadarChart> chartImplementation){
        if(!radarCharts.containsKey(title)){
            String completeTitle = null;
            if(title == null){
                completeTitle = RADAR_CHART;
            } else {
                completeTitle = title;
            }
            //create new instance of user-defined RadarChart implementation
            Constructor ctor;
            RadarChart radarChart = null;
            try {
                ctor = chartImplementation.getDeclaredConstructor(String.class);
                ctor.setAccessible(true);
                radarChart = (RadarChart)ctor.newInstance(completeTitle);
                radarChart.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
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
            radarCharts.put(completeTitle, radarChart);
        }
        return radarCharts.get(title);
    }
    
    /**
     * Resets an existing RadarChart instance and recreates a new empty instance using 
     * the user-specified RadarChart implementation ({@link #radarChartImplementation}). 
     * @return true if the RadarChart should have been reset (and generated)
     */
    protected boolean resetRadarChart(String title){
        if(printRadarCharts && (sim == null || sim.SHOW_STATS_GRAPHS)){
            if(radarCharts.containsKey(title)){
                radarCharts.remove(title).dispose();
            }
        }
        return printRadarCharts && (sim != null ? sim.SHOW_STATS_GRAPHS : true);
    }
    
    /**
     * Resets all instantiated and registered radar charts.
     */
    protected void resetAllRadarCharts(){
        for(String entry: new HashSet<String>(radarCharts.keySet())){
            resetRadarChart(entry);
        }
    }
    
    //can be changed by graphController to avoid running stats (for performance)
    private boolean runStats = true;
    //can be changed via graphController to avoid constantly updating graphs
    private boolean updateGraphs = true;
    //indicates if data culling is activated (can be controlled from StatsGraphController)
    private int dataCullingActivated = -1;
    /**
     * Indicates if new charts should be generated automatically if an unknown chart key/id 
     * is given using {@link #addDataSeriesEntry(String, String, XYDataItem)} or its variants.
     * Remember to set a meaningful default y axis label ({@link #defaultYAxisLabelForAutoGeneratedGraphs} 
     * and also {@link #defaultMinYValueForAutoGeneratedGraphs} as well as 
     *  {@link #defaultMaxYValueForAutoGeneratedGraphs} if fixed y axis range desired.
     */
    public boolean autogenerateGraphForNewChartKey = false;
    /**
     * Default label used for Y axes of automatically generated charts. 
     * Should be set if {@link #autogenerateGraphForNewChartKey} is enabled. 
     * Can be changed at runtime.
     */
    public String defaultYAxisLabelForAutoGeneratedGraphs = "Range";
    /**
     * Default min. y value used for autogenerated graphs. If set to null, 
     * the chart expands based on dataset. 
     * Is only used if {@link #defaultMaxYValueForAutoGeneratedGraphs} has also been set.
     * Should be considered if enabling {@link #autogenerateGraphForNewChartKey}. 
     */
    public Number defaultMinYValueForAutoGeneratedGraphs = null;
    /**
     * Default max. y value used for autogenerated graphs. If set to null, 
     * the chart expands based on dataset.
     * Is only used if {@link #defaultMinYValueForAutoGeneratedGraphs} has also been set.
     * Should be considered if enabling {@link #autogenerateGraphForNewChartKey}. 
     */
    public Number defaultMaxYValueForAutoGeneratedGraphs = null;
    /**
     * Ensures that graphs are printed with dashes (for differentiation when used in combination with unified color)
     */
    public boolean printGraphsAsDashedLines = false;
    /**
     * Ensures that graphs are printed in unified color (specified in unifiedGraphColor) (e.g. for publication output)
     */
    public boolean printGraphsInUnifiedColor = false;
    /**
     * Graph color used when printing graphs in unified color
     */
    public Color unifiedGraphColor = Color.black;
    /**
     * Graph color used for graph background
     */
    public Color graphBackgroundColor = Color.lightGray;
    /**
     * Series colors which should be suppressed during automatic color assignment
     */
    public HashSet<Color> suppressedGraphColors = new HashSet<>();
    /**
     * Prints series color information to console to help identify the color code for
     * particular series (e.g. to suppress an undesired color by adding it to 
     * the suppressedGraphColors collection).
     */
    public boolean printSeriesColorAssignmentDebug = false;
    /**
     * Indicates if horizontal grid lines are drawn
     */
    public boolean drawHorizontalGridLines = true;
    /**
     * Indicates if vertical grid lines are drawn
     */
    public boolean drawVerticalGridLines = true;
    /**
     * Sets paint for horizontal grid lines (Will be set to white or black if background has same color)
     */
    public Paint horizontalGridLinePaint = Color.WHITE;
    /**
     * Sets paint for vertical grid lines (Will be set to white or black if background has same color)
     */
    public Paint verticalGridLinePaint = Color.WHITE;
    /**
     * Ensures series names/labels are printed onto according graph on chart
     */
    public boolean printSeriesLabelsOnChart = true;
    /**
     * Ensures that series names/labels are printed in unified color (specified in unifiedSeriesLabelColor) 
     * even if the series graphs/lines themselves are printed in color.
     */
    public boolean printSeriesLabelsInUnifiedColor = true;
    /**
     * Prints legend on chart.
     */
    public boolean printLegend = false;
    /**
     * Custom legend border. If null, a legend border with 
     * width {@link #legendBorderWidth} is automatically generated.
     */
    public BlockBorder legendBorder = null;
    /**
     * Width of border around legend. Only used if 
     * {@link #legendBorder} is null. Default: 2
     */
    public float legendBorderWidth = 2f;
    /**
     * Color of legend border
     */
    public Color legendBorderColor = Color.BLACK;
    /**
     * Default padding between legend items.
     */
    public RectangleInsets legendItemPadding = new RectangleInsets(0, 10, 0, 20);
    /**
     * Specifies color when printing series names/labels in unified color (independent from graph color).
     */
    public Color unifiedSeriesLabelColor = Color.black;
    /**
     * Allows the choice to print graphs as PDFs or images. 
     * Refine default image format using {@link #setDefaultImageFormat(String)}.
     */
    public boolean printGraphAsPdfElseImg = false;
    /**
     * Scale factor applied when printing chart. 
     * Currently only used when printing time series charts.
     */
    public float printGraphWithScaleFactor = 1f;
    /**
     * Indicates if file names should be printed on charts (for easier identification)
     */
    public boolean printFileNamesOnCharts = true;
    /**
     * Indicates if rounds should be considered when generating filenames.
     * Global switch for turning round addition off (Setting: false). 
     * Should be used with caution!
     * Default: true
     */
    protected boolean considerRoundsInFilenameGeneration = true;
    /**
     * Font for title of time series chart. Default: Tahoma, bold, 20pt
     */
    public Font chartTitleFont = new Font("Tahoma", Font.BOLD, 20);
    /**
     * Indicates whether chart title is shown on chart. If set to 
     * false title is printed in white.
     */
    public boolean showChartTitle = true;
    /**
     * Font for axis labels of time series chart plots. Default: Tahoma, bold, 14pt
     */
    public Font chartAxisLabelFont = new Font("Tahoma", Font.BOLD, 14);
    /**
     * Font for ticks along time series charts. Default: Tahoma, plain, 12pt
     */
    public Font chartAxisTickFont = new Font("Tahoma", Font.PLAIN, 12);
    /**
     * Font for labels used on individual chart series. Default:  Default: SansSerif, plain, 12pt
     * Requires {@link #printSeriesLabelsOnChart} to be enabled.
     */
    public Font seriesLabelFont = new Font("SansSerif", Font.PLAIN, 12);
    /**
     * Font for legend items. Default: SansSerif, plain, 12pt
     */
    public Font legendItemFont = new Font("SansSerif", Font.PLAIN, 12);
    /**
     * Random positioning of name labels in chart for spacing out
     */
    public final boolean randomPositioningOfSeriesNamesOnChart = true;
    
    /**
     * Indicates if datasets should be written to disk when graphs are printed. 
     * Graphs can then be reestablished using the DataUtility.
     */
    public boolean saveDatasetsWhenPrintingCharts = true;
    
    /**
     * Indicates if saved dataset are compressed.
     */
    public boolean zipSavedDatasets = true;
    
    /**
     * Core name of outfile for saved datasets. Does not include 
     * simulation-specific prefix or ending.
     */
    public String datasetOutfile = "Dataset";
    
    /**
     * Outfile suffix used when writing radar chart dataset to 
     * outfile. This suffix is appended to {@link #datasetOutfile}, 
     * which is used for time series data. Does not include 
     * simulation-specific prefix or ending.
     */
    public String radarChartDatasetOutfileSuffix = "_radar";
    
    /**
     * Indicates if all created windows and frame are registered 
     * with PositionSaver and thus managed by it.
     */
    public boolean manageWindowsUsingPositionSaver = true;
    
    /**
     * Prefix used for any administrative console output
     */
    public static final String PREFIX = "Statistics: ";

    /** indicates scheduling of stats */
    public int interval = 1;
    
    /** 
     * Print all graphs (charts and Jung graphs) every x rounds. -1 for no printing. 
     */
    protected Integer PRINT_ALL_GRAPHS_EVERY_X_ROUNDS = 100;
    
    /**
     * Potentially multiple triplets specifying differentiated printing intervals. 
     * Syntax: <begin of frequency check>, <end of frequency check>, <frequency of 
     *         printing between beginning and end>
     */
    protected int[] PRINT_ALL_GRAPHS_INTERVAL_TRIPLETS = new int[0];
    
    /** 
     * Save all stats form contents every x rounds. -1 for no printing. 
     */
    protected Integer SAVE_STATS_FORM_EVERY_X_ROUNDS = 100;
    
    /**
     * Update time series charts every x rounds. Even if stats are stepped 
     * more often, only the values of every x rounds are plotted.
     */
    protected Integer UPDATE_CHARTS_EVERY_X_ROUNDS = -1;
    
    /**
     * Potentially multiple triplets specifying differentiated chart data collection intervals. 
     * Syntax: <begin of frequency check>, <end of frequency check>, <frequency of 
     *         collection between beginning and end>
     */
    protected int[] UPDATE_CHARTS_INTERVAL_TRIPLETS = new int[0];
    
    /**
     * Switch to maintain information if graphs are to be updated this round (if updateGraphs is activated)
     */
    private boolean updateGraphsThisRound = false;
    
    /**
     * Run custom method every specified number of rounds.
     */
    protected Integer RUN_CUSTOM_METHOD_EVERY_X_ROUNDS = 5;
    /**
     * If set to TRUE, all OTHER periodic ACTIVITIES are 
     * performed before the custom method is called. If FALSE,
     * the CUSTOM METHOD will be performed BEFORE all other 
     * periodic activities.
     */
    protected boolean RUN_CUSTOM_METHOD_LAST = false;
    /**
     * Collects data for writing to outfile every specified number of rounds.
     */
    protected Integer COLLECT_DATA_EVERY_X_ROUNDS = -1;
    /**
     * First round to periodic activities (graphs, data collection, custom method) 
     * apart from periodic scheduling to provide snapshot of initial setup 
     * (should be run once everything is initialized, e.g. round 1).
     * This is only considered once. After that only the periodically 
     * scheduled activities are run (every x rounds relative to 0).
     * -1 deactivates initial run.
     */
    protected Integer FIRST_ROUND_TO_RUN_PERIODIC_ACTIVITIES = 1;
    /**
     * Switch to indicate whether data are collected for this round. 
     * Depends on COLLECT_DATA_EVERY_X_ROUNDS.
     */
    private boolean collectDataThisRound = false;
    /**
     * Indicates how long simulation will be run. If set to -1
     * simulation will run forever, else it will be stopped after 
     * specified number of rounds.
     */
    public static Integer RUN_SIMULATION_FOR_X_ROUNDS = -1;
    
    public static final String ROUNDS = "Rounds";
    
    public static final String RADAR_CHART = "Radar Chart";
    
    /**
     * OS-dependent Linebreak symbol
     */
    public static final String LINEBREAK = System.getProperty("line.separator");
    
    /**
     * Frequency of space checking in minutes
     */
    public static int SPACE_CHECK_FREQUENCY = 1;
    
    /**
     * Minimum amount of free memory in MB.
     */
    public static int SPACE_THRESHOLD = 1000;
    
    /**
     * Default number of decimal places for float, double and long representations.
     */
    private static int DEFAULT_FLOAT_DECIMAL_PLACES = 6;
    
    /**
     * Indicates if round information is to be printed to console (i.e. indicating a new round).
     */
    public static boolean printRoundToConsole = false;
    
    /**
     * Specifies if StatsForm should be shown.
     */
    public boolean showStatsForm = true;
    
    /**
     * Instantiates Statistics for use by GridSim simulation. 
     * @param activateGraphController Indicates if graph controller should be activated (offers controls for charts/graphs)
     * @param useMapInListener Indicates if map structure should be used when notifying eventual statistics listener
     * @param schedulingInterval Interval Statistics module is scheduled with (every schedulingInterval round). Recommended: 1 (every round)
     * @param defaultFloatDigits Default number of decimal places for float and double output in statistics output.
     * If set to null, will be scheduled every round.
     */
    public Statistics(boolean activateGraphController, boolean useMapInListener, Integer schedulingInterval, Integer defaultFloatDigits){
        this.useMapInListeners = useMapInListener;
        if(schedulingInterval != null){
            this.interval = schedulingInterval;
        }
        if(activateGraphController){
            graphController = new StatsGraphController(this);
        }
        this.statsCalculator = new StatsCalculator();
        this.DEFAULT_FLOAT_DECIMAL_PLACES = defaultFloatDigits;
        numberFormat.setMaximumFractionDigits(DEFAULT_FLOAT_DECIMAL_PLACES);
    }
    
    /**
     * Instantiates Statistics for use by GridSim simulation. 
     * @param activateGraphController Indicates if graph controller should be activated (offers controls for charts/graphs)
     * @param useMapInListener Indicates if map structure should be used when notifying eventual statistics listener
     * @param schedulingInterval Interval Statistics module is scheduled with (every schedulingInterval round). Recommended: 1 (every round)
     * If set to null, will be scheduled every round.
     */
    public Statistics(boolean activateGraphController, boolean useMapInListener, Integer schedulingInterval){
        this.useMapInListeners = useMapInListener;
        if(schedulingInterval != null){
            this.interval = schedulingInterval;
        }
        if(activateGraphController){
            graphController = new StatsGraphController(this);
        }
        this.statsCalculator = new StatsCalculator();
    }
    
    StatsForm statsForm = null;
    
    /** parameter writer for stats */
    private StatsParamWriter paramWriter = null;
    
    public StatsParamWriter getStatsParamWriter(){
        return this.paramWriter;
    }
    
    /**
     * Prints stats form content to file without filename as part 
     * of the content.
     */
    public void saveStatsFormContentToFile(){
        saveStatsFormContentToFile(false);
    }
    
    /**
     * Prints stats form content to file. Optionally allows the 
     * printing of the filename as part of the content.
     * @param includeFilename
     */
    public void saveStatsFormContentToFile(boolean includeFilename){
        if(statsForm != null){
            statsForm.saveStatsToFile(includeFilename);
        }
    }
    
    /**
     * Sets up stats form for textual output.
     */
    private void setupStatsForm(){
        if(showStatsForm){
            statsForm = new StatsForm();
            registerStatsListener(statsForm);
            statsForm.setVisible(true);
            if(manageWindowsUsingPositionSaver){
                PositionSaver.registerWindow(statsForm);
            }
        } else {
            System.out.println("Note that StatsForm is deactivated.");
        }
    }
    
    private void destroyStatsForm(){
        if(statsForm != null){
            if(manageWindowsUsingPositionSaver){
                PositionSaver.unregisterFrame(statsForm);
            }
            statsForm.dispose();
            statsForm = null;
        }
    }

    /**
     * Is false if {@link #initialize()} has not yet been executed, else true.
     */
    private boolean statsInitialized = false;
    
    /**
     * Indicates if Statistics module has already been initialized.
     * @return
     */
    public boolean initialized(){
        return statsInitialized;
    }
    
    private void initialize(){
        setupStatsForm();
        
        //initialise and register with scheduler
        if(graphController != null){
            graphController.setVisible(true);
            if(manageWindowsUsingPositionSaver){
                PositionSaver.registerWindow(graphController);
            }
        }

        //Start space checker if indicated
        if(startSpaceChecker) {
            if(spaceChecker == null) {
                //per default check for 1GB of free space and interrupt execution if limit is hit
                spaceChecker = new SpaceChecker(null, SPACE_THRESHOLD, SPACE_CHECK_FREQUENCY, 
                		new LowSpaceActionInterruptExecution(sim), true);
            }
            if(!spaceChecker.isRunning()) {
                spaceChecker.start();
            }
        }
        
        //register with scheduler
        sim.schedule.scheduleRepeating(Schedule.EPOCH, 999, this, this.interval);
        step(sim);
        
        //derive current time
        String timeAndPerhapsSimClass = TimeUtility.getCurrentTimeString(TimeUtility.DATE_FORMAT_SEMI_CONCATENATED);
        //save time only (for preparation of outfile - not subfolder - prefix)
        String timeOnly = timeAndPerhapsSimClass;
        //if prefixing with simulation class name is activated, add it to time - but only if user has specified some
        //subfolder elements
        if(prefixSubfolderWithSimulationClassName && !subfolder.isEmpty()){
            timeAndPerhapsSimClass += "_" + sim.getClass().getSimpleName();
        }
        
        //Declare variable that will hold the full path name (including time and so on)
        String fullSubfolderName = null;
        
        //default subfolder to something if not set
        if(subfolder.isEmpty()){
            setGlobalSubfolderForOutput(defaultSubFolder + sim.getClass().getSimpleName(), true);
        }
        //reduced subfolder only containing parent path and time that will be used for actual results if 'tag folders' are activated
        String subfolderPrefix = subfolder.lastIndexOf("\\") == -1 ? (subfolder.lastIndexOf("/") == -1 ? 
                //if no back/slash, only assign time as prefix            
                timeAndPerhapsSimClass : 
                //else prefix of subfolder (all parent paths) and time
                subfolder.substring(0, subfolder.lastIndexOf("/") + 1) + timeAndPerhapsSimClass) :        
                subfolder.substring(0, subfolder.lastIndexOf("\\") + 1) + timeAndPerhapsSimClass;
        //create fill subfolder name including time prefix (not only time as for 'tag' folders')
        if(prefixSubfolderWithTime){
            fullSubfolderName = subfolder.lastIndexOf("\\") == -1 ? (subfolder.lastIndexOf("/") == -1 ? 
                    //just prefix whole subfolder if there is no back/slash in the path
                    timeAndPerhapsSimClass + "_" + subfolder : 
                    //else only prefix last branch
                    subfolder.substring(0, subfolder.lastIndexOf("/") + 1) + timeAndPerhapsSimClass + "_" + subfolder.substring(subfolder.lastIndexOf("/") + 1)) :
                    //else (if backslash found, take last branch of that)
                    subfolder.substring(0, subfolder.lastIndexOf("\\") + 1) + timeAndPerhapsSimClass + "_" + subfolder.substring(subfolder.lastIndexOf("\\") + 1);
        }
        //check for 'tag folders' - folders that contain the full information used for the folder name - actual data lies in folder only carrying date and time (as of path length)
        if(createShortFolderNameAndSeparateTagFolderInstead){
            File tagFolder = new File(fullSubfolderName);
            //create tag folder and assign short folder name for actual stats saving
            if(tagFolder.mkdirs()){
                System.out.println(PREFIX + "Created tag folder and will simplify results folder (Tag folder: " + tagFolder + ").");
                setGlobalSubfolderForOutput(subfolderPrefix);
            } else {
                //if creation doesn't work out, fall back to full file path (better feedback by StatsWriter if there are problems writing to that path)
                System.err.println(PREFIX + "Creation of tag folder failed, keeping original name so simulation retries! (Tag folder name: " + tagFolder + ")");
                setGlobalSubfolderForOutput(fullSubfolderName);
            }
        } else if(prefixSubfolderWithTime){
            setGlobalSubfolderForOutput(fullSubfolderName);
        } //in all other cases the correct folder name is already set
        
        //System.err.println("Simulation class to reflect on for parameters: " + sim.getClass());
        this.outFilePrefix = sim.getClass().getSimpleName() + "_" + timeOnly;
        //now prepare complete path prefixes
        prepareCompletePathAndOutfilePrefix();
        
        //System.out.println("Printing params to " + outFilePrefix + parameterFileSuffix + ending);
        this.paramWriter = new StatsParamWriter(outFilePrefix + parameterFileSuffix + ending, sim.getClass(), sim, 1);
        this.paramWriter.writeParameters();
        
        //redirect StdOut and StdErr if activated (checked within method)
        redirectStdOutAndStdErrToFile(redirectStdOutStdErrAlsoPrintToConsole);
        
        //print stats data only if specified
        if(sim.SAVE_STATS_TO_FILE){
            statsWriter = new StatsDataWriter(outFilePrefix + dataFileSuffix + ending);
        }
        if(sim.SHOW_STATS_GRAPHS){
            setupCharts();
            setupChartFrames();
            printer = new GraphsPrinter(this);
        } else {
            System.out.println(PREFIX + "Note that statistics output (and graphs as well as printer) are deactivated!");
        }
        
        // Check whether to read header file upon initialisation
        // Initialise header file
        this.CSV_HEADER_OUTFILE_NAME = defaultSubFolder + sim.getClass().getSimpleName() + ".csvheaders";
        if(readCsvFileHeadersUponInitialisation && new File(this.CSV_HEADER_OUTFILE_NAME).exists()){
            // Read header file
            Scanner scanner;
            try {
                scanner = new Scanner(new File(this.CSV_HEADER_OUTFILE_NAME));
                List<String> entries = new ArrayList<>();
                while (scanner.hasNextLine()) {
                    if(sortCsvFileHeadersUponReading){
                        // Sort before adding headers
                        entries.add(scanner.nextLine());
                    } else {
                        // Add in same order as in file
                        outFileHeaders.add(scanner.nextLine());
                    }
                }
                // Sort entries and add to header index
                if(!entries.isEmpty()){
                	entries.remove(ROUNDS); // Remove rounds from read file ...
                    Collections.sort(entries); // ... sort entries ...
                    outFileHeaders.add(ROUNDS); // ... and add at front ...
                    outFileHeaders.addAll(entries); // ... before adding the sorted rest.
                    // Write to outfile if requested
                    if(writeSortedCsvFileHeadersToFile){
                        writeCsvHeadersToFile();
                    }
                }
                System.out.println(PREFIX + "Header file '" + this.CSV_HEADER_OUTFILE_NAME + "' for CSV output file schemas loaded (sorted during loading: " + sortCsvFileHeadersUponReading + ").");
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        //now mark as initialized
        statsInitialized = true;
        System.out.println(PREFIX + "Statistics module initialized.");
    }
    
    /**
     * Redirect stdout and stderr to outfile.
     */
    @SimulationParam
    private boolean redirectStdOutStdErr = false;
    
    /**
     * Indicates if redirection should also print to console 
     * in addition to file
     */
    @SimulationParam
    private boolean redirectStdOutStdErrAlsoPrintToConsole = true;
    
    /**
     * Configures whether stdout and stderr should be redirected to 
     * file (Filename specified in GridSim).
     * @param activate Indicates whether redirection to file should be activated.
     * @param alsoPrintToConsole If set to true, prints output to console in addition to file
     */
    public void redirectStdOutAndStdErr(boolean activate, boolean alsoPrintToConsole){
        this.redirectStdOutStdErr = activate;
        this.redirectStdOutStdErrAlsoPrintToConsole = alsoPrintToConsole;
    }
    
    /**
     * Indicates if redirction of stdout and stderr is 
     * configured. Does not imply that it is already activated 
     * (see {@link #redirectionAlreadyActivated()}).
     * @return
     */
    public boolean redirectStdOutAndStdErr(){
        return this.redirectStdOutStdErr;
    }
    
    /**
     * Indicator if redirection has already been activated.
     */
    private boolean redirectionAlreadyExecuted = false;
    
    /**
     * Indicates if redirection has already been activated.
     * @return
     */
    public boolean redirectionAlreadyActivated(){
        return this.redirectionAlreadyExecuted;
    }

    /**
     * Redirects stdout and stderr output into file as specified in simulation 
     * (see {@link #redirectStdOutAndStdErrToFile()}).
     * Uses settings for selective print to console specified via 
     * {@link #redirectStdOutAndStdErr(boolean, boolean)}.
     * Alternatively one can invoke {@link #redirectStdOutAndStdErrToFile(boolean)} 
     * to override it (unless redirection has already been executed before).
     */
    public synchronized void redirectStdOutAndStdErrToFile(){
        redirectStdOutAndStdErrToFile(redirectStdOutStdErrAlsoPrintToConsole);
    }
    
    /**
     * Redirects stdout and stderr output into file as specified in simulation 
     * (see {@link #redirectStdOutAndStdErrToFile()}).
     * Allows selective additional printing of output to console.
     * @param alsoPrintToConsole If set to true, output will also be printed to console, else 
     *         only to outfile
     */
    public synchronized void redirectStdOutAndStdErrToFile(boolean alsoPrintToConsole){
        if(redirectStdOutStdErr && !redirectionAlreadyExecuted){
            File stdOutFile = new File(buildFileNameManually(sim.getStdOutStdErrOutfilename(), false, null, true));
            System.out.println(PREFIX + "Redirecting stdout and stderr to file " + stdOutFile);
            boolean redirect = false;
            if(!stdOutFile.exists()){
                try {
                    if(!stdOutFile.createNewFile()){
                        System.err.println(PREFIX + "Unable to create new file " + stdOutFile.getAbsolutePath() + ", cannot redirect StdOut and StdErr.");
                    } else {
                        redirect = true;
                    }
                } catch (IOException e) {
                    System.err.println(PREFIX + "Exception when creating StdOut/StdErr redirection file. Error: " + e.getMessage());
                    //e.printStackTrace();
                }
            } else {
                redirect = true;
            }
            if(redirect){
                FileOutputStream outWriter;
                try {
                    outWriter = new FileOutputStream(stdOutFile);
                    if(alsoPrintToConsole){
                        //Combined outputstream instance (file and console) for stderr
                        MultiOutputStream multiErr = new MultiOutputStream(System.err, outWriter);
                        //redirect stderr
                        System.setErr(new PrintStream(multiErr));
                        
                        //Combined outputstream instance (file and console) for stdout
                        MultiOutputStream multiOut = new MultiOutputStream(System.out, outWriter);
                        //redirect stdout
                        System.setOut(new PrintStream(multiOut));
                    } else {
                        //only redirect to file
                        System.setOut(new PrintStream(outWriter, true));
                        System.setErr(new PrintStream(outWriter, true));
                    }
                } catch (FileNotFoundException e) {
                    System.err.println(PREFIX + "Error writing to StdOut/StdErr output " + stdOutFile.getName() + ", Error: " + e.getMessage());
                    //e.printStackTrace();
                }
            }
            redirectionAlreadyExecuted = true;
        } else if(redirectionAlreadyExecuted){
            System.err.println(PREFIX + "Stdout and stderr redirection already activated and running. Will not change it at runtime." );
        } else if(!redirectStdOutStdErr){
            //System.err.println("Simulation environment: Stdout and stderr redirection not configured.");
        }
    }
    
    /**
     * Space checker instance run by Stats module
     */
    public SpaceChecker spaceChecker = null;
    /**
     * Indicates if space checker is started by Statistics module (upon initial step() call).
     */
    boolean startSpaceChecker = true;
    
    
    public int totalFields;
    public static long rounds;
    /** indicates the preference for map instead of buffer in listeners (not compulsory) */
    private boolean useMapInListeners = false;
    
    protected void setUseMapInListeners(boolean useMap){
        this.useMapInListeners = useMap;
    }
    
    /**
     * Allows to activate or deactivate processing stats at runtime.
     * @param activate
     */
    public void runStats(boolean activate){
        this.runStats = activate;
        if(sim != null){
            if(!sim.SHOW_STATS_GRAPHS){
                System.out.println(PREFIX + "Running Stats will not have effect as Statistics module is deactivated in simulation setup --> (SHOW_STATS_GRAPHS = false).");
            }
        }
        //update GraphController UI
        if(graphController != null){
            graphController.checkForChangedSimulationSettings();
        }
    }
    
    /**
     * Indicates if stats are run.
     * @return
     */
    public boolean runStatsActivated(){
        return this.runStats;
    }
    
    /**
     * Allows to de/activate the auto updating of graphs at runtime.
     * As a prerequisite statistics need to be running (@link runStats).
     * Can have significant performance impact.
     * @param activate true --> activate chart updates, false --> deactivate
     */
    public void updateStatsGraphs(boolean activate){
        this.updateGraphs = activate;
        if(sim != null){
            if(!sim.SHOW_STATS_GRAPHS){
                System.out.println(PREFIX + "Graphs will not be updated as Statistics module is deactivated in simulation setup --> (SHOW_STATS_GRAPHS = false).");
            }
        }
        //update GraphController UI
        if(graphController != null){
            graphController.checkForChangedSimulationSettings();
        }
    }
    
    /**
     * Indicates if stats graphs and charts are printed at runtime.
     * @return
     */
    public boolean updateStatsGraphsActivated(){
        return this.updateGraphs;
    }
    
    /**
     * Action constant for frequency tuples identification for auto-printing charts.
     */
    private static final String TUPLE_ACTION_CHART_AUTO_PRINT = "chart auto-print";
    /**
     * Action constant for frequency tuples identification for updating chart data (and charts).
     */
    private static final String TUPLE_ACTION_CHART_AUTO_UPDATE = "chart data update";
    
    /**
     * Set the frequency for automated printing of charts to PDFs. 
     * -1 deactivates printing.
     * @param frequency frequency in number of rounds
     */
    public void setChartsAutoPrintFrequency(int frequency){
        PRINT_ALL_GRAPHS_EVERY_X_ROUNDS = frequency;
        if(graphController != null){
            graphController.checkForChangedSimulationSettings();
        }
        if(PRINT_ALL_GRAPHS_EVERY_X_ROUNDS == -1){
            System.out.println(PREFIX + "Automated printing of charts/stats deactivated.");
        } else {
            System.out.println(PREFIX + "Automated printing of charts/stats set to " + PRINT_ALL_GRAPHS_EVERY_X_ROUNDS);
        }
    }
    
    /**
     * Sets frequency for automatic printing for particular simulation step ranges. 
     * Ranges of different print frequencies can be specified using triplets consisting of 
     * [<round at which printing rule starts>, <round until which this triplet is valid, 
     * or -1 for infinite/open-ended>, <frequency of printing (every x rounds)>]. 
     * Can be arbitrary number of triplets. 
     * Operates independent (i.e. in addition) to general auto-print frequency ({@link #setChartsAutoPrintFrequency(int)}). 
     * If multiple prints in the same round, only one will be performed.
     * Method performs basic validity checks (range specification); repeated calls overwrite old specifications. 
     * @param frequencyTuples Tuples as described in method description
     */
    public void setChartsAutoPrintFrequency(int... frequencyTuples){
        setActionFrequencies(TUPLE_ACTION_CHART_AUTO_PRINT, frequencyTuples);
    }
    
    private void setActionFrequencies(String action, int... frequencyTuples){
        if(frequencyTuples.length > 0 && frequencyTuples.length % 3 != 0){
            System.err.println(PREFIX + "Invalid arguments for " + action + " frequency triplets. Syntax: [<begin>, <end>, <frequency of action between begin and end>]+");
            return;
        }
        if(frequencyTuples.length == 0){
            System.err.println(PREFIX + "Empty " + action + " triplet. Ignored.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        int[] filteredTuples = new int[frequencyTuples.length];
        int filteredIndex = 0;
        int correctedCounter;
        for(int i = 0; i < frequencyTuples.length / 3; i++){
            correctedCounter = i * 3;
            builder.append("   - between rounds ")
            .append(frequencyTuples[correctedCounter]).append(" and " )
            .append(frequencyTuples[correctedCounter + 1] == -1 ? "simulation end" : frequencyTuples[correctedCounter + 1]).append(" print charts every ")
            .append(frequencyTuples[correctedCounter + 2]).append(" rounds");
            builder.append(LINEBREAK);
            boolean saveTuple = true;
            if(frequencyTuples[correctedCounter] < 0){
                //beginning cannot be < 0
                builder.append("     --> Error: " + action + " range specification is invalid (beginning (" 
                        + frequencyTuples[correctedCounter] + ") is below zero)");
                builder.append(LINEBREAK);
                saveTuple = false;
            }
            if(frequencyTuples[correctedCounter] > frequencyTuples[correctedCounter + 1] && frequencyTuples[correctedCounter + 1] != -1){
                //identify invalid range (unless end is -1, which implies infinite)
                builder.append("     --> Error: " + action + " range specification is invalid (beginning (" 
                        + frequencyTuples[correctedCounter] + ") greater than ending (" + frequencyTuples[correctedCounter + 1] + "))");
                builder.append(LINEBREAK);
                saveTuple = false;
            }
            if(saveTuple && ((frequencyTuples[correctedCounter + 1] - frequencyTuples[correctedCounter]) < frequencyTuples[correctedCounter + 2]) && frequencyTuples[correctedCounter + 1] != -1){
                //distance greater than frequency (thus never printed). Exception: ending == -1
                builder.append("     --> Error: " + action + " range specification is invalid (range (" 
                        + (frequencyTuples[correctedCounter + 1] - frequencyTuples[correctedCounter]) + ") is smaller than frequency (" + frequencyTuples[correctedCounter + 2] + "))");
                builder.append(LINEBREAK);
                saveTuple = false;
            }
            if(!saveTuple){
                builder.append("     --> Tuple ignored.");
                builder.append(LINEBREAK);
            } else {
                //save beginning
                filteredTuples[filteredIndex] = frequencyTuples[correctedCounter];
                filteredIndex++;
                //save end
                filteredTuples[filteredIndex] = frequencyTuples[correctedCounter + 1];
                filteredIndex++;
                //save frequency
                filteredTuples[filteredIndex] = frequencyTuples[correctedCounter + 2];
                filteredIndex++;
            }
        }
        builder.append("   Setting up ").append(filteredIndex / 3).append(" " + action + " range");
        builder.append((filteredIndex / 3) <= 1 ? "" : "s");
        if(filteredIndex > 0){
            if(filteredIndex < frequencyTuples.length){
                builder.append(" (reduced from ").append(frequencyTuples.length / 3).append(").");
            } else {
                builder.append(".");
            }
            switch(action){
                case TUPLE_ACTION_CHART_AUTO_PRINT: 
                    PRINT_ALL_GRAPHS_INTERVAL_TRIPLETS = new int[filteredIndex];
                    //reduce array to minimal size
                    System.arraycopy(filteredTuples, 0, PRINT_ALL_GRAPHS_INTERVAL_TRIPLETS, 0, filteredIndex);
                    break;
                case TUPLE_ACTION_CHART_AUTO_UPDATE:
                    UPDATE_CHARTS_INTERVAL_TRIPLETS = new int[filteredIndex];
                    //reduce array to minimal size
                    System.arraycopy(filteredTuples, 0, UPDATE_CHARTS_INTERVAL_TRIPLETS, 0, filteredIndex);
                    break;
            }
            
        }
        System.out.println(PREFIX + "Set " + action + " frequency triplets:" + LINEBREAK
                + builder.toString());
    }
    
    /**
     * Checks if a given action should be performed in the given round 
     * based on the frequency tuples configured for this action.
     * @param action Action constant identifying action.
     * @return
     */
    private boolean checkIfActionShouldBeExecuted(String action){
        // Check for printing graphs based on triplets
        
        // Indicates whether chart-related action should be performed
        boolean performAction = false;
        
        int[] arrayToCheck = null;
        switch(action){
            case TUPLE_ACTION_CHART_AUTO_PRINT:
                arrayToCheck = PRINT_ALL_GRAPHS_INTERVAL_TRIPLETS;
                break;
            case TUPLE_ACTION_CHART_AUTO_UPDATE:
                arrayToCheck = UPDATE_CHARTS_INTERVAL_TRIPLETS;
                break;
            default: System.err.println(PREFIX + "Error in " + action + " triplets (Action " + action + 
                        " does not have associated interval specification). Printing aborted....");
                return performAction;
        }
        
        if(arrayToCheck.length > 0){
            if(arrayToCheck.length % 3 != 0){
                System.err.println(PREFIX + "Error in " + action + " triplets. Printing aborted....");
            } else {
                //actually check each triplet
                int correctedCount;
                for(int i = 0; i < (arrayToCheck.length / 3); i++){
                    correctedCount = i * 3;
                    if(arrayToCheck[correctedCount] <= rounds 
                        && (arrayToCheck[correctedCount + 1] == -1 
                            || arrayToCheck[correctedCount + 1] >= rounds)
                        && (rounds - arrayToCheck[correctedCount])
                            % arrayToCheck[correctedCount + 2] == 0
                        //only exception for performing action based on special intervals is round 0 - no action in round 0 as folder setup not complete yet
                        && sim.schedule.getSteps() != 0
                            ){
                        //if rounds between begin and end and modulo of frequency == 0, then perform action this round
                        performAction = true;
                        System.out.println(PREFIX + "Round " + sim.schedule.getSteps() + " - Performing " + action 
                                + " as between " + arrayToCheck[correctedCount] 
                                + " and " 
                                + (arrayToCheck[correctedCount + 1] == -1 ? "simulation end" : arrayToCheck[correctedCount + 1]) 
                                + " and performing action in frequency " 
                                + arrayToCheck[correctedCount + 2] + ".");
                        //can break loop then, as action will only occur once, even if multiple triplets would apply
                        break;
                    }
                }
            }
        }
        return performAction;
    }
    
    /**
     * Set the frequency for execution of the custom method. 
     * -1 deactivates execution.
     * @param frequency frequency in number of rounds
     */
    public void setCustomMethodExecutionFrequency(int frequency){
        RUN_CUSTOM_METHOD_EVERY_X_ROUNDS = frequency;
        if(graphController != null){
            graphController.checkForChangedSimulationSettings();
        }
        if(RUN_CUSTOM_METHOD_EVERY_X_ROUNDS == -1){
            System.out.println(PREFIX + "Automated execution of custom method deactivated.");
        } else {
            System.out.println(PREFIX + "Automated execution of custom method set to " + RUN_CUSTOM_METHOD_EVERY_X_ROUNDS);
        }
    }
    
    /**
     * Sets the frequency with which time series charts are updated, 
     * i.e. values added to their data sets. -1 does not constrain 
     * updating; graphs are then updated with the same frequency as 
     * the statistics stepping frequency.
     * @param frequency Frequency in number of rounds
     */
    public void setChartUpdateFrequency(int frequency){
        UPDATE_CHARTS_EVERY_X_ROUNDS = frequency;
        if(graphController != null){
            graphController.checkForChangedSimulationSettings();
        }
        if(UPDATE_CHARTS_EVERY_X_ROUNDS == -1){
            System.out.println(PREFIX + "Updating frequency of time series graphs set to same as stats stepping frequency (" + this.interval + ")." );
        } else {
            System.out.println(PREFIX + "Updating frequency of time series graphs set to " + UPDATE_CHARTS_EVERY_X_ROUNDS);
        }
    }
    
    /**
     * Sets frequency for chart updating for particular simulation step ranges. 
     * Ranges of different update frequencies can be specified using triplets consisting of 
     * [<round at which updating rule starts>, <round until which this triplet is valid, 
     * or -1 for infinite/open-ended>, <frequency of updating (every x rounds)>]. 
     * Can be arbitrary number of triplets. 
     * Operates independent (i.e. in addition) to general chart update frequency ({@link #setChartUpdateFrequency(int)}). 
     * If multiple updates in the same round, only one will be performed.
     * Method performs basic validity checks (range specification); repeated calls overwrite old specifications. 
     * @param frequencyTuples Tuples as described in method description
     */
    public void setChartUpdateFrequency(int... frequencyTuples){
        setActionFrequencies(TUPLE_ACTION_CHART_AUTO_UPDATE, frequencyTuples);
    }
    
    /**
     * Set the frequency of data collection (how often does the system 
     * save stats output in order to write them to the CSV outfile). 
     * -1 deactivates collecting. Default: -1
     * @param frequency Frequency in number of rounds
     */
    public void setDataCollectionFrequency(int frequency){
        COLLECT_DATA_EVERY_X_ROUNDS = frequency;
        if(graphController != null){
            graphController.checkForChangedSimulationSettings();
        }
        if(COLLECT_DATA_EVERY_X_ROUNDS == -1){
            System.out.println(PREFIX + "Automated execution of data collection deactivated.");
        } else {
            System.out.println(PREFIX + "Automated execution of data collection set to " + COLLECT_DATA_EVERY_X_ROUNDS);
        }
    }
    /**
     * Specifies initial round of running activated periodic functions 
     * (graphs printing, data collection, custom method). 
     * Defaults to 1 (i.e. round 1). -1 deactivates initial run.
     * @param initialRound Round during which all activated periodic functions should be run prior to first periodic execution
     */
    public void setInitialRoundOfRunningPeriodicActivities(int initialRound){
        FIRST_ROUND_TO_RUN_PERIODIC_ACTIVITIES = initialRound;
        if(FIRST_ROUND_TO_RUN_PERIODIC_ACTIVITIES == -1){
            System.out.println(PREFIX + "Run of initial round deactivated.");
        } else {
            System.out.println(PREFIX + "Run of initial round set to " + FIRST_ROUND_TO_RUN_PERIODIC_ACTIVITIES);
        }
    }
    
    /**
     * Set the frequency of saving the stats form. 
     * -1 deactivates saving.
     * @param frequency frequency in number of rounds
     */
    public void setStatsFormSavingFrequency(int frequency){
        SAVE_STATS_FORM_EVERY_X_ROUNDS = frequency;
        if(graphController != null){
            graphController.checkForChangedSimulationSettings();
        }
        if(SAVE_STATS_FORM_EVERY_X_ROUNDS == -1){
            System.out.println(PREFIX + "Automated execution of stats form saving deactivated.");
        } else {
            System.out.println(PREFIX + "Automated execution of stats form saving set to " + SAVE_STATS_FORM_EVERY_X_ROUNDS);
        }
    }
    
    //override switch to allow manual triggering of Statistics step
    private boolean override = false;
    
    /**
     * Allows stepping without a running simulation instance.
     * @param override Indicates if stats should step without invocation by simulation scheduler
     */
    public void step(boolean override){
        if(override){
            this.override = true;
        }
        if(sim != null){
            step(sim);
        } else {
            System.err.println(PREFIX + "Cannot step statistics as no simulation initialized.");
        }
        if(override){
            this.override = false;
        }
    }
    
    /**
     * Steps the Statistics module. Note: Initial call registers it 
     * with simulation scheduler. Needs to be invoked by developer.
     */
    @Override
    public synchronized void step(SimState state) {
        if(runStats || override){
            if(this.sim == null){
                this.sim = (GridSim)state;
                totalFields = sim.GRID_WIDTH * sim.GRID_HEIGHT * sim.GRID_DEPTH;
                initialize();
            }
            //reset collect data switch
            collectDataThisRound = false;
            
            roundBuffer = new StringBuffer();
            //update simulation round and write round information into all stats structures
            rounds = this.sim.schedule.getSteps();
            structuredValues.put(ROUNDS, rounds);
            addToRoundBuffer(new StringBuffer("Rounds: ").append(rounds).toString(), true);
            //output to console activated, or if all agent output is activated in micro2
            if(printRoundToConsole || MTRuntime.getAgentConsoleOutputLevel().equals(AgentConsoleOutputLevels.ALL)){
                System.out.println("Round " + rounds);
            }
            
            //check if data should be written this round, and if so, should we prepend rounds?
            if(COLLECT_DATA_EVERY_X_ROUNDS != -1 
                    && sim.schedule.getSteps() != 0 
                    && (sim.schedule.getSteps() % COLLECT_DATA_EVERY_X_ROUNDS == 0
                    || sim.schedule.getSteps() == FIRST_ROUND_TO_RUN_PERIODIC_ACTIVITIES)){
                collectDataThisRound = true;
            }
            if(collectDataThisRound
                    && prependRound){
                appendToFile(ROUNDS, String.valueOf(rounds));
            }
            
            //check if graphs (time series) are actually updated
            if(updateGraphs){
                
                //check for graph update based on frequency tuples
                updateGraphsThisRound = checkIfActionShouldBeExecuted(TUPLE_ACTION_CHART_AUTO_UPDATE);
                
                if(!updateGraphsThisRound){
                    //only check for updating of graphs if not already activated based on tuples
                    if(UPDATE_CHARTS_EVERY_X_ROUNDS == -1){
                        updateGraphsThisRound = true;
                    } else if(UPDATE_CHARTS_EVERY_X_ROUNDS != -1
                            && (sim.schedule.getSteps() % UPDATE_CHARTS_EVERY_X_ROUNDS == 0)){
                        updateGraphsThisRound = true;
                    } else {
                        updateGraphsThisRound = false;
                    }
                }
            }
            
            //check whether to run the custom method now
            boolean runPeriodicMethod = false;
            //calls a user-defined method periodically
            if(RUN_CUSTOM_METHOD_EVERY_X_ROUNDS != -1 
                    && sim.schedule.getSteps() != 0 
                    && (sim.schedule.getSteps() % RUN_CUSTOM_METHOD_EVERY_X_ROUNDS == 0
                    || sim.schedule.getSteps() == FIRST_ROUND_TO_RUN_PERIODIC_ACTIVITIES)){
                runPeriodicMethod = true;
            }
            //if not to be run called, last execute before other stuff
            if(runPeriodicMethod && !RUN_CUSTOM_METHOD_LAST){
                customMethodThatIsRunPeriodically();
            }
            
            //now step user-implemented stats
            try{
                singleStep();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
            
            // Runs after each round, e.g., for cleanup functionality.
            runAtEndOfEachRound();
            
            //notify all stats listeners about eventual changed stats contents
            deliverToListeners();
            
            if(updateGraphsThisRound){
                //update chart data series in case of newly added data series in datasets
                checkForChangedDataSeries();
            }
            
            //check for printing graphs based on triplets
            boolean printGraphThisRound = checkIfActionShouldBeExecuted(TUPLE_ACTION_CHART_AUTO_PRINT);
            
            //check for graph printing (printing after first round (to get initial view) and then every specified number of rounds)
            if(sim.SHOW_STATS_GRAPHS && ((PRINT_ALL_GRAPHS_EVERY_X_ROUNDS != -1 
                        && sim.schedule.getSteps() != 0 
                        && (sim.schedule.getSteps() % PRINT_ALL_GRAPHS_EVERY_X_ROUNDS == 0
                            || sim.schedule.getSteps() == FIRST_ROUND_TO_RUN_PERIODIC_ACTIVITIES))
                    //print if special interval specified and applies this round
                    || printGraphThisRound)){
                
                //register eventual radar charts for printing if not done so already
                if(!radarCharts.isEmpty() && printRadarCharts){
                    if(!radarChartsRegisteredForPrinting){
                        for(RadarChart entry: radarCharts.values()){
                            registerForPrinting(entry);
                        }
                        radarChartsRegisteredForPrinting = true;
                    }
                }
                
                try{
                    printAllGraphs();
                /*
                 * Can cause NPEs or ConcurrentModification exceptions if data modified during printing. 
                 * More likely for large datasets or fast simulations. Should not occur as print methods and 
                 * charts objects are fully synchronized...
                 */
                } catch (NullPointerException e){
                    e.printStackTrace();
                } catch (ConcurrentModificationException ex) {
                    ex.printStackTrace();
                }
                
                //also save chart data sets
                if(saveDatasetsWhenPrintingCharts){
                    // Time series
                    if(!chartDataMap.isEmpty()){
                        LinkedHashMap<String, DatasetUtility.LabelledDataset> labelledChartDatasets = new LinkedHashMap<>();
                        for(Entry<String, ChartDataSeriesMap> entry: chartDataMap.entrySet()){
                            labelledChartDatasets.put(entry.getKey(), new DatasetUtility().new LabelledDataset(entry.getValue().getChart().getXAxisLabel(), entry.getValue().getChart().getYAxisLabel(), entry.getValue().getDataSeries()));
                        }
                        DatasetUtility.saveTimeSeriesDataToDisk(buildFileNameManually(datasetOutfile, true, ".txt", true), labelledChartDatasets, zipSavedDatasets);
                    }
                    // Radar charts
                    if(!radarCharts.isEmpty() && printRadarCharts){
                        for(Entry<String, RadarChart> entry: radarCharts.entrySet()){
                            //if radar chart exists, write it out as well
                            DatasetUtility.saveCategoryDataToDisk(buildFileNameManually(
                                    new StringBuilder(datasetOutfile).append("_").append(entry.getKey()).append(radarChartDatasetOutfileSuffix).toString(), true, ".txt", true), entry.getValue(), zipSavedDatasets);
                        }
                    }
                }
            }
            //check for stats form printing
            if(SAVE_STATS_FORM_EVERY_X_ROUNDS != -1 
                    && sim.schedule.getSteps() != 0 
                    && (sim.schedule.getSteps() % SAVE_STATS_FORM_EVERY_X_ROUNDS == 0
                    || sim.schedule.getSteps() == FIRST_ROUND_TO_RUN_PERIODIC_ACTIVITIES)){
                saveStatsFormContentToFile(true);
            }
            //check for data writing
            if(collectDataThisRound){
                //append line break
                appendToFile(null, null, true, true);
                //write to file (and close file)
                //writeDataToDiskAndCloseFile();
            }
            //check if periodic custom method should be executed last
            if(runPeriodicMethod && RUN_CUSTOM_METHOD_LAST){
                customMethodThatIsRunPeriodically();
            }
            
            //shut down simulation if number of execution rounds is reached (use discouraged but efficient)
            if(RUN_SIMULATION_FOR_X_ROUNDS != -1 && RUN_SIMULATION_FOR_X_ROUNDS.longValue() == sim.schedule.getSteps()){
                writeDataToDiskAndCloseFile();
                System.out.println(PREFIX + "Simulation ran for " + sim.schedule.getSteps() + " rounds. Will shut down now...");
                sim.finish();
            }
        }
    }
    
    /**
     * Runtime method INVOKED EVERY ROUND to do everything your Statistics 
     * implementation is supposed to do (calculate stats; fill charts, 
     * output buffer and stats form with current information etc.).
     */
    public abstract void singleStep();
    
    /**
     * Method run at the end of each round after {@link #singleStep()}. 
     * Useful for cleanup functionality.
     */
    public abstract void runAtEndOfEachRound();
    
    /**
     * Custom method that is invoked periodically before {@link #singleStep()}.
     * The frequency is configured via {@link #RUN_CUSTOM_METHOD_EVERY_X_ROUNDS} 
     * or StatsGraphController.
     */
    public abstract void customMethodThatIsRunPeriodically();
    
    /**
     * Is called when statistics are reset.
     */
    public abstract void reset();
    
    /**
     * Is called each time a new time series chart is created and allows the 
     * developer to modify the layout.
     * @param chart JFreeChart instance about to be made visible
     * @param plot Convenience parameter for plot wrapped in chart
     * @param stats Statistics instance useful to access global chart settings. Most 
     * global settings will have already been applied when executed though.
     */
    public abstract void formatCharts(TimeSeriesChartWrapper chart, XYPlot plot, Statistics stats);
    
    /**
     * Rounds a double value to two decimals.
     * @param d double value to be rounded
     * @return
     */
    public static Double roundTwoDecimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        Double result = null;
        
        try    {
            result = Double.valueOf(twoDForm.format(d));
        }catch(Exception e){
            System.out.println(PREFIX + "Value causing trouble in Stats: " + e.getCause());
            e.printStackTrace();
            return null;
        }
        return result;
    }
    
    /**
     * Rounds a double value to three decimals.
     * @param d double value to be rounded
     * @return
     */
    public static  Double roundThreeDecimals(double d) {
        DecimalFormat threeDForm = new DecimalFormat("#.###");
        Double result = null;
        
        try    {
            result = Double.valueOf(threeDForm.format(d));
        }catch(Exception e){
            System.out.println(PREFIX + "Value causing trouble in Stats: " + e.getCause());
            e.printStackTrace();
            return null;
        }
        return result;
    }
        
    /** Chart-related stuff */
    
    /**
     * Holds all registered charts with identifier and map of respective data series.
     */
    public LinkedHashMap<String,ChartDataSeriesMap> chartDataMap = new LinkedHashMap<String,ChartDataSeriesMap>();
    
    /**
     * Removes all data series from all charts (e.g. if memory consumption rises).
     */
    public void resetDataSeries(){
        for(Entry<String, ChartDataSeriesMap> entry: chartDataMap.entrySet()){
            resetChart(entry.getValue().getChart(), entry.getValue().getDataSeries());
        }
    }
    
    SimpleSemaphore semaphore = new SimpleSemaphore("Stats DataSeries Semaphore");
    
    /**
     * Removes all data series for a given chart
     * @param chart Chart to remove all series from
     * @param series connection of series to be cleared
     */
    private void resetChart(TimeSeriesChartGenerator chart, HashMap<String,XYSeries> series){
        semaphore.acquire();
        chart.removeAllSeries();
        series.clear();
        semaphore.release();
    }
    
    public abstract void setupCharts();
    
    /**
     * Sets up chart with a given key (which is used both as key and title of chart). 
     * It assumes an x axis representing "Rounds", but requires an y axis description.
     * Note: This method automatically sets the x axis descriptor. Use other method 
     * variants to specify further aspects.
     * @param keyAndTitle Chart key used as unique identifier as well as chart title
     * @param yAxisDesc Y axis description shown on chart
     */
    public void setupChart(String keyAndTitle, String yAxisDesc){
        setupChart(keyAndTitle, ROUNDS, yAxisDesc);
    }
    
    /**
     * Sets up chart with a given key (which is used both as key and title of chart). 
     * It assumes an x axis representing "Rounds", but requires an y axis description.
     * Note: This method automatically sets the x axis descriptor. Use other method 
     * variants to specify further aspects.
     * @param keyAndTitle Chart key used as unique identifier as well as chart title
     * @param yAxisDesc Y axis description shown on chart
     * @param printLegendForThisChart Boolean indicating if legend should be printed for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsOnChart Boolean indicating if labels should be printed on chart itself for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printDashedLines Boolean indicating if series should be printed as dashed lines for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printSeriesInUnifiedColor Boolean indicating if series should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsInUnifiedColor Boolean indicating if labels should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param unifiedColor Unified color applied to series and labels if activated
     */
    public void setupChart(String keyAndTitle, String yAxisDesc, Boolean printLegendForThisChart, Boolean printLabelsOnChart, Boolean printDashedLines, Boolean printSeriesInUnifiedColor, Boolean printLabelsInUnifiedColor, Color unifiedColor){
        setupChart(keyAndTitle, ROUNDS, yAxisDesc, printLegendForThisChart, printLabelsOnChart, printDashedLines, printSeriesInUnifiedColor, printLabelsInUnifiedColor, unifiedColor);
    }
    
    /**
     * Sets up chart with a given key (which is used both as key and title of chart). 
     * It assumes an x axis representing "Rounds", but requires an y axis description 
     * as well as max value specification for y axis. Minimum value for y axis is set 
     * to zero. Use {@link #setupChart(String, String, String, Number, Number)} to 
     * set minimum value manually.
     * Note: This method automatically sets the x axis descriptor. Use other method 
     * variants to specify further aspects.
     * @param keyAndTitle Chart key used as unique identifier as well as chart title
     * @param yAxisDesc Y axis description shown on chart
     * @param yAxisMaxValue max. value y axis
     */
    public void setupChart(String keyAndTitle, String yAxisDesc, Number yAxisMaxValue){
        setupChart(keyAndTitle, ROUNDS, yAxisDesc, 0, yAxisMaxValue);
    }
    
    /**
     * Sets up chart with a given key (which is used both as key and title of chart). 
     * It assumes an x axis representing "Rounds", but requires an y axis description 
     * as well as y axis scale specifications.
     * Note: This method automatically sets the x axis descriptor. Use other method 
     * variants to specify further aspects.
     * @param keyAndTitle Chart key used as unique identifier as well as chart title
     * @param yAxisDesc Y axis description shown on chart
     * @param yAxisMinValue min. value y axis
     * @param yAxisMaxValue max. value y axis
     */
    public void setupChart(String keyAndTitle, String yAxisDesc, Number yAxisMinValue, Number yAxisMaxValue){
        setupChart(keyAndTitle, ROUNDS, yAxisDesc, yAxisMinValue, yAxisMaxValue);
    }
       
    /**
     * Sets up chart with a given key (which is used both as key and title of chart). 
     * It assumes an x axis representing "Rounds", but requires an y axis description 
     * as well as y axis scale specifications.
     * Note: This method automatically sets the x axis descriptor. Use other method 
     * variants to specify further aspects.
     * @param keyAndTitle Chart key used as unique identifier as well as chart title
     * @param yAxisDesc Y axis description shown on chart
     * @param yAxisMinValue min. value y axis
     * @param yAxisMaxValue max. value y axis
     * @param printLegendForThisChart Boolean indicating if legend should be printed for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsOnChart Boolean indicating if labels should be printed on chart itself for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printDashedLines Boolean indicating if series should be printed as dashed lines for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printSeriesInUnifiedColor Boolean indicating if series should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsInUnifiedColor Boolean indicating if labels should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param unifiedColor Unified color applied to series and labels if activated
     */
    public void setupChart(String keyAndTitle, String yAxisDesc, Number yAxisMinValue, Number yAxisMaxValue, Boolean printLegendForThisChart, Boolean printLabelsOnChart, Boolean printDashedLines, Boolean printSeriesInUnifiedColor, Boolean printLabelsInUnifiedColor, Color unifiedColor){
        setupChart(keyAndTitle, ROUNDS, yAxisDesc, yAxisMinValue, yAxisMaxValue, printLegendForThisChart, printLabelsOnChart, printDashedLines, printSeriesInUnifiedColor, printLabelsInUnifiedColor, unifiedColor);
    }
    
    /**
     * Sets up chart with a given key (which is used both as key and title of chart), 
     * x axis description and y axis description.
     * @param keyAndTitle Chart key used as unique identifier as well as chart title
     * @param xAxisDesc X axis description shown on chart
     * @param yAxisDesc Y axis description shown on chart
     */
    public void setupChart(String keyAndTitle, String xAxisDesc, String yAxisDesc){
        setupChart(keyAndTitle, keyAndTitle, xAxisDesc, yAxisDesc, null, null, null, null, null, null);
    }
    
    /**
     * Sets up chart with a given key (which is used both as key and title of chart), 
     * x axis description and y axis description.
     * @param keyAndTitle Chart key used as unique identifier as well as chart title
     * @param xAxisDesc X axis description shown on chart
     * @param yAxisDesc Y axis description shown on chart
     * @param printLegendForThisChart Boolean indicating if legend should be printed for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsOnChart Boolean indicating if labels should be printed on chart itself for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printDashedLines Boolean indicating if series should be printed as dashed lines for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printSeriesInUnifiedColor Boolean indicating if series should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsInUnifiedColor Boolean indicating if labels should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param unifiedColor Unified color applied to series and labels if activated
     */
    public void setupChart(String keyAndTitle, String xAxisDesc, String yAxisDesc, Boolean printLegendForThisChart, Boolean printLabelsOnChart, Boolean printDashedLines, Boolean printSeriesInUnifiedColor, Boolean printLabelsInUnifiedColor, Color unifiedColor){
        setupChart(keyAndTitle, keyAndTitle, xAxisDesc, yAxisDesc, null, null, printLegendForThisChart, printLabelsOnChart, printDashedLines, printSeriesInUnifiedColor, printLabelsInUnifiedColor, unifiedColor);
    }
    
    /**
     * Sets up chart with a given key (which is used both as key and title of chart), 
     * x axis description and y axis description, along with y axis dimensions.
     * @param keyAndTitle Chart key used as unique identifier as well as chart title
     * @param xAxisDesc X axis description shown on chart
     * @param yAxisDesc Y axis description shown on chart
     * @param yAxisMinValue min. value y axis
     * @param yAxisMaxValue max. value y axis
     */
    public void setupChart(String keyAndTitle, String xAxisDesc, String yAxisDesc, Number yAxisMinValue, Number yAxisMaxValue){
        setupChart(keyAndTitle, keyAndTitle, xAxisDesc, yAxisDesc, yAxisMinValue, yAxisMaxValue, null, null, null, null, null, null);
    }
    
    /**
     * Sets up chart with a given key (which is used both as key and title of chart), 
     * x axis description and y axis description, along with y axis dimensions.
     * @param keyAndTitle Chart key used as unique identifier as well as chart title
     * @param xAxisDesc X axis description shown on chart
     * @param yAxisDesc Y axis description shown on chart
     * @param yAxisMinValue min. value y axis
     * @param yAxisMaxValue max. value y axis
     * @param printLegendForThisChart Boolean indicating if legend should be printed for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsOnChart Boolean indicating if labels should be printed on chart itself for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printDashedLines Boolean indicating if series should be printed as dashed lines for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printSeriesInUnifiedColor Boolean indicating if series should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsInUnifiedColor Boolean indicating if labels should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param unifiedColor Unified color applied to series and labels if activated
     */
    public void setupChart(String keyAndTitle, String xAxisDesc, String yAxisDesc, Number yAxisMinValue, Number yAxisMaxValue, Boolean printLegendForThisChart, Boolean printLabelsOnChart, Boolean printDashedLines, Boolean printSeriesInUnifiedColor, Boolean printLabelsInUnifiedColor, Color unifiedColor){
        setupChart(keyAndTitle, keyAndTitle, xAxisDesc, yAxisDesc, yAxisMinValue, yAxisMaxValue, printLegendForThisChart, printLabelsOnChart, printDashedLines, printSeriesInUnifiedColor, printLabelsInUnifiedColor, unifiedColor);
    }
    
    /**
     * Sets up chart with a given key, title, x axis description and y axis description.
     * @param key Chart key used as unique identifier
     * @param title Title shown on the chart
     * @param xAxisDesc X axis description shown on chart
     * @param yAxisDesc Y axis description shown on chart
     */
    public void setupChart(String key, String title, String xAxisDesc, String yAxisDesc){
        setupChart(key, title, xAxisDesc, yAxisDesc, null, null, null, null, null, null);
    }
    
    /**
     * Sets up chart with a given key, title, x axis description and y axis description.
     * @param key Chart key used as unique identifier
     * @param title Title shown on the chart
     * @param xAxisDesc X axis description shown on chart
     * @param yAxisDesc Y axis description shown on chart
     * @param printLegendForThisChart Boolean indicating if legend should be printed for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsOnChart Boolean indicating if labels should be printed on chart itself for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printDashedLines Boolean indicating if series should be printed as dashed lines for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printSeriesInUnifiedColor Boolean indicating if series should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsInUnifiedColor Boolean indicating if labels should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param unifiedColor Unified color applied to series and labels if activated
     */
    public void setupChart(String key, String title, String xAxisDesc, String yAxisDesc, Boolean printLegendForThisChart, Boolean printLabelsOnChart, Boolean printDashedLines, Boolean printSeriesInUnifiedColor, Boolean printLabelsInUnifiedColor, Color unifiedColor){
        setupChart(key, title, xAxisDesc, yAxisDesc, null, null, printLegendForThisChart, printLabelsOnChart, printDashedLines, printSeriesInUnifiedColor, printLabelsInUnifiedColor, unifiedColor);
    }
    
    /**
     * Sets up chart with a given key, title, x axis description and y axis description.
     * @param key Chart key used as unique identifier
     * @param title Title shown on the chart
     * @param xAxisDesc X axis description shown on chart
     * @param yAxisDesc Y axis description shown on chart
     * @param yAxisMinValue min. value y axis
     * @param yAxisMaxValue max. value y axis
     * @param printLegendForThisChart Boolean indicating if legend should be printed for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsOnChart Boolean indicating if labels should be printed on chart itself for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printDashedLines Boolean indicating if series should be printed as dashed lines for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printSeriesInUnifiedColor Boolean indicating if series should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param printLabelsInUnifiedColor Boolean indicating if labels should be printed in unified color for given chart. Value 'null' implies use of global settings,
     *                                     any other value overrides global setting!
     * @param unifiedColor Unified color applied to series and labels if activated
     */
    public void setupChart(String key, String title, String xAxisDesc, String yAxisDesc, Number yAxisMinValue, Number yAxisMaxValue, Boolean printLegendForThisChart, Boolean printLabelsOnChart, Boolean printDashedLines, Boolean printSeriesInUnifiedColor, Boolean printLabelsInUnifiedColor, Color unifiedColor){
        TimeSeriesChartWrapper chart = new TimeSeriesChartWrapper(printLabelsOnChart, printLegendForThisChart, printDashedLines, printSeriesInUnifiedColor, printLabelsInUnifiedColor, unifiedColor);
        //default to whatever is globally set up
        boolean actuallyPrintLegend = this.printLegend;
        if(chart.getPrintLegend() != null){
            actuallyPrintLegend = chart.getPrintLegend();
        }
        //add legend to chart if either globally enabled or overridden by individual chart setups
        if(actuallyPrintLegend){
            chart.getChart().addLegend(new LegendTitle(chart.getChart().getXYPlot()));
            //set length of displayed line in legend item (important for dashed lines): http://www.jfree.org/phpBB2/viewtopic.php?f=3&t=18012
            XYLineAndShapeRenderer xyLineAndShapeRenderer = (XYLineAndShapeRenderer)((XYPlot)chart.getChart().getPlot()).getRenderer();
            xyLineAndShapeRenderer.setLegendLine(new Line2D.Double(-20.0, 0.0, 20.0, 0.0));
            //attempt to center the legend - was buggy in JFreeChart, might be fixed some time
            chart.getChart().getLegend().setHorizontalAlignment(HorizontalAlignment.CENTER);
            //set item padding
            chart.getChart().getLegend().setItemLabelPadding(legendItemPadding);
            //create border around legend
            if(legendBorder != null){
                //use given border specification
                chart.getChart().getLegend().setFrame(legendBorder);
            } else {
                //use default width and color for border
                chart.getChart().getLegend().setFrame(new BlockBorder(legendBorderWidth, legendBorderWidth, legendBorderWidth, legendBorderWidth, legendBorderColor));
            }
            //System.out.println(chart.getChart().getLegend().getItemFont());
            chart.getChart().getLegend().setItemFont(legendItemFont);
            //chart.getChart().getLegend().setItemLabelPadding(new RectangleInsets(50, 50, 10, 10));
        }
        chart.setTitle(title);
        chart.setXAxisLabel(xAxisDesc);
        chart.setYAxisLabel(yAxisDesc);
        //set chart title font
        //System.out.println(chart.getChart().getTitle().getFont());
        chart.getChart().getTitle().setFont(chartTitleFont);
        //Show/hide title
        if(!showChartTitle) {
            //print it in white colour
            TextTitle t = new TextTitle(chart.getChart().getTitle().getText());
            t.setPaint(Color.WHITE);
            chart.getChart().setTitle(t);
        }
        //set axis label fonts
        //System.out.println(((XYPlot)(chart.getChart().getPlot())).getRangeAxis().getLabelFont());
        ((XYPlot)(chart.getChart().getPlot())).getDomainAxis().setLabelFont(chartAxisLabelFont);
        ((XYPlot)(chart.getChart().getPlot())).getRangeAxis().setLabelFont(chartAxisLabelFont);
        //set tick fonts
        //System.out.println(((XYPlot)(chart.getChart().getPlot())).getDomainAxis().getTickLabelFont());
        ((XYPlot)(chart.getChart().getPlot())).getDomainAxis().setTickLabelFont(chartAxisTickFont);
        ((XYPlot)(chart.getChart().getPlot())).getRangeAxis().setTickLabelFont(chartAxisTickFont);
        //set axis min. and max. values
        if(yAxisMinValue != null && yAxisMaxValue != null){
            chart.setYAxisRange(yAxisMinValue.doubleValue(), yAxisMaxValue.doubleValue());
        }
        //set background color
        if(graphBackgroundColor != null){
            chart.getChart().getPlot().setBackgroundPaint(graphBackgroundColor);
        }
        //draw grid lines
        ((XYPlot)(chart.getChart().getPlot())).setDomainGridlinesVisible(drawVerticalGridLines);
        ((XYPlot)(chart.getChart().getPlot())).setRangeGridlinesVisible(drawHorizontalGridLines);
        //set grid line colors - but check if
        if(horizontalGridLinePaint != null){
            ((XYPlot)chart.getChart().getPlot()).setRangeGridlinePaint((drawHorizontalGridLines 
                    && horizontalGridLinePaint.equals(graphBackgroundColor)) 
                    ? (horizontalGridLinePaint.equals(Color.BLACK) ? Color.WHITE : Color.BLACK) 
                    : horizontalGridLinePaint);
        }
        if(verticalGridLinePaint != null){
            ((XYPlot)chart.getChart().getPlot()).setDomainGridlinePaint((drawVerticalGridLines 
                    && verticalGridLinePaint.equals(graphBackgroundColor)) 
                    ? (verticalGridLinePaint.equals(Color.BLACK) ? Color.WHITE : Color.BLACK) 
                    : verticalGridLinePaint);
        }
        //allow developer to fine-tune charts or change global settings
        formatCharts(chart, ((XYPlot)chart.getChart().getPlot()), this);
        //chart.setDataCuller(null);
        //chart.setDataCuller(new MinGapDataCuller(500));
        //store reference to chart in map
        chartDataMap.put(key, new ChartDataSeriesMap(chart, new LinkedHashMap<String,XYSeries>()));
    }
    
    /**
     * Makes all prepared charts visible and assigns them JFrames.
     * Can be called repeated for newly created charts at runtime.
     */
    protected void setupChartFrames(){
        for(String key: chartDataMap.keySet()){
            if(chartDataMap.get(key).getJFrame() == null){
                chartDataMap.get(key).setJFrame(
                        setupChartFrame(new JFrame(),  
                        chartDataMap.get(key).getChart(), true));
            }
        }
    }
    
    /**
     * Prepare individual chart frame
     * @param chartFrame chart frame instance
     * @param chart contained time series chart
     * @param visible Indicator whether the frame should be visible
     * @return Returns reference to frame instance
     */
    private JFrame setupChartFrame(JFrame chartFrame, TimeSeriesChartGenerator chart, boolean visible){
        chartFrame = chart.createFrame();
        chartFrame.getContentPane().setLayout(new BorderLayout());
        chartFrame.getContentPane().add(chart, BorderLayout.CENTER);
        chartFrame.pack();
        //controller.registerFrame(chartFrame);
        chartFrame.setVisible(visible);
        chartFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        if(manageWindowsUsingPositionSaver){
            PositionSaver.registerWindow(chartFrame);
        }
        return chartFrame;
    }
    
    /**
     * Default stroke width for lines in charts, unless {@link #multipleStrokeWidthsForRandomSeriesLines} 
     * is activated. Default: 1.5
     */
    public static float defaultStrokeWidth = 1.5f;
    
    /**
     * Indicates if multiple strokes widths should be used for automatic 
     * assignment of line strokes for series to enhance patterns.
     */
    public static boolean multipleStrokeWidthsForRandomSeriesLines = false;
    
    /**
     * Indicates if series stroke assignment should be randomized (with 
     * fixed seed for reproducability)
     */
    public static boolean randomizeSeriesLineSelection = false;
    
    /**
     * Collection of different stroke widths used for series lines.
     */
    public static ArrayList<Float> strokeWidths = new ArrayList<>();
    
    /**
     * List of strokes with index for reproducible assignment
     */
    public static ArrayList<BasicStroke> basicStrokes = null;
    
    /**
     * Indicates if line stroke collection has been initialized.
     */
    private static boolean strokesInitialized = false;
    
    /**
     * Initializes strokes for different series lines. 
     * Considers additional settings such as 
     * {@link #multipleStrokeWidthsForRandomSeriesLines} 
     * and {@link #randomizeSeriesLineSelection}.
     */
    private synchronized static void initializeSeriesStrokes(){
        if(!strokesInitialized){
            strokeWidths.add(defaultStrokeWidth);
            if(multipleStrokeWidthsForRandomSeriesLines){
                strokeWidths.add(1f);
                strokeWidths.add(2.5f);
            }
            if(basicStrokes == null){
                basicStrokes = new ArrayList<BasicStroke>();
            }
            for(int i = 0; i < strokeWidths.size(); i++){
                defaultStrokeWidth = strokeWidths.get(i);
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {16.0f, 6.0f}, 0.0f
                    ));
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                        1.0f
                    ));
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {8.0f, 8.0f}, 0.0f
                    ));
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {2.0f, 6.0f}, 0.0f
                    ));
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {12.0f, 6.0f, 3.0f, 6.0f}, 0.0f
                    ));
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {9.0f, 6.0f, 5.0f, 6.0f}, 0.0f
                    ));
                /*basicStrokes.add(new BasicStroke(
                        strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {1.0f, 6.0f, 1.0f, 6.0f}, 0.0f
                    ));*/
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {9.0f, 5.0f, 1.0f, 3.0f}, 0.0f
                    ));
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {18.0f, 6.0f, 1.0f, 6.0f}, 0.0f
                    ));
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {4.0f, 6.0f, 1.0f, 6.0f}, 0.0f
                    ));
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {17.0f, 6.0f}, 0.0f
                    ));
                basicStrokes.add(new BasicStroke(
                        defaultStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                        1.0f, new float[] {25.0f, 6.0f, 5.0f, 6.0f}, 0.0f
                    ));
            }
            if(randomizeSeriesLineSelection){
                Collections.shuffle(basicStrokes, new Random(1326465));
            }
            strokesInitialized = true;
        }
    }
    
    /**
     * Returns dashed stroke identified by index. Will cycle if number of 
     * specified strokes is exceeded.
     * @param index Index of stroke
     * @return
     */
    private static BasicStroke getStroke(int index){
        initializeSeriesStrokes();
        if(index >= basicStrokes.size()){
            //recursive call to oneself
            return getStroke(index - basicStrokes.size());
        }
        return basicStrokes.get(index);
    }
    
    CustomLabelGenerator labelGenerator = new CustomLabelGenerator(randomPositioningOfSeriesNamesOnChart);
    
    /**
     * Prints strokes for series in console to allow identification of stroke pattern.
     */
    private final boolean debugStrokes = false;
    
    /**
     * Cache for strokes used per chart (element index as series index)
     */
    private HashMap<String, ArrayList<BasicStroke>> chartStrokeCache = new HashMap<String, ArrayList<BasicStroke>>();
    
    /**
     * Formats all series of a given chart based on the parameters set. Also checks
     * for suppressed colors during color assignment.
     * @param chart chart whose series are to be formatted
     * @param printSeriesNamesOnChart print series names on chart (not only legend)
     * @param printGraphsAsDashedLines print series as dashed lines
     * @param printGraphsInUnifiedColor print series in one colour
     */
    private void formatChartForAllSeries(TimeSeriesChartWrapper chart, boolean printSeriesNamesOnChart, boolean printGraphsAsDashedLines, boolean printGraphsInUnifiedColor){
        if(chart == null){
            throw new RuntimeException(PREFIX + "Invoked formatting method with time series chart reference being null!");
        }
        int ct = chart.getSeriesCount();
        //check if either some individual or global properties set
        if(chart.someOutputPropertiesDefined() || printSeriesNamesOnChart || printGraphsAsDashedLines || printGraphsInUnifiedColor || (!printGraphsInUnifiedColor && !suppressedGraphColors.isEmpty())){
            //cache for suppressed and assigned colors (per chart)
            HashSet<Color> alreadyUsedColor = new HashSet<>(suppressedGraphColors);
            for(int i=0; i<=ct; i++){
                
                //print labels only if explicitly activated or globally activated
                if((chart.getPrintLabels() == null && printSeriesNamesOnChart) || (chart.getPrintLabels() != null && chart.getPrintLabels())){
                    XYPlot plot = (XYPlot)chart.getChart().getXYPlot();
                    XYItemRenderer itemRenderer = plot.getRenderer();
                    if((chart.getPrintLabelsInUnifiedColor() == null && printSeriesLabelsInUnifiedColor) || (chart.getPrintLabelsInUnifiedColor() != null && chart.getPrintLabelsInUnifiedColor())){
                        //try for individual chart setups
                        if(chart.getUnifiedLabelAndOrSeriesColor() != null){
                            itemRenderer.setSeriesItemLabelPaint(i, chart.getUnifiedLabelAndOrSeriesColor());
                        } else {
                            //fall back to global setting
                            if(unifiedSeriesLabelColor == null){
                                throw new RuntimeException(PREFIX + "Specified unified series label color in Statistics is null!");
                            }
                            itemRenderer.setSeriesItemLabelPaint(i, unifiedSeriesLabelColor);
                        }
                    } else {
                        itemRenderer.setSeriesItemLabelPaint(i, itemRenderer.getSeriesPaint(i));
                    }
                    itemRenderer.setDefaultItemLabelFont(seriesLabelFont);
                    //itemRenderer.setBaseItemLabelGenerator(labelGenerator);
                    itemRenderer.setDefaultItemLabelsVisible(true);
                }
                
                //print dashed lines only if explicitly or globally activated
                if((chart.getPrintDashedLines() == null && printGraphsAsDashedLines) || (chart.getPrintDashedLines() != null && chart.getPrintDashedLines())){
                    String chartName = chart.getTitle();
                    //System.out.println("Operating on chart " + chartName + ", no of series: " + ct);
                    if(chartStrokeCache.containsKey(chartName)){
                        if(chartStrokeCache.get(chartName).size() > i){
                            chart.getChart().getXYPlot().getRenderer().setSeriesStroke(i, chartStrokeCache.get(chartName).get(i));
                        } else {
                            BasicStroke stroke = getStroke(i);
                            chartStrokeCache.get(chartName).add(stroke);
                            chart.getChart().getXYPlot().getRenderer().setSeriesStroke(i, stroke);
                        }
                    } else {
                        BasicStroke stroke = getStroke(i);
                        ArrayList<BasicStroke> strokes = new ArrayList<BasicStroke>();
                        strokes.add(stroke);
                        chartStrokeCache.put(chartName, strokes);
                        chart.getChart().getXYPlot().getRenderer().setSeriesStroke(i, stroke);
                    }
                    
                    //Prints stroke pattern along with chart, seriesname
                    if(debugStrokes){
                        try{
                            System.out.println(PREFIX + "Stroke pattern: " + DataStructurePrettyPrinter.decomposeRecursively(chartStrokeCache.get(chartName).get(i).getDashArray(), null) + " for chart '" + chartName + "', series '" + chart.getChart().getXYPlot().getDataset().getSeriesKey(i).toString() + "'");
                        } catch(IllegalArgumentException e){
                            //ignore, can happen when simulation starts and not all series set yet
                        }
                    }
                }
                
                //print series in unified color only if explicitly or globally activated
                if(((chart.getPrintSeriesInUnifiedColor() == null 
                        || chart.getPrintSeriesInUnifiedColor()) && printGraphsInUnifiedColor) || (chart.getPrintLabelsInUnifiedColor() != null && chart.getPrintLabelsInUnifiedColor())){
                    //test for individually specified color first
                    if(chart.getUnifiedLabelAndOrSeriesColor() != null){
                        chart.getChart().getXYPlot().getRenderer().setSeriesPaint(i, chart.getUnifiedLabelAndOrSeriesColor());
                    } else {
                        //falls back to global unified color if not individually specified
                        if(unifiedGraphColor == null){
                            throw new RuntimeException(PREFIX + "Specified unified graph color in Statistics is null!");
                        }
                        chart.getChart().getXYPlot().getRenderer().setSeriesPaint(i, unifiedGraphColor);
                    }
                }
                
                //check for suppressed colors (only if no unified color printing activated)
                if(!printGraphsInUnifiedColor && alreadyUsedColor != null && i < chart.getChart().getXYPlot().getDataset().getSeriesCount()){
                    if(printSeriesColorAssignmentDebug){
                        String name = (String) chart.getChart().getXYPlot().getDataset().getSeriesKey(i);
                        System.out.println(new StringBuilder("= Chart: ").append(chart.getChart().getTitle().getText()));
                        System.out.println(new StringBuilder("== Series: Index: ").append(i).append(", Name: ").append(name));
                        System.out.println(new StringBuilder("=== Color of this series: ").append((Color)chart.getChart().getXYPlot().getRenderer().getSeriesPaint(i)));
                        System.out.println(new StringBuilder("=== Forbidden color(s): ").append(alreadyUsedColor));
                    }
                    Color seriesPaint = (Color)chart.getChart().getXYPlot().getRenderer().getSeriesPaint(i);
                    if(seriesPaint != null){
                        int j = i;
                        while(alreadyUsedColor.contains(seriesPaint) && j < chart.getChart().getXYPlot().getDataset().getSeriesCount()){
                            if(j - i >= 5){
                                String name = (String) chart.getChart().getXYPlot().getDataset().getSeriesKey(i).toString();
                                System.err.println(new StringBuilder("Automated color assignment iterated more than five times looking for unassigned or non-forbidden color for series ") 
                                        .append(name).append(" in chart ").append(chart.getChart().getTitle().getText()) 
                                        .append(".").append(LINEBREAK)
                                        .append("If system blocks, check for potential infinite loop as of limited number of available series colors."));
                            }
                            j++;
                            //if forbidden color is found, use color for next series
                            seriesPaint = (Color)chart.getChart().getXYPlot().getRenderer().getSeriesPaint(j);
                            if(printSeriesColorAssignmentDebug){
                                System.out.println("==== Changed color for series to new candidate color " + seriesPaint + " as of existing assignment");
                            }
                        }
                        //assign paint that has not been used previously for this chart
                        chart.getChart().getXYPlot().getRenderer().setSeriesPaint(i, seriesPaint);
                        //remember all color assignments to avoid duplicate assignment of colors
                        alreadyUsedColor.add((Color)chart.getChart().getXYPlot().getRenderer().getSeriesPaint(i));
                    }
                }
            }
        }
    }
    
    /**
     * De/Activates Data culling on all active charts.
     * @param activate true -> activate, false -> deactivate
     * @param maxEntries maximum number of entries
     */
    public void activateDataCulling(boolean activate, int maxEntries){
        for(Entry<String, ChartDataSeriesMap> entry: chartDataMap.entrySet()){
            if(activate){
                entry.getValue().getChart().setDataCuller(new MinGapDataCuller(maxEntries));
            } else {
                entry.getValue().getChart().setDataCuller(null);
            }
        }
        if(activate){
            dataCullingActivated = maxEntries;
            System.out.println(PREFIX + "Activated data culling with maximum entries number of " + maxEntries);
        } else {
            dataCullingActivated = -1;
            System.out.println(PREFIX + "Deactivated data culling");
        }
        if(graphController != null){
            graphController.checkForChangedSimulationSettings();
        }
    }
    
    /**
     * Indicates if data culling is activated.
     * @return -1 if data culling is deactivated, else other positive values that specify the number of entries.
     */
    public int dataCullingActivated(){
        return dataCullingActivated;
    }
    
        
    /* excluded data series */
    private HashSet<String> excludedDataSeries = new HashSet<String>();
    
    /**
     * Adds data series that is never to be printed in chart (for temporary 
     * exclusion to produce customized charts)
     * @param dataSeries data series key
     */
    public void excludeDataSeriesFromCharts(String dataSeries){
        excludedDataSeries.add(dataSeries);
    }
    
    /**
     * Allows printing of data series that has been previously blocked.
     * @param dataSeries data series key
     */
    public void removeDataSeriesFromExclusion(String dataSeries){
        excludedDataSeries.remove(dataSeries);
    }
    
    /**
     * Checks for added data series in all charts and adds them missing ones
     * to the chart (processed during every Statistics step).
     */
    protected void checkForChangedDataSeries(){
        for(Entry<String,ChartDataSeriesMap> entry: chartDataMap.entrySet()){
            checkForChangedDataSeriesForChart(entry.getValue().getChart(), entry.getValue().getDataSeries());
        }
    }
    
    /**
     * Checks for changed collection of data series outside of chart. If number of 
     * entries in series is different from chart series, all chart series are recreated 
     * from the parameter series. Note: This only occurs when series are added or 
     * removed from charts.
     * @param chart Chart to check
     * @param series Series to compare (and eventually use to rebuild chart series entries)
     */
    private void checkForChangedDataSeriesForChart(final TimeSeriesChartWrapper chart, final HashMap<String,XYSeries> series){
        if(chart != null && series != null){
            if(chart.getSeriesCount() != series.size()){
                chart.removeAllSeries();
                for(String seriesKey: series.keySet()){
                    chart.addSeries(series.get(seriesKey), null);
                    //do the formatting for each line
                    formatChartForAllSeries(chart, printSeriesLabelsOnChart, printGraphsAsDashedLines, printGraphsInUnifiedColor);
                }
            }
        }
    }
    
    /**
     * Adds data series entry for a given chart.
     * @param chartDataKey Chart containing the element's series.
     * @param seriesName series containing the element
     * @param entry Element (combination of X+Y value) to be added
     */
    public void addDataSeriesEntry(final String chartDataKey, final String seriesName, final XYDataItem entry){
        addDataSeriesEntry(chartDataKey, seriesName, ROUNDS, entry);
    }
    
    /**
     * Adds data series entry for a given chart.
     * @param chartDataKey Chart containing the element's series.
     * @param seriesName series containing the element
     * @param xLabel Label for x axis (only applies to newly generated chart on first call)
     * @param entry Element (combination of X+Y value) to be added
     */
    public void addDataSeriesEntry(final String chartDataKey, final String seriesName, String xLabel, final XYDataItem entry){
        if(updateGraphs && sim.SHOW_STATS_GRAPHS 
                && (UPDATE_CHARTS_EVERY_X_ROUNDS == -1 || updateGraphsThisRound)){
            if(excludedDataSeries.contains(seriesName)){
                return;
            }
            semaphore.acquire();
            if(!chartDataMap.containsKey(chartDataKey)){
                if(autogenerateGraphForNewChartKey && initialized()){
                	if (xLabel == null) {
                		xLabel = ROUNDS; //Set label to default
                	}
                    //generate new graph
                    System.err.println(PREFIX + "Generating new chart for unknown chart name '" + chartDataKey + "'.");
                    //create chart
                    if(defaultMinYValueForAutoGeneratedGraphs != null && defaultMaxYValueForAutoGeneratedGraphs != null){
                        //both min. and max. values specified for y axis
                        setupChart(chartDataKey, chartDataKey, xLabel, defaultYAxisLabelForAutoGeneratedGraphs, defaultMinYValueForAutoGeneratedGraphs, defaultMaxYValueForAutoGeneratedGraphs, printLegend, printSeriesLabelsOnChart, printGraphsAsDashedLines, printGraphsInUnifiedColor, printSeriesLabelsInUnifiedColor, unifiedGraphColor);
                    } else if(defaultMaxYValueForAutoGeneratedGraphs != null){
                        //only max. value for y axis specified (min. value defaults to zero)
                        setupChart(chartDataKey, chartDataKey, xLabel, defaultYAxisLabelForAutoGeneratedGraphs, 0, defaultMaxYValueForAutoGeneratedGraphs, printLegend, printSeriesLabelsOnChart, printGraphsAsDashedLines, printGraphsInUnifiedColor, printSeriesLabelsInUnifiedColor, unifiedGraphColor);
                    } else {
                        //no min./max. values specified for y axis
                        setupChart(chartDataKey, chartDataKey, xLabel, defaultYAxisLabelForAutoGeneratedGraphs, printLegend, printSeriesLabelsOnChart, printGraphsAsDashedLines, printGraphsInUnifiedColor, printSeriesLabelsInUnifiedColor, unifiedGraphColor);
                    }
                    //create and register frame
                    setupChartFrames();
                } else {
                    if(!initialized()){
                        System.err.println(PREFIX + "Statistics module not yet initialized. Aborting addition of data series entry.");
                    } else {
                        System.err.println(PREFIX + "No data chart for key " + chartDataKey + " registered. Activate autogenerateGraphForNewChartKey in Statistics implementation to setup charts on the fly.");
                    }
                    semaphore.release();
                    return;
                }
            }
            //add entry
            LinkedHashMap<String,XYSeries> series = chartDataMap.get(chartDataKey).getDataSeries();
            if(!series.containsKey(seriesName)){
                series.put(seriesName, new XYSeries(seriesName));
            }
            series.get(seriesName).add(entry);
            semaphore.release();
        }
    }
    
    /**
     * Adds a complete series consisting of a map of x-y values to a chart for a given series. 
     * Note: Unlike most other methods, this one does not assume time series output related to 
     * the current execution round, but can be used to visualise any sort of complete x-y dataset. 
     * 
     * @param chartDataKey Chart name
     * @param seriesName Series name
     * @param values Map consisting of x and y values
     */
    public void addCompleteDataSeries(final String chartDataKey, final String seriesName, final Map<? extends Number, ? extends Number> values) {
    	addCompleteDataSeries(chartDataKey, seriesName, null, values);
    }
    
    /**
     * Adds a complete series consisting of a map of x-y values to a chart for a given series. 
     * Note: Unlike most other methods, this one does not assume time series output related to 
     * the current execution round, but can be used to visualise any sort of complete x-y dataset. 
     * 
     * @param chartDataKey Chart name
     * @param seriesName Series name
     * @param xLabel Label for x axis (only works for initial call for chart creation)
     * @param values Map consisting of x and y values
     */
    public void addCompleteDataSeries(final String chartDataKey, final String seriesName, final String xLabel, final Map<? extends Number, ? extends Number> values) {
        if(excludedDataSeries.contains(seriesName) || values == null || values.isEmpty()){
            return;
        }
        for (Entry<? extends Number, ? extends Number> entry: values.entrySet()) {
        	addDataSeriesEntry(chartDataKey, seriesName, xLabel, new XYDataItem(entry.getKey(), entry.getValue()));
        }
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param xValue x-value for series
     * @param yValue y-value for series
     */
    public void addDataSeriesEntry(final String chartDataKey, final String seriesName, final Number xValue, final Number yValue){
        if(excludedDataSeries.contains(seriesName) || xValue == null || yValue == null){
            return;
        }
        addDataSeriesEntry(chartDataKey, seriesName, new XYDataItem(xValue, yValue));
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param xValue x-value for series
     * @param yValue y-value for series
     * @param printToOutfile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     */
    public void addDataSeriesEntry(final String chartDataKey, final String seriesName, final Number xValue, final Number yValue, final boolean printToOutfile){
        if(excludedDataSeries.contains(seriesName) || xValue == null || yValue == null){
            return;
        }
        addDataSeriesEntry(chartDataKey, seriesName, new XYDataItem(xValue, yValue));
        if(printToOutfile){
            addToRoundBuffer(new StringBuffer(seriesName).append(": ").append(yValue), true);
        }
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesNameAndValue Series name and y value for current round as key-value pair
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final Entry<String, Number> seriesNameAndValue){
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesNameAndValue.getKey(), seriesNameAndValue.getValue());
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Number yValue){
        if(sim != null){
            if(excludedDataSeries.contains(seriesName) || yValue == null){
                return;
            }
            addDataSeriesEntry(chartDataKey, seriesName, new XYDataItem(sim.schedule.getSteps(), yValue));
        }
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param seriesCsvPrefix Prefix for series when added to CSV outfile (e.g. to customise series name)
     *          ignored if null
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Number yValue, final boolean printIntoFile, final String seriesCsvPrefix){
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue, printIntoFile, seriesCsvPrefix, false, false, false);
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param seriesCsvPrefix Prefix for series when added to CSV outfile (e.g. to customise series name)
     *          ignored if null
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Float yValue, final boolean printIntoFile, final String seriesCsvPrefix){
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue, printIntoFile, seriesCsvPrefix, false, false, false);
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param seriesCsvPrefix Prefix for series when added to CSV outfile (e.g. to customise series name)
     *          ignored if null
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Double yValue, final boolean printIntoFile, final String seriesCsvPrefix){
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue, printIntoFile, seriesCsvPrefix, false, false, false);
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param seriesCsvPrefix Prefix for series when added to CSV outfile (e.g. to customise series name)
     *          ignored if null
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Long yValue, final boolean printIntoFile, final String seriesCsvPrefix){
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue, printIntoFile, seriesCsvPrefix, false, false, false);
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param addToRoundBuffer Indicates if entry is also added to round buffer (stats output window).
     * 
     * Note: If adding entries to round buffer, a line break is automatically appended. Also, 
     * entries are NOT added to structured map (which is delivered to stats listeners).
     * @see #addDataSeriesEntryForCurrentRound(String, String, Number, boolean, boolean, boolean, boolean) 
     * as alternative 
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Number yValue, final boolean printIntoFile, final boolean addToRoundBuffer){
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue, printIntoFile, null, addToRoundBuffer, true, false);
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param addToRoundBuffer Indicates if entry is also added to round buffer (stats output window).
     * 
     * Note: If adding entries to round buffer, a line break is automatically appended. Also, 
     * entries are NOT added to structured map (which is delivered to stats listeners).
     * @see #addDataSeriesEntryForCurrentRound(String, String, Number, boolean, boolean, boolean, boolean) 
     * as alternative 
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Float yValue, final boolean printIntoFile, final boolean addToRoundBuffer){
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue, printIntoFile, null, addToRoundBuffer, true, false);
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param addToRoundBuffer Indicates if entry is also added to round buffer (stats output window).
     * 
     * Note: If adding entries to round buffer, a line break is automatically appended. Also, 
     * entries are NOT added to structured map (which is delivered to stats listeners).
     * @see #addDataSeriesEntryForCurrentRound(String, String, Number, boolean, boolean, boolean, boolean) 
     * as alternative 
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Double yValue, final boolean printIntoFile, final boolean addToRoundBuffer){
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue, printIntoFile, null, addToRoundBuffer, true, false);
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param addToRoundBuffer Indicates if entry is also added to round buffer (stats output window).
     * 
     * Note: If adding entries to round buffer, a line break is automatically appended. Also, 
     * entries are NOT added to structured map (which is delivered to stats listeners).
     * @see #addDataSeriesEntryForCurrentRound(String, String, Number, boolean, boolean, boolean, boolean) 
     * as alternative 
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Long yValue, final boolean printIntoFile, final boolean addToRoundBuffer){
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue, printIntoFile, null, addToRoundBuffer, true, false);
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param seriesCsvPrefix Optional prefix for series name when added to CSV outfile for customisation 
     *          (does not affect chart series name). Empty ("") or null values will be ignored.
     * @param addToRoundBuffer Indicates if entry is also added to round buffer (stats output window).
     * @param addLineBreak Indicates if linebreak is added for entry when entering into round buffer
     * @param addToStructuredMap Indicates if entry is also added to structured map (which is delivered 
     *             to eventual statistics listeners.
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Number yValue, final boolean printIntoFile, final String seriesCsvPrefix, final boolean addToRoundBuffer, final boolean addLineBreak, final boolean addToStructuredMap){
        if(excludedDataSeries.contains(seriesName) || yValue == null){
            return;
        }
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue);
        if(printIntoFile){
            // Append to CSV file and add prefix if not null or empty
            appendToFile(((seriesCsvPrefix != null && !seriesCsvPrefix.isEmpty()) ? seriesCsvPrefix + seriesName : seriesName), yValue);
        }
        if(addToRoundBuffer){
            addKeyValueToRoundBuffer(new StringBuffer(seriesName), yValue, addLineBreak, addToStructuredMap);
        }
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param seriesCsvPrefix Optional prefix for series name when added to CSV outfile for customisation 
     *          (does not affect chart series name). Empty ("") or null values will be ignored.
     * @param addToRoundBuffer Indicates if entry is also added to round buffer (stats output window).
     * @param addLineBreak Indicates if linebreak is added for entry when entering into round buffer
     * @param addToStructuredMap Indicates if entry is also added to structured map (which is delivered 
     *             to eventual statistics listeners.
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Float yValue, final boolean printIntoFile, final String seriesCsvPrefix, final boolean addToRoundBuffer, final boolean addLineBreak, final boolean addToStructuredMap){
        if(excludedDataSeries.contains(seriesName) || yValue == null){
            return;
        }
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue);
        if(printIntoFile){
            // Append to CSV file and add prefix if not null or empty
            appendToFile(((seriesCsvPrefix != null && !seriesCsvPrefix.isEmpty()) ? seriesCsvPrefix + seriesName : seriesName), yValue);
        }
        if(addToRoundBuffer){
            addKeyValueToRoundBuffer(new StringBuffer(seriesName), yValue, addLineBreak, addToStructuredMap);
        }
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param seriesCsvPrefix Optional prefix for series name when added to CSV outfile for customisation 
     *          (does not affect chart series name). Empty ("") or null values will be ignored.
     * @param addToRoundBuffer Indicates if entry is also added to round buffer (stats output window).
     * @param addLineBreak Indicates if linebreak is added for entry when entering into round buffer
     * @param addToStructuredMap Indicates if entry is also added to structured map (which is delivered 
     *             to eventual statistics listeners.
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Double yValue, final boolean printIntoFile, final String seriesCsvPrefix, final boolean addToRoundBuffer, final boolean addLineBreak, final boolean addToStructuredMap){
        if(excludedDataSeries.contains(seriesName) || yValue == null){
            return;
        }
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue);
        if(printIntoFile){
            // Append to CSV file and add prefix if not null or empty
            appendToFile(((seriesCsvPrefix != null && !seriesCsvPrefix.isEmpty()) ? seriesCsvPrefix + seriesName : seriesName), yValue);
        }
        if(addToRoundBuffer){
            addKeyValueToRoundBuffer(new StringBuffer(seriesName), yValue, addLineBreak, addToStructuredMap);
        }
    }
    
    /**
     * Adds data series entry for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param seriesName Series name
     * @param yValue y-value for round of series
     * @param printIntoFile indicates if value (and the series name as header) 
     *             should be added to CSV statistics outfile (without producing a line break afterwards).
     * @param seriesCsvPrefix Optional prefix for series name when added to CSV outfile for customisation 
     *          (does not affect chart series name). Empty ("") or null values will be ignored.
     * @param addToRoundBuffer Indicates if entry is also added to round buffer (stats output window).
     * @param addLineBreak Indicates if linebreak is added for entry when entering into round buffer
     * @param addToStructuredMap Indicates if entry is also added to structured map (which is delivered 
     *             to eventual statistics listeners.
     */
    public void addDataSeriesEntryForCurrentRound(final String chartDataKey, final String seriesName, final Long yValue, final boolean printIntoFile, final String seriesCsvPrefix, final boolean addToRoundBuffer, final boolean addLineBreak, final boolean addToStructuredMap){
        if(excludedDataSeries.contains(seriesName) || yValue == null){
            return;
        }
        addDataSeriesEntryForCurrentRound(chartDataKey, seriesName, yValue);
        if(printIntoFile){
            // Append to CSV file and add prefix if not null or empty
            appendToFile(((seriesCsvPrefix != null && !seriesCsvPrefix.isEmpty()) ? seriesCsvPrefix + seriesName : seriesName), yValue);
        }
        if(addToRoundBuffer){
            addKeyValueToRoundBuffer(new StringBuffer(seriesName), yValue, addLineBreak, addToStructuredMap);
        }
    }
    
    /**
     * Adds multiple data series entries to a given chart.
     * @param chartDataKey Chart identifier holding the data series
     * @param entries Map holding series as key and y value as value
     * @param xyItemKey X value (key) for entry in series
     */
    public void addMultipleDataSeriesEntries(final String chartDataKey, final Map<String,Object> entries, final Number xyItemKey){
        if (chartDataKey == null || entries == null || xyItemKey == null || entries.isEmpty()) {
            return;
        }
        for (Entry<String,Object> entry: entries.entrySet()) {
            addDataSeriesEntry(chartDataKey, entry.getKey(), new XYDataItem(xyItemKey, (Number)entry.getValue()));
        }
    }
    
    //TODO: Make method signatures adaptive for float, double and long with respect to non-scientific representation (numberFormat.format(...))
    
    /**
     * Adds multiple data series entries to a given chart for the current simulation round.
     * @param chartDataKey Chart identifier holding the data series
     * @param entries Map holding series as key and y value as value
     */
    public void addMultipleDataSeriesEntriesForCurrentRound(final String chartDataKey, final Map<String,Number> entries){
        addMultipleDataSeriesEntriesForCurrentRound(chartDataKey, entries, null);
    }
    
    /**
     * Adds multiple data series entries to a given chart for the current simulation round if 
     * values meet or exceed a given threshold.
     * @param chartDataKey Chart identifier holding the data series
     * @param entries Map holding series as key and y value as value
     * @param threshold Threshold for value to be included in chart (null means 'no threshold')
     */
    public void addMultipleDataSeriesEntriesForCurrentRound(final String chartDataKey, final Map<String,Number> entries, Number threshold){
        addMultipleDataSeriesEntriesForCurrentRound(chartDataKey, entries, threshold, false, null);
    }
    
    /**
     * Adds multiple data series entries to a given chart for the current simulation round if 
     * values meet or exceed a given threshold.
     * @param chartDataKey Chart identifier holding the data series
     * @param entries Map holding series as key and y value as value
     * @param printIntoFile indicates if value (and the series name as header) 
     *          should be added to CSV statistics outfile (without producing a line break afterwards)
     */
    public void addMultipleDataSeriesEntriesForCurrentRound(final String chartDataKey, final Map<String,Number> entries, boolean printIntoFile){
        addMultipleDataSeriesEntriesForCurrentRound(chartDataKey, entries, null, printIntoFile, null);
    }
    
    /**
     * Adds multiple data series entries to a given chart for the current simulation round if 
     * values meet or exceed a given threshold.
     * @param chartDataKey Chart identifier holding the data series
     * @param entries Map holding series as key and y value as value
     * @param printIntoFile indicates if value (and the series name as header) 
     *          should be added to CSV statistics outfile (without producing a line break afterwards)
     * @param seriesCsvPrefix Optional prefix for series name when added to CSV outfile for customisation 
     *          (does not affect chart series name). Empty ("") or null values will be ignored.
     */
    public void addMultipleDataSeriesEntriesForCurrentRound(final String chartDataKey, final Map<String,Number> entries, boolean printIntoFile, String seriesCsvPrefix){
        addMultipleDataSeriesEntriesForCurrentRound(chartDataKey, entries, null, printIntoFile, seriesCsvPrefix);
    }
    
    /**
     * Adds multiple data series entries to a given chart for the current simulation round if 
     * values meet or exceed a given threshold.
     * @param chartDataKey Chart identifier holding the data series
     * @param entries Map holding series as key and y value as value
     * @param threshold Threshold for value to be included in chart (null means 'no threshold')
     * @param printIntoFile indicates if value (and the series name as header) 
     *          should be added to CSV statistics outfile (without producing a line break afterwards)
     * @param seriesCsvPrefix Optional prefix for series name when added to CSV outfile for customisation 
     *          (does not affect chart series name). Empty ("") or null values will be ignored.
     */
    public void addMultipleDataSeriesEntriesForCurrentRound(final String chartDataKey, final Map<String,Number> entries, Number threshold, boolean printIntoFile, String seriesCsvPrefix){
        if (chartDataKey == null || entries == null || entries.isEmpty()) {
            return;
        }
        if (threshold == null) {
            for(Entry<String,Number> entry: entries.entrySet()){
                addDataSeriesEntryForCurrentRound(chartDataKey, entry.getKey(), entry.getValue(), printIntoFile, seriesCsvPrefix);
                //addDataSeriesEntry(chartDataKey, entry.getKey(), new XYDataItem(sim.schedule.getSteps(), entry.getValue()));
            }
        } else {
            for(Entry<String,Number> entry: entries.entrySet()){
                if (entry.getValue().floatValue() >= threshold.floatValue()) {
                    addDataSeriesEntryForCurrentRound(chartDataKey, entry.getKey(), entry.getValue(), printIntoFile, seriesCsvPrefix);
                    //addDataSeriesEntry(chartDataKey, entry.getKey(), new XYDataItem(sim.schedule.getSteps(), entry.getValue()));
                }
            }
        }
    }
    
    /**
     * Generates a constant-length midfix indicating 
     * the current rounds.
     * @return
     */
    public static String buildFilenameRoundMidfix(){
        String tempString = "_";
        String postString = "";
        postString += Statistics.rounds;
        while(tempString.length() + postString.length() < 6){
            tempString += "0";
        }
        tempString += postString;
        tempString += "_rds";
        return tempString;
    }
    
    /**
     * Builds a file name that complies with current simulation 
     * settings (ie. global path) and includes user-specified 
     * elements. Optionally, round information is included as 
     * well as a trailing String.
     * Offers greatest flexibility in creating custom filenames.
     * @param coreName Pure (i.e. without rounds etc.) filename without ending
     * @param includeRounds Indicates if round information is included (if global switch {@link #considerRoundsInFilenameGeneration} is activated)
     * @param ending An optional trailing String
     * @param includeGlobalSubfolder Indicates if constructed name should include global subfolder for simulation instance (i.e. full file path relative to project directory)
     * @return
     */
    public String buildFileNameManually(String coreName, boolean includeRounds, String ending, boolean includeGlobalSubfolder){
        String str = outFilePrefix + "_" + coreName;
        if(considerRoundsInFilenameGeneration && includeRounds){
            str += buildFilenameRoundMidfix();
        }
        if(ending != null && !ending.isEmpty()){
            str += ending;
        }
        if(includeGlobalSubfolder){
            str = getGlobalSubfolderForOutput() + StatsWriter.getOsDependentDirectorySeparator() + str;
        }
        return str;
    }
    
    /**
     * Generates filenames for given image format (using constants specified in GraphsPrinter).
     * For remaining parameter description see method variant {@link #buildFileNameManually(String, boolean, String, boolean)}.
     * For text output please specify filename using {@link #buildFileNameManually(String, boolean, String, boolean)}.
     * This variants always considers the global subfolder prefix when generating the name.
     * Other method variants: {@link #buildFileNameManually(String, boolean, String, boolean)}, 
     * {@link #buildFileName(String, boolean, String, boolean)}
     * @param coreName Pure (i.e. without rounds etc.) filename without ending
     * @param includeRounds
     * @param imageFormat File format as specified in GraphsPrinter
     * @return
     */
    public String buildFileName(String coreName, boolean includeRounds, String imageFormat){
        return buildFileName(coreName, includeRounds, imageFormat, true);
    }
    
    /**
     * Generates filenames for given image format (using constants specified in GraphsPrinter).
     * For remaining parameter description see method variant {@link #buildFileNameManually(String, boolean, String, boolean)}.
     * For text output please specify filename using {@link #buildFileNameManually(String, boolean, String, boolean)}.
     * @param coreName Pure (i.e. without rounds etc.) filename without ending
     * @param includeRounds
     * @param imageFormat File format as specified in GraphsPrinter
     * @param includeGlobalSubfolder
     * @return
     */
    public String buildFileName(String coreName, boolean includeRounds, String imageFormat, boolean includeGlobalSubfolder){
        return buildFileNameManually(coreName, includeRounds, GraphsPrinter.getEndingForImageFormat(imageFormat), includeGlobalSubfolder);
    }
    
    /**
     * Sets the default image format for graph printing.
     * @param imageFormat Constant from GraphsPrinter
     */
    public void setDefaultImageFormat(String imageFormat){
        if(GraphsPrinter.imageFormatSupported(imageFormat)){
            GraphsPrinter.IMAGE_FORMAT_DEFAULT = imageFormat;
        } else {
            System.out.println(PREFIX + "The output image format '" + imageFormat + "' is not supported. Current setting: " + GraphsPrinter.IMAGE_FORMAT_DEFAULT);
        }
    }
    
    /**
     * Additional (external) frames registered for printing.
     */
    private ArrayList<JFrame> printRegisterFrames = new ArrayList<JFrame>();
    
    /**
     * Registers a given frame for printing. Does nothing if already registered.
     * @param frame
     */
    public void registerForPrinting(JFrame frame){
        if(frame == null){
            System.err.println(PREFIX + "Cannot register NULL frame for printing. Request ignored.");
            return;
        }
        if(!printRegisterFrames.contains(frame)){
            printRegisterFrames.add(frame);
        }
    }
    
    /**
     * Registers multiple frames for printing. Registers only frames that haven't 
     * been registered previously. 
     * @param frames ArrayList of frames
     */
    public void registerForPrinting(ArrayList<JFrame> frames){
        for(int i = 0; i < frames.size(); i++){
            registerForPrinting(frames.get(i));
        }
    }
    
    /**
     * Registers multiple frames for printing. Registers only frames that haven't 
     * been registered previously. 
     * @param frames Collection of frames
     */
    public void registerForPrinting(Collection<JFrame> frames){
        for(JFrame frame: frames){
            registerForPrinting(frame);
        }
    }
    
    /**
     * Deregisters a given frame from periodic printing.
     * @param frame
     */
    public void deregisterFromPrinting(JFrame frame){
        printRegisterFrames.remove(frame);
    }
    
    /**
     * Deregisters given frames from periodic printing.
     * @param frame
     */
    public void deregisterFromPrinting(ArrayList<JFrame> frames){
        for(int i = 0; i < frames.size(); i++){
            deregisterFromPrinting(frames.get(i));
        }
    }
    
    /**
     * Deregisters a given frames from periodic printing.
     * @param frame
     */
    public void deregisterFromPrinting(Collection<JFrame> frames){
        for(JFrame frame: frames){
            deregisterFromPrinting(frame);
        }
    }
    
    /**
     * Returns all frames registered for printing
     * @return
     */
    public ArrayList<JFrame> getFramesRegisteredForPrinting(){
        return printRegisterFrames;
    }
    
    /**
     * List of image formats to be printed. Add desired formats to list. 
     * If multiple formats are specified, images are printed in all 
     * specified formats each time {@link #printAllGraphs()} is called.
     */
    public ArrayList<String> printMultipleImageFormats = new ArrayList<>();
    
    /**
     * Prints a specified chart.
     * @param identifier Identifier for chart
     */
    public void printChart(final String identifier){
        printChart(identifier, null);
    }
    
    /**
     * Prints a specified chart. 
     * @param identifier Identifier for chart
     * @param filename Filename to be printed on chart
     */
    public void printChart(final String identifier, final String filename){
        if(chartDataMap.containsKey(identifier)){
            JFreeChart chart = chartDataMap.get(identifier).getChart().getChart();
            ChartPanel chartPanel = chartDataMap.get(identifier).getChart().getChartPanel();
            if(printMultipleImageFormats != null && !printMultipleImageFormats.isEmpty()){
                //print in each format if multiple specified
                for(int i = 0; i < printMultipleImageFormats.size(); i++) {
                    printChart(chart, 
                            new Float(chartPanel.getWidth() * printGraphWithScaleFactor).intValue(), 
                            new Float(chartPanel.getHeight() * printGraphWithScaleFactor).intValue(),
                            printMultipleImageFormats.get(i),
                            filename);
                }
            } else {
                //print chart for default format and apply scale factor
                printChart(chart, 
                        new Float(chartPanel.getWidth() * printGraphWithScaleFactor).intValue(), 
                        new Float(chartPanel.getHeight() * printGraphWithScaleFactor).intValue(),
                        GraphsPrinter.IMAGE_FORMAT_DEFAULT,
                        filename);
            }
        }
    }
    
    /**
     * Initiates printing of frame based on frame reference.
     * @param frame Frame instance reference
     */
    public void printFrame(final JFrame frame){
        printFrame(frame, null);
    }
    
    /**
     * Initiates printing of frame based on frame reference.
     * @param frame Frame instance reference
     * @param filename Filename to be printed on output
     */
    public void printFrame(final JFrame frame, final String filename){
        //check for embedded radarchart - and if so, print it via printChart() method
        if(RadarChart.class.isAssignableFrom(frame.getClass())){
            if(((RadarChart)frame).getChart() != null){
                if(printMultipleImageFormats != null && !printMultipleImageFormats.isEmpty()){
                    //print in each format if multiple specified
                    for(int i = 0; i < printMultipleImageFormats.size(); i++) {
                        printChart(((RadarChart)frame).getChart(), frame.getWidth(), frame.getHeight(), printMultipleImageFormats.get(i), filename);
                    }
                } else {
                    printChart(((RadarChart)frame).getChart(), frame.getWidth(), frame.getHeight(), GraphsPrinter.IMAGE_FORMAT_DEFAULT, filename);
                }
            }
        } else {
            if(printMultipleImageFormats != null && !printMultipleImageFormats.isEmpty()){
                //print in each format if multiple specified
                for(int i = 0; i < printMultipleImageFormats.size(); i++) {
                    printFrameActually(frame, false, printMultipleImageFormats.get(i), filename);
                }
            } else {
                //print all other frames as regular images
                printFrameActually(frame, false, GraphsPrinter.IMAGE_FORMAT_DEFAULT, filename);
            }
        }
    }
    
    /**
     * Prints all statistics charts including Jung graphs (e.g. Social Forces graph) 
     * using default image format (can be changed using {@link #setDefaultImageFormat(String)}).
     */
    public void printAllGraphs(){
        for(Entry<String, ChartDataSeriesMap> entry: chartDataMap.entrySet()){
            //print each chart
            printChart(entry.getKey());
        }
        if(sim.graphHandler != null && printJungGraphs){
            printJungGraphs();
        }
        //print additional registered frames or charts
        for(JFrame frame: printRegisterFrames){
            printFrame(frame);
        }
        //eventual simulation-specific graphs
        printGraphs();
    }
    
    /**
     * Checks filenames for saving files and modifies / and \\ 
     * characters to avoid the creation of subfolders.
     * @param filename
     * @return
     */
    private String checkFilenamesForSlashes(String filename){
        return filename.replace("/", "--").replace("\\", "--");
    }
    
    /**
     * Prints Chart using simulation initialization information as PNG 
     * image.
     * @param chart Chart to be printed
     * @param width Width of chart
     * @param height Height of chart
     * @param imageFormat Image format to be used as output. Defaults to {@link GraphsPrinter.IMAGE_FORMAT_DEFAULT}.
     */
    private void printChart(JFreeChart chart, int width, int height, String imageFormat, String givenFilename){
        if(printer != null){
            String filename = checkFilenamesForSlashes(
                    (givenFilename != null && !givenFilename.isEmpty() ? givenFilename : chart.getTitle().getText()));
            if(!printGraphAsPdfElseImg){
                printer.printChartToImage(chart, width, height, buildFileName(filename, true, imageFormat), imageFormat, null, printFileNamesOnCharts);
            } else {
                printer.printChartToPdf(chart, width, height, completePathAndOutFilePrefix + graphsSuffix 
                    + "_" + filename + buildFilenameRoundMidfix() + ".pdf");
            }
        }
    }
    
    /**
     * Prints JFrame as PNG image generating name based on simulation
     * initialization information and frame title (if no explicit filename specified).
     * @param frame Frame to be printed
     * @param useJRobot Use of JRobot for frame printing (or using Graphics2D.print() method)
     * @param imageFormat Image format to be printed to. If set to null, it defaults to {@link GraphsPrinter.IMAGE_FORMAT_DEFAULT}.
     * @param filename Core filename to be printed on output (may amended as part of method)
     */
    private void printFrameActually(final JFrame frame, final boolean useJRobot, String imageFormat, final String filename){
        if(printer != null && frame != null){
            String frameName = checkFilenamesForSlashes((filename != null && !filename.isEmpty() ? filename : frame.getTitle()));
            //check for image format and default if null
            if(imageFormat == null){
                imageFormat = GraphsPrinter.IMAGE_FORMAT_DEFAULT;
            }
            //disable double buffering
            //RepaintManager.currentManager(frame).setDoubleBufferingEnabled(false);
            if(useJRobot){
                printer.printFrameToImageUsingJRobot(frame, completePathAndOutFilePrefix 
                        + "_" + frameName + buildFilenameRoundMidfix() + GraphsPrinter.getEndingForImageFormat(imageFormat), imageFormat);
            } else {
                if(!printGraphAsPdfElseImg){
                    printer.printFrameToImage(frame, buildFileName(frameName + "_img", true, imageFormat), imageFormat);
                } else {
                    printer.printFrameToPDF(frame, completePathAndOutFilePrefix + graphsSuffix 
                        + "_" + frameName + buildFilenameRoundMidfix() + ".pdf");
                }
            }
            //re-enable double buffering
            //RepaintManager.currentManager(frame).setDoubleBufferingEnabled(true);
        } else if(frame == null){
            System.err.println(PREFIX + "Attempted to print NULL frame.");
        }
    }
    
    /** Indicates to use screen capture with JRobot instead of Image libraries when printing Jung graphs
     *  Advantage: works with GlassPane, thus prints what the user sees; Image libraries will lose (some) GlassPane contents.
     *  Disadvantage: requires frame to be in foreground which may sometimes not be achieved in a timely manner.
     */
    public boolean useJRobotForJungGraphPrinting = true;
    /** 
     * Indicates to use both screen capture with JRobot AS WELL AS Image libraries to print Jung graphs.
     * Backup mechanism to ensure that Graphs are saved :)
     */
    public boolean useJRobotAndImageForJungGraphPrinting = false;

    /**
     * Prints all Jung graphs (if they are not excluded from printing in GraphHandler).
     */
    public void printJungGraphs(){
        for(JFrame frame: sim.graphHandler.getPrintableFrames()){
            if(useJRobotAndImageForJungGraphPrinting || useJRobotForJungGraphPrinting){
                printFrameActually(frame, true, null, null);
            }
            if(useJRobotAndImageForJungGraphPrinting || !useJRobotForJungGraphPrinting){
                printFrameActually(frame, false, null, null);
            }
        }
    }
    
    /**
     * Method called when Print Graphs button is pressed. Developer can trigger further printing or other functionality.
     */
    public abstract void printGraphs();
    
    
    /** Round buffer-related stuff */
    
    StringBuffer roundBuffer = null;
    
    
    /**
     * Adds text to a buffer that is printed in the Statistics TextArea (Stats Form).
     * Prints a line break after the string. Alternative: {@link #appendToFile(String, boolean)}.
     * @param text text to be added (as String (slower!), see StringBuffer version of method)
     */
    public void addToRoundBuffer(String text){
        addToRoundBuffer(text, true);
    }
    
    /**
     * Adds text to a buffer that is printed in the Statistics TextArea (Stats Form).
     * @param text text to be added (as String (slower!), see StringBuffer version of method)
     * @param linebreak indicates if linebreak should be produced at the end
     */
    public void addToRoundBuffer(String text, boolean linebreak){
        roundBuffer.append(text);
        if(linebreak){
            roundBuffer.append(MTRuntime.LINE_DELIMITER);
        }
    }
    
    /**
     * Adds text to a buffer that is printed in the Statistics TextArea (Stats Form).
     * Prints a line break after the string. Alternative: {@link #addToRoundBuffer(StringBuffer, boolean)}.
     * @param text text to be added as StringBuffer (faster!)
     */
    public void addToRoundBuffer(StringBuffer text){
        addToRoundBuffer(text, true);
    }
    
    /**
     * Adds text to a buffer that is printed in the Statistics TextArea (Stats Form).
     * @param text text to be added as StringBuffer (faster!)
     * @param linebreak indicates if linebreak should be produced at the end
     */
    public void addToRoundBuffer(StringBuffer text, boolean linebreak){
        roundBuffer.append(text);
        if(linebreak){
            roundBuffer.append(MTRuntime.LINE_DELIMITER);
        }
    }
    
    /**
     * Key value separator used to generate text from key-value pair
     */
    private static final String keyValueSeparator = ": ";
    
    /**
     * Adds a key-value pair to the round buffer (which outputs it as text in the 
     * Statistics window), but optionally also adds it to the structuredValues map 
     * that is sent to listeners for further processing.
     * @param textKey Key of stats entry
     * @param textValue Value of entry (Object)
     * @param linebreak Indicates if a linebreak should be added at the end of this entry
     * @param addToStructuredStatsValue Indicates if the key value pair should be added to structured map delivered to listeners
     */
    public void addKeyValueToRoundBuffer(StringBuffer textKey, Object textValue, boolean linebreak, boolean addToStructuredStatsValue){
        roundBuffer.append(textKey).append(keyValueSeparator).append(textValue);
        if(linebreak){
            roundBuffer.append(MTRuntime.LINE_DELIMITER);
        }
        if(addToStructuredStatsValue){
            addToStatsMap(textKey.toString(), textValue);
        }
    }
    
    /**
     * Adds a key-value pair to the round buffer (which outputs it as text in the 
     * Statistics window), but optionally also adds it to the structuredValues map 
     * that is sent to listeners for further processing.
     * @param textKey Key of stats entry
     * @param textValue Value of entry (Object)
     * @param linebreak Indicates if a linebreak should be added at the end of this entry
     * @param addToStructuredStatsValue Indicates if the key value pair should be added to structured map delivered to listeners
     */
    public void addKeyValueToRoundBuffer(String textKey, Object textValue, boolean linebreak, boolean addToStructuredStatsValue){
        roundBuffer.append(textKey).append(keyValueSeparator).append(textValue);
        if(linebreak){
            roundBuffer.append(MTRuntime.LINE_DELIMITER);
        }
        if(addToStructuredStatsValue){
            addToStatsMap(textKey.toString(), textValue);
        }
    }
    
    /** Structured values (map delivered to listeners) */
    
    private LinkedHashMap<String, Object> structuredValues = new LinkedHashMap<String, Object>();
    
    public LinkedHashMap<String,Object> getStatisticsMap(){
        return structuredValues;
    }
    
    /**
     * Adds an entry to the stats map which is delivered to stats listeners.
     * @param key Key for stats entry
     * @param value Value of stats entry
     */
    public void addToStatsMap(String key, Object value){
        structuredValues.put(key, value);
    }
    
    private ArrayList<StatisticsListener> listeners = new ArrayList<StatisticsListener>();
    
    /**
     * Registers a StatisticsListener that will be invoked during 
     * each Statistics step to receive the current stats.
     * @param listener Listener to be registered
     */
    public void registerStatsListener(StatisticsListener listener){
        if(!listeners.contains(listener)){
            listeners.add(listener);
        }
    }
    
    /**
     * Unregisters a given StatisticsListener. Counterpart to 
     * registerStatsListener()
     * @param listener Listener to be unregistered
     */
    public void unregisterStatsListener(StatisticsListener listener){
        listeners.remove(listener);
    }
    
    /**
     * Delivers current statistics information to all listeners.
     */
    protected void deliverToListeners(){
        for(StatisticsListener listener: listeners){
            listener.updateStats(roundBuffer, structuredValues, useMapInListeners);
        }
    }
    
    /** File saving-related stuff */
    
    /** File IO-related stuff */
    
    /**
     * Sets the global folder for simulation-related output 
     * relative to the user directory (typically project path). 
     * Optionally allows to prefix the folder name with the 
     * time the simulation is initialized as well as class name. 
     * If subfolder path contains subfolders itself (e.g. "results/subfolder"),
     * it only affects the last folder element, i.e. the leaf 
     * of the path (for the previous example "subfolder").
     * Optionally, a short folder name can be used for storing 
     * actual results to reduce file path length (see parameter 
     * description).
     * @param subfolder Subfolder in user directory
     * @param prefixFolderWithTime If true, prefixes path name with 
     *         time of simulation instantiation.
     *         Format: "YYYYMMDD_HHMMSS_"
     * @param prefixFolderWithSimClass If true, prefixes subfolder 
     *         with short version of simulation class name (after time 
     *         prefix if activated using {@link #prefixSubfolderWithTime}).
     *         Format: "simClassName_"
     * @param createShortFolderNameAndSeparateTagFolderInstead Creates 
     *         separate tag folder with full path name but saves result 
     *         in folder that has a short version of the original folder 
     *         name only containing the same timestamp. Purpose of this 
     *         approach is to allow longer filenames without overriding
     *         OS or application filename length thresholds (e.g. files 
     *         cannot be opened unless moved to location with shorter 
     *         path name).
     */
    public static void setGlobalSubfolderForOutput(String subfolder, boolean prefixFolderWithTime, boolean prefixFolderWithSimClass, boolean shortFolderNameAndSeparateTagFolderInstead){
        Statistics.subfolder = StatsWriter.ensureOsCompatibleDirectorySeparator(subfolder);
        prefixSubfolderWithTime = prefixFolderWithTime;
        prefixSubfolderWithSimulationClassName = prefixFolderWithSimClass;
        createShortFolderNameAndSeparateTagFolderInstead = shortFolderNameAndSeparateTagFolderInstead;
        StatsWriter.setGlobalSubfolderName(Statistics.subfolder);
    }
    
    /**
     * Sets the global folder for simulation-related output 
     * relative to the user directory (typically project path). 
     * Optionally allows to prefix the folder name with the 
     * time the simulation is initialized as well as class name. 
     * If subfolder path contains subfolders itself (e.g. "results/subfolder"), 
     * it only affects the last folder element, i.e. the leaf 
     * of the path (for the previous example "subfolder").
     * @param subfolder Subfolder in user directory
     * @param prefixFolderWithTime If true, prefixes path name with 
     *         time of simulation instantiation.
     *         Format: "YYYYMMDD_HHMMSS_"
     * @param prefixFolderWithSimClass If true, prefixes subfolder 
     *         with short version of simulation class name (after time 
     *         prefix if activated using {@link #prefixSubfolderWithTime}).
     *         Format: "simClassName_"
     */
    public static void setGlobalSubfolderForOutput(String subfolder, boolean prefixFolderWithTime, boolean prefixFolderWithSimClass){
        setGlobalSubfolderForOutput(subfolder, prefixFolderWithTime, prefixFolderWithSimClass, false);
    }
    
    /**
     * Sets the global folder for simulation-related output 
     * relative to the user directory (typically project path). 
     * Optionally allows to prefix the folder name with the 
     * time the simulation is initialized. If subfolder path 
     * contains subfolders itself (e.g. "results/subfolder"), 
     * it only affects the last folder element, i.e. the leaf 
     * of the path (for the previous example "subfolder").
     * @param subfolder Subfolder in user directory
     * @param prefixFolderWithTime If true, prefixes path name with 
     *         time of simulation instantiation
     *         Format: "YYYYMMDD_HHMMSS_"
     */
    public static void setGlobalSubfolderForOutput(String subfolder, boolean prefixFolderWithTime){
        setGlobalSubfolderForOutput(subfolder, prefixFolderWithTime, false, false);
    }
    
    /**
     * Sets the global subfolder for simulation output relative to 
     * user directory (generally simulation project path).
     * @param subfolder
     */
    public static void setGlobalSubfolderForOutput(String subfolder){
        setGlobalSubfolderForOutput(subfolder, false);
    }
    
    /**
     * Returns global subfolder relative to project path.
     * @return
     */
    public static String getGlobalSubfolderForOutput(){
        return Statistics.subfolder;
    }
    
    /**
     * Returns a prepared file name that includes simulation filename and starting time as part of the filename.
     * When calling, ensure that Statistics have been initialized, else String is empty.
     * @return
     */
    public static String prepareOutFilePrefix(){
        if(outFilePrefix.isEmpty()){
            throw new RuntimeException("Ensure that Statistics is initialized before calling prepareOutFilePrefix!");
        }
        return outFilePrefix;
    }
    
    /**
     * Prepares complete file prefix including subfolder path, time and simulation class name used for 
     * any produced outfile (including figures).
     */
    private static void prepareCompletePathAndOutfilePrefix(){
        if(completePathAndOutFilePrefix.isEmpty() || !completePathAndOutFilePrefix.contains(outFilePrefix)){
            System.out.println("Outfile prefix: " + outFilePrefix);
            completePathAndOutFilePrefix = subfolder + StatsWriter.getOsDependentDirectorySeparator() + outFilePrefix;
            System.out.println("Generated complete simulation path and file prefix: " + completePathAndOutFilePrefix);
        }
    }
    
    /**
     * Subfolder used for simulation instance
     */
    private static String subfolder = "";
    /**
     * Default subfolder name if no other subfolder specified.
     */
    private static String defaultSubFolder = "results/";
    /**
     * Simulation instance-specific outfile prefix (Simulation class name, date and time). Does not include path information (see {@link #completePathAndOutFilePrefix})
     */
    protected static String outFilePrefix = "";
    /**
     * Contains the complete path (global subfolder) and simulation-specific filename prefix (date + time + simulation class name)
     */
    private static String completePathAndOutFilePrefix = "";
    /**
     * Text file ending
     */
    public static final String ending = ".txt";
    /**
     * Indicates if subfolder should be prefixed with time
     */
    private static boolean prefixSubfolderWithTime = false;
    /**
     * Indicates if subfolder should be prefixed with simulation classname 
     * (following prefixing with time).
     */
    private static boolean prefixSubfolderWithSimulationClassName = false;
    /**
     * Indicates if the specified folder name is only used for a 'tag folder' which does not contain anything
     * but has same creation time. This way, file path size is reduced for the actual files, making it less 
     * likely for them (i.e. path + filename) to overshoot 255 characters.
     */
    private static boolean createShortFolderNameAndSeparateTagFolderInstead = false;
    
    /** specific to stats printing */
    
    StatsDataWriter statsWriter = null;
    StringBuffer filePreviousLineHeader = new StringBuffer();
    StringBuffer fileCurrentLineHeader = new StringBuffer();
    StringBuffer fileCurrentLine = new StringBuffer();
    StringBuffer fileOutputBuffer = new StringBuffer();
    public static final String CSV_DELIMITER = "|";
    private int writeToDataFileEveryNoOfLines = 10;
    private int collectedLinesSinceLastWriteToDataFile = 0;
    
    /** Suffix for parameter file */
    public static final String parameterFileSuffix = "_params";
    /** Suffix for data file suffix */
    public static final String dataFileSuffix = "_data";
    /** Suffix for stats window saving */
    public static final String statsFileSuffix = "_stats";
    /** Suffix for graphs */
    public static final String graphsSuffix = "_graphs";
    
    /**
     * Filename for storing CSV headers generated during runtime - set during Stats initialisation
     */
    public static String CSV_HEADER_OUTFILE_NAME = "csv.headers file name";
    
    /**
     * Indicates if output files should be overwritten if number of entries (i.e. CSV columns) change.
     * If set to true, all previous stats data output is deleted and the file is written with a 
     * new header.
     */
    public boolean overwriteOutputFileUponHeaderChange = false;
    
    /**
     * If set to true, stats data file is closed after every write access and thus 
     * reopened for every subsequent writing (performance penalty).
     */
    public boolean closeFileAfterEveryStatsDataWrite = false;
    
    /**
     * HashSet holding entered headers in order of insertion. Is maintained 
     * throughout simulation runtime. Outfile header can thus only expand, 
     * not retract and never change the order of the elements (when iterating).
     */
    private LinkedHashSet<String> outFileHeaders = new LinkedHashSet<>(); 
    
    /**
     * Indicates whether CSV file headers are saved in a file. 
     * If activated, contents are updated once headers change.
     */
    private boolean saveCsvFileHeadersUponHeaderChange = false;
    
    /**
     * Indicates whether CSV file headers are read upon initialisation of 
     * Statistics module to ensure consistent schema from the beginning.
     */
    private boolean readCsvFileHeadersUponInitialisation = true;
    
    /**
     * Indicates whether CSV file headers are sorted during initialisation.
     */
    private boolean sortCsvFileHeadersUponReading = false;
    
    /**
     * Indicates whether sorted CSV file headers are written back to index file.
     */
    private boolean writeSortedCsvFileHeadersToFile = false;
    
    /**
     * Activates writing of CSV file headers to outfile as they are generated
     * by statistics output, so it can be read by subsequent runs and 
     * consistently populate CSV outfile headers.
     * Activates overwriting of changed output file to ensure the 
     * updated CSV headers are saved to disk.
     * @param activate
     */
    public void setWriteCsvFileHeadersToFile(boolean activate) {
        this.saveCsvFileHeadersUponHeaderChange = activate;
        this.overwriteOutputFileUponHeaderChange = activate;
    }
    
    /**
     * Indicates whether the CSV header file is read upon initialisation and 
     * used as a schema for produced output. Deactivates the overwriting of 
     * changed CSV schemas. Note: If {@link #setWriteCsvFileHeadersToFile(boolean)} 
     * is activated afterwards, overwriting of changed CSV schemas remains activated.
     * If desired, the entries can be sorted alphabetically during initialisation.
     * The sorted entries can further be written back to the file.
     * @param activate
     * @param sortEntriesDuringReading
     * @param writeSortedHeadersBackToFile
     */
    public void setReadCsvFileHeadersFromFile(boolean activate, boolean sortEntriesDuringReading, boolean writeSortedHeadersBackToFile) {
        this.readCsvFileHeadersUponInitialisation = activate;
        this.sortCsvFileHeadersUponReading = sortEntriesDuringReading;
        this.writeSortedCsvFileHeadersToFile = writeSortedHeadersBackToFile;
        this.overwriteOutputFileUponHeaderChange = !activate;
    }
    
    
    /**
     * Holds the values for the respective header fields in a given round. 
     * Is reset after printing new line.
     */
    private HashMap<String, String> outFileValuesForCurrentRound = new HashMap<>();
    
    /**
     * If set to true, the system caches entered stats data headers and only 
     * expands those, but never omits field (even no values are entered). 
     * This way, the stats data headers can accommodate often-changing structure 
     * of stats data (e.g. omitted values) while maintaining a CSV structure.
     */
    private boolean generateAndMaintainOutfileDataStructureIncrementally = true;
    
    /**
     * Default value used for empty stats data field (i.e. existing header but no value).
     * If set to null, it is left empty.
     */
    public String defaultValueForEmptyStatsDataValue = null;
    
    /**
     * Appends header and values to CSV outfile using {@link #CSV_DELIMITER} 
     * for value separation as well as header line. 
     * Allows appending a line break (using newLine). Alternatively, only
     * a line break can be appended, in which case all other fields are ignored.
     * See simplified method versions below.
     * @param headerField description (header) for the value
     * @param content actual value
     * @param newline indicates if a line break should be produced after the entry
     * @param onlyLineBreak Indicates if only line break should be added - ignoring all other parameters
     */
    public void appendToFile(String headerField, String content, boolean newline, boolean onlyLineBreak){
        if(statsWriter != null && collectDataThisRound){
            if(content != null && !onlyLineBreak){
                if(generateAndMaintainOutfileDataStructureIncrementally){
                    //caching approach - add headers if not used yet
                    if(!outFileHeaders.contains(headerField)){
                        outFileHeaders.add(headerField);
                        // Write current CSV headers to outfile if activated - for later reuse
                        if (saveCsvFileHeadersUponHeaderChange) {
                            writeCsvHeadersToFile();
                        }
                    }
                    //save content for this round in map
                    outFileValuesForCurrentRound.put(headerField, content);
                } else {
                    //ad hoc structure, relying on fixed order and same number of headers (faster)
                    if(headerField != null){
                        fileCurrentLineHeader.append(headerField).append(CSV_DELIMITER);
                    }
                    fileCurrentLine.append(content).append(CSV_DELIMITER);
                }
            }
            if(newline || onlyLineBreak){
                //combine maps to StringBuffer lines for further processing
                if(generateAndMaintainOutfileDataStructureIncrementally){
                    //go through all entries for this line add them to outfile in one shot
                    for(String key: outFileHeaders){
                        fileCurrentLineHeader.append(key).append(CSV_DELIMITER);
                        if(outFileValuesForCurrentRound.containsKey(key)){
                            fileCurrentLine.append(outFileValuesForCurrentRound.get(key));
                        } else {
                            if(defaultValueForEmptyStatsDataValue != null){
                                fileCurrentLine.append(defaultValueForEmptyStatsDataValue);
                            }
                        }
                        fileCurrentLine.append(CSV_DELIMITER);
                    }
                }
                //conventional operations
                
                //finishing row
                fileCurrentLineHeader.append(LINEBREAK);
                fileCurrentLine.append(LINEBREAK);
                //check if column headers have changed (e.g. output value added first time)
                if(!fileCurrentLineHeader.toString().equals(filePreviousLineHeader.toString())){
                    //delete original output file if activated and also clear buffer
                    if(filePreviousLineHeader.length() > 0 && overwriteOutputFileUponHeaderChange){
                        if(statsWriter != null){
                            if(statsWriter.deleteFile()){
                                resetOutputBuffer();
                                System.out.println(PREFIX + "Reset stats file as of changed output headers.");
                            } else {
                                System.err.println(PREFIX + "Deleting stats file as of changed output headers failed!");
                            }
                        } else {
                            System.err.println(PREFIX + "StatsWriter is null. Should not be the case when collecting data to write.");
                        }
                    }
                    //if so, rewrite entire column header of outfile
                    fileOutputBuffer.append(fileCurrentLineHeader);
                    //remember previous line for later comparison
                    filePreviousLineHeader = fileCurrentLineHeader;
                 
                }
                fileOutputBuffer.append(fileCurrentLine);
                //reset structures for next line
                fileCurrentLine = new StringBuffer();
                fileCurrentLineHeader = new StringBuffer();
                if(generateAndMaintainOutfileDataStructureIncrementally){
                    //delete all entries for this round's value HashMap (if used)
                    outFileValuesForCurrentRound.clear();
                }
                //write to outfile if buffer threshold is reached
                if(collectedLinesSinceLastWriteToDataFile == writeToDataFileEveryNoOfLines){
                    if(closeFileAfterEveryStatsDataWrite){
                        //close file
                        writeDataToDiskAndCloseFile();
                    } else {
                        //don't close file
                        writeBufferedDataToDisk();
                    }
                } else {
                    //System.out.println("Collected: " + collectedLinesSinceLastWriteToDataFile);
                    collectedLinesSinceLastWriteToDataFile++;
                }
            }
        }
    }
    
    /**
     * Writes current CSV file headers to file for later reuse.
     */
    private void writeCsvHeadersToFile(){
        // Write changed stats headers to file
        FileWriter writer;
        try {
            writer = new FileWriter(this.CSV_HEADER_OUTFILE_NAME);
            boolean firstWrite = true;
            for (String item : outFileHeaders) {
                if (firstWrite) {
                    writer.write(item);
                    firstWrite = false;
                } else {
                    writer.append(item);
                }
                writer.append(LINEBREAK);
            }
            writer.close();
            if (showMessageWhenWritingData) {
                System.out.println(PREFIX + "Writing latest CSV headers to file '" + this.CSV_HEADER_OUTFILE_NAME + "'.");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    

    /**
     * Number format instance for float, double and long output in non-scientific notation (i.e., without e notation)
     */
    private static final NumberFormat numberFormat = NumberFormat.getInstance();
    
    static {
        numberFormat.setMaximumFractionDigits(DEFAULT_FLOAT_DECIMAL_PLACES);
        numberFormat.setGroupingUsed(false);
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)})
     * but neither adds a new header nor a line break at the end of the provided content.
     * @param content
     */
    protected void appendToFile(Float content){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, numberFormat.format(content), false, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)})
     * but neither adds a new header nor a line break at the end of the provided content.
     * @param content
     */
    protected void appendToFile(Double content){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, numberFormat.format(content), false, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)})
     * but neither adds a new header nor a line break at the end of the provided content.
     * @param content
     */
    protected void appendToFile(Long content){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, numberFormat.format(content), false, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)})
     * but neither adds a new header nor a line break at the end of the provided content.
     * @param content
     */
    protected void appendToFile(BigDecimal content){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, content.toString(), false, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)})
     * but neither adds a new header nor a line break at the end of the provided content.
     * @param content
     */
    protected void appendToFile(Number content){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, content.toString(), false, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}) 
     * but neither adds a new header nor a line break at the end of the 
     * provided content.
     * @param content
     */
    protected void appendToFile(String content){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, content, false, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Does not add header but allows to add content and specification of 
     * new line. For producing a line break, simply set content to null and 
     * newLine to true.
     * @param content
     * @param newLine
     */
    protected void appendToFile(Float content, boolean newLine){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, numberFormat.format(content), newLine, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Does not add header but allows to add content and specification of 
     * new line. For producing a line break, simply set content to null and 
     * newLine to true.
     * @param content
     * @param newLine
     */
    protected void appendToFile(Double content, boolean newLine){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, numberFormat.format(content), newLine, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Does not add header but allows to add content and specification of 
     * new line. For producing a line break, simply set content to null and 
     * newLine to true.
     * @param content
     * @param newLine
     */
    protected void appendToFile(Long content, boolean newLine){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, numberFormat.format(content), newLine, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Does not add header but allows to add content and specification of 
     * new line. For producing a line break, simply set content to null and 
     * newLine to true.
     * @param content
     * @param newLine
     */
    protected void appendToFile(BigDecimal content, boolean newLine){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, content.toString(), newLine, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Does not add header but allows to add content and specification of 
     * new line. For producing a line break, simply set content to null and 
     * newLine to true.
     * @param content
     * @param newLine
     */
    protected void appendToFile(Number content, boolean newLine){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(null, content.toString(), newLine, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Does not add header but allows to add content and specification of 
     * new line. For producing a line break, simply set content to null and 
     * newLine to true.
     * @param content
     * @param newLine
     */
    protected void appendToFile(String content, boolean newLine){
        appendToFile(null, content, newLine, false);
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Appends header and values to outfile using CSV separation 
     * with header line.
     * @param headerField Header field
     * @param content Value
     * @param newLine Indicates whether new line character is added after entry
     */
    protected void appendToFile(String headerField, Double content, boolean newLine){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(headerField, numberFormat.format(content), newLine, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Appends header and values to outfile using CSV separation 
     * with header line.
     * @param headerField Header field
     * @param content Value
     * @param newLine Indicates whether new line character is added after entry
     */
    protected void appendToFile(String headerField, Float content, boolean newLine){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(headerField, numberFormat.format(content), newLine, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Appends header and values to outfile using CSV separation 
     * with header line.
     * @param headerField Header field
     * @param content Value
     * @param newLine Indicates whether new line character is added after entry
     */
    protected void appendToFile(String headerField, Long content, boolean newLine){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(headerField, numberFormat.format(content), newLine, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Appends header and values to outfile using CSV separation 
     * with header line.
     * @param headerField Header field
     * @param content Value
     * @param newLine Indicates whether new line character is added after entry
     */
    protected void appendToFile(String headerField, BigDecimal content, boolean newLine){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(headerField, content.toString(), newLine, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Appends header and values to outfile using CSV separation 
     * with header line.
     * @param headerField Header field
     * @param content Value
     * @param newLine Indicates whether new line character is added after entry
     */
    protected void appendToFile(String headerField, Number content, boolean newLine){
    	//if (content != null) { // Do check to prevent NPE
    		appendToFile(headerField, content.toString(), newLine, false);
    	//}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Appends header and values to outfile using CSV separation 
     * with header line. Does NOT add a line break at the end.
     * Suppresses appending if content is null.
     * @param headerField
     * @param content
     */
    protected void appendToFile(String headerField, Float content){
        //if (content != null) { // Do check to prevent NPE
        	appendToFile(headerField, content, false);
        //}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Appends header and values to outfile using CSV separation 
     * with header line. Does NOT add a line break at the end.
     * Suppresses appending if content is null.
     * @param headerField
     * @param content
     */
    protected void appendToFile(String headerField, Double content){
        //if (content != null) { // Do check to prevent NPE
        	appendToFile(headerField, content, false);
        //}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Appends header and values to outfile using CSV separation 
     * with header line. Does NOT add a line break at the end.
     * Suppresses appending if content is null.
     * @param headerField
     * @param content
     */
    protected void appendToFile(String headerField, Long content){
        //if (content != null) { // Do check to prevent NPE
        	appendToFile(headerField, content, false);
        //}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Appends header and values to outfile using CSV separation 
     * with header line. Does NOT add a line break at the end.
     * Suppresses appending if content is null.
     * @param headerField
     * @param content
     */
    protected void appendToFile(String headerField, Number content){
        //if (content != null) { // Do check to prevent NPE
        	appendToFile(headerField, content, false);
        //}
    }
    
    /**
     * Writes to CSV file (see {@link #appendToFile(String, String, boolean)}).
     * Appends header and values to outfile using CSV separation 
     * with header line. Does NOT add a line break at the end.
     * @param headerField
     * @param content
     */
    protected void appendToFile(String headerField, String content){
        appendToFile(headerField, content, false, false);
    }
    
    /**
     * Sets the number of lines buffered before those are written to the 
     * statistics output CSV file (Performance advantage when producing a lot of 
     * output).
     * Current default: 10
     * @param numberOfLines
     */
    public void setNoOfLinesBufferedBeforeWritingToFile(int numberOfLines){
        System.out.println(PREFIX + "Output file line buffer size changed from " 
                + this.writeToDataFileEveryNoOfLines + " to " + numberOfLines);
        this.writeToDataFileEveryNoOfLines = numberOfLines;
    }
    
    /**
     * Returns the current file writing buffer size (in lines).
     * @return
     */
    public Integer getNoOfLinesBufferedBeforeWritingToFile(){
        return writeToDataFileEveryNoOfLines;
    }
    
    /** indicates of round information is automatically added each round */
    private boolean prependRound = true;
    
    /**
     * Indicates if round information is automatically added to CSV file 
     * at the beginning of each round.
     * Default: true
     * @param append boolean indicating if round information is prepended.
     */
    public void prependRoundInformationInDataFile(boolean append){
        prependRound = append;
    }
    
    /**
     * Indicates if round information is prepended when printing to data files.
     * @return
     */
    public boolean getRoundInformationPrependedInDataFile(){
        return prependRound;
    }
    
    /**
     * Controls whether the system will produce a console message when 
     * writing data to files.
     */
    public boolean showMessageWhenWritingData = false;
    
    /**
     * Resets output buffer.
     */
    private void resetOutputBuffer(){
        fileOutputBuffer = new StringBuffer();
        collectedLinesSinceLastWriteToDataFile = 0;
    }
    
    /**
     * Writes buffered stats data to disk (WITHOUT CLOSING the file), 
     * clearing the buffer that maintains data for writeEveryNoOfLines rounds
     * before writing (for performance). SHOULD be called before closing the file.
     */
    private void writeBufferedDataToDisk(){
        if(statsWriter != null){
            if(fileOutputBuffer.length() > 0){
                //write to file
                if(showMessageWhenWritingData){
                    System.out.println(PREFIX + "Writing collected data to CSV file '" + statsWriter.getFilename() + "'.");
                }
                statsWriter.write(fileOutputBuffer);
                //reset buffer
                resetOutputBuffer();
            }
        }
    }
    
    /**
     * Writes directly to file without applying any CSV-related formatting 
     * or buffering it. Will write eventual buffered data (configured via 
     * setNoOfLinesAsBuffer()) to file before adding its own content. 
     * Does NOT close the file. Should only be used for output that is 
     * relevant for interpreting the file correctly with other tools. 
     * Actual simulation data should be written using a variant of
     * {@link #appendToFile(String, String, boolean)}.
     * @param content Content to be written
     */
    protected void writeDirectlyToFile(String content){
        if(statsWriter != null){
            writeBufferedDataToDisk();
            statsWriter.write(content);
        }
    }
    
    /**
     * Writes all pending buffered data to disk and closes the file.
     * Activates file appending, so further content can be appended 
     * later by writing to the file again.
     */
    private void writeDataToDiskAndCloseFile(){
        if(statsWriter != null){
            writeBufferedDataToDisk();
            statsWriter.close();
            statsWriter.allowAppendingToFile(true);
        }
    }
    
    /**
     * Method to be called when shutting down simulation to reset stats. 
     * Writes buffered data to disk and closes file, clears graphs.
     */
    public void resetStats(){
        if(statsWriter != null){
            writeDataToDiskAndCloseFile();
        }
        for(Entry<String, ChartDataSeriesMap> entry: chartDataMap.entrySet()){
            if(manageWindowsUsingPositionSaver){
                PositionSaver.unregisterFrame(entry.getValue().getJFrame());
            }
            entry.getValue().clearResources();
        }
        chartDataMap.clear();
        resetDataSeries();
        chartStrokeCache.clear();
        resetAllRadarCharts();
        //kill additionally registered frames
        for(JFrame frame: printRegisterFrames){
            if(frame != null){
                frame.dispose();
            }
        }
        printRegisterFrames.clear();
        System.out.println(PREFIX + "Statistics cleared.");
        if(graphController != null){
            if(manageWindowsUsingPositionSaver){
                PositionSaver.unregisterFrame(graphController);
            }
            graphController.dispose();
            graphController = null;
        }
        destroyStatsForm();
        statsCalculator.shutdownListeners();
        //Stop SpaceChecker - can be called even if not started.
        if(spaceChecker != null) {
            spaceChecker.stop();
        }
        //reset specializations
        reset();
        if(graphController != null){
            graphController.dispose();
        }
    }

    
    /** Utility functions */
    
    /**
     * Indicates if a given Double value is between specified boundaries.
     * @param lowerBoundary lower boundary
     * @param upperBoundary upper boundary
     * @param inclusive indicates if boundaries are inclusive
     * @param valueToTest value to test against
     * @return
     */
    public static boolean between(Double lowerBoundary, Double upperBoundary, boolean inclusive, Double valueToTest){
        if(inclusive){
            if(valueToTest >= lowerBoundary && valueToTest <= upperBoundary){
                return true;
            } else {
                return false;
            }
        } else {
            if(valueToTest > lowerBoundary && valueToTest < upperBoundary){
                return true;
            } else {
                return false;
            }
        }
    }
    
    /**
     * Indicates if a given Float value is between specified boundaries.
     * @param lowerBoundary lower boundary
     * @param upperBoundary upper boundary
     * @param inclusive indicates if boundaries are inclusive
     * @param valueToTest value to test against
     * @return
     */
    public static boolean between(Float lowerBoundary, Float upperBoundary, boolean inclusive, Float valueToTest){
        if(inclusive){
            if(valueToTest >= lowerBoundary && valueToTest <= upperBoundary){
                return true;
            } else {
                return false;
            }
        } else {
            if(valueToTest > lowerBoundary && valueToTest < upperBoundary){
                return true;
            } else {
                return false;
            }
        }
    }
    
    /**
     * Indicates if a given Integer value is between specified boundaries.
     * @param lowerBoundary lower boundary
     * @param upperBoundary upper boundary
     * @param inclusive indicates if boundaries are inclusive
     * @param valueToTest value to test against
     * @return
     */
    public static boolean between(Integer lowerBoundary, Integer upperBoundary, boolean inclusive, Integer valueToTest){
        return between(new Double(lowerBoundary), new Double(upperBoundary), inclusive, new Double(valueToTest));
    }
    
}
