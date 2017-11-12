/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.v7.ui.Table;

/**
 * Table with styles creating a smaller representation.
 * 
 * @author K. Benedyczak
 * @param <T>
 */
@Deprecated
public class SmallTableDeprecated extends Table
{
	
	public SmallTableDeprecated(String caption)
	{
		super(caption);
		setup();
	}

	public SmallTableDeprecated()
	{
		setup();
	}
	
	private void setup()
	{
		addStyleName(Styles.vTableNoHorizontalLines.toString());
		addStyleName(Styles.vSmall.toString());
	}
}
