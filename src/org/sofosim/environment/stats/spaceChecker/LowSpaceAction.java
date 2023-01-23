package org.sofosim.environment.stats.spaceChecker;

public interface LowSpaceAction {

	/**
	 * Action performed upon detection of low space by SpaceChecker.
	 */
	public void performLowSpaceAction();
	
	/**
	 * Action performed once low space issue is resolved.
	 */
	public void releaseLowSpaceAction();
	
	/**
	 * Indicates if low space action is currently active (has been applied but not yet reset).
	 * @return
	 */
	public boolean lowSpaceActionActive();
	
}
