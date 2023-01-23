package org.sofosim.forceLayout;

import java.util.HashSet;
import org.nzdis.micro.DefaultSocialRole;
import org.nzdis.micro.inspector.annotations.Inspect;
import org.nzdis.micro.messaging.MTRuntime;
import org.sofosim.clustering.VertexPoint3D;
import org.sofosim.environment.GridSim;
import org.sofosim.graph.initializers.ForceGraphInitializer;
import org.sofosim.nadico.CommunicationSpace;
import org.sofosim.nadico.NAdico;
import org.sofosim.tags.Tag;

/**
 * Provides method signatures for plane weight and tag handling. 
 * Signatures must be preceded by 'public', else Scala won't be able 
 * to implement them in a subclass (Lesson learned)
 * 
 * @author cfrantz
 *
 * @param <T>
 */
public abstract class IndivWeightProvider<T> extends DefaultSocialRole implements VertexDebugOutputListener<VertexPoint3D<String>>{

	protected GridSim sim = null;
	
	public IndivWeightProvider(){
		
	}
	
	public IndivWeightProvider(final GridSim sim) {
		this.sim = sim;
	}

	private HashSet<T> tags = new HashSet<T>();
	
	public void addTag(T tag){
		tags.add(tag);
		//System.out.println(me() + " have added tag " + tag);
		CommunicationSpace.shareTags(me(), (HashSet<Tag>) tags);
	}
	
	public void removeTag(T tag){
		tags.remove(tag);
		CommunicationSpace.shareTags(me(), (HashSet<Tag>) tags);
	}
	
	public void removeAllTags(){
		tags.clear();
		CommunicationSpace.shareTags(me(), (HashSet<Tag>) tags);
	}
	
	@Inspect
	public HashSet<T> getTags(){
		return tags;
	}
	
	public boolean haveTags(){
		return !tags.isEmpty();
	}
	
	/**
	 * Is called during every execution round in order to get 
	 * the weight for an individual plane.
	 * @param plane
	 * @return weight for that plane
	 */
	public abstract Float getPlaneWeight(String plane);
	
	/**
	 * Is called during every execution round in order to get 
	 * the individual's weight for a given tag.
	 * @param tag
	 * @return weight for the given tag
	 */
	public abstract Float getTagWeight(T tag);
	
	private boolean weightProviderRegistered = false;
	
	/**
	 * Checks if this agent is registered as weight provider for planes.
	 * Registers it if not done so.
	 */
	public void checkForWeightProviderRegistration(){
		if(sim.graphHandler != null && !weightProviderRegistered){
	      ((ForceDirectedLayout)sim.graphHandler.getGraphInitializer(ForceGraphInitializer.FORCES_GRAPH).getLayout()).registerWeightProvider(new VertexPoint3D<String>(me()), this);
	      weightProviderRegistered = true;
	    }
	}
	
	/**
	 * Returns text that is to be printed on the GlassPane or other form of UI.
	 * By default it prints associated tags and eventual rules. However, it 
	 * can be overridden to specify custom output.
	 * @return
	 */
	@Inspect
	public String getTextToPrint(){
		StringBuffer out = new StringBuffer();
		
		if(CommunicationSpace.showAgentNameOnIndividualUi){
			out.append(me()).append(MTRuntime.LINE_DELIMITER);
		}
		
		if(CommunicationSpace.showSuggestedRulesOnIndividualUi){
			NAdico suggestedRule = CommunicationSpace.getSuggestedRule(me());
			if(suggestedRule != null){
				out.append("Suggested Rule: ");
				out.append(suggestedRule).append(MTRuntime.LINE_DELIMITER);
			}
		}
		if(CommunicationSpace.showCodifiedRulesOnIndividualUi){
			HashSet<NAdico> rules = CommunicationSpace.getCodifiedRules(me());
			if(rules != null){
				out.append("Rules: ");
				for(NAdico rule: rules){
					out.append(rule).append(MTRuntime.LINE_DELIMITER);
				}
			}
		}
		if(CommunicationSpace.showTagsOnIndividualUi && haveTags()){
			out.append(getTags().toString());
		}
		return out.toString();
	}

	private StringBuffer debugOutput = new StringBuffer();
	//steps of non-use of debugOutput after which this instance unsubscribes its listener
	private int idleDebugRoundsBeforeDeactivation = 25;
	//last step debug has been called
	private long lastRoundOfDebugCall = 0l;
	//indicates if listener is initialized
	private boolean debugInitialized = false;
	
	@Inspect
	public StringBuffer debugOutput(){
		if(sim == null){
			throw new RuntimeException("Ensure to assign a GridSim reference to IndivWeightProvider before attempting to activate debug output.");
		}
		//also check for deactivated graph handler (i.e. no forces)
		if(!debugInitialized && sim.graphHandler != null){
			((ForceDirectedLayout)sim.graphHandler.getGraphInitializer(ForceGraphInitializer.FORCES_GRAPH).getLayout()).registerVertexDebugOutputListener(this);
			System.out.println("Registered outputlistener for " + me());
			debugInitialized = true;
		}
		lastRoundOfDebugCall = sim.schedule.getSteps();
		return debugOutput;
	}
	
	/**
	 * Checks for the deregistration of debug listeners.
	 */
	public void checkForDebugOutputDeregistration(){
		//also check for deactivated graph handler (i.e. no forces)
		if(debugInitialized && sim.graphHandler != null){
		    if(sim.schedule.getSteps() - lastRoundOfDebugCall >= idleDebugRoundsBeforeDeactivation){
		      ((ForceDirectedLayout)sim.graphHandler.getGraphInitializer(ForceGraphInitializer.FORCES_GRAPH).getLayout()).deregisterVertexDebugOutputListener(this);
		      debugOutput = new StringBuffer();
		      debugInitialized = false;
		      System.out.println("Deregistered outputlistener for " + me());
		    }
	    }
	}
	
	public void receiveForceDebugOutput(StringBuffer debugOutput){
		this.debugOutput = debugOutput;
	}
	
	public VertexPoint3D<String> getVertexOfInterest(){
		return new VertexPoint3D<String>(me(), null);
	}
	
}
