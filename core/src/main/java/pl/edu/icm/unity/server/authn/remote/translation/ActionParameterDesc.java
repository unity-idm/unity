/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

/**
 * Describes {@link TranslationAction} parameter.
 * @author K. Benedyczak
 */
public class ActionParameterDesc
{
	private boolean manadtory;
	private String name;
	private String description;
	private int typicalSize;
	
	public ActionParameterDesc(boolean manadtory, String name, String description,
			int typicalSize)
	{
		this.manadtory = manadtory;
		this.name = name;
		this.description = description;
		this.typicalSize = typicalSize;
	}

	public boolean isManadtory()
	{
		return manadtory;
	}

	public String getName()
	{
		return name;
	}

	public String getDescription()
	{
		return description;
	}

	public int getTypicalSize()
	{
		return typicalSize;
	}
}
