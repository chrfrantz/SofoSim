package org.sofosim.forceLayout;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import javax.vecmath.Point3d;
import org.apache.commons.collections15.Transformer;
import org.nzdis.micro.inspector.PlatformInspectorGui;
import org.nzdis.micro.inspector.PlatformInspectorListener;
import org.sofosim.clustering.AttractionClusterer;
import org.sofosim.clustering.CentroidWithMinMaxValues;
import org.sofosim.clustering.ClusterInformationHandler;
import org.sofosim.clustering.ClusterUtility;
import org.sofosim.clustering.DBSCAN;
import org.sofosim.clustering.SpatialProximityClusterer;
import org.sofosim.clustering.VertexPoint3D;
import org.sofosim.environment.GridSim;
import org.sofosim.environment.annotations.SimulationParam;
import org.sofosim.environment.stats.Statistics;
import org.sofosim.forceLayout.transformers.VertexShapeRenderer;
import org.sofosim.forceLayout.util.CircleShape;
import org.sofosim.forceLayout.util.CircleShape3D;
import org.sofosim.forceLayout.util.LineShape;
import org.sofosim.forceLayout.util.TextShape;
import org.sofosim.graph.GraphChangeListener;
import org.sofosim.graph.initializers.ForceGraphInitializer;
import org.sofosim.nadico.CommunicationSpace;
import org.sofosim.planes.SocialPlane;
import org.sofosim.planes.SocialPlaneInitializer;
import org.sofosim.planes.TagSocialPlane;
import org.sofosim.structures.DirectionVector;
import org.sofosim.structures.DirectionVector3D;
import org.sofosim.structures.ForceDistanceContainer;
import org.sofosim.tags.Tag;
import org.sofosim.util.ProximityCalculator3D;
import sim.display.Console;
import sim.field.network.Edge;
import sim.util.Double3D;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;

