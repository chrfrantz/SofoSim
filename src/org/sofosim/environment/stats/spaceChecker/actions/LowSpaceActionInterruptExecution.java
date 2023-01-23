package org.sofosim.environment.stats.spaceChecker.actions;

import org.sofosim.environment.GridSim;
import org.sofosim.environment.stats.spaceChecker.LowSpaceAction;

public class LowSpaceActionInterruptExecution implements LowSpaceAction{

	private GridSim sim;
	
	/**
	 * This implementation of LowSpaceAction interrupts simulation execution.
	 * @param sim
	 */
	public LowSpaceActionInterruptExecution(GridSim sim) {
		this.sim = sim;
	}
	
	@Override
	public void performLowSpaceAction() {
		sim.interruptExecution = true;
	}

	@Override
	public void releaseLowSpaceAction() {
		sim.interruptExecution = false;
	}

	@Override
	public boolean lowSpaceActionActive() {
		return sim.interruptExecution;
	}

}
