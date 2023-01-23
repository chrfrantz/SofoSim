package org.sofosim.graph;

import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.apache.commons.collections15.Transformer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.sofosim.environment.GridSim;
import org.sofosim.environment.stats.StatsCalculator;
import org.sofosim.forceLayout.ForceDirectedLayout;
import edu.uci.ics.jung.algorithms.layout.AggregateLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;

public class GraphHandler<V> {

	protected final boolean multiGraph = true;
	//final boolean makeAdditionalAggregateGraph = false;
	//final boolean makeForcesGraph = true;
	public final GridSim sim;
	private final GraphRegistry graphRegistry;
	private HashMap<String, Graph> graphs = null;
	//private static HashMap<String, JFrame> graphFrames = null;
	private HashMap<String, Layout> layouts = null;
	
	JungController jungController = null;
	public static final Integer graphXSize = 600;
	public static final Integer graphYSize = graphXSize;
	public static final float frameScaleFactor = 1.05f;
	public static final boolean doBenchmarkForceGraph = false;
	public static boolean fullForceGraphUi = false;
	
	public static final String UNIFIED_GRAPH = "Aggregate Graph";
	//public static final String CHEATER_INFORMATION_GRAPH = "Cheater Information Graph";
	//public static final String EMPLOYMENT_RELATIONSHIP_GRAPH = "Employment Relationship Graph";
	//public static final String TRADER_INFO_GRAPH = "Trader Information Graph";
	//public static final String FORCES_GRAPH = "Forces Graph";
	public static ArrayList<String> excludeFromUnifiedGraph = new ArrayList<String>();
	public static ArrayList<String> includeIntoForcesGraph = new ArrayList<String>();
	public static ArrayList<String> excludeGraphsFromShowing = new ArrayList<String>();
	/* for reuse outside of GraphHandler */
	//public ForceDirectedLayout<V,?> forceGraphLayout = null;
	
	public GraphHandler(GridSim sim){
		this.sim = sim;
		this.graphRegistry = new GraphRegistry<V>(this);
		graphs = new HashMap<String, Graph>();
		//graphFrames = new HashMap<String, JFrame>();
		layouts = new HashMap<String, Layout>();
		this.jungController = new JungController(this);
		this.jungController.setVisible(true);
	}
	
	public void registerGraph(GraphInitializer graph){
		graphRegistry.registerGraph(graph);
	}
	
	public GraphInitializer getGraphInitializer(String graphName){
		return graphRegistry.getGraphInitializer(graphName);
	}
	
	public boolean hasGraph(String graphName){
		return graphRegistry.hasGraph(graphName);
	}
	
	public boolean addVertex(String graphName, V vertexName){
		System.out.println("Added vertex '" + vertexName + "' to graph '" + graphName + "'");
		return graphRegistry.addVertex(graphName, vertexName);
	}
	
	public boolean removeVertex(String graphName, V vertexName){
		System.out.println("Removed vertex '" + vertexName + "' from graph '" + graphName + "'");
		return graphRegistry.removeVertex(graphName, vertexName);
	}
	
	public void registerGraphChangeListener(GraphChangeListener listener){
		graphRegistry.registerGraphChangeListener(listener);
	}
	
	public void deregisterGraphChangeListener(GraphChangeListener listener){
		graphRegistry.unregisterGraphChangeListener(listener);
	}
	
//	
//	public JFrame getFrame(String graphName){
//		if(multiGraph){
//			return graphFrames.get(graphName);
//		} else {
//			return graphFrames.get(UNIFIED_GRAPH);
//		}
//	}
//	
//	public boolean hasFrame(String graphName){
//		return graphFrames.containsKey(graphName);
//	}
//	
//	/**
//	 * Returns all registered JFrames, whether updated or not.
//	 * @return
//	 */
//	public ArrayList<JFrame> getFrames(){
//		ArrayList<JFrame> frames = new ArrayList<JFrame>(graphFrames.values());
//		return frames;
//	}
//	
//	/**
//	 * Returns all JFrames that are to be printed (invisible ones are suppressed)
//	 * @return
//	 */
//	public ArrayList<JFrame> getPrintableFrames(){
//		ArrayList<JFrame> frames = new ArrayList<JFrame>();
//		for(Entry<String, JFrame> entry: graphFrames.entrySet()){
//			if(!excludeGraphsFromShowing.contains(entry.getKey())){
//				frames.add(entry.getValue());
//			}
//		}
//		return frames;
//	}
	
	
	/*
	private boolean makeAggregateGraph(String graphName){
		if(multiGraph && makeAdditionalAggregateGraph && !excludeFromUnifiedGraph.contains(graphName)){
			return true;
		}
		return false;
	}
	
	private boolean addEdgeToForcesGraph(String graphName){
		if(multiGraph && makeAdditionalAggregateGraph && includeIntoForcesGraph.contains(graphName)){
			return true;
		}
		return false;
	}*/
	
	
	
