package org.sofosim.environment.stats.printer;

public class StatsDataWriter extends StatsWriter {

	/**
	 * Instantiates new StatsDataWriter with given filename.
	 * @param filename Filename to write to
	 */
	public StatsDataWriter(String filename) {
		super(filename);
	}
	
	/**
	 * Instantiates new StatsDataWriter with given filename and 
	 * additional option to append data to existing file, or to 
	 * overwrite it (overwrite overrides appending)
	 * @param filename Filename to write to
	 * @param appendData Append data to existing file
	 * @param overwriteFile Delete and recreate file and the write to it
	 */
	public StatsDataWriter(String filename, boolean appendData, boolean overwriteFile){
		super(filename, appendData, overwriteFile);
	}

}
