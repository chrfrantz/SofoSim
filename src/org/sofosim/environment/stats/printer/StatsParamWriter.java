package org.sofosim.environment.stats.printer;

import org.sofosim.environment.stats.StatsReflector;

public class StatsParamWriter extends StatsWriter {

	/** class to be inspected (for static methods) */
	private Class simClass = null;
	/** instance to be inspected - is used to infer class if not provided */
	private Object instance = null;
	/** superclass levels that will be inspected with reflection */
	private Integer superClassLevelsToInspect = 0;
	private String prefix = "Stats Writer: ";
	
	/**
	 * Instantiates ParameterWriter for given class and instance as well as 
	 * inspection depth for superclasses.
	 * @param filename file to write to
	 * @param simClass class to be inspected
	 * @param instance instance to be inspected
	 * @param superClassLevelsToInspect number of superclass inspection levels
	 */
	public StatsParamWriter(String filename, Class simClass, Object instance, Integer superClassLevelsToInspect) {
		super(filename);
		if(simClass == null && instance == null){
			throw new IllegalArgumentException(prefix + "Either class or instance need to be specified.");
		}
		this.simClass = simClass;
		this.instance = instance;
		if(superClassLevelsToInspect != 0){
			this.superClassLevelsToInspect = superClassLevelsToInspect;
		}
	}
	
	/**
	 * Inspects currently set class and object and writes the result to filename 
	 * specified in constructor.
	 */
	public void writeParameters(){
		write(StatsReflector.readCurrentParameters(this.simClass, this.instance, superClassLevelsToInspect));
		close();
		System.out.println(prefix + "Wrote file '" + getFilename() + "'.");
	}
	
	/**
	 * Will set this instance to new class and object as well as inspection depth.
	 * New filename will be inferred from class name (which is inferred from 
	 * instance if not provided) as well as timestamp generated as part of 
	 * previous file's suffix.
	 * @param otherSimClass class to be inspected
	 * @param otherInstance instance to be inspected
	 * @param superClassLevelsToInspect superclass inspection levels
	 */
	public void inspect(Class otherSimClass, Object otherInstance, Integer superClassLevelsToInspect){
		String firstPart = null;
		if(otherSimClass == null && otherInstance != null){
			firstPart = otherInstance.getClass().getName();
		} else if(otherSimClass != null){
			firstPart = otherSimClass.getClass().getName();
		} else {
			throw new IllegalArgumentException(prefix + "Either class or instance need to be specified.");
		}
		//redetermine filename based on existing filename timestamp information and new class name
		setFilename(firstPart + getFilename().substring(getFilename().indexOf("_"), getFilename().length()));
		this.simClass = otherSimClass;
		this.instance = otherInstance;
		if(superClassLevelsToInspect != null){
			this.superClassLevelsToInspect = superClassLevelsToInspect;
		} else {
			//default to zero if null specified
			this.superClassLevelsToInspect = 0;
		}
		writeParameters();
		//write(StatsReflector.readCurrentParameters(otherSimClass, otherInstance));
		//close();
		//System.out.println(prefix + "Wrote file '" + getFilename() + "'.");
	}

}
