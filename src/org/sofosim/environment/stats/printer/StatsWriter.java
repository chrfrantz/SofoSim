package org.sofosim.environment.stats.printer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Properties;

import org.sofosim.environment.stats.OrderedProperties;

public abstract class StatsWriter {

	private String filename = null;
	private static String subfolderName = null;
	private File file = null;
	private static String prefix = "FileWriter: ";
	private FileOutputStream fileOutStream = null;
	private OutputStreamWriter outStreamWriter = null;
	private final int minimumFreeSpaceInMB = 50;
	public boolean performSizeCheck = false;
	
	/** Global disabler of writing any output. */
	public static boolean enabled = true;
	
	private static boolean osTypePrinted = false;
	protected static boolean debug = false;
	
	/**
	 * Switch to memorize that append message has been printed (and not do it repeatedly)
	 */
	private boolean printedAppendMessage = false;
	/**
	 * Indicates if appending of data to existing file is allowed.
	 */
	private boolean appendData = false;
	/**
	 * Indicates if overwriting of existing file is allowed. Overrides appending of data!
	 */
	private boolean overwriteFiles = false;
	
	/**
	 * Initializes the StatsWriter with a given 
	 * filename.
	 * @param filename Filename to write to
	 */
	public StatsWriter(String filename){
		setFilename(filename);
	}
	
	/**
	 * Initializes a new StatsWriter instance with a given filename.
	 * Allows configuration whether new data should be appended if 
	 * file exists. Overwrite will overwrite file instead (delete and write new one).
	 * @param filename Filename to write to
	 * @param allowAppend indicates if appending to existing files is allowed
	 * @param overwrite indicates whether files should be overwritten instead of appending
	 */
	public StatsWriter(String filename, boolean allowAppend, boolean overwrite){
		setFilename(filename);
		this.appendData = allowAppend;
		this.overwriteFiles = overwrite;
	}
	
	/**
	 * Returns the filename data will be written to. 
	 * The filename does not include the directory path.
	 * @return
	 */
	public synchronized String getFilename(){
		return filename;
	}
	
	/**
	 * Upon setting a new file name, a file
	 * opened with another name will be closed.
	 * @param filename
	 */
	public synchronized void setFilename(String filename){
		checkForSlashesInStrings(filename, true);
		printedAppendMessage = false;
		if(this.filename == null){
			this.filename = filename;
			return;
		}
		if(!this.filename.equals(filename)){
			close();
			file = null;
			//set new filename
			this.filename = filename;
		}
		//else filename is already set and necessarily the same
	}
	
	/**
	 * Sets the subfolder name for all applications that access 
	 * StatsWriter across all StatsWriter instances for output purposes.
	 * @param subfolderName
	 */
	public static synchronized void setGlobalSubfolderName(String subfolderName){
		checkForSlashesInStrings(subfolderName, false);
		StatsWriter.subfolderName = ensureOsCompatibleDirectorySeparator(subfolderName);
		System.out.println(prefix + "Set global subfolder '" + StatsWriter.subfolderName + "'.");
	}
	
	/**
	 * Specifies if appending to existing file is allowed.
	 * @param allow
	 */
	public void allowAppendingToFile(boolean allow){
		this.appendData = allow;
	}
	