public class ForceDirectedLayout<V, E> extends AbstractLayout<V, E> implements
		IterativeContext, GraphChangeListener<V>, PlatformInspectorListener {

	public final GridSim sim;
	public final Integer xGridSize;
	public final Integer yGridSize;
	public final Integer zGridSize;
	public final Boolean toroidal;
	private final ProximityCalculator3D dirCalc;
	private ArrayList<V> vertices = null;
	private ArrayList<V> secondaryVertices = null;
	private HashMap<V, ForceDistanceContainer> verticesInPrivateZone = new HashMap<V, ForceDistanceContainer>();
	// Double forceMultiplier = 1.0;
	// Double damping = 0.5;

	/**
	 * Indicates if vertices are initialized with random positions, else all
	 * vertices start randomized around the centroid.
	 */
	@SimulationParam
	public final boolean startWithRandomPositions = true;

	/**
	 * Random range around centroid for initial positioning
	 */
	private int randomRangeAroundCentroid = 5;
	
	/**
	 * Holds centroid of visualization once requested for first time.
	 */
	private Point3d centroidOfVisualization = null;
	
	/**
	 * Ensures that centroid information is printed only once (for debug purposes).
	 */
	private boolean centroidInfoPrinted = false;
	

	/**
	 * Returns centroid of visualization.
	 * 
	 * @return
	 */
	protected Point3d getVisualizationCentroid() {
		if (centroidOfVisualization == null) {
			centroidOfVisualization = new Point3d(xGridSize / 2, yGridSize / 2,
					zGridSize / 2);
		}
		if (!centroidInfoPrinted) {
			printSofoSimOutput("Centroid of visualization: "
					+ centroidOfVisualization);
			centroidInfoPrinted = true;
		}
		return new Point3d(centroidOfVisualization.x
				+ sim.random().nextInt(randomRangeAroundCentroid)
				- randomRangeAroundCentroid / 2, centroidOfVisualization.y
				+ sim.random().nextInt(randomRangeAroundCentroid)
				- randomRangeAroundCentroid / 2, centroidOfVisualization.z
				+ sim.random().nextInt(randomRangeAroundCentroid)
				- randomRangeAroundCentroid / 2);
	}

	/**
	 * Maximum movement in a given round (-1.0 for unlimited movement)
	 */
	@SimulationParam
	private final double maxMovementPerStep;
	
	/**
	 * Indicates if sectors should be drawn in UI. 
	 */
	public static final boolean drawAndManageSectors = true;
	
	/**
	 * Use sector-based calculation (if maxDistance != -1 (!), else it is just 
	 * showing sectors on UI)
	 */
	@SimulationParam
	public static final boolean useSectorBasedCalculation = false;
	@SimulationParam
	public final int xSizeOfSector;
	@SimulationParam
	public final int ySizeOfSector;
	@SimulationParam
	public final int zSizeOfSector;
	@SimulationParam
	public final int vertexDiameter;
	
	/**
	 *  Indicates if forces should be shown in UI
	 */
	public static boolean drawForces = true;
	
	/** 
	 * Indicates if drawing of max distances is activated on UI. 
	 * Requires activation of {@link #drawAndManageSectors}.
	 */
	public static boolean drawMaxDistances = false;
	
	/** 
	 * Indicates if drawing of min distances is activated on UI. 
	 * Requires activation of {@link #drawAndManageSectors}.
	 */
	public static boolean drawMinDistances = false;
	
	/** 
	 * Indicates if drawing of max attraction distances is activated on UI. 
	 * Requires activation of {@link #drawAndManageSectors}.
	 */
	public static boolean drawMaxAttractionDistances = false;
	
	/**
	 * Indicates if visible sectors should be drawn. 
	 * Requires activation of {@link #useSectorBasedCalculation}.
	 */
	public static boolean drawVisibleSectors = false;
	
	/**
	 * Indicates if sector density is drawn on UI. 
	 * Works independent of {@link #useSectorBasedCalculation}.
	 */
	public static boolean drawSectorDensity = false;
	
	/**
	 * Indicates if cluster stats should be drawn on screen. 
	 * Requires activation of {@link #clusteringOfVertices}.
	 */
	public static boolean printClusterStats = false;
	
	/**
	 * Font used to print cluster statistics on the Social Forces Graph (glasspane)
	 */
	private final Font clusterStatsFont = new Font("Arial", Font.BOLD, 12);
	
	/**
	 * Font used to print individual tag information on the Social Forces Graph (glasspane)
	 */
	private final Font individualTagFont = new Font("Arial", Font.BOLD, 12);
	
	/**
	 * Stroke used for social plane lines 
	 */
	private final BasicStroke socialPlaneLineStroke = new BasicStroke(1f);

	/** 
	 * Some planes may be perceptionally constrained (see plane implementation). 
	 * This can be overridden using this parameter. Only useful if perception distance is limited (!= -1).
	 */
	@SimulationParam
	public static final boolean makeAllPlanesDistanceIndependent = false;
	
	/**
	 * Indicates if forces and distances should be drawn on GlassPane instead of content pane
	 */
	final boolean drawOnGlassPane = true;
	
	private ForceGlassPane glassPane = null;
	final boolean drawForcesOnGlassPane = false;
	final boolean drawDistancesOnGlassPane = false;
	
	/**
	 * Indicates if tags should be drawn on GlassPane.
	 * Only relevant if display of tags is activated.
	 */
	public boolean drawTagsOnGlassPane = false;
	
	/**
	 * Indicates if clusters on force graph should be drawn on GlassPane
	 * (if {@link #clusteringOfVertices} is activated).
	 */
	final boolean drawClustersOnGlassPane = true;

	/**
	 * Indicates if the bias towards grid center should be activated. 
	 * (Helps visualisation if toroidal grid is activated).
	 */
	@SimulationParam
	public boolean useCenterBias = false;
	
	/**
	 * automatic adjustment of minimal distance depending on experienced push
	 * force
	 */
	// boolean forceDependentMinimalDistance = false;
	// no repulsion by surrounding individuals if transiting their range to
	// another area
	// DOESN'T WORK OUT AS NO SPACING OUT POSSIBLE IN DENSE SECTORS
	final boolean noRepulsionWhenTransiting = false;

	/** indicates if Mason's 3D visualizer should be activated */
	public final boolean use3d;

	/**
	 * Default setting for printing 3D lines between vertices. 
	 * Can be changed at simulation runtime using 
	 * {@link #activate3dLines(boolean)}.
	 */
	public static boolean print3dLinesPerDefault = false;
	
	/** 
	 * Indicates if 3D social plane links are plotted 
	 */
	private boolean print3dLines = print3dLinesPerDefault;

	/** 
	 * Indicates if plotting of 3D social plane links is activated. 
	 */
	public boolean print3dLines() {
		return print3dLines;
	}

	/** 
	 * De/Activates plotting of 3D lines in visualizer 
	 */
	public void activate3dLines(boolean activate) {
		print3dLines = activate;
		if (!print3dLines) {
			// remove all edges if printing 3D links has been deactivated
			clearAllEdges();
		}
	}

	/**
	 * Default setting for printing 2D lines between vertices. 
	 * Can be changed at simulation runtime using 
	 * {@link #activate2dLines(boolean)}.
	 */
	public static boolean print2dLinesPerDefault = true;
	
	/** indicates if 2D social plane links are plotted */
	private boolean print2dLines = print2dLinesPerDefault;

	/** indicates if links in 2D visualizers are plotted */
	public boolean print2dLines() {
		return print2dLines;
	}

	/** De/Activates plotting of 2D lines in visualizer */
	public void activate2dLines(boolean activate) {
		print2dLines = activate;
	}

	/** individual to be highlighted for displaying attractor */
	public V highlightedIndividual = null;
	public HashSet<String> individualsToBeGreyedOut = new HashSet<String>();

	// debugging-related
	@SimulationParam
	public static boolean debug = false;
	@SimulationParam
	// debug information for initialization phase
	final boolean initDebug = true;
	String filename = null;
	
	/** 
	 * Indicates if vertex output listeners are informed about the debug output
	 * (if {@link #debug} is activated).
	 */
	@SimulationParam
	final boolean writeDebugToVertexOutputListeners = debug && true;
	
	/**
	 * Indicates if writing of any debug output to IO, ie. either FileWriter or 
	 * console will occur.
	 */
	@SimulationParam
	final boolean writeDebugToIO = debug && true;
	
	/**
	 * Indicates if output is written to console or file (depends on
	 * {@link #writeDebugToIO} and on activation of {@link #debug}).
	 */
	final boolean writeDebugToOutfileInsteadConsole = false;
	
	static ForceGraphWriter writer = null;
	private StringBuffer debugBuffer = null;
	private StringBuffer detailDebugBuffer = null;
	public static final String LINE_SEPARATOR = System
			.getProperty("line.separator");
	/**
	 *  Listeners for debug output (means to get it into actual agent for inspection)
	 */
	private HashMap<V, VertexDebugOutputListener> vertexDebugListener = new HashMap<V, VertexDebugOutputListener>();

	private static final String prefix = "SofoSim: ";

	/**
	 * Prints output from within force framework
	 * 
	 * @param output
	 */
	private void printSofoSimOutput(String output) {
		if (initDebug) {
			System.out.println(new StringBuffer(prefix).append(output));
		}
	}

	/**
	 * Registers DebugOutputListener for given vertex. Each vertex can have one
	 * DebugOutputListener.
	 * 
	 * @param listener
	 */
	public void registerVertexDebugOutputListener(
			VertexDebugOutputListener listener) {
		vertexDebugListener.put((V) listener.getVertexOfInterest(), listener);
	}

	/**
	 * Deregisters DebugOutputListener for given vertex. Each vertex can have
	 * one DebugOutputListener.
	 * 
	 * @param listener
	 */
	public void deregisterVertexDebugOutputListener(
			VertexDebugOutputListener listener) {
		vertexDebugListener.remove((V) listener.getVertexOfInterest());
	}

	/**
	 * Notifies the listener for this specific vertex and delivers the output.
	 * 
	 * @param vertex
	 * @param output
	 */
	private void notifyDebugOutputListener(V vertex, StringBuffer output) {
		VertexDebugOutputListener v = vertexDebugListener.get(vertex);
		if (v != null) {
			// if(vertexDebugListener.containsKey(vertex)){
			// vertexDebugListener.get(vertex).receiveForceDebugOutput(output);
			v.receiveForceDebugOutput(output);
		}
	}

	/**
	 * Returns true if a debug output listener has been registered for the given
	 * vertex.
	 * 
	 * @param vertex
	 * @return
	 */
	private boolean debugForVertexActivated(V vertex) {
		return vertexDebugListener.containsKey(vertex);
	}

	/** === DISTANCE-RELATED STUFF === */
	
	// 50 //intermediate 200 //120
	/** minimal distance of two vertices (privacy zone) */
	@SimulationParam
	public final int minimalDistance;// = vertexDiameter * 2;
	
	/** Extension of private zone (minimal distance) to avoid continuous rapid switch between attraction and repulsion 
	 *  between two agents - i.e. no attraction inside this extended zone */
	@SimulationParam
	public final int toleranceZone;
	
	// 100 //intermediate 300 //200
	/** Maximal distance for which any perception can occur */
	@SimulationParam
	public final int maximalDistance = -1;// 200;
	
	// 250 //intermediate 400 //300 //200
	/** Maximal distance in which attraction will occur */
	@SimulationParam
	public final int maximalAttractionDistance = 0;
	
	/** 
	 * Maximal perception distance automatically derived from {@link #maximalDistance}, 
	 * {@link #maximalAttractionDistance} and {@link #minimalDistance} to ensure consistent behaviour.
	 * Will be automatically set to the highest of all distances.
	 */
	@SimulationParam
	public final int maximalPerceptionDistance;
	
	// will be used to calculate outgroup repulsion - max. distance in which it
	// is acting (-1 = unlimited)
	/** maximal distance at which repulsion will occur */
	@SimulationParam
	public final int maximalRepulsionDistance;// = vertexDiameter * 50;
	
	/**
	 * Repulsion setting towards outgroup members
	 */
	@SimulationParam
	final float outGroupRepulsion;
	
	/**
	 * Indicates if out-group repulsion ({@link #outGroupRepulsion}) is activated.
	 */
	@SimulationParam
	final boolean activateOutGroupRepulsion;

	/** 
	 * Indicates if distant individuals' attraction is amplified if outside tolerance zone (to accelerate convergence). 
	 */
	@SimulationParam
	final boolean amplifyAttractionForDistantVertices;

	/** 
	 * Specifies the power used to calculate the distance-dependent amplification (<distance>^<power>) of attraction 
	 */
	@SimulationParam
	final float amplificationPowerForDistantDependentAttraction;

	/**
	 * Indicates use of individual weights for different planes (queried from individuals during each round).
	 * If individuals return null weight, it is ignored for calculation.
	 */
	public static boolean useIndividualWeights = true;

	/**
	 * Individual weight providers (generally agents)
	 */
	private HashMap<V, IndivWeightProvider> weightProviders = new HashMap<V, IndivWeightProvider>();

	/**
	 * Registers a weight provider for individual weights for given planes. Only
	 * one provider can be registered per vertex.
	 * 
	 * @param vertex
	 *            vertex this provider relates to
	 * @param provider
	 *            provider
	 */
	public void registerWeightProvider(V vertex, IndivWeightProvider provider) {
		printSofoSimOutput("Registered social plane weight provider " + vertex);
		weightProviders.put(vertex, provider);
	}

	/**
	 * Removes a registered individual weight provider.
	 * 
	 * @param vertex
	 */
	public void removeWeightProvider(V vertex) {
		weightProviders.remove(vertex);
	}

	/**
	 * Requests plane weight from an individual weight provider.
	 * Returns null if none specified
	 * 
	 * @param vertex
	 * @param plane
	 * @return
	 */
	private Float requestWeightForPlaneFromProvider(V vertex, String plane) {
		if (weightProviders.containsKey(vertex)) {
			// System.out.println("Have registered weight provider " + vertex);
			// System.out.println("Requesting weight from " + vertex +
			// " for plane " + plane + ": " +
			// ((IndivWeightProvider<Tag>)weightProviders.get(vertex)).getPlaneWeight(plane));
			// System.out.println(weightProviders.get(vertex).toString());
			return ((IndivWeightProvider<Tag>) weightProviders.get(vertex))
					.getPlaneWeight(plane);
		}
		return null;
	}

	/**
	 * Retrieves the individual tag weight from a given IndivWeightProvider.
	 * 
	 * @param vertex
	 *            vertex representing the individual
	 * @param tag
	 *            tag of interest
	 * @return
	 */
	private Float requestTagWeightForTagFromProvider(V vertex, Tag tag) {
		if (weightProviders.containsKey(vertex)) {
			return weightProviders.get(vertex).getTagWeight(tag);
		}
		return null;
	}

	/**
	 * Requests text to be printed on UI. May return null if nothing to be
	 * printed.
	 * 
	 * @param vertex
	 * @return
	 */
	private String requestTextToBePrintedForVertex(V vertex) {
		if (weightProviders.containsKey(vertex)) {
			return weightProviders.get(vertex).getTextToPrint();
		}
		return null;
	}

	/** === CLUSTERING-RELATED === */
	
	/** 
	 * Activates clustering of vertices based on proximity (DBSCAN). 
	 */
	@SimulationParam
	public static boolean clusteringOfVertices = true;
	
	private SpatialProximityClusterer<V> spatialClusterer = null;
	@SimulationParam
	/** Maximal distance between cluster members (to consider them clustered) - only used during initialization, can be changed at runtime via getClusterer() */
	public final float maxClusterNeighbourDistance;// = minimalDistance * 1.25f;
	@SimulationParam
	/** Minimal number of vertices to consider them a 'cluster' - only used during initialization, can be changed at runtime via getClusterer() */
	public final int minNumberOfMembersInCluster = 3;
	/**
	 * Maintains current (i.e. updated) positions of vertices as input for
	 * cluster calculation in each iteration.
	 */
	private ArrayList<V> clusterVertexPoints = null;
	
	/**
	 * Clusters are colored in UI. If history of old colors (and reuse) should
	 * be kept set to false. Else colors will newly generated when detecting a
	 * new cluster.
	 */
	private final boolean purgeUnusedColors = false;
	
	/**
	 * Indicates if there second-level clustering based on tags should be 
	 * activated.
	 */
	@SimulationParam
	public static boolean clusterSecondLevel = clusteringOfVertices && false;
	private AttractionClusterer<V> attractionClusterer = null;

	/** ===== Cluster label printing-related stuff */
	
	/** horizontal radius offset from centroid when printing cluster information (only used if not enough vertical space around cluster position) */
	@SimulationParam
	public final float clusterXInformationPrintingOffset = 2.5f;
	
	/** vertical radius offset factor from centroid when printing cluster information */
	@SimulationParam
	public final float clusterYInformationPrintingOffset = 1.3f;

	/**
	 * Factor to reduce effect of combined displacement in x and y direction
	 * (rounding corners), Also used to reduce sensitivity to border proximity
	 * to stabilize label positioning in x dimension
	 */
	@SimulationParam
	float offsetSquareReductionFactor = 0.7f;
	
	/**
	 * Min. absolute distance from grid border on x dimension (e.g. 0.1 *
	 * xGridSize) if distance using clusterXInformationPrintingOffset is too
	 * small to allow readability (i.e. cluster radius to small) - compared
	 * using Math.max()
	 */
	@SimulationParam
	float minXBorderThreshold = 0.1f;
	
	/**
	 * Min. absolute distance from grid border on y dimension (e.g. 0.05 *
	 * yGridSize) if distance using clusterYInformationPrintingOffset is too
	 * small to allow readability (i.e. cluster radius to small) - compared
	 * using Math.max()
	 */
	@SimulationParam
	float minYBorderThreshold = 0.1f;

	/** Indicates if tags attached to an agent should be highlighted */
	public boolean highlightTags = false;
	public HashMap<String, Color> tagColors = new HashMap<String, Color>();

	/** maintaining calculated forces for printing in Ui */
	private HashMap<V, Double> forcesMap = new HashMap<V, Double>();

	/** history of vertex positions to manage reregistration to new sectors */
	private HashMap<V, Point3d> positionHistory = new HashMap<V, Point3d>();
	/** map handling the mapping of 2D sectors and vertices within those sectors */
	private HashMap<Point2D, HashSet<V>> sectorMembers2D = new HashMap<Point2D, HashSet<V>>();
	/** map handling the mapping of 3D sectors and vertices within those sectors */
	private HashMap<Point3d, HashSet<V>> sectorMembers3D = new HashMap<Point3d, HashSet<V>>();
	
	/** will hold the sector scale (i.e. number of sectors after initializeSectors() has been called (for transposing positions) */
	@SimulationParam
	private int sectorScale = 0;

	/** data structure holding third dimension */
	private HashMap<Point2D, Float> thirdDimension = new HashMap<Point2D, Float>();

	private Point3d getXYZForPoint(V vertex) {
		/*
		 * if(debug && writeDebugToIO){
		 * System.out.println("Retrieving 3D point for 2D point " + point); }
		 */
		// primary source: JUNG
		Point2D p2D = transform(vertex);
		if (use3d) {
			Double3D point = sim.getMason3dLocations()
					.getObjectLocation(vertex);
			if (point != null) {
				return new Point3d(p2D.getX(), p2D.getY(), point.z);
			} else {
				// provide centroid
				if (!startWithRandomPositions) {
					return getVisualizationCentroid();
				}
				// make up new z dimension
				return new Point3d(p2D.getX(), p2D.getY(), new Integer(
						sim.random().nextInt(zGridSize)).doubleValue());
			}
		} else {
			// if value is new and positioning is not randomized, provide
			// centroid value
			if (p2D.getX() == Double.MAX_VALUE
					&& p2D.getY() == Double.MAX_VALUE
					&& !startWithRandomPositions) {
				Point3d p2Temp = getVisualizationCentroid();
				p2Temp.z = 0;
				return p2Temp;
			}
			// set to zero if no 3D
			return new Point3d(p2D.getX(), p2D.getY(), 0);
		}
	}

	private void setXYZPoint(V vertex, Point3d point3d) {
		dirCalc.doDimensionBoundaryChecks(point3d, vertex.toString());
		// Point2D finalPoint = new Point2D.Float((float)point3d.x,
		// (float)point3d.y);
		// thirdDimension.put(finalPoint, (float)point3d.z);
		// System.out.println("Setting position: " + point3d);
		// if(use3d){
		try {
			sim.getMason3dLocations().setObjectLocation(vertex,
					new Double3D(point3d.x, yGridSize - point3d.y, point3d.z));
		} catch (NullPointerException e) {
			// do nothing
		}
		// }
	}

	public ForceDirectedLayout(Graph graph,
			Transformer<V, Point2D> initializer, GridSim simulation,
			SocialPlaneInitializer planeInitializer) {
		super(graph);
		// set final values
		this.sim = simulation;
		this.xGridSize = sim.GRID_WIDTH;
		this.yGridSize = sim.GRID_HEIGHT;
		this.zGridSize = sim.GRID_DEPTH;
		this.toroidal = sim.TOROIDAL;
		this.use3d = sim.USE_3D;
		this.xSizeOfSector = xGridSize / 10;
		this.ySizeOfSector = yGridSize / 10;
		if (use3d) {
			this.zSizeOfSector = zGridSize / 10;
		} else {
			this.zSizeOfSector = 0;
		}
		this.vertexDiameter = xGridSize / 100;
		this.minimalDistance = vertexDiameter * 2;
		// additional distance in which agents are not attracted to smoothen
		// movements
		// this.toleranceZone = Math.max(minimalDistance, new
		// Float(this.minimalDistance * 2f).intValue());
		this.toleranceZone = minimalDistance;
		// still to figure out if this makes sense
		// this.maximalRepulsionDistance = -1;
		this.maximalRepulsionDistance = vertexDiameter * 50;
		this.maxClusterNeighbourDistance = Math.max(minimalDistance,
				toleranceZone) * 1.25f;
		this.maximalPerceptionDistance = (maximalDistance == -1) ? -1 : Math
				.max(Math.max(maximalAttractionDistance, maximalDistance),
						minimalDistance);
		// -1.0 is unlimited
		this.maxMovementPerStep = 5;// -1.0;
		// needs to be made dependent on in-group extent of individual
		this.outGroupRepulsion = 1.5f;// 3.5f;
		this.activateOutGroupRepulsion = true;
		// indicate if attraction for distant vertices should be increased
		// (distance^2) to accelerate convergence speed
		this.amplifyAttractionForDistantVertices = false;
		this.amplificationPowerForDistantDependentAttraction = 1.1f;
		this.dirCalc = new ProximityCalculator3D(xGridSize, yGridSize,
				zGridSize, toroidal);
		printSofoSimOutput("Force Field values assigned. Registering social planes ...");
		// initialize social planes
		registerPlanesFromSocialPlaneInitializer(planeInitializer);
		if (initializer != null) {
			this.setInitializer(initializer);
		}
		// initialize colors for visualization in second-level clustering and
		// tag-based differentiation in UI
		secClusterColors.put(0, secClusterCol1);
		secClusterColors.put(1, secClusterCol2);
		secClusterColors.put(2, secClusterCol3);
		secClusterColors.put(3, secClusterCol4);
		secClusterColors.put(4, secClusterCol5);
		secClusterColors.put(5, secClusterCol6);
		secClusterColors.put(6, secClusterCol7);
		secClusterColors.put(7, secClusterCol8);
		secClusterColors.put(8, secClusterCol9);
		// register listener so any click on an agent allows to highlight it on
		// the visualizer
		PlatformInspectorGui.getInstance().registerListener(this);
		initializeOnce();
	}

	private void initializeOnce() {
		if (debug && writeDebugToIO && writeDebugToOutfileInsteadConsole) {
			this.filename = Statistics.prepareOutFilePrefix() + "_Forces.txt";
			this.writer = new ForceGraphWriter(filename);
		}

		// set maximal perception distance
		/*
		 * if(maximalDistance == -1){ maximalPerceptionDistance = -1; } else {
		 * maximalPerceptionDistance =
		 * Math.max(Math.max(maximalAttractionDistance, maximalDistance),
		 * minimalDistance); }
		 */
		if ((useSectorBasedCalculation || drawAndManageSectors)
				&& xSizeOfSector != 0 && ySizeOfSector != 0) {
			initializeSectors();
		}
		// write ForceLayout-related params
		// new StatsParamWriter(Statistics.prepareOutFilePrefix() +
		// "_forceParams.txt", this.getClass(), this).writeParameters();
	}

	public boolean attractionClustererActivated() {
		return clusterSecondLevel;
	}

	public void activateAttractionClusterer(boolean active) {
		if (active) {
			initializeAttractionClusterer();
			clusterSecondLevel = true;
		} else {
			clusterSecondLevel = false;
		}
	}

	/**
	 * Returns currently used sectors.
	 * 
	 * @return
	 */
	public HashMap<Point3d, HashSet<V>> getSectors() {
		return sectorMembers3D;
	}

	/**
	 * Returns number of sectors.
	 * 
	 * @return
	 */
	public double getSectorCount() {
		if (use3d) {
			return Math.pow(sectorScale, 3);
		} else {
			return Math.pow(sectorScale, 2);
		}
	}

	public ProximityCalculator3D getProximityCalculator() {
		return dirCalc;
	}

	private void initializeClusterer() {
		if (clusteringOfVertices && spatialClusterer == null) {
			this.spatialClusterer = new DBSCAN<V>(maxClusterNeighbourDistance,
					minNumberOfMembersInCluster, dirCalc);
			initializeAttractionClusterer();
		}
	}

	private void initializeAttractionClusterer() {
		if (attractionClusterer == null) {
			this.attractionClusterer = new AttractionClusterer<V>();
			// colors are now initialized in constructor
		}
	}

	/**
	 * holds color indices for second level clustering (and tag-based
	 * highlighting in UI)
	 */
	private HashMap<Integer, Color> secClusterColors = new HashMap<Integer, Color>();

	Color secClusterCol1 = Color.orange;
	Color secClusterCol2 = Color.yellow;
	Color secClusterCol3 = Color.black;
	Color secClusterCol4 = Color.cyan;
	Color secClusterCol5 = Color.pink;
	Color secClusterCol6 = Color.green;
	Color secClusterCol7 = Color.magenta;
	Color secClusterCol8 = Color.gray;
	Color secClusterCol9 = Color.lightGray;
	// blue is another color

	/** Register for color listeners */
	private ArrayList<SecondaryColorsListener> secondaryColorsListener = new ArrayList<SecondaryColorsListener>();

	/** Alpha value for secondary clustering colors. */
	private final int secondaryAndTagColorAlpha = 75;

	/**
	 * Registers secondary color listener (will only be called if second-level
	 * clustering is activated (clusterSecondLevel = true).
	 * 
	 * @param listener
	 *            Listener instance to be registered.
	 */
	public void registerSecondaryColorsListener(SecondaryColorsListener listener) {
		secondaryColorsListener.add(listener);
	}

	/**
	 * Unregisters secondary color listener.
	 * 
	 * @param listener
	 *            Listener instance to be deregistered.
	 */
	public void unregisterSecondaryColorListener(
			SecondaryColorsListener listener) {
		if (secondaryColorsListener.contains(listener)) {
			secondaryColorsListener.remove(listener);
		}
	}

	/** Font used in Ui for second-level cluster information */
	private Font subClusterCountFont = new Font("Arial", Font.PLAIN, 24);

	/**
	 * Sends updated list of secondary cluster colors to listeners.
	 */
	private void callSecondaryColorsToListeners() {
		HashMap<String, Color> secSphereCols = new HashMap<String, Color>();
		for (Entry<String, Integer> entry : secondaryPlanes.entrySet()) {
			secSphereCols.put(entry.getKey(),
					secClusterColors.get(entry.getValue()));
		}
		System.err
				.println("Sent updated secondary cluster colors to listeners");
		for (int i = 0; i < secondaryColorsListener.size(); i++) {
			secondaryColorsListener.get(i).receiveUpdatedSecondaryColors(
					secSphereCols);
		}
	}

	/**
	 * Sends updated list of tag references to listeners
	 */
	private void callHighlightedTagColorsToListeners() {
		// System.err.println("Sent updated tag colors to listeners");
		for (int i = 0; i < secondaryColorsListener.size(); i++) {
			secondaryColorsListener.get(i).receiveUpdatedSecondaryColors(
					tagColors);
		}
	}

	/**
	 * Maintains index for combination of discovered plane (stable cluster
	 * association) and color (to ensure unique coloring)
	 */
	private HashMap<String, Integer> secondaryPlanes = new HashMap<String, Integer>();

	/**
	 * Register for tag distribution listeners (only called if second-level
	 * clustering is deactivated)
	 */
	private ArrayList<TagDistributionListener> tagDistributionListener = new ArrayList<TagDistributionListener>();

	/**
	 * Registers a tag distribution listener (will send tag distribution
	 * periodically), but only if second level clustering is deactivated
	 * (clusterSecondLevel = false).
	 * 
	 * @param listener
	 *            Listener instance to be registered.
	 */
	public void registerTagDistributionListener(TagDistributionListener listener) {
		tagDistributionListener.add(listener);
	}

	/**
	 * Unregisters tag distribution listener.
	 * 
	 * @param listener
	 *            Listener instance to be deregistered.
	 */
	public void unregisterTagDistributionListener(
			TagDistributionListener listener) {
		if (tagDistributionListener.contains(listener)) {
			tagDistributionListener.remove(listener);
		}
	}

	/**
	 * Sends current tag distribution to listeners.
	 */
	private void callTagDistributionListeners() {
		for (int i = 0; i < tagDistributionListener.size(); i++) {
			tagDistributionListener.get(i).receiveTagDistribution(
					CommunicationSpace.getTagDistribution());
		}
	}

	/**
	 * Returns the DBSCAN clusterer instance and initializes it if not done so
	 * before.
	 * 
	 * @return
	 */
	public SpatialProximityClusterer<V> getProximityClusterer() {
		initializeClusterer();
		return spatialClusterer;
	}

	/**
	 * Returns the (second-level) attraction clusterer and initializes if not
	 * done so before.
	 * 
	 * @return
	 */
	public AttractionClusterer<V> getAttractionClusterer() {
		initializeClusterer();
		return attractionClusterer;
	}

	private LinkedHashMap<String, SocialPlane<V>> planes = new LinkedHashMap<String, SocialPlane<V>>();

	/**
	 * Returns the initialized spheres
	 * 
	 * @return
	 */
	public LinkedHashMap<String, SocialPlane<V>> getPlanes() {
		return planes;
	}

	/**
	 * Returns the last force strength for a given vertex.
	 * 
	 * @param vertex
	 * @return
	 */
	public Double getForceForVertex(V vertex) {
		return forcesMap.get(vertex);
	}

	public static ForceGraphWriter getWriter() {
		return writer;
	}

	VertexShapeRenderer vertexShapeRenderer = null;

	// set to true if updating of graph should be done by Mason scheduler
	// instead of JUNG
	boolean runVisualization = true;

	@Override
	public boolean done() {
		if (sim.usingMasonSchedulerForGraphs()) {
			System.out.println(prefix + "Switched to Mason scheduling.");
			return true;
		} else {
			// if set to false, a dedicated thread will update the graph which
			// is faster, but not steppable/stoppable
			if (sim.console != null) {
				// System.out.println("PlayState: " +
				// MaghribiSimUi.console.getPlayState());
				if (sim.console.getPlayState() == Console.PS_PLAYING) {
					runVisualization = true;
				} else if (sim.console.getSimulation().state.schedule.getSteps() == 0) {
					// ensure initial spacing out of agents
					runVisualization = true;
				} else {
					runVisualization = false;
				}
			}
			return false;
		}
	}

	/** registered listeners for cluster changed */
	private ArrayList<ForceClusterListener> clusterListeners = new ArrayList<ForceClusterListener>();

	/**
	 * Adds new Listener for clustering results.
	 * 
	 * @param listener
	 */
	public void registerClusterListener(ForceClusterListener listener) {
		clusterListeners.add(listener);
	}

	/**
	 * Removes a listener for clustering results.
	 * 
	 * @param listener
	 */
	public void unregisterClusterListener(ForceClusterListener listener) {
		clusterListeners.remove(listener);
	}

	/** information handler for printing of cluster information */
	private ClusterInformationHandler clusterInformationHandler = null;

	/** Registers a new information handler for printing cluster information */
	public void registerClusterInformationHandler(
			ClusterInformationHandler handler) {
		this.clusterInformationHandler = handler;
	}

	/**
	 * Distributes clustering results to all registered listeners.
	 * 
	 * @param clusters
	 */
	private void callClusterListeners(
			final LinkedHashMap<ArrayList<V>, Color> clusters,
			final int totalNumberOfClusteredAgents) {
		/*
		 * new Thread(new Runnable(){ public void run(){
		 */
		for (int i = 0; i < clusterListeners.size(); i++) {
			clusterListeners.get(i).receiveClusterResults(clusters,
					totalNumberOfClusteredAgents);
		}
		/*
		 * } }).start();
		 */
	}

	private DirectionVector calculateCenterBias(Point2D location,
			boolean toroidal) {
		DirectionVector distVector = dirCalc.calculateGridDistance(location,
				new Point2D.Float(sim.GRID_WIDTH / (float) 2, sim.GRID_HEIGHT
						/ (float) 2), toroidal, true);
		return distVector;
	}

	private DirectionVector3D calculateCenterBias(Point3d location,
			boolean toroidal) {
		DirectionVector3D distVector = dirCalc.calculateGridDistance(location,
				new Point3d(sim.GRID_WIDTH / (float) 2, sim.GRID_HEIGHT
						/ (float) 2, sim.GRID_DEPTH / (float) 2), toroidal);
		return distVector;
	}

	/**
	 * holds sum of individual forces for currently iterated vertex in x
	 * direction
	 */
	private Double xForce = 0.0;
	/**
	 * holds sum of individual forces for currently iterated vertex in y
	 * direction
	 */
	private Double yForce = 0.0;
	/**
	 * holds sum of individual forces for currently iterated vertex in z
	 * direction
	 */
	private Double zForce = 0.0;
	/**
	 * sum of overall force to get a brief indication if forces exist for
	 * boolean check; (the x + y force sums might result in zero force overall)
	 */
	private Double overallForce = 0.0;

	/**
	 * Manual override for stepping. Will switch from JUNG scheduler to Mason
	 * scheduler. Subsequent graph updates will thus appear slower.
	 */
	public void manualStep() {
		sim.useMasonSchedulerForForceGraphs(true);
		runVisualization = true;
		step();
	}

	@Override
	public void step() {
		// System.out.println("Step");
		if (!runVisualization) {
			return;
		}
		if (clusteringOfVertices) {
			initializeClusterer();
		}

		vertices = new ArrayList<V>(graph.getVertices());
		// System.out.println("Vertices: " + vertices);
		// initialize vector
		if (clusteringOfVertices) {
			clusterVertexPoints = new ArrayList<V>(vertices.size());
		}

		if (validateDistances) {
			// reset distance stats if validation is activated
			actualVsDesiredDistance.clear();
			actualVsDesiredDistanceOutsidePrivateZone.clear();
			distanceDeviationRatio.clear();
			distanceDeviationRatioOutsidePrivateZones.clear();
		}
		// System.out.println("Vertices: " + vertices);
		// now iterate through all vertices
		for (int k = 0; k < vertices.size(); k++) {
			V vertexOne = vertices.get(k);

			// clear highlighted individuals for next round (or reset if
			// deselected)
			if (highlightedIndividual == null
					|| (highlightedIndividual != null && highlightedIndividual == vertexOne)) {
				individualsToBeGreyedOut.clear();
			}

			// original 2D point from JUNG framework - to set cleanly at the end
			// - but no operation on that
			Point2D onePoint2D = transform(vertexOne);
			// operation on 3D point
			Point3d onePoint = getXYZForPoint(vertexOne);
			// System.out.println("Getting pos " + vertexOne + ": " + onePoint);
			/*
			 * if(use3d){ //add vertex to 3D representation or update its
			 * position if different in both visualizers (incl. inversion of y
			 * axis) (based on 2D comparison) Double3D threeLoc =
			 * sim.getMason3dLocations().getObjectLocation(vertexOne);
			 * if(threeLoc == null || threeLoc.x != onePoint.getX() ||
			 * (yGridSize - threeLoc.y) != onePoint.getY() || threeLoc.z !=
			 * onePoint.getZ()){
			 * sim.getMason3dLocations().setObjectLocation(vertexOne, new
			 * Double3D(onePoint.getX(), yGridSize - onePoint.getY(),
			 * onePoint.getZ())); } }
			 */

			// vector calculation:
			// http://answers.yahoo.com/question/index?qid=20071027121208AAFPJan
			// http://www.ibm.com/developerworks/java/library/j-antigrav/
			xForce = 0.0;
			yForce = 0.0;
			zForce = 0.0;
			overallForce = 0.0;

			if (validateDistances) {
				// reset validation force calculation
				validationXForce = 0.0;
				validationYForce = 0.0;
				validationZForce = 0.0;
				validationXForceOutsidePrivateZones = 0.0;
				validationYForceOutsidePrivateZones = 0.0;
				validationZForceOutsidePrivateZones = 0.0;
				insidePrivateZone = false;
			}
			/*
			 * if(useCenterBias){
			 * 
			 * //calculate center bias DirectionVector3D centerDistance =
			 * calculateCenterBias(onePoint, toroidal); Point3d
			 * centerDistanceAsCoordinate =
			 * centerDistance.convertVectorToCartesianScreenCoordinates();
			 * //sqrt bias double centerDistanceLength =
			 * Math.sqrt(centerDistance.getLength()) / 500; xForce +=
			 * centerDistanceAsCoordinate.getX() * centerDistanceLength; yForce
			 * += centerDistanceAsCoordinate.getY() * centerDistanceLength;
			 * if(use3d){
			 * 
			 * } else {
			 * 
			 * 
			 * } //constant bias //xForce += centerBias.getX() *
			 * this.centerBias; //yForce += centerBias.getY() * this.centerBias;
			 * }
			 */

			if (debug
					&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
				// reset debug output buffer
				debugBuffer = new StringBuffer();
				debugBuffer.append(LINE_SEPARATOR).append(vertexOne)
						.append("'s Force at start: ").append(LINE_SEPARATOR);
				debugBuffer.append("X force: ").append(xForce)
						.append(LINE_SEPARATOR);
				debugBuffer.append("Y force: ").append(yForce)
						.append(LINE_SEPARATOR);
				debugBuffer.append("Z force: ").append(zForce)
						.append(LINE_SEPARATOR);
			}

			/*
			 * int minimalDistance = ForceDirectedLayout.minimalDistance;
			 * if(forceDependentMinimalDistance &&
			 * forcesMap.containsKey(vertexOne)){ int fc =
			 * forcesMap.get(vertexOne).intValue(); if(fc < 0){ minimalDistance
			 * -= fc * 0.5; } }
			 */

			// if using sector-based calculation and having another than
			// infinite perception distance
			if (useSectorBasedCalculation && maximalPerceptionDistance != -1) {
				if (secondaryVertices != null) {
					// get visible vertices given my maximum range of perception
					secondaryVertices = getVisibleVertices(onePoint);
					// System.out.println("Can potentially see " +
					// secondaryVertices.size() + " vertices.");
				} else {
					// initially do full calculation as no vertex is yet
					// registered
					secondaryVertices = vertices;
				}
			} else {
				secondaryVertices = vertices;
			}

			// fill up secondaryVertices with all vertices that are
			// location-independent (and not within perception range)
			if (makeAllPlanesDistanceIndependent) {
				for (SocialPlane sphere : planes.values()) {
					if (!sphere.perceptionallyConstrained()) {
						try {
							secondaryVertices
									.addAll(sphere
											.getPerceptionallyIndependentVertices(vertexOne));
						} catch (NullPointerException e) {
							// do nothing
						} catch (ConcurrentModificationException e) {
							// do nothing either
						}
					}
				}
			}

			// calculate force between this and all other vertices. They are
			// accumulated automatically.
			// for(V vertex2: secondaryVertices){
			for (int l = 0; l < secondaryVertices.size(); l++) {
				V vertexTwo = secondaryVertices.get(l);
				// Point2D twoPoint2D = transform(vertexTwo);
				// use helper construct to access z dimension
				Point3d twoPoint = getXYZForPoint(vertexTwo);
				if (!vertexOne.equals(vertexTwo)
						&& debug
						&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
					debugBuffer.append(
							"Coordinate for " + vertexTwo + ": " + twoPoint)
							.append(LINE_SEPARATOR);
					debugBuffer.append(
							"About to calculate force between " + onePoint
									+ " and " + twoPoint)
							.append(LINE_SEPARATOR);
				}
				calculateForceBetween(vertexOne, onePoint, vertexTwo, twoPoint);
			}

			// sum up all individual forces and determine actual movement
			Point3d ultimateTargetPos = new Point3d(onePoint.x + xForce,
					onePoint.y + yForce, onePoint.z + zForce);
			// relevant for VALIDATION --> see further down

			// if vertices are not exposed to repulsion when transiting they
			// will eventually always reach their target
			/*
			 * if(noRepulsionWhenTransiting){
			 * if(dirCalc.calculateAbsoluteDistance(onePoint,
			 * ultimateTargetPos).getLength() > minimalDistance){ //no repulsion
			 * as ultimately desired target position is out of range } else {
			 * System.out.println("Overall force: " + overallForce); //if target
			 * in proximity, check all related vertices for(Entry<V,
			 * ForceDistanceContainer> entry: verticesInPrivateZone.entrySet()){
			 * DirectionVector3D distVector = entry.getValue().distanceVector;
			 * System.out.println("Force: " + entry.getValue().force);
			 * //calculate repulsion from reachable ultimate target and indicate
			 * whether force is attracting or repulsing (to choose in- or
			 * outgroup repulsion) double force =
			 * calculateRepulsion((entry.getValue().force > 0),
			 * distVector.getLength());
			 * System.out.println("Resulting repulsing force between " +
			 * onePoint + " and " + transform(entry.getKey()) + ": " + force);
			 * Point3d convertedForceCoords =
			 * distVector.convertAngleToCartesianScreenCoordinates(); xForce +=
			 * convertedForceCoords.getX() * force; yForce +=
			 * convertedForceCoords.getY() * force; zForce +=
			 * convertedForceCoords.getZ() * force; overallForce += force; }
			 * verticesInPrivateZone.clear(); //calculate new ultimate target
			 * position given amended forces ultimateTargetPos = new
			 * Point3d(onePoint.getX() + xForce, onePoint.getY() + yForce,
			 * onePoint.getZ() + zForce); } }
			 */

			if (debug
					&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
				debugBuffer.append("--- SUM OF FORCES on ").append(vertexOne)
						.append(" - My position: ").append(onePoint)
						.append(LINE_SEPARATOR);
				debugBuffer.append("Ultimate target pos: ")
						.append(ultimateTargetPos).append(LINE_SEPARATOR);
				debugBuffer.append("XForce sum: ").append(xForce)
						.append(LINE_SEPARATOR);
				debugBuffer.append("YForce sum: ").append(yForce)
						.append(LINE_SEPARATOR);
				debugBuffer.append("ZForce sum: ").append(zForce)
						.append(LINE_SEPARATOR);
			}

			// calculate composite force vector (without transposing)
			DirectionVector3D dirVect = dirCalc.calculateAbsoluteDistance(
					onePoint, ultimateTargetPos);
			// get desired force strength
			double resultantForce = dirVect.getLength();
			// update map with force information
			forcesMap.put(vertexOne, resultantForce
					* (overallForce < 0 ? -1 : 1));
			// non-transposed target coordinate
			Point3d targetCoord = null;
			if (maxMovementPerStep == -1.0) {
				targetCoord = ultimateTargetPos;
			} else {
				// max movement per round is constrained

				if (debug
						&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
					debugBuffer.append("Vector: ").append(dirVect.toString())
							.append(LINE_SEPARATOR);
					debugBuffer
							.append("Desired target coordinates: ")
							.append(dirVect
									.convertVectorToCartesianScreenCoordinates())
							.append(LINE_SEPARATOR);
					debugBuffer.append("Force: ").append(resultantForce)
							.append(LINE_SEPARATOR);
				}

				// reduce force to physically reachable distance in one step
				// (maximum movement) --- -1 for unlimited movement
				if (resultantForce < 0) {
					resultantForce = Math.max(resultantForce,
							-maxMovementPerStep);
				} else if (resultantForce > 0) {
					resultantForce = Math.min(resultantForce,
							maxMovementPerStep);
				}
				// adjust length in vector
				dirVect.setLength(resultantForce);

				if (debug
						&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
					debugBuffer
							.append("Reduced target coordinates: ")
							.append(dirVect
									.convertVectorToCartesianScreenCoordinates())
							.append(LINE_SEPARATOR);
				}

				// obtain adjusted relative cartesian coordinates for shortened
				// force strength
				targetCoord = dirVect
						.convertVectorToCartesianScreenCoordinates();
				// add to current location
				targetCoord.set(onePoint.x + targetCoord.x, onePoint.y
						+ targetCoord.y, onePoint.z + targetCoord.z);

				if (debug
						&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
					debugBuffer.append("Calculated target coordinates: ")
							.append(targetCoord).append(LINE_SEPARATOR);
				}
			}

			// VALIDATION (stuff taken from earlier)
			// compare desired and actual target position before transposing
			// actual position to avoid skewed results (if one requires
			// transposing and the other not (e.g. 599 vs. 601 if grid size is
			// 600))
			if (validateDistances) {
				Point3d validationTargetPos = new Point3d(onePoint.x
						+ validationXForce, onePoint.y + validationYForce,
						onePoint.z + validationZForce);
				Double desiredDistance = dirCalc.calculateGridDistance(
						onePoint, validationTargetPos).getLength();
				if (validateDistanceOnCompoundForceLevel) {
					if (maxMovementPerStep != -1.0) {
						// - does same transformation as original code -->
						// conversion to vector and reconversion to coordinates
						// - minor differences
						DirectionVector3D validationDirVect = dirCalc
								.calculateAbsoluteDistance(onePoint,
										validationTargetPos);
						Double validationResultantForce = validationDirVect
								.getLength();

						// do force resultant reduction as in full version -
						// should it do?
						if (validationResultantForce < 0) {
							validationResultantForce = Math.max(
									validationResultantForce,
									-maxMovementPerStep);
						} else if (resultantForce > 0) {
							validationResultantForce = Math.min(
									validationResultantForce,
									maxMovementPerStep);
						}
						System.err
								.println("CAUTION: Resultant length reduction applied for validation calculation. Do you want that?");

						if (debug
								&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
							debugBuffer
									.append("Validation: Vector length reduction (as of maxMovementPerRound) from ")
									.append(validationDirVect.getLength())
									.append(" to ")
									.append(validationResultantForce)
									.append(LINE_SEPARATOR);
						}
						// adjust length in vector
						validationDirVect.setLength(validationResultantForce);

						Point3d validationMappedCoordinate = validationDirVect
								.convertVectorToCartesianScreenCoordinates();
						validationMappedCoordinate.add(onePoint);
						// System.out.println("Orig validation target vs. recalculated: "
						// +
						// validationTargetPos.distance(validationMappedCoordinate));
						validationTargetPos = validationMappedCoordinate;
						// update desired target position
						desiredDistance = dirCalc.calculateGridDistance(
								onePoint, validationTargetPos).getLength();
					}
					// difference between ideal (correct) position and enforced
					// one (based on repulsion)
					Double difference = dirCalc.calculateGridDistance(
							validationTargetPos, targetCoord).getLength();

					if (!Double.isInfinite(difference)
							&& sim.schedule.getSteps() > 5
							// TODO exclude low values
							&& difference >= 0.1 && desiredDistance >= 0.1) {
						// calculate difference between calculated target
						// distance (including natural repulsion and force
						// manipulations (maxMovementStep)) and purely
						// force-based target location
						actualVsDesiredDistance.put(vertexOne.toString(),
								difference);

						if (!insidePrivateZone) {
							// System.out.println("Registered difference for " +
							// vertexOne.toString());
							actualVsDesiredDistanceOutsidePrivateZone.put(
									vertexOne.toString(), difference);
						}
						// relative distance of (distance between valid and
						// skewed point) to (distance from current location to
						// valid point)
						// System.out.println(vertexOne.toString() +
						// ": Distance between two coords (right and skewed): "
						// + validationTargetPos.distance(targetCoord));
						// System.out.println("Distance between current coordinate and right target: "
						// + onePoint.distance(validationTargetPos));
						// System.out.println("Relative difference: " +
						// validationTargetPos.distance(targetCoord)/onePoint.distance(validationTargetPos));
						distanceDeviationRatio.put(vertexOne.toString(),
								difference / desiredDistance);
						if (!insidePrivateZone) {
							distanceDeviationRatioOutsidePrivateZones.put(
									vertexOne.toString(), difference
											/ desiredDistance);
						}
						if (actualVsDesiredDistance.get(vertexOne.toString()) > 50
								|| distanceDeviationRatio.get(vertexOne
										.toString()) > 10) {
							System.out
									.println(new StringBuffer(vertexOne
											.toString())
											.append(": Difference between desired point ")
											.append(validationTargetPos)
											.append(" and actual ")
											.append(targetCoord)
											.append(": ")
											.append(LINE_SEPARATOR)
											.append(actualVsDesiredDistance
													.get(vertexOne.toString()))
											.append(", desired distance: ")
											.append(desiredDistance)
											.append(", relative deviation: ")
											.append(distanceDeviationRatio
													.get(vertexOne.toString()))
											.append(" --- in private zone: ")
											.append(insidePrivateZone));
						}
						if (debug
								&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
							debugBuffer
									.append("Difference between desired target point ")
									.append(validationTargetPos)
									.append(LINE_SEPARATOR)
									.append(" and actual target (incl. natural repulsion) ")
									.append(targetCoord)
									.append(": ")
									.append(difference)
									.append(LINE_SEPARATOR)
									.append(" (Distance from current to desired position: ")
									.append(desiredDistance).append("),")
									.append(LINE_SEPARATOR).append(" ratio: ")
									.append(difference / desiredDistance)
									.append(" -- in private zone: ")
									.append(insidePrivateZone)
									.append(LINE_SEPARATOR);
						}
					}
				} else {
					if (sim.schedule.getSteps() > 5) {
						// individual calculation
						// sum of aggregated differences divided by 'correct'
						// distance -> errors/correct distance -> 1 means: as
						// many errors as correct distance
						distanceDeviationRatio.put(
								vertexOne.toString(),
								actualVsDesiredDistance.get(vertexOne
										.toString()) / desiredDistance);
						if (!insidePrivateZone) {
							distanceDeviationRatioOutsidePrivateZones.put(
									vertexOne.toString(),
									actualVsDesiredDistanceOutsidePrivateZone
											.get(vertexOne.toString())
											/ desiredDistance);
						}
						if (debug
								&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
							Double debugDiff = actualVsDesiredDistance
									.get(vertexOne.toString());
							Double debugRatio = distanceDeviationRatio
									.get(vertexOne.toString());
							debugBuffer
									.append("Difference between desired target point ")
									.append(validationTargetPos)
									.append(LINE_SEPARATOR)
									.append(" and actual target (incl. natural repulsion) ")
									.append(targetCoord)
									.append(": ")
									.append(debugDiff)
									.append(LINE_SEPARATOR)
									.append(" (Distance from current to desired position: ")
									.append(desiredDistance).append("),")
									.append(LINE_SEPARATOR).append(" ratio: ")
									.append(debugRatio)
									.append(" -- in private zone: ")
									.append(insidePrivateZone)
									.append(LINE_SEPARATOR);
						}
					}
				}
			}

			// transpose target coordinates for toroidal grid
			targetCoord = dirCalc.transposeTarget(targetCoord, toroidal);

			/*
			 * if(!dirCalc.doDimensionBoundaryChecks(targetCoord,
			 * vertexOne.toString())){ throw new
			 * RuntimeException("Invalid coordinate for " + vertexOne.toString()
			 * + " detected: " + targetCoord + " Before transpose: " + before);
			 * }
			 */

			if (debug
					&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
				debugBuffer.append("Transposed target coords: ")
						.append(targetCoord).append(LINE_SEPARATOR);
			}

			// set new location of vertex - done
			if (onePoint.equals(targetCoord)) {
				// System.out.println("Same pos.");
			} else {
				// System.out.println("Not same pos.");
				// set original JUNG 2D point
				onePoint2D.setLocation(targetCoord.x, targetCoord.y);
				// onePoint.setLocation(targetCoord);
				// if(use3d){
				// update position in 3D graph (invert y axis)
				setXYZPoint(vertexOne, targetCoord);
				// sim.getMason3dLocations().setObjectLocation(vertexOne, new
				// Double3D(targetCoord.getX(), yGridSize - targetCoord.getY(),
				// targetCoord.getZ()));
				// }
				if (drawAndManageSectors) {
					registerVertexWithSectors(vertexOne, targetCoord);
				}
			}
			// add new position of vertex for cluster calculation
			if (clusteringOfVertices && clusterVertexPoints != null) {
				// create new vertex instance - dodgy but fast
				V vertices = (V) new VertexPoint3D<String>(
						((VertexPoint3D<String>) vertexOne).vertex,
						new Point3d(onePoint.x, onePoint.y, onePoint.z));
				// fill vector if clustering is done
				clusterVertexPoints.add(vertices);
			}

			if (drawForces && overallForce != 0) {
				// raw force sum (very long lines)
				// double endX = onePoint.getX() + xForce;
				// double endY = onePoint.getY() + yForce;

				// adjust the length of the vector to make it more visible than
				// reduced resultant force
				DirectionVector3D dirCalcDisplay = dirVect;
				dirCalcDisplay.setLength(resultantForce * 3);
				Point3d end = dirCalcDisplay
						.convertVectorToCartesianScreenCoordinates();
				double endX = onePoint.x + end.x;
				double endY = onePoint.y + end.y;

				// reduced and transposed force (hardly visible if max. movement
				// steps is very low)
				// double endX = targetCoord.getX();
				// double endY = targetCoord.getY();

				// endX = dirCalc.transposeTargetX(endX, toroidal);
				// endY = dirCalc.transposeTargetY(endY, toroidal);
				if (vertexShapeRenderer == null) {
					vertexShapeRenderer = ((ForceGraphInitializer) sim.graphHandler
							.getGraphInitializer(ForceGraphInitializer.FORCES_GRAPH)).renderer;
				} else {
					// may still be null in the initial rounds (depending on
					// initialization order)
					vertexShapeRenderer.forceLine.put(vertexOne, new LineShape(
							0, onePoint2D, new Point2D.Double(endX, endY),
							(overallForce > 0), null, null));
				}
			}

			// drawing on glass pane is only useful if done() is set to false,
			// because graph has an own update thread (no delay)
			if (drawOnGlassPane) {
				initializeGlassPane();
				if (drawForcesOnGlassPane && overallForce != 0) {

					// raw force sum
					double endX = onePoint.x + xForce;
					double endY = onePoint.y + yForce;
					// reduced and transposed force (hardly visible if max.
					// movement steps is very low)
					// double endX = targetCoord.getX();
					// double endY = targetCoord.getY();

					// endX = dirCalc.transposeTargetX(endX, toroidal);
					// endY = dirCalc.transposeTargetY(endY, toroidal);
					Color col = null;
					if (overallForce > 0) {
						col = Color.BLACK;
					} else {
						col = Color.RED;
					}

					glassPane.paintLine(new LineShape(calculateLineShapeId(
							vertexOne, vertexOne, col), onePoint2D,
							new Point2D.Double(endX, endY), (overallForce > 0),
							col, null));
				}
				if (drawDistancesOnGlassPane) {
					// draw distances around agents
					// System.out.println("Minimal distance: " +
					// minimalDistance);
					glassPane.paintCircle(new CircleShape(onePoint2D,
							minimalDistance));
					// glassPane.paintCircle(new CircleShape(onePoint,
					// maximalDistance));
				}
				if (drawTagsOnGlassPane) {
					glassPane.paintText(new TextShape(onePoint2D,
							requestTextToBePrintedForVertex(vertexOne),
							individualTagFont, Color.BLACK));
				}
			}
			// finally write all debug stuff for this vertex
			if (debug && writeDebugToIO) {
				if (writeDebugToOutfileInsteadConsole) {
					writer.write(debugBuffer);
				} else {
					System.out.print(debugBuffer);
				}
			}
			if (debug && writeDebugToVertexOutputListeners) {
				notifyDebugOutputListener(vertexOne, debugBuffer);
			}
		}
		// processing of individual vertices done - now compound operations

		if (clusteringOfVertices && clusterVertexPoints != null
				&& !clusterVertexPoints.isEmpty()) {
			// cluster vertices
			spatialClusterer.setVertexPoints(clusterVertexPoints);
			spatialClusterer.applyClustering();
			ArrayList<ArrayList<V>> clusters = spatialClusterer
					.getLastResultList();
			LinkedHashMap<ArrayList<V>, Color> clusterColorMap = new LinkedHashMap<ArrayList<V>, Color>();
			int totalNumberOfClusteredAgents = 0;
			for (int i = 0; i < clusters.size(); i++) {
				ArrayList<V> clusterVerticesList = clusters.get(i);
				boolean contains = false;
				Color clusterColor = null;
				// if cluster color is defined, retrieve it from index
				boolean repeatColorSelection = false;
				do {
					try {
						for (Entry<V, Color> colorEntry : clusterColorIndex
								.entrySet()) {
							if (clusterVerticesList.contains(colorEntry
									.getKey())) {
								clusterColor = colorEntry.getValue();
								contains = true;
								if (purgeUnusedColors) {
									usedColors.put(colorEntry.getKey(),
											colorEntry.getValue());
								}
								break;
							}
						}
					} catch (ConcurrentModificationException e) {
						// do it again in case there is a concurrent access
						repeatColorSelection = true;
					}
				} while (repeatColorSelection);

				// else assign new color
				if (!contains) {
					do {
						// get new color for cluster
						clusterColor = getNewColor();
					} while (clusterColorIndex.values().contains(clusterColor));
					V newKey = clusterVerticesList.get(random
							.nextInt(clusterVerticesList.size()));
					clusterColorIndex.put(newKey, clusterColor);
					if (purgeUnusedColors) {
						usedColors.put(newKey, clusterColor);
					}
				}
				// System.out.println("Cluster " + i + ": " +
				// clusterVerticesList.size());
				// Point2D centroid =
				// ClusterUtility.calculateCentroid(clusterVerticesList).centroid;
				// System.out.println("Centroid of cluster: " + centroid);

				// adding to total number of clustered agents
				totalNumberOfClusteredAgents += clusterVerticesList.size();

				ArrayList<String> secClusters = new ArrayList<String>();
				if (clusterSecondLevel) {
					attractionClusterer
							.clusterMatrixEntries(clusterVerticesList);
					secClusters = attractionClusterer
							.getListOfClusterCharacteristics();

					// System.out.println("SubClusters for super cluster " +
					// clusterVerticesList.size() + ": (" + secClusters.size() +
					// "), " + secClusters.toString());
				}

				if (drawClustersOnGlassPane && glassPane != null) {

					// iterating the cluster elements for every agent
					for (int j = 0; j < clusterVerticesList.size(); j++) {
						V vPoint = clusterVerticesList.get(j);

						// System.out.println("Attractor spheres for " +
						// vPoint.vertex + ": " +
						// attractionClusterer.getSpheresForAgent((V)
						// vPoint.vertex));
						if (clusterSecondLevel) {
							ArrayList<String> agentPlanes = attractionClusterer
									.getSpheresForAgent(vPoint);
							if (agentPlanes != null && !agentPlanes.isEmpty()) {
								String planesForVertex = agentPlanes.toString();
								if (!secondaryPlanes
										.containsKey(planesForVertex)) {
									secondaryPlanes.put(planesForVertex,
											secondaryPlanes.size());
									callSecondaryColorsToListeners();
									// System.out.println("Now " +
									// secondarySpheres.size() + " entries.");
								}
								// paint circle around vertices in according
								// colours
								Color subCol = secClusterColors
										.get(secondaryPlanes
												.get(planesForVertex));
								if (subCol == null) {
									// if not enough colors predefined, generate
									// new one and assign to index
									System.err
											.println("Not enough colours defined for secondary clusters. Current "
													+ secClusterColors.size()
													+ " colors available.\nColor missing for sphere "
													+ planesForVertex
													+ "(Index: "
													+ secondaryPlanes
															.get(planesForVertex)
													+ ")");
									System.out
											.print("Determining new cluster color... ");
									do {
										// get new color and ensure it has not
										// been used yet
										subCol = getNewColor();
									} while (secClusterColors
											.containsValue(subCol));
									// add new color
									secClusterColors.put(secondaryPlanes
											.get(planesForVertex), subCol);
									// inform UI listener
									callSecondaryColorsToListeners();
									System.out.println(subCol);
								}
								// display filled circle around clustered vertex
								subCol = new Color(subCol.getRed(),
										subCol.getGreen(), subCol.getBlue(),
										secondaryAndTagColorAlpha);
								int circleDist = (int) maxClusterNeighbourDistance;
								//paint second-level clusters
								glassPane.paintCircle(new CircleShape3D(
										((VertexPoint3D) vPoint).point,
										circleDist, true, subCol)
										.toCircleShape());
							}
						} else {
							// first level cluster visualization
							glassPane.paintCircle(new CircleShape3D(
									((VertexPoint3D) vPoint).point,
									(int) maxClusterNeighbourDistance, true,
									clusterColor).toCircleShape());
							// additionally, send tag distribution information
							// to eventual listeners (displayed in text area
							// otherwise showing information on second-level
							// clustering
							callTagDistributionListeners();
						}
					}

					// calculate centroid of cluster in order to print it (under
					// consideration of toroidal grid)
					CentroidWithMinMaxValues centroidCalc = ClusterUtility
							.calculateCentroid(
									(List<VertexPoint3D>) clusterVerticesList,
									maxClusterNeighbourDistance,
									sim.GRID_WIDTH, sim.GRID_HEIGHT, toroidal);
					Point2D centroid = centroidCalc.centroid;
					if (clusterSecondLevel && clusterVerticesList.size() > 1) {
						// indicate number of sub-clusters on centroid (if
						// second level clustering as activated)
						glassPane.paintText(new TextShape(centroid, String
								.valueOf(secClusters.size()),
								subClusterCountFont, Color.BLACK));
					}
					// paint centroid of cluster
					
					glassPane.paintCircle(new CircleShape(centroid, 3, true,
							Color.black));

					// print cluster information from CommunicationSpace
					// System.out.println("Printing stats for cluster containing "
					// + clusterVerticesList.size() + " individuals.");
					if (printClusterStats && clusterInformationHandler != null) {
						Point2D offsetPoint = centroid;
						// derive radius in order to dynamically place cluster
						// information outside of cluster if possible
						float xRadius = centroidCalc.xRadius;
						float yRadius = centroidCalc.yRadius;

						// if splits in clusters, derive optimal printing place
						// from that information
						/*
						 * if(centroidCalc.xSplit || centroidCalc.ySplit){
						 * //System.out.print("Split detected, majority ");
						 * if(centroidCalc.xSplit){
						 * if(centroidCalc.xMajorityOnLeft){ //print on right of
						 * centroid //System.out.println("left"); offsetPoint =
						 * new Point2D.Double(centroid.getX() + radius *
						 * clusterXInformationPrintingOffset, centroid.getY());
						 * } else { //print to left of centroid
						 * //System.out.println("right"); offsetPoint = new
						 * Point2D.Double(centroid.getX() - radius *
						 * clusterXInformationPrintingOffset, centroid.getY());
						 * } } if(centroidCalc.ySplit){
						 * if(centroidCalc.yMajorityOnTop){ //print on bottom of
						 * centroid //System.out.println("top"); offsetPoint =
						 * new Point2D.Double(centroid.getX(), centroid.getY() +
						 * radius * clusterYInformationPrintingOffset); } else {
						 * //print on top of centroid
						 * //System.out.println("bottom"); offsetPoint = new
						 * Point2D.Double(centroid.getX(), centroid.getY() -
						 * radius * clusterYInformationPrintingOffset); } }
						 * //System.out.println("Split detected: Centroid: " +
						 * centroid + ", radius: " + radius +
						 * ", plans to print " + offsetPoint); } else {
						 */

						// if no split, conventional check for space
						boolean printToLeft = false;
						boolean printToRight = false;
						boolean printToTop = false;
						boolean printToBottom = false;
						/*
						 * Move to the left or right only if cluster is close to
						 * the edge (be lenient with distance to borders if
						 * large xRadius (i.e. wide cluster)). If cluster is
						 * small, take 10% of grid width
						 */
						if (offsetPoint.getX()
								+ Math.max(
										xGridSize * minXBorderThreshold,
										xRadius
												* clusterXInformationPrintingOffset
												* offsetSquareReductionFactor) >= xGridSize) {
							printToLeft = true;
							// offsetPoint = new
							// Point2D.Double(offsetPoint.getX() - radius *
							// clusterXInformationPrintingOffset,
							// offsetPoint.getY());
						} else if (offsetPoint.getX()
								- Math.max(
										xGridSize * minXBorderThreshold,
										xRadius
												* clusterXInformationPrintingOffset
												* offsetSquareReductionFactor) <= 0) {
							printToRight = true;
							// offsetPoint = new
							// Point2D.Double(offsetPoint.getX() + radius *
							// clusterXInformationPrintingOffset,
							// offsetPoint.getY());
						} // else no change in x orientation, i.e. if not at the
							// edge, cluster information will be printed above
							// or below centroid
							// printing information with vertical offset to
							// avoid unreadability if printed on centroid
						if (offsetPoint.getY()
								+ Math.max(
										yGridSize * minYBorderThreshold,
										yRadius
												* clusterYInformationPrintingOffset
												* 1.5) >= yGridSize) {
							printToTop = true;
							// offsetPoint = new
							// Point2D.Double(offsetPoint.getX(),
							// offsetPoint.getY() - radius *
							// clusterYInformationPrintingOffset);
						} else {
							/*
							 * default: information is printed below actual
							 * cluster - unless there has already been a
							 * displacement in horizontal direction (x) OR -
							 * there is NOT enough space and it has not been
							 * manipulated in X direction
							 */
							// if(offsetPoint.equals(centroid)){
							if (!printToLeft && !printToRight) {
								printToBottom = true;
								// offsetPoint = new
								// Point2D.Double(offsetPoint.getX(),
								// offsetPoint.getY() + radius *
								// clusterYInformationPrintingOffset);
							} else if (offsetPoint.getY()
									- Math.max(
											yGridSize * minYBorderThreshold,
											yRadius
													* clusterYInformationPrintingOffset) <= yGridSize) {
								printToBottom = true;
								// offsetPoint = new
								// Point2D.Double(offsetPoint.getX() ,
								// offsetPoint.getY() + radius *
								// clusterYInformationPrintingOffset);
							}
						}

						if (printToLeft && printToTop) {
							offsetPoint = new Point2D.Double(offsetPoint.getX()
									- xRadius * offsetSquareReductionFactor
									* clusterXInformationPrintingOffset,
									offsetPoint.getY() - yRadius
											* offsetSquareReductionFactor
											* clusterYInformationPrintingOffset);
						} else if (printToLeft && printToBottom) {
							offsetPoint = new Point2D.Double(offsetPoint.getX()
									- xRadius * offsetSquareReductionFactor
									* clusterXInformationPrintingOffset,
									offsetPoint.getY() + yRadius
											* offsetSquareReductionFactor
											* clusterYInformationPrintingOffset);
						} else if (printToRight && printToTop) {
							offsetPoint = new Point2D.Double(offsetPoint.getX()
									+ xRadius * offsetSquareReductionFactor
									* clusterXInformationPrintingOffset,
									offsetPoint.getY() - yRadius
											* offsetSquareReductionFactor
											* clusterYInformationPrintingOffset);
						} else if (printToRight && printToBottom) {
							offsetPoint = new Point2D.Double(offsetPoint.getX()
									+ xRadius * offsetSquareReductionFactor
									* clusterXInformationPrintingOffset,
									offsetPoint.getY() + yRadius
											* offsetSquareReductionFactor
											* clusterYInformationPrintingOffset);
						} else {
							// check individual offset
							if (printToLeft) {
								offsetPoint = new Point2D.Double(
										offsetPoint.getX()
												- xRadius
												* clusterXInformationPrintingOffset,
										offsetPoint.getY());
							} else if (printToRight) {
								offsetPoint = new Point2D.Double(
										offsetPoint.getX()
												+ xRadius
												* clusterXInformationPrintingOffset,
										offsetPoint.getY());
							} else if (printToTop) {
								// print a bit higher when printing on top
								// because of text length (e.g. multiline)
								// eating into distance
								offsetPoint = new Point2D.Double(
										offsetPoint.getX(),
										offsetPoint.getY()
												- yRadius
												* clusterYInformationPrintingOffset);
							} else if (printToBottom) {
								offsetPoint = new Point2D.Double(
										offsetPoint.getX(),
										offsetPoint.getY()
												+ yRadius
												* clusterYInformationPrintingOffset);
							}
						}
						// System.out.println("Printing position: " +
						// offsetPoint);
						// }
						// finally print it
						glassPane.paintText(new TextShape(
										offsetPoint,
										clusterInformationHandler
												.getClusterStatsAsString(clusterVerticesList),
										clusterStatsFont, Color.black));
					}

					// add color to cluster for drawing in textarea
					clusterColorMap.put(clusterVerticesList, clusterColor);

				}

			}
			// inform all listeners about first level clustering results
			callClusterListeners(clusterColorMap, totalNumberOfClusteredAgents);
			if (purgeUnusedColors) {
				// purging unused colors from index
				if (clusterColorIndex.size() != usedColors.size()) {
					clusterColorIndex = new HashMap<V, Color>(usedColors);
				}
				usedColors.clear();
			}
			if (clusterSecondLevel) {
				attractionClusterer.clearAttractionValues();
				// System.out.println("next round");
			}
			// System.out.println("Clustered vertices: " + clusteredVertices +
			// ", unclustered: " + Math.rint(new
			// Float(vertices.size()-clusteredVertices)));
		}
		// print highlighted tags references in second box instead of clustering
		// information
		if (highlightTags && !clusterSecondLevel) {
			callHighlightedTagColorsToListeners();
		} else if (!clusteringOfVertices && !clusterSecondLevel) {
			// update tag distribution if no clustering whatsoever and no
			// highlighting
			callTagDistributionListeners();
		}
		//System.out.println("Round done.");
	}

	float projectNumberOfColors = 60;
	float interval = 360 / projectNumberOfColors;
	int colorIndex = 0;
	boolean useHSL = false;

	private Color getNewColor() {
		/*
		 * float interval = 360 / (projectNumberOfColors); for (float x = 0; x <
		 * 360; x += interval){ Color c = Color.getHSBColor(x / 360, 1, 1); }
		 */
		Color c = null;
		if (useHSL) {
			c = Color.getHSBColor(colorIndex * interval / 360f, 1, 1);
			colorIndex++;
			return c;
		} else {
			float r = random.nextFloat() + 0.5f;
			float g = random.nextFloat() + 0.5f;
			float b = random.nextFloat() + 0.5f;
			try {
				c = new Color(r, g, b, clusterColorAlpha);
			} catch (IllegalArgumentException e) {
				r -= 0.5f;
				g -= 0.5f;
				b -= 0.5f;
				c = new Color(r, g, b, clusterColorAlpha);
			}
		}
		return c;
	}

	float clusterColorAlpha = 0.2f;
	Random random = new Random();
	HashMap<V, Color> clusterColorIndex = new HashMap<V, Color>();
	HashMap<V, Color> usedColors = new HashMap<V, Color>();

	private void initializeGlassPane() {
		if (glassPane == null) {
			if (sim.graphHandler.hasGraph(ForceGraphInitializer.FORCES_GRAPH)) {
				// System.out.println("Initialized GlassPane");
				glassPane = (ForceGlassPane) sim.graphHandler
						.getGraphInitializer(ForceGraphInitializer.FORCES_GRAPH)
						.getGlassPane();
				// System.out.println("GlassPane: " + glassPane);
			} else {
				// commented to allow initial spacing out of agents before
				// simulation start
				// throw new
				// RuntimeException("Graph is not registered but GlassPane is called - should never happen!");
			}
		}
	}

	/** Alpha value for links between different vertices on spheres */
	private int linkColorAlpha = 100;

	/** VALIDATION STUFF */

	/**
	 * map holding difference between desired and actual distances between
	 * agents
	 */
	private HashMap<String, Double> actualVsDesiredDistance = new HashMap<String, Double>();

	/**
	 * map holding difference between desired and actual distances between
	 * agents that are OUTSIDE of private zones
	 */
	private HashMap<String, Double> actualVsDesiredDistanceOutsidePrivateZone = new HashMap<String, Double>();

	/**
	 * map holding difference between desired and actual distances between
	 * agents as ratio (relative to 'correct distance')
	 */
	private HashMap<String, Double> distanceDeviationRatio = new HashMap<String, Double>();

	/**
	 * map holding difference between desired and actual distances between
	 * agents that are OUTSIDE of private zones as ratio (relative to 'correct
	 * distance')
	 */
	private HashMap<String, Double> distanceDeviationRatioOutsidePrivateZones = new HashMap<String, Double>();

	/**
	 * Returns the difference between actual and desired grid distance of
	 * individuals. Returns empty map if validateDistances is deactivated.
	 * 
	 * @return
	 */
	public HashMap<String, Double> getActualVsDesiredDistances() {
		return this.actualVsDesiredDistance;
	}

	/**
	 * Returns the difference between actual and desired grid distance of
	 * individuals that are OUTSIDE private zones. Returns empty map if
	 * validateDistances is deactivated.
	 * 
	 * @return
	 */
	public HashMap<String, Double> getActualVsDesiredDistancesOutsidePrivateZones() {
		return this.actualVsDesiredDistanceOutsidePrivateZone;
	}

	/**
	 * Returns the error/correct distance ratio for all agents.
	 * 
	 * @return
	 */
	public HashMap<String, Double> getDistanceDeviationsRatio() {
		return this.distanceDeviationRatio;
	}

	/**
	 * Returns the error/correct distance ratio for all agents OUTSIDE private
	 * zones.
	 * 
	 * @return
	 */
	public HashMap<String, Double> getDistanceDeviationsRatioOutsidePrivateZones() {
		return this.distanceDeviationRatioOutsidePrivateZones;
	}

	@SimulationParam
	/** Indicates if force distance deviation based on natural repulsion and other confounding factors.
	 *  Results are accessible on a per-round basis using getActualVsDesiredDistances(). */
	public final boolean validateDistances = false;

	@SimulationParam
	/** indicates if validation of distances happens on compound force level (calculation of total force on individual) 
	 *  using the compound vector direction (i.e. differences on individual level can cancel each other out -> vector addition),
	 *  by accumulation of all individual force distance deviations per agent (without potential to cancel each other out -> distance value addition).
	 */
	public final boolean validateDistanceOnCompoundForceLevel = true;

	/**
	 * holds sum of individual forces for currently iterated vertex in x
	 * direction for validation purposes (without natural repulsion)
	 */
	private Double validationXForce = 0.0;
	/**
	 * holds sum of individual forces for currently iterated vertex in y
	 * direction for validation purposes (without natural repulsion)
	 */
	private Double validationYForce = 0.0;
	/**
	 * holds sum of individual forces for currently iterated vertex in z
	 * direction for validation purposes (without natural repulsion)
	 */
	private Double validationZForce = 0.0;

	/**
	 * holds sum of individual forces for currently iterated vertex in x
	 * direction for validation purposes (without natural repulsion) for
	 * individuals that are OUTSIDE of private zones
	 */
	private Double validationXForceOutsidePrivateZones = 0.0;
	/**
	 * holds sum of individual forces for currently iterated vertex in y
	 * direction for validation purposes (without natural repulsion) for
	 * individuals that are OUTSIDE of private zones
	 */
	private Double validationYForceOutsidePrivateZones = 0.0;
	/**
	 * holds sum of individual forces for currently iterated vertex in z
	 * direction for validation purposes (without natural repulsion) for
	 * individuals that are OUTSIDE of private zones
	 */
	private Double validationZForceOutsidePrivateZones = 0.0;

	/** cache holding all registered 3D edges to be printed in visualizer */
	private HashMap<String, Edge> edgePrintCache = new HashMap<String, Edge>();

	/**
	 * Generate unique to identifier printed edge.
	 * 
	 * @param vertexOne
	 * @param vertexTwo
	 * @param plane
	 * @return
	 */
	private String generateEdgeId(V vertexOne, V vertexTwo, String plane) {
		HashSet<String> tempSet = new HashSet<String>();
		tempSet.add(vertexOne.toString());
		tempSet.add(vertexTwo.toString());
		return new StringBuilder(tempSet.toString()).append(plane).toString();
	}

	/**
	 * Add new edge for 3D visualizer
	 * 
	 * @param edgeId
	 * @param edge
	 */
	private void addEdge(String edgeId, Edge edge) {
		if (!edgePrintCache.containsKey(edgeId)) {
			edgePrintCache.put(edgeId, edge);
			sim.getEdges().addEdge(edge);
		}
		// System.out.println("Added edge for " + vertexOne.toString());
	}

	/**
	 * Remove edge from 3D edge registry (and visualizer)
	 * 
	 * @param edgeId
	 */
	private void removeEdge(String edgeId) {
		try {
			sim.getEdges().removeEdge(edgePrintCache.remove(edgeId));
		} catch (NullPointerException e) {
			System.out.println("NPE when trying to remove edge from 3D graph: "
					+ edgeId);
		} catch (IndexOutOfBoundsException ex){
			System.out.println("IndexOutOfBounds when trying to remove edge from 3D graph: " + edgeId);
		}
	}

	/**
	 * Clear all edges that have a specified String in their key (agent name or
	 * plane)
	 * 
	 * @param nodeName
	 */
	public void clearEdgesForNode(String nodeName) {
		ArrayList<String> keysForEdges = new ArrayList<String>(
				edgePrintCache.keySet());
		for (int i = 0; i < keysForEdges.size(); i++) {
			if (keysForEdges.get(i).contains(nodeName)) {
				// System.out.println("Cleared registry for key component " +
				// nodeName + ": " + keysForEdges.get(i));
				removeEdge(keysForEdges.get(i));
			}
		}
	}

	/**
	 * Removes all edges from 3D visualizer (fast).
	 */
	public void clearAllEdges() {
		// removing all nodes will remove all edges
		sim.getEdges().removeAllNodes();
	}

	/** indicator if agent is inside private zone of other agent(s) */
	private boolean insidePrivateZone = false;

	/**
	 * Calculates force between two vertices (and their respective points). The
	 * result is added to the xForce and yForce for vertexOne used to calculate
	 * the overall resultant force once all individual forces have been
	 * calculated.
	 * 
	 * @param vertexOne
	 * @param onePoint
	 * @param vertexTwo
	 * @param twoPoint
	 */
	private void calculateForceBetween(V vertexOne, Point3d onePoint,
			V vertexTwo, Point3d twoPoint) {
		if (!vertexOne.equals(vertexTwo)) {
			// System.out.println("Calculating force between " + vertexOne +
			// " and " + vertexTwo);
			// acts in favour of attraction and thus results in clumping if
			// increased

			Double force = 0.0;

			// current distance between the two vertices
			DirectionVector3D distVector = dirCalc.calculateGridDistance(
					onePoint, twoPoint, toroidal);
			double dist = distVector.getLength();

			// System.out.println("Max. perception distance: " +
			// maximalPerceptionDistance);
			// System.out.println("Distance: " + dist);

			// only calculate if in perception range (to avoid unnecessary
			// calculations and 'artifact-effect' in case of
			// distance-insensitive spheres)

			float tempForce = 0.0f;
			// System.out.println("planes size: " + planes.size());
			if (debug && (writeDebugToIO || writeDebugToVertexOutputListeners)) {
				if (detailDebugBuffer == null || detailDebugBuffer.length() > 0) {
					detailDebugBuffer = new StringBuffer();
				}
				if (debugForVertexActivated(vertexOne)) {
					debugBuffer.append("ME: ").append(vertexOne)
							.append(": Am in position: ").append(onePoint)
							.append(", temp. xForce: ").append(xForce)
							.append(", temp. yForce: ").append(yForce)
							.append(", temp. zForce: ").append(zForce)
							.append(", temp. Force: ").append(overallForce)
							.append(LINE_SEPARATOR);
					debugBuffer.append("Distance Vector: ").append(distVector)
							.append(LINE_SEPARATOR);
				}
			}
			// plane calculations for both perceptionally constrained planes and
			// unconstrained ones
			for (SocialPlane plane : planes.values()) {
				tempForce = 0.0f;
				// calculate force only if distance between vertices greater
				// than minimal distance
				if (dist > toleranceZone) {
					// if the perception distance and the perception on the
					// plane is limited and no distance independence is
					// activated
					if (plane.perceptionallyConstrained()
							&& maximalPerceptionDistance != -1
							&& !makeAllPlanesDistanceIndependent) {
						// then only calculate force if the distance is smaller
						// than max perceivable distance
						if (dist <= maximalPerceptionDistance) {
							// other vertex INSIDE perception range
							tempForce += plane.getForceTowards(vertexOne,
									vertexTwo, dist);
						}
						// else do nothing, as other vertex is outside of
						// perception range on this plane
					} else {
						/*
						 * else: - the plane might be distance-dependent (or
						 * not), but either the distance is unlimited, or
						 * distance-independence is activated for all planes, so
						 * the force is calculated
						 */
						tempForce += plane.getForceTowards(vertexOne,
								vertexTwo, dist);
					}
					/*
					 * if(plane.perceptionallyConstrained() &&
					 * maximalPerceptionDistance != -1 &&
					 * maximalPerceptionDistance <= dist &&
					 * !useDistanceIndependenceInPlanes){ //|| maximalDistance
					 * == -1){ //if plane has limited perception range and
					 * global distance-independence override is not activated
					 * if(dist <= maximalPerceptionDistance || maximalDistance
					 * == -1){ //other vertex INSIDE perception range
					 * //System.out
					 * .println("Calculating constrained perception: smaller " +
					 * (dist < maximalPerceptionDistance)); tempForce +=
					 * plane.getForceTowards(vertexOne, vertexTwo, dist); } else
					 * { //other vertex OUTSIDE perception range
					 * //System.out.println("Distance too far: " + dist); } }
					 * else { tempForce += plane.getForceTowards(vertexOne,
					 * vertexTwo, dist); }
					 */
					if (debug
							&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
						detailDebugBuffer.append("   Attraction on ")
								.append(plane.getName()).append(": ")
								.append(tempForce);
					}
				} else {
					// distance too small - vertices are inside each other's
					// private zone
					if (debug
							&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
						detailDebugBuffer.append("   Attraction on ")
								.append(plane.getName()).append(": ")
								.append("0 - too close");
					}
				}
				// include distance depended amplification to increase
				// convergence for distant attracted individuals.
				if (amplifyAttractionForDistantVertices) {
					if (tempForce > 0 && dist > toleranceZone) {
						// square attraction for distance if outside tolerance
						// zone
						tempForce *= Math
								.pow(dist,
										amplificationPowerForDistantDependentAttraction);
						if (debug
								&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
							detailDebugBuffer
									.append(" -> distance-dependent amplification with ")
									.append(Math
											.pow(dist,
													amplificationPowerForDistantDependentAttraction))
									.append(": ").append(tempForce);
						}
					}
				}

				// if force has been changed by this sphere, multiply with
				// weight factor
				if (tempForce != 0.0f) {
					// individual weight factor from registered agents
					if (useIndividualWeights) {
						// get weight for plane
						Float idvPlaneWeight = requestWeightForPlaneFromProvider(
								vertexOne, plane.getName()); 
						//only consider if weight specified by individual (i.e. not null)
						if(idvPlaneWeight != null){
							tempForce *= idvPlaneWeight;
						}
						if (debug
								&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
							detailDebugBuffer
									.append(" -> indiv. weighing with ")
									.append(requestWeightForPlaneFromProvider(
											vertexOne, plane.getName()))
									.append(": ").append(tempForce);
						}
					}
					// global weight factor
					tempForce *= plane.weightFactor;
					if (debug
							&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
						detailDebugBuffer.append(" -> global weighing with ")
								.append(plane.weightFactor).append(": ")
								.append(tempForce);
					}
					// add force for this plane (after weighing) to sum of all
					// planes
					force += tempForce;
					// System.out.println("Multiplied with " +
					// sphere.weightFactor);
				}
				if (debug
						&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
					detailDebugBuffer.append(LINE_SEPARATOR);
				}

				if (drawOnGlassPane) {
					initializeGlassPane();
					// generate edge key to check for 3D representation
					String edgeId = null;
					if (use3d && print3dLines) {
						edgeId = generateEdgeId(vertexOne, vertexTwo,
								plane.getName());
					}
					// System.out.println("Plane: " + plane +
					// plane.linkDrawingEnabled());
					// draw lines between entities if attraction between them
					if (plane.linkDrawingEnabled()
							&& (print2dLines || (use3d && print3dLines))
							&& ((tempForce != 0.0) || !plane.isEnabled())) {
						if (highlightedIndividual == null
								|| (highlightedIndividual.equals(vertexOne) && !individualsToBeGreyedOut
										.contains(vertexTwo.toString()))) {
							// check for highlighted individual links only -
							// else print for everybody
							if (print2dLines) {
								Color col = plane
										.getColor(vertexOne, vertexTwo);
								if (col != null) {
									col = new Color(col.getRed(),
											col.getGreen(), col.getBlue(),
											linkColorAlpha);
									// LineShape lShape = new LineShape(0, new
									// Point2D.Float((float)onePoint.x,
									// (float)onePoint.y), new
									// Point2D.Float((float)twoPoint.x,
									// (float)twoPoint.y), true, col);
									// System.out.println("Print to line.");
									glassPane.paintLine(new LineShape(0,
											new Point2D.Float(
													(float) onePoint.x,
													(float) onePoint.y),
											new Point2D.Float(
													(float) twoPoint.x,
													(float) twoPoint.y), true,
											col, socialPlaneLineStroke));
								}
							}
							if (use3d && print3dLines) {
								// System.out.println("Printing 3d lines " +
								// print3dLines);
								// add edge (between vertexOne and vertexTwo) if
								// not already existing
								// if(!edgePrintCache.containsKey(edgeId)){
								addEdge(edgeId, new Edge(vertexOne, vertexTwo,
										null));
								// }
							}
						} else {
							if (use3d && print3dLines
									&& !highlightedIndividual.equals(vertexOne)
									&& !highlightedIndividual.equals(vertexTwo)) {
								// remove edge (if exists) - but check that no
								// highlighted individual is involved (one line
								// per relationship, not multiple like in 2D)
								// System.out.println("Trying to remove edge " +
								// edgeId);
								removeEdge(edgeId);
							}
						}
					} else {// if(!plane.linkDrawingEnabled()){
						// remove edge if no attraction on that plane
						if (use3d) {
							if (print3dLines) {
								// edge removal will be done by UI switches
								// remove edge (if exists)
								removeEdge(edgeId);
							}
						}
					}
				}
				if (highlightTags && !clusterSecondLevel) {
					if (plane.getClass().getSuperclass()
							.equals(TagSocialPlane.class)
							&& !((TagSocialPlane) plane).getTags(vertexOne)
									.isEmpty()) {
						Color color = null;
						// returns stringified collection - converted to set
						// first to ensure unified tags
						String tagKey = new HashSet<V>(
								((Collection<V>) ((TagSocialPlane) plane)
										.getTags(vertexOne))).toString();
						if (tagColors.containsKey(tagKey)) {
							// if already color assigned, take
							color = tagColors.get(tagKey);
						} else {
							// generate new one for that tag combination
							color = secClusterColors.get(tagColors.size());
							tagColors.put(tagKey, color);

						}
						// refine color with alpha value
						// System.out.println("Apl: " +
						// color.getTransparency());
						color = new Color(color.getRed(), color.getGreen(),
								color.getBlue(), secondaryAndTagColorAlpha);
						// System.out.println("Should print something in " +
						// color);
						glassPane.paintCircle(new CircleShape3D(onePoint,
								(int) maxClusterNeighbourDistance, true, color)
								.toCircleShape());
					}
				}
				if (clusterSecondLevel) {
					if (tempForce != 0.0f) {
						// saves attraction value for particular agent-plane
						// combination for later clustering
						attractionClusterer.addAttractionValue(vertexOne,
								plane.getName(), (float) tempForce);
					}
				}
			} // END Plane iterations

			// if no direct attraction exists, grey out the target individual
			if (force == 0.0 && highlightedIndividual != null
					&& highlightedIndividual.equals(vertexOne)) {
				individualsToBeGreyedOut.add(vertexTwo.toString());
			}

			// store force value before inclusion of repulsion
			double forceWithoutNaturalRepulsion = force;

			// Calculation of distance-based Attraction/Repulsion measures

			// if transition through private zones is allowed, do not calculate
			if (!noRepulsionWhenTransiting) {
				if (debug
						&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
					detailDebugBuffer.append("   Repulsion");
					// is continued in repulsion calculation methods
				}

				// do calculate if transit is NOT allowed
				force = calculateRepulsion(force, dist);
				if (debug
						&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
					detailDebugBuffer.append(LINE_SEPARATOR);
				}
			} else {
				// determine which vertices are within privacy zone
				if (dist < minimalDistance) {
					verticesInPrivateZone.put(vertexTwo,
							new ForceDistanceContainer(distVector, force));
				}
			}

			// add force to vector forces
			Point3d convertedForceCoords = distVector
					.convertAngleToCartesianScreenCoordinates();
			xForce += convertedForceCoords.x * force;
			yForce += convertedForceCoords.y * force;
			zForce += convertedForceCoords.z * force;
			overallForce += force;

			// Validation of force strength (without natural repulsion)
			if (validateDistances) {
				Point3d convertedNonRepulsingForceCoords = convertedForceCoords;
				// calculate the total 'correct force' in any way (for
				// percentage calculation later on)
				validationXForce += convertedNonRepulsingForceCoords.x
						* forceWithoutNaturalRepulsion;
				validationYForce += convertedNonRepulsingForceCoords.y
						* forceWithoutNaturalRepulsion;
				validationZForce += convertedNonRepulsingForceCoords.z
						* forceWithoutNaturalRepulsion;
				if (!insidePrivateZone) {
					validationXForceOutsidePrivateZones += convertedNonRepulsingForceCoords.x
							* forceWithoutNaturalRepulsion;
					validationYForceOutsidePrivateZones += convertedNonRepulsingForceCoords.y
							* forceWithoutNaturalRepulsion;
					validationZForceOutsidePrivateZones += convertedNonRepulsingForceCoords.z
							* forceWithoutNaturalRepulsion;
				}
				if (!validateDistanceOnCompoundForceLevel) {
					convertedNonRepulsingForceCoords.x = convertedNonRepulsingForceCoords.x
							* forceWithoutNaturalRepulsion;
					convertedNonRepulsingForceCoords.y = convertedNonRepulsingForceCoords.y
							* forceWithoutNaturalRepulsion;
					convertedNonRepulsingForceCoords.z = convertedNonRepulsingForceCoords.z
							* forceWithoutNaturalRepulsion;
					// calculate resulting coordinates INCLUDING repulsion
					Point3d repulsingForceCoords = new Point3d(
							convertedForceCoords.x * force,
							convertedForceCoords.y * force,
							convertedForceCoords.z * force);

					/*
					 * value for all deviations for that agent (sum of deviating
					 * distances to all other agents). Thus distances cannot
					 * cancel each other (based on the direction) as in the
					 * compound version (validateDistanceOnCompoundForceLevel ->
					 * true).
					 */
					Double difference = dirCalc.calculateGridDistance(
							repulsingForceCoords,
							convertedNonRepulsingForceCoords).getLength();
					if (!insidePrivateZone) {
						// only for agents that are not in private zones
						if (actualVsDesiredDistanceOutsidePrivateZone
								.containsKey(vertexOne.toString())) {
							actualVsDesiredDistanceOutsidePrivateZone.put(
									vertexOne.toString(),
									actualVsDesiredDistanceOutsidePrivateZone
											.get(vertexOne.toString())
											+ difference);
						} else {
							actualVsDesiredDistanceOutsidePrivateZone.put(
									vertexOne.toString(), difference);
						}
					}
					if (actualVsDesiredDistance.containsKey(vertexOne
							.toString())) {
						actualVsDesiredDistance.put(
								vertexOne.toString(),
								actualVsDesiredDistance.get(vertexOne
										.toString()) + difference);
					} else {
						actualVsDesiredDistance.put(vertexOne.toString(),
								difference);
					}
					// percentage will be calculated at round end (in step()
					// method)
				}
			}

			// have force amount at this point
			if (debug
					&& (writeDebugToIO || (writeDebugToVertexOutputListeners && debugForVertexActivated(vertexOne)))) {
				// debugBuffer = new StringBuffer();
				debugBuffer.append("---TARGET: ").append(vertexTwo)
						.append(": Others pos: ").append(twoPoint)
						.append(" ---- my position: ").append(onePoint)
						.append(LINE_SEPARATOR);
				debugBuffer.append("Distance: ").append(dist)
						.append(LINE_SEPARATOR);
				debugBuffer.append("   -- Forces on individual planes").append(
						LINE_SEPARATOR);
				// add details collected for individual planes
				debugBuffer.append(detailDebugBuffer);
				debugBuffer.append("   --").append(LINE_SEPARATOR);
				debugBuffer.append("Force: ").append(force)
						.append(LINE_SEPARATOR);
				double angleXY = distVector.getXYAngle();
				double angleXZ = distVector.getXZAngle();
				debugBuffer.append("Radians XY: ").append(angleXY);
				debugBuffer.append(" - Degree: ")
						.append(distVector.getXYAngleInDegrees())
						.append(LINE_SEPARATOR);
				debugBuffer.append("Radians XZ: ").append(angleXZ);
				debugBuffer.append(" - Degree: ")
						.append(distVector.getXZAngleInDegrees())
						.append(LINE_SEPARATOR);
				debugBuffer.append("Converted Force Coordinate: ")
						.append(convertedForceCoords).append(LINE_SEPARATOR);
				debugBuffer.append("X force addition: ")
						.append(convertedForceCoords.x * force)
						.append(", xForce accumulated: ").append(xForce)
						.append(LINE_SEPARATOR);
				debugBuffer.append("Y force addition: ")
						.append(convertedForceCoords.y * force)
						.append(", yForce accumulated: ").append(yForce)
						.append(LINE_SEPARATOR);
				debugBuffer.append("Z force addition: ")
						.append(convertedForceCoords.z * force)
						.append(", zForce accumulated: ").append(zForce)
						.append(LINE_SEPARATOR);
				debugBuffer.append("-------IDV FORCE to/from ")
						.append(vertexTwo).append(" END-------")
						.append(LINE_SEPARATOR);
				/*
				 * if(writeDebugToOutfileInsteadConsole){
				 * writer.write(debugBuffer); } else {
				 * System.out.print(debugBuffer); }
				 */
			}

			// at this stage, we have total accumulated force up to vertexTwo
			// (in xForce, yForce and zForce)

		}
	}

	/**
	 * This method calculates the repulsion for a given vertex and adds it to
	 * the given force value.
	 * 
	 * @param force
	 *            strength of force between two vertices
	 * @param distance
	 *            distance between both vertices
	 * @return
	 */
	private double calculateRepulsion(double force, double distance) {
		double netDistance = distance - minimalDistance;
		if (netDistance <= 0) {
			// System.out.println("inside private zone");
			insidePrivateZone = true;
		}
		if (force > 0) {
			// force *= Math.max(netDistance, 0);
			// in-group repulsion if too close
			force += calcIngroupRepulsion(netDistance);
			// System.out.println("Calculated ingroup repulsion");
		} else {
			// out-group repulsion
			force += calcOutgroupRepulsion(netDistance);
			// System.out.println("Calculated outgroup repulsion: " +
			// calcOutgroupRepulsion(netDistance));
		}
		return force;
	}

	/**
	 * This method calculates the repulsion for a vertex, given its distance and
	 * indication if it is a positive force (different repulsion for positive
	 * and negative attraction) -- ONLY USED IF TRANSIT THROUGH PRIVATE ZONES IS
	 * ACTIVATED TO IDENTIFY ONLY REPULSION FROM ULTIMATE TARGET!
	 * 
	 * @param positiveForce
	 *            boolean indicating if force is positive
	 * @param distance
	 *            distance between both vertices
	 * @return
	 */
	/*
	 * private double calculateRepulsion(boolean positiveForce, double
	 * distance){ double netDistance = distance - minimalDistance; double force
	 * = 0.0; //REPULSION DESPITE ATTRACTION (IF TOO CLOSE) if(positiveForce){
	 * //force *= Math.max(netDistance, 0); //in-group repulsion if too close
	 * force += calcIngroupRepulsion(netDistance); } else { //FURTHER REPULSION
	 * DESPITE ALREADY EXISTING REPULSION //out-group repulsion force +=
	 * calcOutgroupRepulsion(netDistance); } return force; }
	 */

	private double calcIngroupRepulsion(double netDistance) {
		if (activateOutGroupRepulsion) {
			// INSIDE PRIVATE ZONE
			if (netDistance < 0) {
				// only write debug if detail buffer is somewhat filled (i.e.
				// activated)
				if (debug && detailDebugBuffer.length() > 0) {
					detailDebugBuffer.append(" - InGroup: ").append(
							(Math.pow(2, Math.abs(netDistance)) * -1));
				}
				// linear repulsion if inside private zone
				return (Math.pow(2, Math.abs(netDistance)) * -1);
			}
			// OUTSIDE PRIVATE ZONE
			// no repulsion if outside private zone
		}
		return 0.0;
	}

	private double calcOutgroupRepulsion(double netDistance) {
		if (activateOutGroupRepulsion) {
			// INSIDE PRIVATE ZONE
			if (netDistance < 0) {
				// only write debug if detail buffer is somewhat filled (i.e.
				// activated)
				if (debug && detailDebugBuffer.length() > 0) {
					detailDebugBuffer
							.append(" - OutGroup (IN intimate zone): ").append(
									(Math.pow(3, Math.abs(netDistance)) * -1));
				}
				// logarithmic repulsion if inside private zone
				return (Math.pow(3, Math.abs(netDistance)) * -1);
			}
			// OUTSIDE PRIVATE ZONE
			if (maximalRepulsionDistance == -1
					|| netDistance <= maximalRepulsionDistance) {
				if (outGroupRepulsion == 0.0) {
					if (debug && detailDebugBuffer.length() > 0) {
						detailDebugBuffer.append(
								" - OutGroup (OUTSIDE intimate zone): ")
								.append(outGroupRepulsion);
					}
					return outGroupRepulsion;
				}
				// only write debug if detail buffer is somewhat filled (i.e.
				// activated)
				if (debug && detailDebugBuffer.length() > 0) {
					detailDebugBuffer.append(
							" - OutGroup (OUTSIDE intimate zone): ").append(
							Math.pow(outGroupRepulsion, 3)
									/ Math.max(Math.pow(netDistance, 1.2), 1)
									* -1);
				}
				// linear repulsion if outside private zone
				// netDistance is > 0, else it would be inside privacy zone
				// (handled above)
				return Math.pow(outGroupRepulsion, 3)
						/ Math.max(Math.pow(netDistance, 1.2), 1) * -1;
			}
		}
		// OUTSIDE MAX. REPULSION DISTANCE
		return 0.0;
	}

	/*
	 * double normaliseBearing(double ang) { if (ang > Math.PI) ang -=
	 * 2*Math.PI; if (ang < -Math.PI) ang += 2*Math.PI; return ang; }
	 * 
	 * public double absbearing( double x1,double y1, double x2,double y2 ) {
	 * double xo = x2-x1; double yo = y2-y1; double h = 0;//getRange( x1,y1,
	 * x2,y2 ); if( xo > 0 && yo > 0 ) { return Math.asin( xo / h ); } if( xo >
	 * 0 && yo < 0 ) { return Math.PI - Math.asin( xo / h ); } if( xo < 0 && yo
	 * < 0 ) { return Math.PI + Math.asin( -xo / h ); } if( xo < 0 && yo > 0 ) {
	 * return 2.0*Math.PI - Math.asin( -xo / h ); } return 0; }
	 */

	/*
	 * public void calcNewPosition(Point2D one, Point2D two, Double force){
	 * Double dX = one.getX() - two.getX(); Double dY = one.getY() - two.getY();
	 * 
	 * dX *= damping; dY *= damping; transformer.feedback(key, val) }
	 */

	private ArrayList<SocialPlane> planeInitializers = new ArrayList<SocialPlane>();

	/**
	 * Registers all social planes specified in the passed initializer with the
	 * Force graph.
	 * 
	 * @param initializer
	 */
	public void registerPlanesFromSocialPlaneInitializer(
			SocialPlaneInitializer initializer) {
		if (initializer != null) {
			Collection<SocialPlane> planes = initializer.getSocialPlanes(this);
			System.out.println(prefix + "Registered social planes " + planes);
			this.planeInitializers.addAll(planes);
			System.out.println(prefix + "Have " + this.planeInitializers.size()
					+ " planes.");
		}
	}

	@Override
	public void initialize() {
		printSofoSimOutput("Initialize in ForceDirLayout called.");
		// System.out.println(Thread.currentThread().getStackTrace()[2]);
		// System.out.println(Thread.currentThread().getStackTrace()[3]);
		/*
		 * if(dirCalc == null){ dirCalc = new ProximityCalculator(xGridSize,
		 * yGridSize, toroidal, sim.random); }
		 */

		if (this.planeInitializers.isEmpty()) {
			throw new RuntimeException("No Social Planes specified!");
		}
		for (int i = 0; i < this.planeInitializers.size(); i++) {
			SocialPlane plane = (SocialPlane) this.planeInitializers.get(i);
			// avoid problems with repeated calls to initialize
			if (!planes.containsKey(plane.getName())) {
				planes.put(plane.getName(), plane);
			}
		}
	}

	private void initializeSectors() {
		// initialize();
		int numberOfXSectors = new Double(new Double(xGridSize) / xSizeOfSector)
				.intValue();
		int numberOfYSectors = new Double(new Double(yGridSize) / ySizeOfSector)
				.intValue();
		if (new Double(xGridSize) % xSizeOfSector > 0) {
			numberOfXSectors++;
			System.err.println("X size of sector (" + xSizeOfSector
					+ ") is not optimal for screen size " + xGridSize);
		}
		if (new Double(yGridSize) % ySizeOfSector > 0) {
			numberOfYSectors++;
			System.err.println("Y size of sector (" + ySizeOfSector
					+ ") is not optimal for screen size " + yGridSize);
		}
		// set sector scale
		sectorScale = numberOfXSectors;
		if (use3d) {
			int numberOfZSectors = new Double(new Double(zGridSize)
					/ zSizeOfSector).intValue();
			if (new Double(zGridSize) % zSizeOfSector > 0) {
				numberOfZSectors++;
				System.err.println("Z size of sector (" + zSizeOfSector
						+ ") is not optimal for screen size " + zGridSize);
			}
			if (numberOfXSectors != numberOfYSectors
					|| numberOfXSectors != numberOfZSectors
					|| numberOfYSectors != numberOfZSectors) {
				throw new RuntimeException(
						"Non-symmetrical dimensions for grid - will skew calculations and outcome.");
			}
			System.out
					.println("Number of sectors: " + Math.pow(sectorScale, 3));
		} else {
			if (numberOfXSectors != numberOfYSectors) {
				throw new RuntimeException(
						"Non-symmetrical dimensions for grid - will skew calculations and outcome.");
			}
			System.out
					.println("Number of sectors: " + Math.pow(sectorScale, 2));
		}
		calculateVisibleSectors();
		// register GraphChangeListener to be informed about changed regarding
		// vertices
		sim.graphHandler.registerGraphChangeListener(this);
	}

	// public static ArrayList<Point2D> visibleSectors2D = new
	// ArrayList<Point2D>();
	public static ArrayList<Point3d> visibleSectors3D = new ArrayList<Point3d>();

	@SimulationParam
	// calculates the granularity of iteration steps in radius to determine
	// visible sectors (only used when sector-based calculation is activated)
	int granularityOfAnglesToDetermineVisibleSectors = 100;

	/**
	 * Calculates relative sectors visible from location 0,0 (using the given
	 * maximal perception distance) and saves them to visibleSectors.
	 */
	private void calculateVisibleSectors() {
		if (maximalPerceptionDistance == -1) {
			return;
		}
		if (use3d) {
			// add center itself
			visibleSectors3D.add(new Point3d(0, 0, 0));
			// get extreme distances
			double iterationSteps = Math.PI
					/ granularityOfAnglesToDetermineVisibleSectors;
			// create length vector with extended vision in order to capture
			// corners
			DirectionVector3D vec = new DirectionVector3D(
					maximalPerceptionDistance
							+ Math.sqrt(Math.pow(xSizeOfSector / 2, 2)
									+ Math.pow(ySizeOfSector / 2, 2)), 0, 0);

			// for each angle step on the z axis
			for (int m = -granularityOfAnglesToDetermineVisibleSectors; m < granularityOfAnglesToDetermineVisibleSectors; m++) {
				vec.setXZAngle(iterationSteps * m);
				// counterclockwise from -180 (left) via 0 to +180 degree (in
				// radians)
				for (int n = -granularityOfAnglesToDetermineVisibleSectors; n < granularityOfAnglesToDetermineVisibleSectors; n++) {
					vec.setXYAngle(iterationSteps * n);
					Point3d relativeDistance = null;
					Point3d targetSector = null;
					relativeDistance = vec
							.convertVectorToCartesianScreenCoordinates();
					// if the balance of the sector size is more than half of
					// the sector size, round up coordinate
					if (Math.abs(relativeDistance.x % xSizeOfSector) > xSizeOfSector / 2) {
						if (relativeDistance.x < 0) {
							relativeDistance.set(relativeDistance.x
									- xSizeOfSector / 2, relativeDistance.y,
									relativeDistance.z);
						} else {
							relativeDistance.set(relativeDistance.x
									+ xSizeOfSector / 2, relativeDistance.y,
									relativeDistance.z);
						}
					}
					if (Math.abs(relativeDistance.y % ySizeOfSector) > ySizeOfSector / 2) {
						if (relativeDistance.y < 0) {
							relativeDistance.set(relativeDistance.x,
									relativeDistance.y - ySizeOfSector / 2,
									relativeDistance.z);
						} else {
							relativeDistance.set(relativeDistance.x,
									relativeDistance.y + ySizeOfSector / 2,
									relativeDistance.z);
						}
					}
					if (Math.abs(relativeDistance.z % zSizeOfSector) > zSizeOfSector / 2) {
						if (relativeDistance.x < 0) {
							relativeDistance.set(relativeDistance.x,
									relativeDistance.y, relativeDistance.z
											- zSizeOfSector / 2);
						} else {
							relativeDistance.set(relativeDistance.x,
									relativeDistance.y, relativeDistance.z
											+ zSizeOfSector / 2);
						}
					}
					targetSector = convertCartesianCoordinateToSectorCoordinate(relativeDistance);
					if (targetSector != null
							&& !visibleSectors3D.contains(targetSector)) {
						visibleSectors3D.add(targetSector);
						// System.out.println("Relative coordinates: " +
						// relativeDistance);
						// System.out.println("Converted to sectorCoordinate: "
						// + targetSector);
					}
				}
			}
			// calculate visible sectors in between the extremes
			// find highest and lowest individual x's, y's and z's
			Double lowestX = null;
			Double lowestY = null;
			Double lowestZ = null;
			Double highestX = null;
			Double highestY = null;
			Double highestZ = null;
			for (Point3d inVis : visibleSectors3D) {
				if (lowestX == null) {
					lowestX = inVis.x;
				}
				if (lowestY == null) {
					lowestY = inVis.y;
				}
				if (lowestZ == null) {
					lowestZ = inVis.z;
				}
				if (highestX == null) {
					highestX = inVis.x;
				}
				if (highestY == null) {
					highestY = inVis.y;
				}
				if (highestZ == null) {
					highestZ = inVis.z;
				}
				if (inVis.x < lowestX) {
					lowestX = inVis.x;
				}
				if (inVis.y < lowestY) {
					lowestY = inVis.y;
				}
				if (inVis.z < lowestZ) {
					lowestZ = inVis.z;
				}
				if (inVis.x > highestX) {
					highestX = inVis.x;
				}
				if (inVis.y > highestY) {
					highestY = inVis.y;
				}
				if (inVis.z > highestZ) {
					highestZ = inVis.z;
				}
			}
			// fill up coordinate gaps in visibleSectors
			while (lowestZ <= highestZ) {
				while (lowestY <= highestY) {
					Double tpLowestX = null;
					Double tpHighestX = null;
					for (Point3d inVis : visibleSectors3D) {
						/*
						 * comparison should be ok (despite being of type double) as 
						 * values have been assigned mutually previously.
						 * Reason for type double is that Point3d contains coordinate
						 * values of type double, not int. 
						 */
						if (inVis.z == lowestZ) {
							if (inVis.y == lowestY) {
								if (tpLowestX == null) {
									tpLowestX = inVis.x;
								}
								if (tpHighestX == null) {
									tpHighestX = inVis.x;
								}
								if (inVis.x < tpLowestX) {
									tpLowestX = inVis.x;
								}
								if (inVis.x > tpHighestX) {
									tpHighestX = inVis.x;
								}
							}
						}
					}
					// System.out.println("Filling up from " + tpLowestX +
					// " to " + tpHighestX + " for Y " + lowestY);
					for (int i = tpLowestX.intValue() + 1; i < tpHighestX
							.intValue(); i++) {
						Point3d tempPt = new Point3d(i, lowestY, lowestZ);
						if (!visibleSectors3D.contains(tempPt)) {
							visibleSectors3D.add(tempPt);
						}
					}
					lowestY++;
				}
				lowestZ++;
			}
		} else {
			// add center itself
			visibleSectors3D.add(new Point3d(0, 0, 0));
			// get extreme distances
			double iterationSteps = Math.PI
					/ granularityOfAnglesToDetermineVisibleSectors;
			// create length vector with extended vision in order to capture
			// corners
			DirectionVector3D vec = new DirectionVector3D(
					maximalPerceptionDistance
							+ Math.sqrt(Math.pow(xSizeOfSector / 2, 2)
									+ Math.pow(ySizeOfSector / 2, 2)), 0, 0);

			// counterclockwise from -180 (left) via 0 to +180 degree (in
			// radians)
			for (int n = -granularityOfAnglesToDetermineVisibleSectors; n < granularityOfAnglesToDetermineVisibleSectors; n++) {
				vec.setXYAngle(iterationSteps * n);
				Point3d relativeDistance = null;
				Point3d targetSector = null;
				relativeDistance = vec
						.convertVectorToCartesianScreenCoordinates();
				// if the balance of the sector size is more than half of the
				// sector size, round up coordinate
				if (Math.abs(relativeDistance.x % xSizeOfSector) > xSizeOfSector / 2) {
					if (relativeDistance.x < 0) {
						relativeDistance.set(relativeDistance.x - xSizeOfSector
								/ 2, relativeDistance.y, 0);
					} else {
						relativeDistance.set(relativeDistance.x + xSizeOfSector
								/ 2, relativeDistance.y, 0);
					}
				}
				if (Math.abs(relativeDistance.y % ySizeOfSector) > ySizeOfSector / 2) {
					if (relativeDistance.y < 0) {
						relativeDistance.set(relativeDistance.x,
								relativeDistance.y - ySizeOfSector / 2, 0);
					} else {
						relativeDistance.set(relativeDistance.x,
								relativeDistance.y + ySizeOfSector / 2, 0);
					}
				}

				targetSector = convertCartesianCoordinateToSectorCoordinate(relativeDistance);
				if (targetSector != null
						&& !visibleSectors3D.contains(targetSector)) {
					visibleSectors3D.add(targetSector);
					// System.out.println("Relative coordinates: " +
					// relativeDistance);
					// System.out.println("Converted to sectorCoordinate: " +
					// targetSector);
				}
			}
			// calculate visible sectors in between the extremes
			// find highest and lowest individual x's and y's
			Double lowestX = null;
			Double lowestY = null;
			Double highestX = null;
			Double highestY = null;
			for (Point3d inVis : visibleSectors3D) {
				if (lowestX == null) {
					lowestX = inVis.x;
				}
				if (lowestY == null) {
					lowestY = inVis.y;
				}
				if (highestX == null) {
					highestX = inVis.x;
				}
				if (highestY == null) {
					highestY = inVis.y;
				}
				if (inVis.x < lowestX) {
					lowestX = inVis.x;
				}
				if (inVis.y < lowestY) {
					lowestY = inVis.y;
				}
				if (inVis.x > highestX) {
					highestX = inVis.x;
				}
				if (inVis.y > highestY) {
					highestY = inVis.y;
				}
			}
			// fill up coordinate gaps in visibleSectors
			while (lowestY <= highestY) {
				Double tpLowestX = null;
				Double tpHighestX = null;
				for (Point3d inVis : visibleSectors3D) {
					if (inVis.y == lowestY) {
						if (tpLowestX == null) {
							tpLowestX = inVis.x;
						}
						if (tpHighestX == null) {
							tpHighestX = inVis.x;
						}
						if (inVis.x < tpLowestX) {
							tpLowestX = inVis.x;
						}
						if (inVis.x > tpHighestX) {
							tpHighestX = inVis.x;
						}
					}
				}
				// System.out.println("Filling up from " + tpLowestX + " to " +
				// tpHighestX + " for Y " + lowestY);
				for (int i = tpLowestX.intValue() + 1; i < tpHighestX
						.intValue(); i++) {
					Point3d tempPt = new Point3d(i, lowestY, 0);
					if (!visibleSectors3D.contains(tempPt)) {
						visibleSectors3D.add(tempPt);
					}
				}
				lowestY++;
			}
		}
		System.out.println("Calculated visible sectors: "
				+ visibleSectors3D.size() + ": " + visibleSectors3D);
	}

	private ArrayList<V> getVisibleVertices(Point3d ownLocation) {
		ArrayList<V> visibleVertices = new ArrayList<V>();
		Point3d ownSector = convertCartesianCoordinateToSectorCoordinate(ownLocation);
		try {
			// for(Point2D visibleSector: visibleSectors){
			for (int i = 0; i < visibleSectors3D.size(); i++) {
				Point3d visibleSector = visibleSectors3D.get(i);
				Point3d translatedSector = new Point3d(ownSector.x
						+ visibleSector.x, ownSector.y + visibleSector.y,
						ownSector.z + visibleSector.z);
				// System.out.print("Translated surrounding sector from " +
				// translatedSector);
				translatedSector = dirCalc.transposeTarget(translatedSector,
						sectorScale, toroidal);
				// System.out.println("to " + translatedSector);
				if (sectorMembers3D.containsKey(translatedSector)) {
					try {
						visibleVertices.addAll(sectorMembers3D
								.get(translatedSector));
					} catch (NegativeArraySizeException e) {
						return getVisibleVertices(ownLocation);
					}
				}
			}
		} catch (ConcurrentModificationException e) {
			// System.out.println("ConcurrentMod Exception: " +
			// visibleVertices);
			// return getVisibleVertices(ownLocation);
			return visibleVertices;
		}
		return visibleVertices;
	}

	/*
	 * private ArrayList<V> getVisibleVertices(Point2D ownLocation){
	 * ArrayList<V> visibleVertices = new ArrayList<V>(); Point2D ownSector =
	 * convertCartesianCoordinateToSectorCoordinate(ownLocation); try{
	 * //for(Point2D visibleSector: visibleSectors){ for(int i=0;
	 * i<visibleSectors2D.size(); i++){ Point2D visibleSector =
	 * visibleSectors2D.get(i); Point2D translatedSector = new
	 * Point2D.Double(ownSector.getX() + visibleSector.getX(), ownSector.getY()
	 * + visibleSector.getY());
	 * //System.out.print("Translated surrounding sector from " +
	 * translatedSector); translatedSector =
	 * dirCalc.transposeTarget(translatedSector, sectorScale, toroidal);
	 * //System.out.println("to " + translatedSector);
	 * if(sectorMembers2D.containsKey(translatedSector)){ try{
	 * visibleVertices.addAll(sectorMembers2D.get(translatedSector)); } catch
	 * (NegativeArraySizeException e){ return getVisibleVertices(ownLocation); }
	 * } } } catch(ConcurrentModificationException e){
	 * //System.out.println("ConcurrentMod Exception: " + visibleVertices);
	 * //return getVisibleVertices(ownLocation); return visibleVertices; }
	 * return visibleVertices; }
	 */

	private int calculateLineShapeId(V vertexOne, V vertexTwo, Color color) {
		HashSet<V> toSort = new HashSet<V>();
		toSort.add(vertexOne);
		toSort.add(vertexTwo);
		return toSort.hashCode() + color.getRGB();
	}

	/**
	 * Registers a vertex (with its new position) with the updated sector.
	 * 
	 * @param vertex
	 * @param newPosition
	 */
	private void registerVertexWithSectors(V vertex, Point3d newPosition) {
		Point3d lastPosition = null;
		// retrieve old position from history (and update)
		if (positionHistory.containsKey(vertex)) {
			lastPosition = positionHistory.get(vertex);
		}
		// System.out.println("Reregistered from position " + lastPosition +
		// " to " + newPosition);
		positionHistory.put(vertex, newPosition);
		// calculate new sector
		Point3d newSector = convertCartesianCoordinateToSectorCoordinate(newPosition);
		boolean registerNewOne = true;
		// unregister from last sector coordinate
		if (lastPosition != null) {
			Point3d oldSector = convertCartesianCoordinateToSectorCoordinate(lastPosition);
			if (!oldSector.equals(newSector)) {
				if (sectorMembers3D.containsKey(oldSector)) {
					sectorMembers3D.get(oldSector).remove(vertex);
					// System.out.println("Last coord. after removal of " +
					// vertex + ": " + sectorMembers3D.get(oldSector));
				}
			} else {
				registerNewOne = false;
			}
		}
		// register in new sector coordinate
		if (registerNewOne) {
			if (!sectorMembers3D.containsKey(newSector)) {
				HashSet<V> tempSet = new HashSet<V>();
				tempSet.add(vertex);
				sectorMembers3D.put(newSector, tempSet);
			} else {
				sectorMembers3D.get(newSector).add(vertex);
			}
		}
	}

	/**
	 * Removes a vertex from the sector registry (useful if vertex has been
	 * deleted).
	 * 
	 * @param vertex
	 */
	private void removeVertexFromSectors(V vertex) {
		Point3d lastPosition = null;
		// retrieve old position from history (and update)
		if (positionHistory.containsKey(vertex)) {
			lastPosition = positionHistory.get(vertex);
		}
		if (lastPosition != null) {
			Point3d oldSector = convertCartesianCoordinateToSectorCoordinate(lastPosition);
			if (sectorMembers3D.containsKey(oldSector)) {
				sectorMembers3D.get(oldSector).remove(vertex);
				if (debug && writeDebugToIO) {
					String msg = "Last coordinate of vertex " + vertex + ": "
							+ lastPosition;
					if (writeDebugToOutfileInsteadConsole) {
						writer.write(msg);
					} else {
						System.out.print(msg);
					}
				}
			}
			// remove from history to reduce list size
			positionHistory.remove(vertex);
		}
	}

	public Point2D convertCartesianCoordinateToSectorCoordinate(
			Point2D cartesianCoordinate) {
		// System.out.println("Coordinate: " + cartesianCoordinate.getX() + ", "
		// + cartesianCoordinate.getY() + ", Sector size: " + xSizeOfSector);
		// System.out.println("Conversion: " + new
		// Double(cartesianCoordinate.getX() / xSizeOfSector).intValue() + ", "
		// + new Double(cartesianCoordinate.getX() / xSizeOfSector).intValue());
		Point2D rawConversion = new Point2D.Double(new Float(
				cartesianCoordinate.getX() / (float) xSizeOfSector).intValue(),
				new Float(cartesianCoordinate.getY() / (float) ySizeOfSector)
						.intValue());
		return rawConversion;
	}

	public Point3d convertCartesianCoordinateToSectorCoordinate(
			Point3d cartesianCoordinate) {
		// System.out.println("Coordinate: " + cartesianCoordinate.getX() + ", "
		// + cartesianCoordinate.getY() + ", Sector size: " + xSizeOfSector);
		// System.out.println("Conversion: " + new
		// Double(cartesianCoordinate.getX() / xSizeOfSector).intValue() + ", "
		// + new Double(cartesianCoordinate.getX() / xSizeOfSector).intValue());
		Point3d rawConversion = new Point3d(new Float(cartesianCoordinate.x
				/ (float) xSizeOfSector).intValue(), new Float(
				cartesianCoordinate.y / (float) ySizeOfSector).intValue(),
				new Float(cartesianCoordinate.z / (float) zSizeOfSector)
						.intValue());
		return rawConversion;
	}

	@Override
	public void reset() {
	}

	public void shutdown() {
		if (writer != null) {
			writer.close();
		}
	}

	@Override
	public void vertexAdded(V vertex) {
		// no need to set - will be set after first iteration
		// sim.getMason3dLocations().setObjectLocation(vertex, null);
	}

	@Override
	public void vertexRemoved(V vertex) {
		removeVertexFromSectors(vertex);
		// remove vertex from 3D graph
		sim.getMason3dLocations().remove(vertex);
		if (use3d && print3dLines) {
			// check for dead node
			clearEdgesForNode(vertex.toString());
			sim.getEdges().removeNode(vertex);
		}
	}

	@Override
	public void agentSelected(String agentName) {
		highlightedIndividual = (V) new VertexPoint3D<String>(agentName);
		manualStep();
	}

}