	/*
	public boolean addVertex(String graphName, String name){
		if(sim.maintainGraphs){
			graphName = checkGraphMode(graphName);
			if(makeAggregateGraph(graphName)){
				if(!graphs.containsKey(UNIFIED_GRAPH)){
					graphs.put(UNIFIED_GRAPH, generateGraph());
				}
				if(!graphs.get(UNIFIED_GRAPH).containsVertex(name)){
					graphs.get(UNIFIED_GRAPH).addVertex(name);
				}
			}
			//add forces graph vertex
			if(makeForcesGraph){
				if(!graphs.containsKey(FORCES_GRAPH)){
					graphs.put(FORCES_GRAPH, generateGraph());
				}
				if(!graphs.get(FORCES_GRAPH).containsVertex(name)){
					graphs.get(FORCES_GRAPH).addVertex(name);
				}
			}
			if(!graphs.containsKey(graphName)){
				graphs.put(graphName, generateGraph());
			}
			Boolean result = null;
			if(!graphs.get(graphName).containsVertex(name)){
				result = graphs.get(graphName).addVertex(name);
			}
			return result;
		}
		return false;
	}
	
	public boolean addEdge(String graphName, RelationshipEdge edge, String source, String target, EdgeType edgeType){
		if(sim.maintainGraphs){
			graphName = checkGraphMode(graphName);
			if(makeAggregateGraph(graphName)){
				if(!graphs.containsKey(UNIFIED_GRAPH)){
					graphs.put(UNIFIED_GRAPH, generateGraph());
				}
				if(!graphs.get(UNIFIED_GRAPH).containsEdge(edge)){
					graphs.get(UNIFIED_GRAPH).addEdge(edge, source, target, edgeType);
				}
			}
			//maintain forces graph
			if(addEdgeToForcesGraph(graphName)){
				if(!graphs.get(FORCES_GRAPH).containsEdge(edge)){
					graphs.get(FORCES_GRAPH).addEdge(edge, source, target, edgeType);
				}
			}
			if(!graphs.containsKey(graphName)){
				graphs.put(graphName, generateGraph());
			}
			return graphs.get(graphName).addEdge(edge, source, target, edgeType);
		}
		return false;
	}
	
	public boolean removeEdge(String graphName, RelationshipEdge edge){
		if(sim.maintainGraphs){
			graphName = checkGraphMode(graphName);
			if(makeAggregateGraph(graphName)){
				if(!graphs.containsKey(UNIFIED_GRAPH)){
					graphs.put(UNIFIED_GRAPH, generateGraph());
				}
				graphs.get(UNIFIED_GRAPH).removeEdge(edge);
			}
			//maintain forces graph
			if(addEdgeToForcesGraph(graphName)){
				graphs.get(FORCES_GRAPH).removeEdge(edge);
			}
			if(!graphs.containsKey(graphName)){
				graphs.put(graphName, generateGraph());
			}
			return graphs.get(graphName).removeEdge(edge);
		}
		return false;
	}*/
	
	//Layout<String, RelationshipEdge> layout = null;
	//VertexLocationTransformer vertexLocationTransformer = null;
	Transformer<String, String> vertexLabelTransformer = null;
	Transformer<String, Paint> vertexColorTransformer = null;
	Transformer<String, String> vertexToolTipTransformer = null;
	//<RelationshipEdge, Paint> edgeColorTransformer = null;
	//Transformer<RelationshipEdge, Paint> edgeColorTransformer = null;
	
	//ForceBasedGraphClusterer clusterer = null;
	AggregateLayout aggregateLayout = null;
	
	public void visualizeGraphs(boolean override){
		ArrayList<GraphInitializer> graphList = graphRegistry.getGraphRegister();
		for(int i=0; i<graphList.size(); i++){
			GraphInitializer init = graphList.get(i);
			if(init.enabled()){
				if(!init.isInitialized()){
					System.out.println("Initializing graph ");
					init.initializeGraph(graphRegistry.getGraph(init.getGraphName()), sim);
				} else {
					if(doBenchmarkForceGraph){
						long start = System.nanoTime();
						init.visualizeGraph();	
						long diff = System.nanoTime() - start;
						System.out.println(init.getGraphName() + ": Graph Calculation in ns: " + diff);
					} else {
						init.visualizeGraph();
					}
				}
			}
		}
		
		/*for(Entry<String, Graph> entry: graphs.entrySet()){
			if(!excludeGraphsFromShowing.contains(entry.getKey())){
				if(entry.getKey().equals(FORCES_GRAPH) && override){
					if(doBenchmarkForceGraph){
						long start = System.nanoTime();
						visualizeGraph(entry.getKey(), override);	
						long diff = System.nanoTime() - start;
						System.out.println("Forces Graph Calculation in ns: " + diff);
					} else {
						visualizeGraph(entry.getKey(), override);
					}
				} else {
					visualizeGraph(entry.getKey(), override);
				}
			}
		}*/
	} 
	
