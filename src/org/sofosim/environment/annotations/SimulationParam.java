package org.sofosim.environment.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation marks a given field as simulation parameter. SofoSim extracts all fields
 * marked as simulation parameters before simulation start and stores them into a file in order 
 * to keep track of simulation parameter choices for respective simulations. 
 * 
 * @author cfrantz
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface SimulationParam {

}
