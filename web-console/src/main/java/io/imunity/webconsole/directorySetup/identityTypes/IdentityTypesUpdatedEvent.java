/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.identityTypes;

import java.util.Collection;

import pl.edu.icm.unity.base.entity.IdentityType;
import pl.edu.icm.unity.webui.bus.Event;

/**
 * Notifies that the identity types where updated
 * @author K. Benedyczak
 */
class IdentityTypesUpdatedEvent implements Event
{
	private Collection<IdentityType> identityTypes;

	IdentityTypesUpdatedEvent(Collection<IdentityType> identityTypes)
	{
		this.identityTypes = identityTypes;
	}

	Collection<IdentityType> getIdentityTypes()
	{
		return identityTypes;
	}
}
