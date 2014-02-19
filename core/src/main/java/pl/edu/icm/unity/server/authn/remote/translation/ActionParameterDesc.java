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
	private boolean mandatory;
	private String name;
	private String description;
	private int typicalSize;
	
	public ActionParameterDesc(boolean mandatory, String name, String description,
			int typicalSize)
	{
		this.mandatory = mandatory;
		this.name = name;
		this.description = description;
		this.typicalSize = typicalSize;
	}

	public boolean isMandatory()
	{
		return mandatory;
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