	/**
	 * Write functionality. Opens resources if necessary but does 
	 * not close them (for performance). Use close() to close all
	 * resources.
	 * @param dataToWrite Data to write. Yet only String and StringBuffer are supported.
	 */
	public synchronized void write(Object dataToWrite){
		if(enabled){
			if(file == null){
				if(filename == null){
					System.err.println(prefix + "No filename provided. Writing aborted.");
					return;
				} else {
					file = null;
					try {
						if(StatsWriter.subfolderName != null && !StatsWriter.subfolderName.equals("")){
							file = getDirectory(StatsWriter.subfolderName);
							if(!file.isDirectory()){
								if(!file.mkdirs()){
									System.err.println(prefix + "Failed when creating subfolder " + StatsWriter.subfolderName + ".");
									return;
								}
							}
						}
						//if no subfolder is provided, retrieve a file reference pointing to the current user.dir folder
						file = getFile(StatsWriter.subfolderName);
						if(debug){
							System.out.println(prefix + "Testing on existence of file " + file.getAbsolutePath());
						}
						if(!file.createNewFile()){
							if(overwriteFiles){
								if(!file.delete()){
									System.err.println(prefix + "Deletion of file " + file.getName() + " failed. Writing aborted.");
									return;
								} else {
									//second attempt after deletion
									if(!file.createNewFile()){
										System.err.println(prefix + "File " + file.getName() + " could not be created after deletion of previous file. Writing aborted.");
										return;
									} else {
										System.out.println(prefix + "Writing request for existing file. Will overwrite existing file '" + file.getName() + "'.");
									}
								}
							} else {
								if(!appendData){
									System.err.println(prefix + "File with name " + file.getName() + " already exists. Writing aborted as appending (via allowAppendingToFile()) is not activated.");
									return;
								} else {
									if(!printedAppendMessage){
										//print information only once, else considerable I/O overhead for repeated append operations.
										System.out.println(prefix + "Writing request for existing file. Will try to append data to existing file '" + file.getName() + "'.");
										printedAppendMessage = true;
									}
								}
							}
						}
					} catch (IOException e) {
						System.err.println(prefix + "File " + file.getAbsolutePath() + " could not be created or accessed.\n" +
								"Ensure that you did not specify folders as part of the file name but via setGlobalSubfolderName().\n" +
								"See error details in following exception:");
						e.printStackTrace();
						file = null;
						return;
					}
				}
			}
			if(file != null){
				if(performSizeCheck) {
					try {
						if(Files.getFileStore(file.toPath()).getUsableSpace() < minimumFreeSpaceInMB * 1024 * 1024){
							System.err.println(prefix + "Not enough free space on drive (less than " + minimumFreeSpaceInMB + " MB, available: " + file.getUsableSpace() + ") to write file " + file.getAbsolutePath() + ".");
							close();
							return;
						}
					} catch (IOException e) {
						System.err.println(prefix + "Problems when determining free space to write file " + file.getName() + ". Writing aborted.");
						e.printStackTrace();
						return;
					}
				}
				
				if(fileOutStream == null){
					try {
						fileOutStream = new FileOutputStream(file, appendData);
					} catch (FileNotFoundException e) {
						System.err.println(prefix + "File " + file.getName() + " has not been found. Writing aborted.");
						return;
					}
				}
				
				boolean written = false;
				
				if(dataToWrite.getClass().equals(String.class) || dataToWrite.getClass().equals(StringBuffer.class)){
					if(outStreamWriter == null){
						outStreamWriter = new OutputStreamWriter(fileOutStream);
					}
					try {
						outStreamWriter.write(dataToWrite.toString());
						outStreamWriter.flush();
					} catch (IOException e) {
						System.err.println(prefix + "Error when writing to file " + file.getName() + ".");
						e.printStackTrace();
						return;
					}
					written = true;
					
				}
				//special handling for Java Properties objects
				if(dataToWrite.getClass().equals(Properties.class)){
					try {
						((Properties)dataToWrite).store(fileOutStream, null);
					} catch (IOException e) {
						System.err.println(prefix + "Error when properties to file " + file.getName() + ".");
						e.printStackTrace();
						return;
					}
					written = true;
				}
				//special handling for OrderedProperties (ordered version of properties)
				if(dataToWrite.getClass().equals(OrderedProperties.class)){
					try {
						((OrderedProperties)dataToWrite).store(fileOutStream, null);
					} catch (IOException e) {
						System.err.println(prefix + "Error when properties to file " + file.getName() + ".");
						e.printStackTrace();
						return;
					}
					written = true;
				}
				
				if(debug && written){
					System.out.println(prefix + "Wrote data to file " + file.getName() + ": " + dataToWrite.toString());
				}
				if(!written){
					System.err.println(prefix + "Provided data type not supported for output.");
				}
			} else {
				System.err.println(prefix + "File opening operations for file '" + filename + "' have failed.");
			}
		}
	}
	
	public void writeAndClose(Object dataToWrite){
		write(dataToWrite);
		close();
	}
	
	/**
	 * Attempts to delete stats data file. Returns true when succeeding or if 
	 * no file available yet. False is returned if file exists but cannot be deleted.
	 * @return
	 */
	public boolean deleteFile(){
		if(enabled){
			if(file != null){
				if(file.exists()){
					if(debug){
						System.out.println(prefix + "Closing data file before deleting.");
					}
					close();
					if(debug){
						System.out.println(prefix + "Attempting to delete stats data file '" + filename + "'.");
					}
					return file.delete();
				} else {
					System.out.println(prefix + "Aborted stats data file deletion as it is non-existent.");
					return true;
				}
			}
		}
		return true;
	}
	
	private File getDirectory(String subfolderName){
		return getFileOrDirectory(subfolderName, true);
	}
	
