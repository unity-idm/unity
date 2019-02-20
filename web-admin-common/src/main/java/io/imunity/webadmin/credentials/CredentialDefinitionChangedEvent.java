/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.credentials;

import pl.edu.icm.unity.webui.bus.Event;

/**
 * General notification about a change in credential definition settings.
 * @author K. Benedyczak
 */
public class CredentialDefinitionChangedEvent implements Event
{
	private boolean updatedExisting;
	private String name;

	public CredentialDefinitionChangedEvent(boolean updatedExisting, String name)
	{
		super();
		this.updatedExisting = updatedExisting;
		this.name = name;
	}

	public boolean isUpdatedExisting()
	{
		return updatedExisting;
	}

	public String getName()
	{
		return name;
	}
}
