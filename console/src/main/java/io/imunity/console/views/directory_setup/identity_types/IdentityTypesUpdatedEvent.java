/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.identity_types;

import java.util.Collection;

import io.imunity.vaadin.endpoint.common.bus.Event;
import pl.edu.icm.unity.base.identity.IdentityType;

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
