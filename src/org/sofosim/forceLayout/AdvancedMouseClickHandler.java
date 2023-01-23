package org.sofosim.forceLayout;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.Timer;
import org.nzdis.micro.inspector.PlatformInspectorGui;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;

public class AdvancedMouseClickHandler<V> implements GraphMouseListener<V>,
		ActionListener {

	private static final Integer timerinterval = (Integer) Toolkit
			.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
	Timer timer = new Timer(timerinterval, this);
	private static final boolean debug = false;
	public static final int LEFT_BUTTON = MouseEvent.BUTTON1;
	public static final int MOUSE_WHEEL = MouseEvent.BUTTON2;
	public static final int RIGHT_BUTTON = MouseEvent.BUTTON3;
	private ForceDirectedLayout l = null;

	public AdvancedMouseClickHandler(ForceDirectedLayout layout) {
		this.l = layout;
	}

	@Override
	public void graphClicked(V vertex, MouseEvent e) {
		isDoubleClick(vertex, e);
	}

	MouseEvent lastEvent;
	V lastVertex = null;

	public void actionPerformed(ActionEvent e) {
		timer.stop();
		// single click
		redirectClickToListener(lastVertex, lastEvent, false);
	}

	private void isDoubleClick(V vertex, final MouseEvent e) {

		if (e.getClickCount() > 2) {
			System.out.println("Detected more than two clicks.");
			return;
		}

		lastVertex = vertex;
		lastEvent = e;

		if (timer.isRunning()) {
			timer.stop();
			redirectClickToListener(vertex, e, true);
		} else {
			timer.restart();
		}
	}

	private void redirectClickToListener(V vertex, MouseEvent e,
			boolean wasDoubleClick) {
		if (e.getButton() == LEFT_BUTTON) {
			if (wasDoubleClick) {
				if (debug) {
					System.out.println("Double left click on " + vertex);
				}
				doubleClickLeftPressed(vertex);
			} else {
				if (debug) {
					System.out.println("Single left click on " + vertex);
				}
				singleClickLeftPressed(vertex);
			}
		} else if (e.getButton() == RIGHT_BUTTON) {
			if (wasDoubleClick) {
				if (debug) {
					System.out.println("Double right click on " + vertex);
				}
				doubleClickRightPressed(vertex);
			} else {
				if (debug) {
					System.out.println("Single right click on " + vertex);
				}
				singleClickRightPressed(vertex);
			}
		} else if (e.getButton() == MOUSE_WHEEL) {
			if (debug) {
				if (wasDoubleClick) {
					System.out.println("Doubleclick on Mouse Wheel");
				} else {
					System.out.println("Single click on Mouse Wheel");
				}
			}
		} else {
			System.err.println("Unknown button pressed on mouse: "
					+ e.getButton());
		}
	}

	private void singleClickLeftPressed(V vertex) {
		System.out.println("Set focus on individual " + vertex.toString());
		l.highlightedIndividual = vertex;
	}

	private void doubleClickLeftPressed(V vertex) {
		// System.out.println("Looking for " + vertex.toString());
		PlatformInspectorGui.highlightNode(vertex.toString());
	}

	private void singleClickRightPressed(V vertex) {
		System.out.println("Reset highlighting of agent.");
		l.highlightedIndividual = null;
	}

	private void doubleClickRightPressed(V vertex) {
		System.out.println("Reset highlighting of agent.");
		l.highlightedIndividual = null;
	}

	@Override
	public void graphPressed(V arg0, MouseEvent arg1) {
		//System.out.println("Graph event pressed: " + arg1);
	}

	@Override
	public void graphReleased(V arg0, MouseEvent arg1) {
		//System.out.println("Graph event released: " + arg1);

	}

}
