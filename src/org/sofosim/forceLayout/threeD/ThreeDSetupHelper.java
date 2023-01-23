package org.sofosim.forceLayout.threeD;

import java.awt.Color;
import javax.swing.JFrame;

import org.frogberry.windowPositionSaver.PositionSaver;
import org.sofosim.environment.GridSim;
import sim.display.Controller;
import sim.display.GUIState;
import sim.display3d.Display3D;
import sim.portrayal3d.continuous.ContinuousPortrayal3D;
import sim.portrayal3d.network.NetworkPortrayal3D;
import sim.portrayal3d.network.SpatialNetwork3D;
import sim.portrayal3d.simple.WireFrameBoxPortrayal3D;

public class ThreeDSetupHelper {

	/** 3D display to be visualized in frame */
	private static Display3D display = null;
	/** display frame */
    private static JFrame displayFrame = null;
    /** Portrayal holding vertices of simulation */
    public static ContinuousPortrayal3D vertexPortrayal = null;
    /** Portrayal holding edges - dynamically linked to SofoSim data structures */
    private static NetworkPortrayal3D edgePortrayal = null;
	
    /**
     * Generates a JFrame with specified parameters and prepares a 3D display. 
     * Reference to generated display is handled internally.
     * @param controller display controller from Mason
     * @param sim simulation instance to operate in
     * @param xDim x dimension of model (visualizer will be slightly bigger to provide sufficient space)
     * @param yDim y dimension of model (see above)
     * @param zDim z dimension of model (see above)
     * @param wireFrameColor color of wire frame containing the cubic model
     */
	public static void generate3DVisualization(Controller controller, GUIState sim, double xDim, double yDim, double zDim, Color wireFrameColor){
		
		vertexPortrayal = new ContinuousPortrayal3D();
		edgePortrayal = new NetworkPortrayal3D();
		WireFrameBoxPortrayal3D wireFrameP = new WireFrameBoxPortrayal3D(0, 0, 0, xDim, yDim, zDim, wireFrameColor);
		
		//setup display with sufficient dimensions to leave some space in visualizer
		display = new Display3D(xDim + 200, yDim + 200, sim);
		//scale to maximum dimension (if differing)
		display.scale(1.0 / Math.max(xDim, yDim));
		
		//attach all portrayals
		display.attach(wireFrameP, "WireFrame");
	    display.attach(vertexPortrayal, "3D Vertices");
	    display.attach(edgePortrayal, "3D Edges");
	    
	    //set background to white
	    display.setBackdrop(Color.white);

	    //create JFrame to hold display
	    displayFrame = display.createFrame();
	    
	    //register it and make it visible
	    controller.registerFrame(displayFrame);
	    displayFrame.setVisible(true);
	    //register with position saver
	    PositionSaver.registerWindow(displayFrame);
	}
	
	/**
	 * Starts a generated 3D visualization. It needs to be generated beforehand.
	 * This method links actual data from the fully instantiated simulation 
	 * against the portrayals and layout created with generate3DVisualization().
	 * @param sim simulation instance
	 */
	public static void start3DVisualization(GridSim sim){
		if(displayFrame == null){
			throw new RuntimeException("Call to generate3DVisualization() before start3DVisualization().");
		} else {
			display.destroySceneGraph();
			//link portrayals to data
			vertexPortrayal.setField(sim.getMason3dLocations());
			edgePortrayal.setField(new SpatialNetwork3D(sim.getMason3dLocations(), sim.getEdges()));
			//reset display to recreate model
			display.reset();
		    display.createSceneGraph();
		    // redraw the display
		    display.repaint();
		    System.out.println("Should show 3D graph!!!!");
		}
	}
	
}
