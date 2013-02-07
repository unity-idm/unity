/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * A {@link LocalAuthnMethod} configured with {@link LocalAuthnMethodConfiguration}.
 * @author K. Benedyczak
 */
public class LocalAuthnVerification
{
	private String id;
	private String description;
	private LocalAuthnMethodConfiguration config;

	public LocalAuthnVerification(String id, String description,
			LocalAuthnMethodConfiguration config)
	{
		this.id = id;
		this.description = description;
		this.config = config;
	}

	public LocalAuthnMethodConfiguration getConfig()
	{
		return config;
	}

	public String getId()
	{
		return id;
	}

	public String getDescription()
	{
		return description;
	}
}
