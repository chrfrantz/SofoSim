package org.sofosim.graph.initializers;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.frogberry.windowPositionSaver.PositionSaver;
import org.sofosim.clustering.VertexPoint3D;
import org.sofosim.environment.GridSim;
import org.sofosim.forceLayout.AdvancedMouseClickHandler;
import org.sofosim.forceLayout.ForceDirectedLayout;
import org.sofosim.forceLayout.ForceGlassPane;
import org.sofosim.forceLayout.ForceGraphUiGenerator;
import org.sofosim.forceLayout.transformers.ForceLocationVertexTransformer;
import org.sofosim.forceLayout.transformers.VertexShapeRenderer;
import org.sofosim.graph.GraphHandler;
import org.sofosim.graph.GraphInitializer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.picking.PickedState;

public class ForceGraphInitializer extends GraphInitializer {

	public static final String FORCES_GRAPH = "Social Force Graph";
	public static VertexShapeRenderer renderer = null;
	private JFrame graphControlPanel = null;
	
	public String getGraphName(){
		return FORCES_GRAPH;
	}
	
	public void initializeGraph(Graph graph, GridSim sim){

		layout = new ForceDirectedLayout<VertexPoint3D<String>, Integer>(graph, null, sim, planeInitializer);
		
		layout.setSize(new Dimension(sim.GRID_WIDTH, sim.GRID_HEIGHT));
		ForceLocationVertexTransformer locationVertexTransformer = new ForceLocationVertexTransformer((ForceDirectedLayout) layout);
		layout.setInitializer(locationVertexTransformer);
		System.out.println("Layout initialized: " + layout);
		//locationVertexTransformer.setLayout((ForceDirectedLayout) layout);
		//layout-dependent tooltip transformer
		/*if(transformers.getVertexToolTipTransformer() != null){
			Transformer<String, String> vertexToolTipTransformer = new VertexLabelTransformer(layout, sim);
		}*/
		
		//instantiate viewer
		VisualizationViewer vv = new VisualizationViewer(layout);
		if(sim.showSocialForcesGraphUiInSingleFrame()){
			vv.setPreferredSize(new Dimension((int)(GraphHandler.frameScaleFactor * sim.GRID_WIDTH), (int)(GraphHandler.frameScaleFactor * sim.GRID_HEIGHT)));
		} else {
			vv.setPreferredSize(new Dimension((int)sim.GRID_WIDTH, (int)sim.GRID_HEIGHT));
		}
		//Mouse setup
		DefaultModalGraphMouse<Integer,Number> graphMouse = new DefaultModalGraphMouse<Integer,Number>(1.1f, 1/1.1f);
		graphMouse.setMode(Mode.PICKING);
		vv.setGraphMouse(graphMouse);
		
		//register custom mouse listener that can identify doubleclicks and so on for picking of individuals
		vv.addGraphMouseListener(new AdvancedMouseClickHandler<String>((ForceDirectedLayout) layout));
		
		//adds support to access picked items (and to at least print the number of picked items)
		final PickedState pickedState = vv.getPickedVertexState();
		pickedState.addItemListener(new ItemListener() {
			
			//Reference to thread
			private Thread checkThread = null;
			//time of last pick
			Long previousPick = null;
			//time out after which item change events are not considered related
			private Long timeoutInMs = 250L;
			//time after which thread is shut down
			private Long shutdownThreadAfterWaitingInMs = 5000L;
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				previousPick = System.currentTimeMillis();
				if(checkThread == null){
					checkThread = new Thread(new Runnable(){

						Long previous = null;
						Long printedValue = null;
						//print upon first pick
						boolean printOverride = false;
						//switch to shutdown thread
						boolean run = true;
						
						@Override
						public void run() {
							while(run){
								previous = previousPick;
								try {
									Thread.sleep(timeoutInMs);
								} catch (InterruptedException e) {}
								//print if the current timestamp's value has not been printed
								if((!previous.equals(printedValue) 
										//and the previous is not the same as current one
										&& !previousPick.equals(previous) 
										//and the number of items is not zero
										&& pickedState.getSelectedObjects().length != 0)
									//if it is the first run, in which we always print
									|| printOverride){
									//memorize printed value
									printedValue = previous;
									if(printOverride){
										//override only once
										printOverride = false;
									}
									System.out.println("Picked " + pickedState.getSelectedObjects().length + " vertices.");
								}
								//if not used long enough, shutdown thread
								if((System.currentTimeMillis() - previousPick) > shutdownThreadAfterWaitingInMs){
									//shutdown thread
									run = false;
									//reset reference to thread
									checkThread = null;
								}
							}
							//System.out.println("Social Force Graph Item Selection thread timed out.");
						}
						
					});
					checkThread.setName("Social Forces Graph Item Selection Listener");
					checkThread.start();
				}
			}
			
		});
		
		//register transformers
		if(transformers == null){
			throw new RuntimeException("No Graph Transformers specified.");
		}
		if(transformers.getFillPaintTransformer(layout) != null){
			vv.getRenderContext().setVertexFillPaintTransformer(transformers.getFillPaintTransformer(layout));
		}
		if(transformers.getVertexLabelTransformer(layout) != null){
			vv.getRenderContext().setVertexLabelTransformer(transformers.getVertexLabelTransformer(layout));
		}
		if(transformers.getVertexToolTipTransformer(layout) != null){
			vv.setVertexToolTipTransformer(transformers.getVertexToolTipTransformer(layout));
		}
		System.out.println("Force Graph Transformers registered.");
		//prepare frame
		graphFrame = new JFrame(getGraphName());
		graphFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		graphFrame.getContentPane().add(vv);
		//prepare glass pane
		//System.out.println("GraphFrame: " + graphFrame + ", layout: " + layout);
		glassPane = new ForceGlassPane(graphFrame, (ForceDirectedLayout) layout);
		if(sim.showSocialForcesGraphUiInSingleFrame()){
			ForceGraphUiGenerator.generateForceGraphUi(graphFrame, vv, (ForceDirectedLayout) layout, true);
		} else {
			graphControlPanel = new JFrame("Force Graph Control Panel");
			graphControlPanel.setSize(new Dimension(sim.GRID_WIDTH, sim.GRID_HEIGHT));
			ForceGraphUiGenerator.generateForceGraphUi(graphControlPanel, vv, (ForceDirectedLayout) layout, false);
			graphControlPanel.setVisible(true);
			PositionSaver.registerWindow(graphControlPanel);
		}
		graphFrame.setGlassPane(glassPane);
		glassPane.setVisible(true);
		//System.out.println("GlassPane initialized: " + glassPane.toString());
		renderer = new VertexShapeRenderer((ForceDirectedLayout) layout, transformers.getVertexColorTransformer(layout));
		vv.getRenderer().setVertexRenderer(renderer);
		graphFrame.pack();
		graphFrame.setName(getGraphName());
		SwingUtilities.invokeLater(new Runnable(){

			@Override
			public void run() {
				graphFrame.setVisible(true);
			}
			
		});
		
		PositionSaver.registerWindow(graphFrame);
		//mark the successful initialization
		initialized = true;
	}
	
	public void visualizeGraph(){
		if(((ForceDirectedLayout)layout).sim.usingMasonSchedulerForGraphs()){
			((ForceDirectedLayout)layout).step();
			graphFrame.repaint();
		}
	}

	@Override
	public void shutdownGraph() {
		if(graphControlPanel != null){
			graphControlPanel.dispose();
		}
		graphFrame.dispose();
	}

}
