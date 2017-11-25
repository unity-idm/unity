/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Grid;

/**
 * Table with styles creating a smaller representation.
 * 
 * @author K. Benedyczak
 * @param <T>
 */
public class SmallGrid<T> extends Grid<T>
{
	public SmallGrid(String caption)
	{
		super(caption);
		setup();
	}

	public SmallGrid()
	{
		setup();
	}

	public SmallGrid(Class<T> clazz)
	{
		super(clazz);
		setup();
	}
	
	private void setup()
	{
		addStyleName(Styles.uGridNoHorizontalLines.toString());
	}
}
