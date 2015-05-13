/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identitytype;

import java.util.Collection;

import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.bus.Event;

/**
 * Notifies that the identity types where updated
 * @author K. Benedyczak
 */
public class IdentityTypesUpdatedEvent implements Event
{
	private Collection<IdentityType> identityTypes;

	public IdentityTypesUpdatedEvent(Collection<IdentityType> identityTypes)
	{
		this.identityTypes = identityTypes;
	}

	public Collection<IdentityType> getIdentityTypes()
	{
		return identityTypes;
	}
}
