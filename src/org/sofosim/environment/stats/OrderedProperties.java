package org.sofosim.environment.stats;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * This class provides an alternative for Java Properties. In contrast to 
 * Java Properties it keeps the entries in the order of entering, allows 
 * to retrieve boolean values reliably, but only supports a subset of the 
 * functionality provided by Java Properties (such as store()). 
 * 
 * @author cfrantz
 *
 */
public class OrderedProperties {

	private LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
	private boolean propertiesConformingOutput = true;
	
	public OrderedProperties(){
		
	}
	
	public OrderedProperties(boolean javaPropertiesConformingOutput){
		propertiesConformingOutput = javaPropertiesConformingOutput;
	}
	
	public OrderedProperties(Map<String, String> coll){
		properties.putAll(coll);
	}
	
	public void put(String key, String value){
		properties.put(key, value);
	}
	
	public void setProperty(String key, String value){
		properties.put(key, value);
	}
	
	public boolean containsProperty(String key){
		return properties.containsKey(key);
	}
	
	public String getProperty(String key){
		return properties.get(key);
	}
	
	public String get(String key){
		return properties.get(key);
	}
	
	/**
	 * Gets a known boolean property and returns its boolean value.
	 * Comparison occurs on String level with values converted to 
	 * lower case.
	 * @param property
	 * @return
	 */
	public boolean getBoolean(String property){
		if(properties.containsKey(property)){
			return (properties.get(property).toLowerCase().equals(Boolean.TRUE.toString()));// || properties.get(property).equals("true"));
		} else {
			return false;
		}
	}
	
	/**
	 * Writes a given property set to an output stream. This output is largely 
	 * compatible with Java's Properties' load() method, so entries can be 
	 * reimported into the generic Java Properties.
	 * @param out
	 * @param comments
	 * @throws IOException
	 */
	public void store(OutputStream out, String comments) throws IOException{
		StringBuffer outBuffer = new StringBuffer();
		String separator = System.getProperty("line.separator");
		outBuffer.append("#").append(new Date(System.currentTimeMillis())).append(separator);
		if(comments != null){
			outBuffer.append("#").append(comments.replace(separator, " ")).append(separator);
		}
		for(Entry<String, String> entry: properties.entrySet()){
			outBuffer.append(replaceInvalidCharacters(entry.getKey())).append("=").append(replaceInvalidCharacters(entry.getValue())).append(separator);
		}
		OutputStreamWriter writer = new OutputStreamWriter(out);
		writer.write(outBuffer.toString());
		writer.flush();
	}
	
	/**
	 * Prefixes special characters in String with backslash to ensure they 
	 * are printed in outputs and loaded back into Properties. This only 
	 * works if proportiesConformingOutput is activated (conforms to output 
	 * of Java Properties).
	 * @param input
	 * @return
	 */
	private String replaceInvalidCharacters(String input){
		if(propertiesConformingOutput){
			input = input.replace(" ", "\\ ");
			input = input.replace("#", "\\#");
			input = input.replace("!", "\\!");
			input = input.replace(":", "\\:");
			input = input.replace("=", "\\=");
		}
		return input;
	}
	
	/**
	 * Returns the OrderProperties as conventional Java Properties
	 * @return
	 */
	public Properties getAsProperties(){
		Properties props = new Properties();
		props.putAll(properties);
		return props;
	}
	
}
