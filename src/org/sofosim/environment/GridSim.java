package org.sofosim.environment;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.frogberry.windowPositionSaver.PositionSaver;
import org.nzdis.micro.MTConnector;
import org.nzdis.micro.random.MersenneTwister;
import org.nzdis.micro.util.StackTracePrinter;
import org.sofosim.clustering.VertexPoint3D;
import org.sofosim.environment.annotations.SimulationParam;
import org.sofosim.environment.stats.Statistics;
import org.sofosim.forceLayout.IndivWeightProvider;
import org.sofosim.graph.GraphHandler;
import org.sofosim.nadico.CommunicationSpace;
import sim.display.Console;
import sim.engine.SimState;
import sim.field.continuous.Continuous3D;
import sim.field.network.Network;

public abstract class GridSim extends SimState {

	/**
	 * Seed used for initialisation. Only maintained for automated parameter documentation.
	 */
	@SimulationParam
	private long seed;
	
	/**
	 * Random Number Generator overriding unsynchronized version from Mason.
	 */
	private MersenneTwister random = null;
	/**
	 * Counter for access to RNG
	 */
	private int randomCallCounter = 0;
	
	public MersenneTwister random(){
		if(debugCallsToRandom){
			System.out.println("Simulation environment: Requested RNG reference for " + randomCallCounter + " times. Caller: " + StackTracePrinter.getCaller());
			randomCallCounter++;
		}
		return random;
	}
	
	/**
	 * X dimension
	 */
	@SimulationParam
	public final Integer GRID_WIDTH;
	/**
	 * Y dimension
	 */
	@SimulationParam
	public final Integer GRID_HEIGHT;
	/**
	 * Z dimension
	 */
	@SimulationParam
	public final Integer GRID_DEPTH;
	/**
	 * Indicator if 3D visualization (and coordinate system) is used
	 */
	@SimulationParam
	public final boolean USE_3D;
	/**
	 * Indicator if grid is toroidal
	 */
	@SimulationParam
	public final Boolean TOROIDAL;
	private Statistics stats = null;
	/**
	 * Switch that configures showing of statistics graphs
	 */
	@SimulationParam
	public Boolean SHOW_STATS_GRAPHS = true;
	/**
	 * Switch to de/activate printing of statistics (text output) to file.
	 */
	@SimulationParam
	public Boolean SAVE_STATS_TO_FILE = true;
	
	/**
	 * If activated, Direct 3D support is deactivated if running Java 8 to avoid blocking UI 
	 * when using GraphsPrinter. Default: false;
	 * Related issue: http://www.javaprogrammingforums.com/awt-java-swing/29583-gui-freeze-d3d-screen-updater-thread-blocked.html 
	 */
	@SimulationParam
	public static boolean deactivateD3DForJava8 = false;
	
	/**
	 * Indicates if simulation is running on Java 8.
	 * @return
	 */
	public static boolean runningJava8(){
		return System.getProperty("java.version").startsWith("1.8.");
	}
	
	/**
	 * Deactivates D3D support.
	 */
	private static void deactivateD3D(){
		System.setProperty("sun.java2d.d3d", "false");
	}
	
	/**
	 * Indicates if stdout and stderr should be redirected. 
	 * It does not imply that it is already activated.
	 * @return
	 */
	public boolean redirectStdOutAndStdErrToBeActivated(){
		return stats.redirectStdOutAndStdErr();
	}
	
	/**
	 * Activates redirection of stdout and stderr output 
	 * into outfile (see {@link #setStdOutErrOutfile(String)}).
	 * @param redirect Indicates whether stdout and stderr should be redirected
	 * @param alsoPrintToConsole Indicates if output should be printed to console in 
	 * 		addition to outfile
	 */
	public void redirectStdOutAndStdErr(boolean redirect, boolean alsoPrintToConsole){
		stats.redirectStdOutAndStdErr(redirect, alsoPrintToConsole);
		if(stats.initialized()){
			if(redirect && redirectStdOutAndStdErrToBeActivated()){
				stats.redirectStdOutAndStdErrToFile(alsoPrintToConsole);
			}
		}
	}
	
	/**
	 * Filename for redirected stdout/stderr output
	 */
	@SimulationParam
	private String stdOutErrOutfile = "ConsoleOutput.txt";
	
	/**
	 * Specifies filename used for stdout/stderr redirection 
	 * (see {@link #redirectStdOutAndStdErr(boolean)}).
	 * @param filename Filename for outfile
	 */
	public void setStdOutErrOutfile(String filename){
		this.stdOutErrOutfile = filename;
		if(!stats.redirectionAlreadyActivated()){
			stats.redirectStdOutAndStdErrToFile();
		}
	}
	
	/**
	 * Returns filename for redirected stdout/stderr output.
	 * @return
	 */
	public String getStdOutStdErrOutfilename(){
		return this.stdOutErrOutfile;
	}
	
	/**
	 * Indicates whether calls to random number generator should be logged to console.
	 */
	public boolean debugCallsToRandom = false;
	
	/**
	 * Indicates if Mason scheduler is used for forces graph (or JUNG-builtin threading).
	 */
	@SimulationParam
	private Boolean useMasonSchedulerForForcesGraph = true;
	
	public Console console = null;
	private boolean forceGraphUiInSingleFrame = true;
	
	/**
	 * Reference to GraphHandler
	 */
	@SimulationParam
	public GraphHandler<VertexPoint3D<String>> graphHandler;
	
	/**
	 * Wait time (in milliseconds) in between when printing graphs
	 */
	@SimulationParam
	public static final Integer WAIT_BETWEEN_PRINTS_IN_MS = 100;
	