	public static final String TITLE_ROUNDS_SEPARATOR = " - ";
	
	/**
	 * Visualize a given graph and override continuous scheduling
	 * @param graphName
	 * @param override
	 */
	
	/*
	public void shutdown(){
		if(graphFrames.containsKey(FORCES_GRAPH)){
			((ForceDirectedLayout)layouts.get(FORCES_GRAPH)).shutdown();
		}
		for(Graph graph: graphs.values()){
			graph = null;
		}
		graphs.clear();
		for(JFrame frame: graphFrames.values()){
			frame.dispose();
			frame = null;
		}
		graphFrames.clear();
		for(JFrame frame: distributionFrames.values()){
			frame.dispose();
			frame = null;
		}
		distributionFrames.clear();
		distributionChartMap.clear();
		jungController.dispose();
		jungController = null;
	}*/
	
	/** printing of graph measures */
	
	public StatsCalculator getGraphMeasures(String graphName){
		Graph g = graphRegistry.getGraph(graphName);
		StatsCalculator calc = new StatsCalculator();
		Collection<String> vtxs = g.getVertices();
		//System.out.println("Vertex count: " + vtxs.size());
		//System.out.println("Number of entries: " + calc.getNumberOfEntries(graphName));
		Iterator<String> it = vtxs.iterator();
		//Double sum = 0.0;
		while(it.hasNext()){
			//Integer val = g.inDegree(it.next());
			calc.enterValue(graphName, g.inDegree(it.next()));
			//sum += val;
		}
		//System.out.println("Sum: " + sum);
		return calc;
	}
	
	private HashMap<String, JFrame> distributionFrames = new HashMap<String, JFrame>();
	/*
	public JFrame showDegreeDistribution(String graphName){
		JFrame frame = null;
		String vertexStartsWith = null;
		if(graphName.equals(CHEATER_INFORMATION_GRAPH)){
			vertexStartsWith = sim.AGENT_PREFIX;
		} else if(graphName.equals(EMPLOYMENT_RELATIONSHIP_GRAPH)){
			vertexStartsWith = sim.AGENT_PREFIX;
		}
		if(distributionFrames.containsKey(graphName)){
			frame = distributionFrames.get(graphName); 
			//frame.getContentPane().remove(0);
			//frame.getContentPane().add(createChartContainer(graphName));
			//distributionChartMap.get(graphName).getXYPlot().setDataset(calculateDistribution(graphName));
			createOrUpdateChart(graphName, calculateDistribution(graphName, vertexStartsWith, false));
			frame.repaint();
		} else {
			frame = new JFrame("Degree Distribution - " + graphName);
			frame.setSize(graphXSize, graphYSize);
			JPanel panel = createChartContainer(graphName, vertexStartsWith);
			frame.getContentPane().add(panel);
			frame.setVisible(true);
			PositionSaver.registerWindow(frame);
			distributionFrames.put(graphName, frame);
			waitAWhile();
		}
		return frame;
	}*/
	
	private IntervalXYDataset calculateDistribution(String graphName) {
		return calculateDistribution(graphName, null, true);
	}
	
