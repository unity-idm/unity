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
	private String descriptionKey;
	private int typicalSize;
	
	public ActionParameterDesc(boolean mandatory, String name, String descriptionKey,
			int typicalSize)
	{
		this.mandatory = mandatory;
		this.name = name;
		this.descriptionKey = descriptionKey;
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

	public String getDescriptionKey()
	{
		return descriptionKey;
	}

	public int getTypicalSize()
	{
		return typicalSize;
	}
}
