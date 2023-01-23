package org.sofosim.environment.stats.spaceChecker;

import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JOptionPane;

public class SpaceChecker {

	private Path pathToDrive = null;
	private int minimumMb = 0;
	private long timeoutInMs = 120000;
	Thread thread = null;
	private LowSpaceAction action = null;
	private boolean showGuiForRecheck = true;
	private boolean showConsoleOutput = true;
	
	/**
	 * Instantiates SpaceChecker for given path that ensures a minimum of free space 
	 * or shows message to user. If not given (i.e. null), current user directory 
	 * will be used. Minimum MB level is 1, else checker will not start.
	 * If minimum frequency is below 1 minute, default value of 2 minutes will be used.
	 * Specify an action performer to specify a low action activity.
	 * @param path
	 * @param minimumFreeMb
	 * @param timeoutInMinutes
	 * @param action Action to be executed upon low memory detection
	 * @param showGuiForRecheck Indicates if GUI is shown that allows user to initiate recheck
	 */
	public SpaceChecker(String path, int minimumFreeMb, int timeoutInMinutes, LowSpaceAction action, boolean showGuiForRecheck) {
		if(path == null || path.isEmpty()) {
			//Use project directory
			path = System.getProperty("user.dir");
		}
		this.pathToDrive = new File(path).toPath();
		this.minimumMb = minimumFreeMb;
		if(timeoutInMinutes > 1) {
			this.timeoutInMs = timeoutInMinutes * 60000;
		}
		if(action != null) {
			this.action = action;
		}
		this.showGuiForRecheck = showGuiForRecheck;
	}
	
	public void setTimeout(int timeoutInMinutes) {
		this.timeoutInMs = timeoutInMinutes * 60000;
		System.out.println("SpaceChecker: SpaceChecker frequency set to " + timeoutInMinutes + " minutes.");
	}
	
	/**
	 * Starts SpaceChecker.
	 */
	public void start() {
		if(minimumMb < 1) {
			System.err.println("SpaceChecker: No need for checking free space, since only alarming once space is used up.");
			return;
		}
		if(pathToDrive == null) {
			System.err.println("SpaceChecker: Path to drive is not configured.");
			return;
		}
		thread = new Thread(new Runnable(){

			@Override
			public void run() {
				System.out.println("SpaceChecker: Checking path " + pathToDrive.toString() + 
						" for free space threshold of " + minimumMb + " MB every " + 
						(timeoutInMs / (float)60000) + " minutes.");
				while(true) {
					try {
						if((Files.getFileStore(pathToDrive).getUsableSpace() / (1024 * 1024)) < minimumMb) {
							//JOptionPane.showMessageDialog(null, "Your free space on drive " + pathToDrive.toString() + " is below " + minimumMb + " MB.", "Disk space limit reached", JOptionPane.ERROR_MESSAGE);
							if(showConsoleOutput) {
								System.out.println("SpaceChecker: Below minimal free space (" + minimumMb + " MB).");
							}
							if(action != null) {
								if(!action.lowSpaceActionActive()) {
									if(showConsoleOutput) {
										System.out.println("SpaceChecker: Performing low space action.");
									}
									action.performLowSpaceAction();
								}
							}
							if(showGuiForRecheck) {
								int response = JOptionPane.showConfirmDialog(null, "Your free space on drive " + pathToDrive.toString() + 
										" is below " + minimumMb + " MB.\nClick 'Yes' to recheck free space immediately.", 
										"Disk space limit reached",  JOptionPane.YES_NO_OPTION);
								if(response == JOptionPane.YES_OPTION) {
									if(showConsoleOutput) {
										System.out.println("Performing recheck...");
									}
									continue;
								}
							}
						} else {
							if(action != null) {
								if(action.lowSpaceActionActive()) {
									if(showConsoleOutput) {
										System.out.println("SpaceChecker: Releasing low space action.");
									}
									action.releaseLowSpaceAction();
								}
							}
						}
						
					} catch (HeadlessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						Thread.sleep(timeoutInMs);
					} catch (InterruptedException e) {
						//interruption will be intentional
						break;
					}
				}
			}
			
		});
		thread.start();
	}
	
	/**
	 * Indicates whether SpaceChecker is running.
	 * @return
	 */
	public boolean isRunning() {
		return thread != null && thread.isAlive();
	}
	
	/**
	 * Stops SpaceChecker.
	 */
	public void stop() {
		if(thread != null) {
			thread.interrupt();
			System.out.println("SpaceChecker: Checking stopped.");
			thread = null;
		}
	}
	
}
