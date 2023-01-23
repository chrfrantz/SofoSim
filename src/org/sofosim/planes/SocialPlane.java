package org.sofosim.planes;

import java.awt.Color;
import java.util.Set;
import org.sofosim.forceLayout.ForceDirectedLayout;

public abstract class SocialPlane<V> {

	private String name = null;
	protected ForceDirectedLayout layout = null;
	private boolean enabled;
	private boolean drawLinks = false;
	private boolean perceptionallyConstrained = true;
	private Color planeLinkColor = Color.black;
	public Float weightFactor;
	private final Float defaultWeightFactor;
	private int alpha = 150;
	
	/**
	 * Instantiates a new SocialPlane with a given layout, name, indicator if this plane 
	 * is perceptionally constrained (if the layout does not override it and if the 
	 * perception distance is not unlimited), color and default weight factor.
	 * @param layout
	 * @param nameOfPlane
	 * @param perceptionallyConstrained
	 * @param planeColor
	 * @param weightFactor
	 */
	public SocialPlane(ForceDirectedLayout layout, String nameOfPlane, boolean perceptionallyConstrained, Color planeColor, Float weightFactor, boolean drawLinks){
		this.layout = layout;
		this.name = nameOfPlane;
		this.drawLinks = drawLinks;
		this.perceptionallyConstrained = perceptionallyConstrained;
		this.planeLinkColor = new Color(planeColor.getRed(), planeColor.getGreen(), planeColor.getBlue(), alpha);
		if(weightFactor != null){
			this.weightFactor = weightFactor;
		} else {
			this.weightFactor = 1.0F;
		}
		this.defaultWeightFactor = this.weightFactor;
		this.enabled = enabledAtStart();
		String prefix = "'" + nameOfPlane + "'";
		System.err.println("Created SOCIAL PLANE " + prefix + ", enabled: " + this.enabled);
		if(!this.perceptionallyConstrained){
			System.err.println(prefix + " is distance-independent. Even if you constrain perception distance this plane with work with unlimited perception.");
		}
		if(this.perceptionallyConstrained && (layout.maximalPerceptionDistance == -1 || layout.makeAllPlanesDistanceIndependent)){
			System.err.println(prefix + "s constrained perception not active as either \nunlimited visibility activated or limited plane perception globally overridden.");
		}
	}
	
	public String getName(){
		return this.name;
	}
	
	public abstract Color getColor(V sourceVertex, V targetVertex);
	
	int differenceFactor = 5;
	
	public Color calculateColorShades(Integer gradient){
		Color newCol = null;
		
		boolean tryDarker = false;
		try{
			newCol = this.planeLinkColor;
			for(int i=(gradient - 1); i<(gradient - 1) * differenceFactor; i++){
				newCol = newCol.brighter();
			}
			if(newCol.getRGB() == this.planeLinkColor.getRGB()){
				tryDarker = true;
			}
		} catch(IllegalArgumentException e){
			System.out.println("Exception on color range");
			tryDarker = true;
		}
		if(tryDarker){
			newCol = this.planeLinkColor;
			for(int i=(gradient - 1); i<(gradient - 1) * differenceFactor; i++){
				newCol = newCol.darker();
			}
		}
		newCol = new Color(newCol.getRed(), newCol.getGreen(), newCol.getBlue(), alpha);
		return newCol;
	}
	
	public void enableLinkDrawing(){
		drawLinks = true;
	}
	
	public void disableLinkDrawing(){
		drawLinks = false;
		if(layout.use3d && layout.print3dLines() && enabled){
			//clear eventual edges from 3D visualizer - fast
			layout.clearEdgesForNode(name);
		}
	}
	
	public boolean linkDrawingEnabled(){
		//only draw links if plane is enabled
		return drawLinks && enabled;
	}
	
	public boolean isEnabled(){
		return enabled;
	}
	
	public void enable(){
		this.enabled = true;
	}
	
	public void disable(){
		this.enabled = false;
		if(layout.use3d && layout.print3dLines()){
			//clear eventual edges from 3D visualizer - fast
			layout.clearEdgesForNode(name);
		}
	}
	
	public boolean perceptionallyConstrained(){
		return perceptionallyConstrained;
	}
	
	public double getForceTowards(V sourceVertex, V targetVertex, double distance){
		if(this.enabled){
			try{
				return getForce(sourceVertex, targetVertex, distance);
			} catch(NullPointerException e){
				//do nothing
			}
		}
		return 0.0;
	}
	
	protected abstract double getForce(V sourceVertex, V targetVertex, double distance);
	
	public abstract Set<V> getPerceptionallyIndependentVertices(V perceivingVertex);
	
	protected abstract boolean enabledAtStart();
	
	public String toString(){
		return name + " (Weight: " + weightFactor + ")";
	}
	
	public void resetWeight(){
		this.weightFactor = this.defaultWeightFactor;
	}
	
	/*
	public float getWeight(){
		return weightFactor;
	}
	
	public void setWeight(Float weight){
		this.weightFactor = weight;
	}*/
	
}