	/**
	 * Quick switch to allow interruption of execution. Should be used with care! Stops execution completely.
	 */
	public boolean interruptExecution = false;
	
	
	/**
	 * Returns the RNG seed as part of the parameter set.
	 * @return
	 */
	/*@SimulationParam
	public long getRandomNumberGeneratorSeed(){
		return seed();
	}*/
	
	/**
	 * Agent directory of all registered agents for quick access - 
	 * indexed by agent name.
	 */
	public LinkedHashMap<String, IndivWeightProvider> agentDirectory = new LinkedHashMap<String, IndivWeightProvider>();
	
	/**
	 * Returns a list of all registered agents whose names start with a given string.
	 * @param startString String each agent's name should start with
	 * @return List of agents whose names start with given string
	 */
	public ArrayList<String> getAllAgentNamesStartingWith(String startString){
		return getAllAgentNamesStartingWithOrContain(startString, true);
	}
	
	/**
	 * Returns a list of all registered agents whose names contain a given string.
	 * @param containedString String each agent's name should contain
	 * @return List of agents whose names contain the given string
	 */
	public ArrayList<String> getAllAgentNamesContain(String containedString){
		return getAllAgentNamesStartingWithOrContain(containedString, false);
	}
	
	/**
	 * Returns all agents from the agent directory whose names either start with 
	 * a given string or contain it.
	 * @param stringOfInterest String to be checked in each agent's name
	 * @param startsWith Indicates if names should start with stringOfInterest (true), or else (false) only contain it
	 * @return List of all agents matching the criterion
	 */
	private ArrayList<String> getAllAgentNamesStartingWithOrContain(final String stringOfInterest, final boolean startsWith){
		ArrayList<String> results = new ArrayList<>();
		for(String name: agentDirectory.keySet()){
			if(startsWith){
				if(name.startsWith(stringOfInterest)){
					results.add(name);
				}
			} else {
				if(name.contains(stringOfInterest)){
					results.add(name);
				}
			}
		}
		return results;
	}
	
	
	/*public synchronized void addToAgentDirectory(String agentName, IndivWeightProvider agent){
		this.agentDirectory.put(agentName, agent);
	}
	
	public synchronized LinkedHashMap<String, IndivWeightProvider> getAgentDirectory(){
		return agentDirectory;
	}*/
	
	private Network edges = null;
	private Continuous3D locations3d = null;
	
	private static final String PREFIX = "Simulation core: ";
	
	public GridSim(Statistics stats, long seed) {
		this(false, 1, 1, 1, false, false, stats, seed);
	}
	
	public GridSim(Boolean useGraph, Integer gridWidth, Integer gridHeight, Integer gridDepth, Boolean toroidal, Boolean use3D, Statistics stats, long seed) {
		super(seed);
		//save seed locally
		this.seed = seed;
		this.random = new MersenneTwister(seed);
		GRID_WIDTH = gridWidth;
		GRID_HEIGHT = gridHeight;
		GRID_DEPTH = gridDepth;
		TOROIDAL = toroidal;
		USE_3D = use3D;
		if(deactivateD3DForJava8 && runningJava8()){
			deactivateD3D();
		}
		this.stats = stats;
		if(useGraph){
			this.graphHandler = new GraphHandler<VertexPoint3D<String>>(this);
		}
		System.out.println(PREFIX + "GraphHandler initialized");
		this.locations3d = new Continuous3D(1.0, GRID_WIDTH, GRID_HEIGHT, GRID_DEPTH);
		this.edges = new Network();
	}
	
	/**
	 * Returns instance of Statistics used in this simulation.
	 * @return
	 */
	public Statistics getStatistics(){
		return stats;
	}
	
	public Continuous3D getMason3dLocations(){
		return locations3d;
	}
	
	public Network getEdges(){
		return edges;
	}
	
	/**
	 * Sets the StdOut/StdErr redirection filename. In order to redirect, activate {@link #redirectStdOutStdErr}.
	 * @param filename Filename to be used. If null or "", system uses default name {@link #stdOutErrOutfile}.
	 */
	public void setRedirectStdOutStdErrFilename(String filename){
		if(filename != null && !filename.isEmpty()){
			this.stdOutErrOutfile = filename;
		}
	}
	
	/**
	 * Activates the use of the Mason scheduler to run the graphs.
	 * @param activate
	 */
	public void useMasonSchedulerForForceGraphs(boolean activate){
		if(activate){
			System.out.println(PREFIX + "Activated use of Mason scheduler for Social Force graph.");
		} else {
			System.out.println(PREFIX + "Deactivated use of Mason scheduler for Social Force graph.");
		}
		this.useMasonSchedulerForForcesGraph = activate;
	}
	
	/**
	 * Indicates if the use of Mason scheduling for graphs is activated.
	 * @return
	 */
	public boolean usingMasonSchedulerForGraphs(){
		return this.useMasonSchedulerForForcesGraph;
	}
	
	/**
	 * Indicates if the Social Forces Graph UI should be shown in one frame 
	 * or if visualization and controles are separated into two frames 
	 * (e.g. to allow video recording).
	 * @param activate
	 */
	public void showSocialForcesGraphUiInSingleFrame(boolean activate){
		this.forceGraphUiInSingleFrame = activate;
	}
	
	/**
	 * Indicates if Social Forces Graph UI is shown in a single frame or
	 * separated into two frames (to isolate graph from controls).
	 * @return
	 */
	public boolean showSocialForcesGraphUiInSingleFrame(){
		return this.forceGraphUiInSingleFrame;
	}
	
	@Override
	public void finish() {
		super.finish();
		MTConnector.shutdown();
		if(stats != null){
			stats.resetStats();
		}
		CommunicationSpace.reset();
		if(graphHandler != null){
			graphHandler.shutdown();
		}
		PositionSaver.clearAllRegisteredWindows();
		schedule.reset();
	}

}
