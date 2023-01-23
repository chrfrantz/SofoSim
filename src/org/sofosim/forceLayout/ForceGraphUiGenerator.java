package org.sofosim.forceLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.sofosim.clustering.VertexPoint3D;
import org.sofosim.planes.SocialPlane;
import org.sofosim.tags.Tag;

import edu.uci.ics.jung.visualization.VisualizationViewer;

public class ForceGraphUiGenerator<V> {
	
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

	public static void generateForceGraphUi(final JFrame graphFrame, final VisualizationViewer vv, final ForceDirectedLayout l, boolean positionControlPanelToEast){
		//add control buttons for spheres and general buttons at page end
		JPanel controls = new JPanel();
		LinkedHashSet<JComponent> controlsPageEnd = new LinkedHashSet<JComponent>();
		//GENERAL BUTTONS AT PAGE END FOR FORCES DISPLAY, DENSITY, ...
		//prepare lowest button line with display-related buttons
		JToggleButton btnForce = new JToggleButton("Show Forces");
		btnForce.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.drawForces = true;
				} else {
					l.drawForces = false;
				}
				
			}
		});
		btnForce.setSelected(ForceDirectedLayout.drawForces);
		controlsPageEnd.add(btnForce);
		
		//compile distance buttons into JPanel, then add it to controls
		JToggleButton btnMinDistance = new JToggleButton("Min. Dist.");
		btnMinDistance.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.drawMinDistances = true;
				} else {
					l.drawMinDistances = false;
				}
				
			}
		});
		btnMinDistance.setSelected(ForceDirectedLayout.drawMinDistances);
		JToggleButton btnMaxDistance = new JToggleButton("Max. Dist.");
		btnMaxDistance.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.drawMaxDistances = true;
				} else {
					l.drawMaxDistances = false;
				}
				
			}
		});
		btnMaxDistance.setSelected(ForceDirectedLayout.drawMaxDistances);
		JToggleButton btnMaxAttrDistance = new JToggleButton("Max. Attraction Dist.");
		btnMaxAttrDistance.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.drawMaxAttractionDistances = true;
				} else {
					l.drawMaxAttractionDistances = false;
				}
				
			}
		});
		btnMaxAttrDistance.setSelected(ForceDirectedLayout.drawMaxAttractionDistances);
		//add to panel
		JPanel distanceButtonPanel = new JPanel();
		//setting vertical gap for JPanel to zero
		((FlowLayout)distanceButtonPanel.getLayout()).setVgap(0);
		//((FlowLayout)testPanel.getLayout()).setHgap(0);
		distanceButtonPanel.add(btnMinDistance);
		distanceButtonPanel.add(btnMaxDistance);
		distanceButtonPanel.add(btnMaxAttrDistance);
		//add to control panel
		controlsPageEnd.add(distanceButtonPanel);
		
		JToggleButton btnSector = new JToggleButton("Show visible sectors");
		btnSector.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.drawVisibleSectors = true;
				} else {
					l.drawVisibleSectors = false;
				}
				
			}
		});
		btnSector.setSelected(ForceDirectedLayout.drawVisibleSectors);
		//only add if sector-based calculation is activated
		if(ForceDirectedLayout.useSectorBasedCalculation){
			controlsPageEnd.add(btnSector);
		}
		JToggleButton btnDensity = new JToggleButton("Show sector density");
		btnDensity.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.drawSectorDensity = true;
				} else {
					l.drawSectorDensity = false;
				}
			}
			
		});
		btnDensity.setSelected(ForceDirectedLayout.drawSectorDensity);
		controlsPageEnd.add(btnDensity);
		
		//PLANE TOGGLE BUTTONS
		LinkedHashSet<JComponent> controlsSphereToggleButtons = new LinkedHashSet<JComponent>();
		LinkedHashMap<String, SocialPlane<String>> planes = l.getPlanes();
		//automatically adjust layout depending on number of planes and further buttons at page end
		controls.setLayout(new GridLayout((int)Math.ceil(controlsPageEnd.size()/(double)planes.size()) + 2, planes.size(), 5, 5));
		final SocialPlane[] planeArray = new SocialPlane[planes.size()];
		int count = 0;
		for(final Entry<String,SocialPlane<String>> entry: planes.entrySet()){
			final JCheckBox chkBox = new JCheckBox("Show links");
			chkBox.addItemListener(new ItemListener() {
				
				@Override
				public void itemStateChanged(ItemEvent e) {
					if(e.getStateChange() == ItemEvent.SELECTED){
						((SocialPlane<String>)l.getPlanes().get(entry.getValue().getName())).enableLinkDrawing();
					} else {
						((SocialPlane<String>)l.getPlanes().get(entry.getValue().getName())).disableLinkDrawing();
					}
				}
				
			});
			chkBox.setSelected(((SocialPlane<String>)l.getPlanes().get(entry.getValue().getName())).linkDrawingEnabled());
			controls.add(chkBox);
			final JToggleButton tglButton = new JToggleButton(entry.getValue().getName());
			tglButton.addItemListener(new ItemListener() {
				
				@Override
				public void itemStateChanged(ItemEvent e) {
					if(e.getStateChange() == ItemEvent.SELECTED){
						((SocialPlane<String>)l.getPlanes().get(tglButton.getText())).enable();
					} else {
						((SocialPlane<String>)l.getPlanes().get(tglButton.getText())).disable();
					}
				}
				
			});
			if(((SocialPlane<String>)l.getPlanes().get(tglButton.getText())).isEnabled()){
				tglButton.setSelected(true);
			} else {
				tglButton.setSelected(false);
			}
			//tglButton.setPreferredSize(new Dimension(50, tglButton.getHeight()));
			controlsSphereToggleButtons.add(tglButton);
			planeArray[count] = entry.getValue();
			count++;
		}
		//now add all sphere toggle buttons
		for(JComponent component: controlsSphereToggleButtons){
			controls.add(component);
		}
		//now add all buttons at page end
		for(JComponent component: controlsPageEnd){
			controls.add(component);
		}
		
		//EAST PANEL for advanced sphere controls and clustering
		//prepare east panel to hold all components east of the main graph
		JPanel eastPanel = new JPanel();
		eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));
		eastPanel.add(Box.createVerticalStrut(10));
		
		//prepare panel for sphere weights and modification buttons
		final JPanel planeWeightPanel = new JPanel();
		planeWeightPanel.setLayout(new BoxLayout(planeWeightPanel, BoxLayout.Y_AXIS));
		planeWeightPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		//sort array by weight
		sortArray(planeArray);
		//final JPanel sphereTitlePanel = new JPanel(new BorderLayout());
		final JPanel planePanel = new JPanel();
		final JList planeList = new JList(planeArray);
		
		//add buttons to move sphere priority up and down
		JButton btnUp = new JButton("Move Up");
		btnUp.setMargin(new Insets(btnUp.getMargin().top, 3, btnUp.getMargin().bottom, 3));
		JButton btnDown = new JButton("Move Down");
		btnDown.setMargin(new Insets(btnDown.getMargin().top, 3, btnDown.getMargin().bottom, 3));
		btnUp.addActionListener( new ActionListener(){
			
			public void actionPerformed( ActionEvent e ){
				int index = planeList.getSelectedIndex();
				if(index == -1){
					JOptionPane.showMessageDialog(planePanel, "No plane selected for reordering.", "Swapping weight of plane", JOptionPane.ERROR_MESSAGE);
				}
				else if(index > 0){
					Object objectToMove = planeArray[index];
					float weightOfObjectToMove = planeArray[index].weightFactor;
					Object objectToBeMoved = planeArray[index - 1];
					float weightOfObjectToBeMoved = planeArray[index - 1].weightFactor;
					//move spheres but exchange associated weights
					planeArray[index - 1] = (SocialPlane) objectToMove;
					planeArray[index - 1].weightFactor = weightOfObjectToBeMoved;
					planeArray[index] = (SocialPlane) objectToBeMoved;
					planeArray[index].weightFactor = weightOfObjectToMove;
					planeList.setSelectedIndex(index - 1);
				}
			}
		});

		btnDown.addActionListener( new ActionListener(){
			
			public void actionPerformed( ActionEvent e ){
				int index = planeList.getSelectedIndex();
				if(index == -1){
					JOptionPane.showMessageDialog(planePanel, "No plane selected for reordering.", "Swapping weight of plane", JOptionPane.ERROR_MESSAGE);
				}
				else if(index < planeArray.length - 1){
					Object objectToMove = planeArray[index];
					float weightOfObjectToMove = planeArray[index].weightFactor;
					Object objectToBeMoved = planeArray[index + 1];
					float weightOfObjectToBeMoved = planeArray[index + 1].weightFactor;
					//move spheres but exchange associated weights
					planeArray[index + 1] = (SocialPlane) objectToMove;
					planeArray[index + 1].weightFactor = weightOfObjectToBeMoved;
					planeArray[index] = (SocialPlane) objectToBeMoved;
					planeArray[index].weightFactor = weightOfObjectToMove;
					planeList.setSelectedIndex(index + 1);
				}
			}
		});
		
		//add text field for weight entering already to be able to access variable from JList MouseListener
		final JTextField txtWeight = new JTextField("1.0");
		txtWeight.setPreferredSize(new Dimension(75, 20));
		
		planeList.addMouseListener(new MouseListener(){

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(planeList.getSelectedIndex() != -1){
					txtWeight.requestFocus();
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		//add list to spherePanel
		planePanel.add(planeList);
		//create panel only for +/- buttons
		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(btnUp, BorderLayout.CENTER);
		buttonPanel.add(btnDown, BorderLayout.SOUTH);
		//add buttons to sphere selection panel
		planePanel.add(buttonPanel);
		
		//add plane-related stuff to planeWeightPanel
		planeWeightPanel.add(planePanel);
		
		//textfield initialized above JList MouseListener
		//automatically highlight content when clicking into text field
		txtWeight.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				//txtWeight.selectAll();
			}
		});
		txtWeight.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent arg0) {
				txtWeight.selectAll();
			}
			
			@Override
			public void focusGained(FocusEvent arg0) {
				txtWeight.selectAll();
			}
		});
		
		//weight button definition
		final JButton btnSetWeight = new JButton("Set Weight");
		btnSetWeight.setMargin(new Insets(btnSetWeight.getMargin().top, 3, btnSetWeight.getMargin().bottom, 3));
		btnSetWeight.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(planeList.getSelectedIndex() != -1 && !txtWeight.getText().isEmpty()){
					btnSetWeight.requestFocus();
					try{
						Float weight = Float.parseFloat(txtWeight.getText());
						SocialPlane sph = planeArray[planeList.getSelectedIndex()];
						sph.weightFactor = weight;
						sortArray(planeArray);
						planeList.setListData(planeArray);
						//set moved item as selected in list
						for(int i=0; i<planeArray.length; i++){
							if(planeArray[i].getName().equals(sph.getName())){
								planeList.setSelectedIndex(i);
							}
						}
					} catch(NumberFormatException e){
						JOptionPane.showMessageDialog(planeWeightPanel, "Please ensure you entered a float value, not '" + txtWeight.getText() + "'.", "Number format error when changing weights", JOptionPane.ERROR_MESSAGE);
					}
					txtWeight.requestFocus();
				} else {
					JOptionPane.showMessageDialog(planeWeightPanel, "Nothing will happen unless you choose a plane and provide a value.", "Not enough information entered for plane weight change.", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			
		});
		//add key listener to automatically set weight when enter is hit
		txtWeight.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KeyEvent.VK_ENTER
						&& txtWeight.isFocusOwner()){
					btnSetWeight.doClick();
				}
			}
		});
		//add button to unify weights in all spheres
		JButton btnUnifyWeights = new JButton("Unify Weights");
		btnUnifyWeights.setMargin(new Insets(btnUnifyWeights.getMargin().top, 3, btnUnifyWeights.getMargin().bottom, 3));
		btnUnifyWeights.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(!txtWeight.getText().isEmpty()){
					btnSetWeight.requestFocus();
					try{
						Float weight = Float.parseFloat(txtWeight.getText());
						for(int i=0; i<planeArray.length; i++){
							planeArray[i].weightFactor = weight;
						}
						planeList.setListData(planeArray);
					} catch(NumberFormatException e){
						JOptionPane.showMessageDialog(planeWeightPanel, "Please ensure you entered a float value, not '" + txtWeight.getText() + "'.", "Number format error when unifying weights", JOptionPane.ERROR_MESSAGE);
					}
					txtWeight.requestFocus();
				} else {
					JOptionPane.showMessageDialog(planeWeightPanel, "Please provide a weight value.", "Not enough information entered for plane weight change.", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			
		});
		//add button to reset weights to original values
		JButton btnResetWeights = new JButton("Reset Weights");
		btnResetWeights.setMargin(new Insets(btnResetWeights.getMargin().top, 3, btnResetWeights.getMargin().bottom, 3));
		btnResetWeights.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for(int i=0; i<planeArray.length; i++){
					planeArray[i].resetWeight();
				}
				sortArray(planeArray);
			}
			
		});
		//automatically update value in textfield based on clicked sphere
		planeList.addMouseListener(new MouseListener(){

			@Override
			public void mouseClicked(MouseEvent arg0) {
				SocialPlane selectedItem = (SocialPlane) planeList.getSelectedValue();
				txtWeight.setText(selectedItem.weightFactor.toString());
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		//panel holding sphere weight modification functionality
		JPanel modificationPanel = new JPanel();
		modificationPanel.add(txtWeight);
		modificationPanel.add(btnSetWeight);
		modificationPanel.add(btnUnifyWeights);
		modificationPanel.add(btnResetWeights);
		//add to planeWeight panel
		planeWeightPanel.add(modificationPanel);
		
		//panel for center bias + Individual weight activation
		JCheckBox chkCenterBias = new JCheckBox("Center Bias");
		chkCenterBias.setSelected(l.useCenterBias);
		chkCenterBias.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.useCenterBias = true;
				} else {
					l.useCenterBias = false;
				}
				
			}
		});
		//individual weights
		JCheckBox chkUseIndividualWeights = new JCheckBox("Use Individual Weights");
		chkUseIndividualWeights.setSelected(l.useIndividualWeights);
		chkUseIndividualWeights.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.useIndividualWeights = true;
				} else {
					l.useIndividualWeights = false;
				}
				
			}
		});
		
		JPanel centerBiasAndIndivSwitchPanel = new JPanel();
		centerBiasAndIndivSwitchPanel.add(chkCenterBias);
		centerBiasAndIndivSwitchPanel.add(chkUseIndividualWeights);
		//add center bias and individual switch panel to planeWeight panel
		planeWeightPanel.add(centerBiasAndIndivSwitchPanel);
		//finally add all to east panel
		eastPanel.add(planeWeightPanel);
		
		//add small vertical distance before cluster panel
		eastPanel.add(Box.createVerticalStrut(5));
		
		//===== Cluster-related controls ======
		//panel holding all cluster-related components
		final JPanel clusterPanel = new JPanel();
		clusterPanel.setLayout(new BoxLayout(clusterPanel, BoxLayout.Y_AXIS));
		clusterPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		
		//panel for switches
		final JPanel clusterPanelSwitches = new JPanel();
		
		JCheckBox chkClustering = new JCheckBox("Detect Clusters");
		chkClustering.setSelected(l.clusteringOfVertices);
		chkClustering.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.clusteringOfVertices = true;
				} else {
					l.clusteringOfVertices = false;
				}
			}
		});
		clusterPanelSwitches.add(chkClustering);
		JCheckBox chkSubClustering = new JCheckBox("Detect Subclusters");
		chkSubClustering.setSelected(l.attractionClustererActivated());
		chkSubClustering.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.activateAttractionClusterer(true);
				} else {
					l.activateAttractionClusterer(false);
				}
			}
		});
		clusterPanelSwitches.add(chkSubClustering);
		//add tag highlighting switch
		JCheckBox chkTagHighlighting = new JCheckBox("Highlight tags");
		chkTagHighlighting.setSelected(l.highlightTags);
		chkTagHighlighting.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
					l.highlightTags = true;
				} else {
					l.highlightTags = false;
				}
			}
		});
		clusterPanelSwitches.add(chkTagHighlighting);
		//add all cluster/tag-related checkbox to bigger compound panel
		clusterPanel.add(clusterPanelSwitches);
		
		
		//panel for parameters
		final JPanel clusterPanelParameters = new JPanel();
		clusterPanelParameters.setLayout(new BoxLayout(clusterPanelParameters, BoxLayout.Y_AXIS));
		//cluster weight and distance settings
		final JPanel clusterPanelParametersDistance = new JPanel();
		JLabel lblClusterDistance = new JLabel("Max. distance of members");
		clusterPanelParametersDistance.add(lblClusterDistance);
		//cluster distance
		final JTextField txtClusterDistance = new JTextField(String.valueOf(l.maxClusterNeighbourDistance));
		txtClusterDistance.setPreferredSize(new Dimension(40, 20));
		txtClusterDistance.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				txtClusterDistance.selectAll();
			}
		});
		
		txtClusterDistance.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				txtClusterDistance.selectAll();
			}
		});
		
		final JButton btnSetDistance = new JButton("Set new distance");
		btnSetDistance.setMargin(new Insets(btnSetDistance.getMargin().top, 3, btnSetDistance.getMargin().bottom, 3));
		btnSetDistance.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Float value = null;
				btnSetDistance.requestFocus();
				try{
					value = Float.parseFloat(txtClusterDistance.getText());
					if(value != null){
						l.getProximityClusterer().setMaximalDistanceOfClusterMembers(value);
					} 
				} catch(NumberFormatException ex){
					JOptionPane.showMessageDialog(clusterPanelParametersDistance, "Please enter valid number for 'distance'.", "Error when changing maximum distance in cluster", JOptionPane.ERROR_MESSAGE);
				}
				txtClusterDistance.requestFocus();
			}
			
		});
		txtClusterDistance.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KeyEvent.VK_ENTER
						&& txtClusterDistance.isFocusOwner()){
					btnSetDistance.doClick();
				}
			}
		});
		clusterPanelParametersDistance.add(txtClusterDistance);
		clusterPanelParametersDistance.add(btnSetDistance);
		clusterPanelParameters.add(clusterPanelParametersDistance);
		
		//min. cluster members
		final JPanel clusterPanelParametersNumMembers = new JPanel();
		JLabel lblMinMembers = new JLabel("Min. number of members");
		clusterPanelParametersNumMembers.add(lblMinMembers);
		final JTextField txtMinNumberOfMembers = new JTextField(String.valueOf(l.minNumberOfMembersInCluster));
		txtMinNumberOfMembers.setPreferredSize(new Dimension(30, 20));
		txtMinNumberOfMembers.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				txtMinNumberOfMembers.selectAll();
			}
		});
		txtMinNumberOfMembers.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				txtMinNumberOfMembers.selectAll();
			}
		});
		
		final JButton btnSetMinNumberOfMembers = new JButton("Set min. members");
		btnSetMinNumberOfMembers.setMargin(new Insets(btnSetMinNumberOfMembers.getMargin().top, 3, btnSetMinNumberOfMembers.getMargin().bottom, 3));
		btnSetMinNumberOfMembers.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Integer value = null;
				btnSetMinNumberOfMembers.requestFocus();
				try{
					value = Integer.parseInt(txtMinNumberOfMembers.getText());
					if(value != null){
						l.getProximityClusterer().setMinimalNumberOfMembersForCluster(value);
					} 
				} catch(NumberFormatException ex){
					JOptionPane.showMessageDialog(clusterPanelParametersNumMembers, "Please enter valid number for 'minimum number of members'.", "Error when changing minimum number of members", JOptionPane.ERROR_MESSAGE);
				}
				txtMinNumberOfMembers.requestFocus();
			}
			
		});
		txtMinNumberOfMembers.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KeyEvent.VK_ENTER
						&& txtMinNumberOfMembers.isFocusOwner()){
					btnSetMinNumberOfMembers.doClick();
				}
			}
		});
		clusterPanelParametersNumMembers.add(txtMinNumberOfMembers);
		clusterPanelParametersNumMembers.add(btnSetMinNumberOfMembers);
		//construct full parameter panel
		clusterPanelParameters.add(clusterPanelParametersNumMembers);
		//add parameters to cluster planel
		clusterPanel.add(clusterPanelParameters);
		
		//panel containing cluster information for text area
		JPanel clusterPanelFirstLevelInfoBox = new JPanel();
		
		class ListeningTextArea extends JTextPane implements ForceClusterListener<VertexPoint3D>{

			StyledDocument doc = null;
			Style style = null;
			
			public ListeningTextArea(){
				doc = getStyledDocument();
				setStyledDocument(doc);
				style = addStyle("Basis", null);
				setBackground(Color.DARK_GRAY);
			}
			
			@Override
			public void receiveClusterResults(final LinkedHashMap<ArrayList<VertexPoint3D>, Color> clusters, Integer totalNumberOfAgents) {
				setForeground(Color.WHITE);
				setText(new StringBuilder("").append("Detected ").append(clusters.size()).append(" clusters.").append(LINE_SEPARATOR).toString());
				for(Entry<ArrayList<VertexPoint3D>, Color> entry: clusters.entrySet()){
					//Style style = this.addStyle("Style", null);
					Color tempCol = entry.getValue();
					//convert color to non-transparent
					StyleConstants.setForeground(style, new Color(tempCol.getRed(), tempCol.getGreen(), tempCol.getBlue()));
					int sizeOfCluster = entry.getKey().size();
					float quota = sizeOfCluster / (float)totalNumberOfAgents;
					try {
						doc.insertString(doc.getLength(), new StringBuilder("").append("Size: ")
								.append(entry.getKey().size())
								.append(", Quota: ").append(quota).append(LINE_SEPARATOR).toString(), style);
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		}
		ListeningTextArea tarClusterCount = new ListeningTextArea();
		l.registerClusterListener(tarClusterCount);
		JScrollPane scrollPane = new JScrollPane(tarClusterCount);
		scrollPane.setPreferredSize(new Dimension(330, 150));
		clusterPanelFirstLevelInfoBox.add(scrollPane);
		//add first level info box to cluster panel
		clusterPanel.add(clusterPanelFirstLevelInfoBox);
		
		//panel containing second level cluster information
		JPanel clusterPanelSecondLevelInfoBox = new JPanel();
		
		class ColorListeningTextArea extends JTextPane implements SecondaryColorsListener, TagDistributionListener{

			StyledDocument doc = null;
			Style style = null;
			
			public ColorListeningTextArea(){
				doc = getStyledDocument();
				setStyledDocument(doc);
				style = addStyle("Basis", null);
				setBackground(Color.DARK_GRAY);
			}
			
			@Override
			public void receiveUpdatedSecondaryColors(
					HashMap<String, Color> secondaryColors) {
				setText("");
				setForeground(Color.WHITE);
				//setText("Detected " + secondaryColors.size() + " clusters.\n");
				for(Entry<String, Color> entry: secondaryColors.entrySet()){
					//Style style = this.addStyle("Style", null);
					Color tempCol = entry.getValue();
					if(tempCol != null){
						//convert color to non-transparent
						StyleConstants.setForeground(style, new Color(tempCol.getRed(), tempCol.getGreen(), tempCol.getBlue()));
						try {
							doc.insertString(doc.getLength(), new StringBuilder("")
									.append(entry.getKey())
									.append(LINE_SEPARATOR).toString(), style);
						} catch (BadLocationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}

			//from TagDistributionListener - will only be called if second-level clustering is deactivated
			@Override
			public void receiveTagDistribution(HashMap<Tag, Integer> tags) {
				if(tags.isEmpty()){
					setText("No Tags registered.");
				} else {
					setText("");
				}
				setForeground(Color.WHITE);
				for(Entry<Tag, Integer> entry: tags.entrySet()){
					try {
						doc.insertString(doc.getLength(), new StringBuilder("")
							.append(entry.getKey()).append(": ").append(entry.getValue()).append(LINE_SEPARATOR).toString(), null);
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						return;
					}
				}
			}
			
		}
		ColorListeningTextArea tarColorList = new ColorListeningTextArea();
		//register for color listening
		l.registerSecondaryColorsListener(tarColorList);
		//register for tag distribution updates
		l.registerTagDistributionListener(tarColorList);
		JScrollPane scrollPane2 = new JScrollPane(tarColorList);
		scrollPane2.setPreferredSize(new Dimension(330, 100));
		clusterPanelSecondLevelInfoBox.add(scrollPane2);
		//add second-level info box to cluster panel
		clusterPanel.add(clusterPanelSecondLevelInfoBox);
		
		//'Show tags' checkbox
		final JPanel clusterPanelShowTagsButton = new JPanel();
		//individual tags
		final JCheckBox chkShowIndividualTags = new JCheckBox("Show individual tags");
		chkShowIndividualTags.setSelected(l.drawTagsOnGlassPane);
		chkShowIndividualTags.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(chkShowIndividualTags.isSelected()){
					l.drawTagsOnGlassPane = true;
				} else {
					l.drawTagsOnGlassPane = false;
				}
			}
			
		});
		clusterPanelShowTagsButton.add(chkShowIndividualTags);
		//cluster tags
		final JCheckBox chkShowClusterTags = new JCheckBox("Show cluster tags");
		chkShowClusterTags.setSelected(l.printClusterStats);
		chkShowClusterTags.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(chkShowClusterTags.isSelected()){
					l.printClusterStats = true;
				} else {
					l.printClusterStats = false;
				}
			}
			
		});
		clusterPanelShowTagsButton.add(chkShowClusterTags);
		clusterPanel.add(clusterPanelShowTagsButton);
		
		//finally add cluster panel east panel
		eastPanel.add(clusterPanel);
		
		//panel for switch for link printing in 2D and 3D
		final JPanel printSwitchPanel = new JPanel();
		//checkBox for 2D link printing
		final JCheckBox chkPrint2d = new JCheckBox("Print 2D links");
		chkPrint2d.setSelected(l.print2dLines());
		chkPrint2d.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(chkPrint2d.isSelected()){
					l.activate2dLines(true);
				} else {
					l.activate2dLines(false);
				}
			}
		});
		printSwitchPanel.add(chkPrint2d);
		//checkBox for 3D link printing
		final JCheckBox chkPrint3d = new JCheckBox("Print 3D links");
		chkPrint3d.setSelected(l.print3dLines());
		chkPrint3d.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(chkPrint3d.isSelected()){
					l.activate3dLines(true);
				} else {
					l.activate3dLines(false);
				}
			}
		});
		printSwitchPanel.add(chkPrint3d);
		//add to east panel
		eastPanel.add(printSwitchPanel);
		
		//frame rate check box
		/* TODO last panel used should be BorderLayout to allow NORTH positioning of elements
		 * needs to be moved to next lower panel if new one is added to ensure alignment of 
		 * components to top
		 */
		JPanel utilityPanel = new JPanel(new BorderLayout());
		JCheckBox chkFrameRate = new JCheckBox("Show Frame Rate");
		chkFrameRate.setSelected(true);
		chkFrameRate.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				try{
					if(e.getStateChange() == ItemEvent.SELECTED){
						((ForceGlassPane)graphFrame.getGlassPane()).activateFrameRateCalculation(true);
					} else {
						((ForceGlassPane)graphFrame.getGlassPane()).activateFrameRateCalculation(false);
					}
				} catch(ClassCastException ex){
					JOptionPane.showMessageDialog(graphFrame, "Cannot toggle frame rate as force frame is run separately from control frame.");
				}
			}
		});
		//NORTH alignment within panel
		utilityPanel.add(chkFrameRate, BorderLayout.NORTH);
		JButton chkRepaint = new JButton("Repaint (switches to stepping scheduler)");
		chkRepaint.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				l.manualStep();
				graphFrame.repaint();
			}
			
		});
		utilityPanel.add(chkRepaint);
		//eastPanel.add(Box.createVerticalStrut(20));
		eastPanel.add(utilityPanel);
		
		
		//mouse modes
		/*JComboBox comboBox = ((DefaultModalGraphMouse)vv.getGraphMouse()).getModeComboBox();
		comboBox.addItemListener(((DefaultModalGraphMouse)vv.getGraphMouse()).getModeListener());
		controls.add(comboBox);*/
		if(positionControlPanelToEast){
			graphFrame.getContentPane().add(eastPanel, BorderLayout.EAST);
		} else {
			graphFrame.getContentPane().add(eastPanel);
		}
		graphFrame.getContentPane().add(controls, BorderLayout.PAGE_END);
	}
	
	private static void sortArray(SocialPlane[] sphereArray){
		boolean swapped = false;
		do{
			swapped = false;
			for(int i=1; i<sphereArray.length; i++){
				if(sphereArray[i].weightFactor > sphereArray[i-1].weightFactor){
					Object origValue = sphereArray[i-1];
					sphereArray[i-1] = sphereArray[i];
					sphereArray[i] = (SocialPlane) origValue;
					swapped = true;
				}
			}
		} while(swapped);
	}
	
}
