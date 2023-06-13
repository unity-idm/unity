/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.identityTypes;

import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;

/**
 * Represent grid identity type entry.
 * 
 * @author P.Piernik
 *
 */
class IdentityTypeEntry
{
	public final IdentityType type;
	public final IdentityTypeDefinition typeDefinition;

	IdentityTypeEntry(IdentityType type, IdentityTypeDefinition typeDefinition)
	{
		super();
		this.type = type;
		this.typeDefinition = typeDefinition;
	}

}
