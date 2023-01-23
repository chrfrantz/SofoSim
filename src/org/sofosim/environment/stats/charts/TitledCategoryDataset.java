package org.sofosim.environment.stats.charts;

import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Adds the notion of title to a DefaultCategoryDataset. 
 * @author cfrantz
 *
 */
public class TitledCategoryDataset extends DefaultCategoryDataset{

	public final String title;
	
	public TitledCategoryDataset(String title){
		this.title = title;
	}
}
