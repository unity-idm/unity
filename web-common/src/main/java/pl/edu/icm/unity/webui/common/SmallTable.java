/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Table;

public class SmallTable extends Table
{
	
	public SmallTable(String caption)
	{
		super(caption);
		setup();
	}

	public SmallTable()
	{
		setup();
	}
	
	private void setup()
	{
		addStyleName(Styles.vTableNoHorizontalLines.toString());
		addStyleName(Styles.vSmall.toString());
	}
}
