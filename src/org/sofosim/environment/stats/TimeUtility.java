package org.sofosim.environment.stats;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtility {

	public static final String DATE_FORMAT_CONCATENATED = "yyyyMMddHHmmss";
	public static final String DATE_FORMAT_SEMI_CONCATENATED = "yyyyMMdd_HHmmss";
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	public static final String TIME_FORMAT_NOW = "HH:mm:ss";
	
	
	/**
	 * Returns the current time as Date object.
	 * 
	 * @return current time as Date object
	 */
	public static Date getCurrentTime(){
		Calendar cal = Calendar.getInstance();
		return cal.getTime();
	}
	
	/**
	 * Returns the formatted current date/time as String (for output/logging purposes)
	 * supported formats: all static fields in TimeUtility class.
	 * @param dateFormat - dateFormat to be used 
	 * @return String representation of current time.
	 */
	public static String getCurrentTimeString(String dateFormat){
		 /*String dateFormat = "";
		 if(includingDate){
			 dateFormat = DATE_FORMAT_NOW;
		 } else {
			 dateFormat = TIME_FORMAT_NOW;
		 }*/
		 SimpleDateFormat simpleFormat = new SimpleDateFormat(dateFormat);
		 return simpleFormat.format(getCurrentTime());
	}
	
}
