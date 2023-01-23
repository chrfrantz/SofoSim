package org.sofosim.forceLayout.transformers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import javax.vecmath.Point3d;
import org.apache.commons.collections15.Transformer;
import org.sofosim.clustering.VertexPoint3D;
import org.sofosim.forceLayout.ForceDirectedLayout;
import org.sofosim.forceLayout.util.LineShape;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;

public class VertexShapeRenderer<V,E> implements Renderer.Vertex<V, E>{

	private final ForceDirectedLayout layout;
	private final int offset;
	private Transformer<V, Paint> colorTransformer;
	/** force lines for individuals */
	public HashMap<V, LineShape> forceLine = new HashMap<V, LineShape>();
	
	Stroke originalStroke = null;
	final boolean showForces = true;
	
	public VertexShapeRenderer(ForceDirectedLayout<V,E> layout, Transformer<V, Paint> colorTransformer){
		this.layout = layout;
		this.offset = layout.vertexDiameter;
		this.colorTransformer = colorTransformer;
		//set color transformer if not set as parameter
		if(this.colorTransformer == null){
			this.colorTransformer = (Transformer<V, Paint>) new Transformer<VertexPoint3D<String>, Paint>(){

				@Override
				public Paint transform(VertexPoint3D<String> arg0) {
					return Color.RED;
				}
				
			};
			System.out.println("Set default color transformer as none specified in transformer set.");
		}
	}

    @Override 
    public void paintVertex(RenderContext<V, E> rc,
        Layout<V, E> layout, V vertex) {
      GraphicsDecorator graphicsContext = rc.getGraphicsContext();
      Point2D center = layout.transform(vertex);
      Point3d center3D = new Point3d(center.getX(), center.getY(), 0);
      graphicsContext.setColor(Color.black);
      if(originalStroke == null){
    	  originalStroke = graphicsContext.getStroke();
      }
      graphicsContext.setStroke(originalStroke);
      //vertex color itself
      graphicsContext.setColor((Color)colorTransformer.transform(vertex));
      //paint filled circle around vertex point
      paintCircle(graphicsContext, center, offset, Color.black, true, null);
      if(ForceDirectedLayout.drawMinDistances){
	      //minimal distance circle
	      paintCircle(graphicsContext, center, this.layout.minimalDistance * 2, Color.orange, false, null);
      }
	  if(ForceDirectedLayout.drawMaxDistances){
	      //maximum distance circle
	      paintCircle(graphicsContext, center, this.layout.maximalDistance * 2, Color.gray, false, null);
	  }
	  if(ForceDirectedLayout.drawMaxAttractionDistances){
	      //maximum attraction circle
	      paintCircle(graphicsContext, center, this.layout.maximalAttractionDistance * 2, Color.green, false, null);
      }
      //force
      if(showForces && forceLine.containsKey(vertex)){
    	  LineShape lShape = forceLine.get(vertex);
    	  if(lShape.attracting){
    		  graphicsContext.setColor(Color.BLACK);
    	  } else {
    		  graphicsContext.setColor(Color.RED);
    	  }
    	  Shape shape = new Line2D.Double(lShape.startingPoint, lShape.endPoint);
    	  graphicsContext.draw(shape);
    	  forceLine.remove(vertex);
      }
      //visualize visible sectors
      if(ForceDirectedLayout.drawVisibleSectors){
	      Point3d ownSector = this.layout.convertCartesianCoordinateToSectorCoordinate(center3D);
	      //for(Point2D visibleSector: ForceDirectedLayout.visibleSectors){
	      ArrayList<Point3d> visibleSectors = ForceDirectedLayout.visibleSectors3D;
	      for(int i = 0; i < visibleSectors.size(); i++){
	    	  Point3d visibleSector = visibleSectors.get(i);
	    	  Point3d translatedSector = new Point3d(ownSector.x + visibleSector.x, ownSector.y + visibleSector.y, ownSector.z + visibleSector.z);
	    	  graphicsContext.setStroke(new BasicStroke(5));
	    	  graphicsContext.drawRect(new Double(translatedSector.x * this.layout.xSizeOfSector).intValue(), new Double(translatedSector.y * this.layout.ySizeOfSector).intValue(), this.layout.xSizeOfSector, this.layout.ySizeOfSector);
	      }
      }
    }
    
    /**
     * Paints a circle and optionally fills.
     * @param graphicsContext
     * @param center Center coordinate of circle
     * @param diameter diameter of circle
     * @param color color of circle line
     * @param fill boolean indicator if circle should be filled
     * @param fillColor fill color to be used
     * @return
     */
    private Shape paintCircle(GraphicsDecorator graphicsContext, Point2D center, int diameter, Color color, boolean fill, Color fillColor){
    	Shape shape = new Ellipse2D.Double(center.getX() - diameter / (float)2, center.getY() - diameter / (float)2, diameter, diameter);
        if(fill){
        	if(fillColor != null){
        		graphicsContext.setColor(fillColor);
        	}
        	graphicsContext.fill(shape);
        }
    	if(color != null){
        	graphicsContext.setColor(color);
        }
        graphicsContext.draw(shape);
        return shape;
    }
}
