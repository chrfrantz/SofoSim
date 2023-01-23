package org.sofosim.batching;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.frogberry.windowPositionSaver.PositionSaver;
import org.nzdis.micro.MTConnector;
import org.sofosim.environment.GridSim;

import sim.display.GUIState;

public abstract class BatchedExperiments {

	
	public abstract void batchSpecification();
	
	/**
	 * Steps a given Mason UI instance for a given number of rounds.
	 * @param ui Mason simulation UI instance
	 * @param rounds Number of rounds it should be stepped for
	 * @param id Description of simulation used in console output to indicate start and stopping of stepping
	 */
	public static void stepUi(GUIState ui, int rounds, String id){
		System.out.println(MTConnector.getCurrentTimeString(true) + ": Start of simulation " + id);
		ui.start();
		waitALittle();
		while(ui.state.schedule.getSteps() < rounds){
			ui.step();
			//Check if execution has been interrupted (e.g. because of lack of space)
			if(((GridSim)ui.state).interruptExecution) {
				System.out.println(MTConnector.getCurrentTimeString(true) + 
						": Simulation execution interrupted by switching GridSim interruptExecution switch.");
				while(((GridSim)ui.state).interruptExecution) {
					waitALittle();
				}
			}
		}
		ui.finish();
		PositionSaver.shutdown();
		System.out.println(MTConnector.getCurrentTimeString(true) + ": Finished simulation " + id);
	}
	
	/**
	 * Blocks further processing for 5 seconds.
	 */
	private static void waitALittle(){
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Appends a string of input parameters along with generated simulation results folder name to a given outfile.
	 * The purpose of this is to log completed simulation runs. To be called after simulation start() returns, so it 
	 * is executed as the last step of the execution of a simulation instance.
	 * @param logFile Log file to write to
	 * @param simulationParams Simulation params as string array
	 * @param simulationFolderName Folder name for simulation results
	 */
	public static void writeCompletedParameterLog(String logFile, String[] simulationParams, String simulationFolderName) {
		// Collect input parameters and write to debug outfile
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < simulationParams.length; i++) {
        	buffer.append(simulationParams[i]).append(" ");
        }
        // Append results folder name
        buffer.append("# ").append(simulationFolderName).append(System.getProperty("line.separator"));
        // Write the string to outfile (append)
        try {
			FileUtils.writeStringToFile(new File(logFile), buffer.toString(), true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
