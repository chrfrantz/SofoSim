package org.sofosim.environment.stats.printer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipWriter {

	protected class StringOutputStream extends OutputStream {
		
		StringBuilder mBuf = new StringBuilder();

		  public void write(int bite) throws IOException {
		    mBuf.append((char) bite);
		  }

		  public StringBuilder getString() {
		    return mBuf;
		  }
	}
	
	
	/**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
	
    /**
     * Reads a given zip file and extracts the content of the first contained file 
     * and returns it as ArrayList containing the individual line entries.
     * @param filename Zip file to extract file from
     * @return
     */
	public static List<String> readDataFromZipFile(String filename) {
		if(filename == null || filename.isEmpty()) {
			System.err.println("ZipWriter: Empty filename is not permissible (read()).");
			return null;
		}
		
		//Inspired by: http://www.codejava.net/java-se/file-io/programmatically-extract-a-zip-file-using-java
		try {
			ZipInputStream zipIn = new ZipInputStream(new FileInputStream(filename));
	        ZipEntry entry = zipIn.getNextEntry();
	        // iterates over entries in the zip file
	        while (entry != null) {
	            //String filePath = destDirectory + File.separator + entry.getName();
	            if (!entry.isDirectory()) {
	                List<String> list = convertStringBuilderToArrayList(getContent(zipIn));
	                //only return first entry
	                zipIn.closeEntry();
	                zipIn.close();
	                return list;
	            } else {
	                //ignore any directory
	            }
	            zipIn.closeEntry();
	            entry = zipIn.getNextEntry();
	        }
	        zipIn.close();
	        System.err.println("ZipWriter: Could not find file '" + filename + "' or it did not contain content.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns the content from a ZipInputStream as StringBuilder.
	 * @param zipIn
	 * @return
	 */
	private static StringBuilder getContent(ZipInputStream zipIn) {
		StringOutputStream outStream = new ZipWriter().new StringOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(outStream);
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        try {
			while ((read = zipIn.read(bytesIn)) != -1) {
			    bos.write(bytesIn, 0, read);
			}
	        bos.close();
	        return outStream.getString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
	}
	
	/**
	 * Converts StringBuilder content into an ArrayList by tokenizing on linebreaks.
	 * @param builder
	 * @return
	 */
	private static ArrayList<String> convertStringBuilderToArrayList(StringBuilder builder) {
		ArrayList<String> list = new ArrayList<String>();
		if(builder.length() > 0) {
			StringTokenizer tokenizer = new StringTokenizer(builder.toString(), System.getProperty("line.separator"));
			while(tokenizer.hasMoreTokens()) {
				list.add(tokenizer.nextToken());
			}
		}
		return list;
	}
	
	/**
	 * Writes data into zipfile of same name as inserted filename.
	 * @param filename Name for added file
	 * @param chartname Name of chart
	 * @param data Actual data to be added to zipped file
	 * @param datasetExtension Extension of original target file (to replace with .zip extension)
	 * @param zipFileExtension Zip file extension. If null, defaults to .zip
	 * @param 
	 */
	public static void writeDataToZipFile(String filename, String chartname, StringBuilder data, String datasetExtension, String zipFileExtension) {
		if(filename == null || filename.isEmpty()) {
			System.err.println("ZipWriter: Empty filename is not permissible.");
			return;
		}
		if(data == null) {
			System.err.println("ZipWriter: Null payload data is not permissible.");
			return;
		}
		
		String zipFilename = null;
		if(filename.contains(datasetExtension)) {
			//Derive ZIP filename
			zipFilename = filename.replaceAll(datasetExtension, (zipFileExtension == null ? ".zip" : zipFileExtension));
		} else {
			//Just blindly append
			zipFilename = filename + (zipFileExtension == null ? ".zip" : zipFileExtension);
		}
		
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(zipFilename);
			ZipOutputStream zos = new ZipOutputStream(fos);
	        //add a new Zip Entry to the ZipOutputStream
	        ZipEntry ze = new ZipEntry(new File(filename).getName());
	        zos.putNextEntry(ze);
			zos.write(String.valueOf(data).getBytes(), 0, String.valueOf(data).getBytes().length);
			
			//close zip entry
            zos.closeEntry();
            //close resources
            zos.close();
            fos.close();
            System.out.println("ZipWriter: Wrote content " + (chartname != null ? "for chart '" 
					+ chartname + "' " : "") + "to outfile '" + zipFilename + "'.");
             
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