	private File getFile(String subfolderName){
		return getFileOrDirectory(subfolderName, false);
	}
	
	/**
	 * Returns the full pathname to the project folder for file creation, 
	 * including a subfolder if passed as parameter (only name, no back-/slashes).
	 * @param subfolderName Subfolder name
	 * @param getDirectory Indicates whether only directory name should be returned (as opposed filename)
	 * @return
	 */
	private File getFileOrDirectory(String subfolderName, boolean getDirectory) {
        File propertiesFile = null;
        
        String dirSeparator = getOsDependentDirectorySeparator();
    	String tempFilename = "";
    	if(subfolderName != null && !subfolderName.equals("")){
    		tempFilename = subfolderName;
    	}
    	if(!getDirectory){
    		tempFilename += dirSeparator + filename;
    	}
        propertiesFile = new File(System.getProperty("user.dir")
                + dirSeparator + tempFilename);
            
        if (debug) {
        	String placeHolder = "file";
        	if(getDirectory){
        		placeHolder = "directory";
        	}
            System.out.println(prefix + "Path of " + placeHolder + " is " + propertiesFile.toString());
        }
        return propertiesFile;
    }

	/**
	 * Returns operating system-dependent directory separator.
	 * Supports UNIX/Linux and Windows.
	 * @return
	 */
	public static String getOsDependentDirectorySeparator(){
		if ((System.getProperty("user.dir")).substring(0, 1).equals("/")) {
			//UNIX
			printOsDebug("UNIX/Linux");
			return "/";
		}
		if ((System.getProperty("user.dir")).substring(1, 2).equals(":")) {
			//Windows
			printOsDebug("Windows");
			return "\\";
		}
		System.err.println(prefix + "Could not recognize operating system!");
		return null;
	}
	
	/**
	 * Checks if a given filename or subfolder name contains slashes or backslashes. 
	 * For files these are not allowed, for directories they are automatically replaced 
	 * in an OS-dependent manner.
	 * @param directoryOrFileName Folder or file name to test (may also be null or empty)
	 * @param isFileName Indicates if input should be treated as filename (no slashes allowed), else directory
	 * @return corrected subfolder name (if slash replacement has taken place)
	 */
	private static String checkForSlashesInStrings(String directoryOrFileName, boolean isFileName){
		if(directoryOrFileName != null && !directoryOrFileName.equals("")){
			if(isFileName){
				//files may not contains backslashes or slashes
				if(directoryOrFileName.contains("/") || directoryOrFileName.contains("\\")){
					throw new IllegalArgumentException("The specified subfolder or filename '" + directoryOrFileName + "' name must not include slashes or backslashes!");
				}
			} else {
				//directory
				return ensureOsCompatibleDirectorySeparator(directoryOrFileName);
			}
		}
		return directoryOrFileName;
	}
	
	/**
	 * Ensures OS-compatible directory separators if given path consists of 
	 * multiple directories (e.g. "results/first"), i.e. converts them into 
	 * respective formats (e.g. "results\\first" for Windows).
	 * @param folderNames
	 * @return
	 */
	public static String ensureOsCompatibleDirectorySeparator(String folderNames){
		if(folderNames.contains("\\")){
			folderNames = folderNames.replace("\\", getOsDependentDirectorySeparator());
		}
		if(folderNames.contains("/")){
			folderNames = folderNames.replace("/", getOsDependentDirectorySeparator());
		}
		return folderNames;
	}
	
	private static void printOsDebug(String OS){
		if (debug && !osTypePrinted) {
            if (debug) {
                System.out.println(prefix + "Detected " +  OS + " system ....");
            }
            osTypePrinted = true;
        }
	}
	
	/**
	 * Closes all open resources
	 */
	public void close(){
		if(enabled){
			//check on open file
			if(outStreamWriter != null){
				try {
					outStreamWriter.close();
				} catch (IOException e) {
					System.err.println(prefix + "Error when closing OutStreamWriter for file " + file.getName() + ".");
					e.printStackTrace();
				}
				outStreamWriter = null;
			}
			if(fileOutStream != null){
				try {
					fileOutStream.close();
				} catch (IOException e) {
					System.err.println(prefix + "Error when closing FileOutputStream for file " + file.getName() + ".");
					e.printStackTrace();
				}
				fileOutStream = null;
			}
			//file = null;
			if(debug){
				System.out.println(prefix + "Closed file " + filename + ".");
			}
		}
	}
}
