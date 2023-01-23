package org.sofosim.environment.stats.charts;

import java.util.HashMap;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;
import org.nzdis.micro.random.MersenneTwisterFast;

public class CustomLabelGenerator implements XYItemLabelGenerator{

	private MersenneTwisterFast random = null;
	int randomNumberRange = 9;
	boolean randomPosition = true;
	
	public CustomLabelGenerator(boolean randomPositioningOfLabels){
		//fixed seed to have reproducible label positioning
		this.random = new MersenneTwisterFast(4234820);
		this.randomPosition = randomPositioningOfLabels;
	}
	
	HashMap<String, Float> settings = new HashMap<String, Float>();
	
	@Override
	public String generateLabel(XYDataset dataset, int series, int item) {
		
		// Mason 17 --> casting
		String seriesName = (String)dataset.getSeriesKey(series);
		// Mason 19 --> String is comparable
		//String seriesName = dataset.getSeriesKey(series).toString();
		if(randomPosition){
			if(!settings.containsKey(seriesName)){
				Float newValue = new Float(new Float(random.nextInt(randomNumberRange) + 1) / (float)randomNumberRange);
				//ensure unique positions and cater for chart borders
				while(settings.values().contains(newValue) || newValue < 0.1 || newValue > 0.9){
					//System.out.println("Map: " + settings.size());
					//System.out.println("Map: " + settings.toString());
					//System.out.println("Value: " + newValue);
					newValue = new Float(new Float(random.nextInt(randomNumberRange) + 1) / (float)randomNumberRange);
				}
				settings.put(seriesName, newValue);
				//expand randomness if number of series are getting too high
				if(settings.size() > ((float)randomNumberRange) * 0.8){
					randomNumberRange *= 2;
				}
			}
			if (item == Math.round((int)dataset.getItemCount(series) * settings.get(seriesName))){
				return seriesName;
			}
		} else {
			//centered positioning of labels
			if (item == Math.rint(dataset.getItemCount(series) * 0.5)){
				return seriesName;
			}
		}
		
		
		//avoid division/zero
		//int incSeries = series + 10;
		//System.out.println("Series items: " + dataset.getItemCount(series));
		//System.out.println("Series name: " + (String)dataset.getSeriesKey(series));
		//System.out.println("Series: " + dataset.getItemCount(series)/incSeries);
		//System.out.println("Dataset: " + dataset.)
		//float ratio = this.stats.sim.random.nextInt(10)+1/10;
		//System.out.println("Position: " + getNextRatio());
		//if (item == (int)dataset.getItemCount(series)*0.5){ ///item == dataset.getItemCount(series) - 1)
		/* attach label to last data entry */
		//if(item == dataset.getItemCount(series) - 1)){
		/* attach label to last-10 entry */
		/*
		if (dataset.getItemCount(series) > 50){
				if(item == dataset.getItemCount(series) - 50){
					return (String)dataset.getSeriesKey(series);
				}
		} else if (dataset.getItemCount(series) > 20){
				if(item == dataset.getItemCount(series) - 20){ 
					return (String)dataset.getSeriesKey(series);
				}
		} else if(item == dataset.getItemCount(series) - 1){
			return (String)dataset.getSeriesKey(series);
		}*/
		
		/*
		if (item == Math.round((int)dataset.getItemCount(series)*0.5)){
			if(pre){
				pre = false;
				return fix + (String)dataset.getSeriesKey(series);
			} else {
				pre = true;
				return (String)dataset.getSeriesKey(series) + fix;
			}
			//return (String)dataset.getSeriesKey(series);
		}*/
        return null;
	}

}
