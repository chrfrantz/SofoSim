package org.sofosim.environment.stats;

import java.lang.reflect.Field;

import org.nzdis.micro.util.DataStructurePrettyPrinter;
import org.sofosim.environment.annotations.SimulationParam;


public class StatsReflector {

	public static Class parameterHolder = null;
	public static Class parameterAnnotation = SimulationParam.class;
	public static boolean debug = false;
	
	/**
	 * This method returns the annotated static fields
	 * (annotation type indicated by the parameterAnnotation field) 
	 * of a class (set in the class parameterHolder field or passed as parameter),
	 * and returns is as Java properties. 
	 * For inspection of superclasses @see {@link #readCurrentParameters(Class, Object, Integer)}.
	 * @param inspectionTarget class holding parameter annotations
	 * @param instance instance of that class (if not static fields)
	 * @return Properties
	 */
	public static OrderedProperties readCurrentParameters(Class inspectionTarget, Object instance){
		return readCurrentParameters(inspectionTarget, instance, 0);
	}
	
	/**
	 * This method returns the annotated static fields
	 * (annotation type indicated by the parameterAnnotation field) 
	 * of a class (set in the class parameterHolder field or passed as parameter),
	 * and returns is as Java properties. If specified, superclasses can be included 
	 * in reflection (Parameter: superClassLevelsToInspect).
	 * @param inspectionTarget class holding parameter annotions
	 * @param instance instance of that class (if not static fields)
	 * @param superClassLevelsToInspect indicates number of recursive superclass inspections
	 * @return Properties
	 */
	public static OrderedProperties readCurrentParameters(Class inspectionTarget, Object instance, Integer superClassLevelsToInspect){
		
		Class params = inspectionTarget;
		
		/* if not specified by parameter, refer to static field instead */
		if(params == null){
			params = StatsReflector.parameterHolder;
		}
		
		if(params == null){
			if(instance != null){
				params = instance.getClass();
				//System.out.println("Determined reflected class from instance: " + instance.getClass());
			} else {
				throw new IllegalArgumentException("Parameters not read as neither type (i.e. class) nor instance provided!");
			}
		}
		
		//Properties parameters = new Properties();
		OrderedProperties parameters = new OrderedProperties(false);
		//Annotation[] annotations = parameterHolder.getDeclaredAnnotations();
		
		/*if(parameterHolder.getDeclaredAnnotations().length == 0){
			System.out.println("No Parameters provided in simulation class '" + params.getName() + "'");
		}*/
		/*
		for(int i=0; i<annotations.length; i++){
			System.out.println(annotations[i]);
		}
		*/
		//recursively inspect specified class
		inspect(params, instance, parameters, null, superClassLevelsToInspect);
		
		return parameters;
	}
	
	/**
	 * Recursively inspects given class for SimulationParam annotation and 
	 * adds both field name and field value to specified OrderedProperties. 
	 * If not null, a prefix will be put in front of the field name (which 
	 * is particularly useful to identify nested properties). Inspection 
	 * can include superclasses of the inspected class. Level for that are 
	 * specified in parameter superClassLevelsToInspect
	 * (0 -> no inspection of super class)
	 * @param parameterHolder
	 * @param parameters
	 * @param prefix
	 * @param superClassLevelsToInspect indicates the recursive superclass inspection levels
	 */
	private static void inspect(Class parameterHolder, Object instance, OrderedProperties parameters, String prefix, Integer superClassLevelsToInspect){
		if(superClassLevelsToInspect > 0 && parameterHolder.getSuperclass() != null){
			inspect(parameterHolder.getSuperclass(), instance, parameters, prefix, superClassLevelsToInspect - 1);
		} //else {
			for (Field field : parameterHolder.getDeclaredFields()) {
				field.setAccessible(true);
			    if (field.isAnnotationPresent(parameterAnnotation)) {
			    	if(debug){
			    		System.out.println("Field '"+ field.getName() + "' has parameter annotation '" + parameterAnnotation.getSimpleName() + "'.");
			    	}
			    	if(prefix == null){
			    		prefix = "";
			    	}
			    	try {
			    		//System.out.println(field.getGenericType());
			    		parameters.put(prefix + field.getName(), DataStructurePrettyPrinter.decomposeRecursively(field.get(instance), null).toString());
			    		//System.out.println("Field: " + field.getName() + " - " + field.get(instance));
						//try if it contains linked annotated elements
						//System.out.println("Analyzing: " + field.getName() + ": " + field.get(null) + " - "+ field.get(null).getClass().isAnnotationPresent(parameterAnnotation));
			    		//System.out.println("Field: " + field.get(null).getClass().isPrimitive());
			    		inspect(field.get(instance).getClass(), field.get(instance), parameters, field.getName() + " - ", 0);
			    		
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NullPointerException e){
						//if NPE, try again, probably fields are static
						if(instance != null){
							try {
								parameters.put(prefix + field.getName(), field.get(null).toString());
								inspect(field.get(null).getClass(), field.get(null), parameters, field.getName() + " - ", 0);
							} catch (IllegalArgumentException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (IllegalAccessException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (NullPointerException e1){
								System.err.println("Simulation Parameter Reflection: Field '" + field.getName() + "' in class '" + parameterHolder.getSimpleName() + "' is making trouble.");
							}
						}
						//System.err.println("Field '" + field.getName() + "' seems to be null or doesn't have accessor specified.");
					}
			    }
			}
		//}
	}
	
}