	/**
	 * Calculate degree distribution for graph, with optional constraint on 
	 * nodes (identified by node name prefix), and selection if both 
	 * indegree and outdegree distributions should be plotted.
	 * @param graphName
	 * @param vertexNameStartsWith
	 * @param inDegreeAndOutDegree
	 * @return
	 */
	private IntervalXYDataset calculateDistribution(String graphName, String vertexNameStartsWith, boolean inDegreeAndOutDegree) {
		HistogramDataset localHistogramDataset = new HistogramDataset();
		Graph g = graphRegistry.getGraph(graphName);
		ArrayList<String> vtxs = new ArrayList<String>(g.getVertices());
		//filter vertices with name prefix
		if(vertexNameStartsWith != null && !vertexNameStartsWith.isEmpty()){
			for(int i=0; i< vtxs.size(); i++){
				if(!vtxs.get(i).startsWith(vertexNameStartsWith)){
					vtxs.remove(i);
					i--;
				}
			}
		}
		Iterator<String> it = vtxs.iterator();
		double[] arrayOfDouble = new double[vtxs.size()];
		int i = 0;
		double tempValue = 0.0;
		Double minValue = null;
		Double maxValue = null;
		while(it.hasNext()){
			tempValue = g.inDegree(it.next());
			arrayOfDouble[i] = tempValue;
			i++;
			if(maxValue == null || tempValue > maxValue){
				maxValue = tempValue;
			}
			if(minValue == null || tempValue < minValue){
				minValue = tempValue;
			}
		}
		if(minValue == null){
			minValue = 0.0;
		}
		if(maxValue == null){
			maxValue = 0.0;
		}
		localHistogramDataset.addSeries("Indegree", arrayOfDouble, 100, minValue, maxValue);
		if(inDegreeAndOutDegree){
			it = vtxs.iterator();
			arrayOfDouble = new double[vtxs.size()];
			i = 0;
			tempValue = 0.0;
			minValue = null;
			maxValue = null;
			while(it.hasNext()){
				tempValue = g.outDegree(it.next());
				arrayOfDouble[i] = tempValue;
				i++;
				if(maxValue == null || tempValue > maxValue){
					maxValue = tempValue;
				}
				if(minValue == null || tempValue < minValue){
					minValue = tempValue;
				}
			}
			if(minValue == null){
				minValue = 0.0;
			}
			if(maxValue == null){
				maxValue = 0.0;
			}
			localHistogramDataset.addSeries("Outdegree", arrayOfDouble, 100, minValue, maxValue);
		}
		return localHistogramDataset;
	}

	private HashMap<String, JFreeChart> distributionChartMap = new HashMap<String, JFreeChart>();
	
	private JFreeChart createOrUpdateChart(String graphName, IntervalXYDataset paramIntervalXYDataset){
		if(distributionChartMap.containsKey(graphName)){
			JFreeChart chart = distributionChartMap.get(graphName);
			chart.getXYPlot().setDataset(paramIntervalXYDataset);
		} else {
			distributionChartMap.put(graphName, createChart(graphName, paramIntervalXYDataset));
		}
		return distributionChartMap.get(graphName);
	}
	
	private JFreeChart createChart(String graphName, IntervalXYDataset paramIntervalXYDataset) {
		JFreeChart localJFreeChart = ChartFactory.createHistogram(
				"Degree Distribution - " + graphName, null, null, paramIntervalXYDataset,
				PlotOrientation.HORIZONTAL, true, true, true);
		XYPlot localXYPlot = (XYPlot) localJFreeChart.getPlot();
		localXYPlot.setDomainPannable(true);
		localXYPlot.setRangePannable(true);
		localXYPlot.setForegroundAlpha(0.85F);
		NumberAxis localNumberAxis = (NumberAxis) localXYPlot.getRangeAxis();
		localNumberAxis.setStandardTickUnits(NumberAxis
				.createIntegerTickUnits());
		XYBarRenderer localXYBarRenderer = (XYBarRenderer) localXYPlot
				.getRenderer();
		localXYBarRenderer.setDrawBarOutline(false);
		localXYBarRenderer.setBarPainter(new StandardXYBarPainter());
		localXYBarRenderer.setShadowVisible(false);
		return localJFreeChart;
	}
	
	public JPanel createChartContainer(String graphName, String vertexNameStartsWith) {
		JFreeChart localJFreeChart = createOrUpdateChart(graphName, calculateDistribution(graphName, vertexNameStartsWith, false));
		ChartPanel localChartPanel = new ChartPanel(localJFreeChart);
		localChartPanel.setMouseWheelEnabled(true);
		return localChartPanel;
	}
	
	public static void waitAWhile(){
		try {
			Thread.sleep(GridSim.WAIT_BETWEEN_PRINTS_IN_MS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns all JFrames that are to be printed (invisible ones are suppressed)
	 * @return
	 */
	public ArrayList<JFrame> getPrintableFrames(){
		ArrayList<JFrame> frames = new ArrayList<JFrame>();
		if(graphRegistry != null && graphRegistry.getGraphRegister() != null){
			for(Object entry: graphRegistry.getGraphRegister()){
				if(!excludeGraphsFromShowing.contains(((GraphInitializer)entry).getGraphName())){
					frames.add(((GraphInitializer)entry).graphFrame);
				}
			}
		}
		return frames;
	}
	
	public void shutdown(){
		this.jungController.dispose();
		this.graphRegistry.shutdownAllGraphs();
	}

}
