/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.types.basic.IdentityParam;

class IdentityPresentationUtil
{
	private final MessageSource msg;
	private final IdentityTypeSupport idTypeSupport;
	
	IdentityPresentationUtil(MessageSource msg, IdentityTypeSupport idTypeSupport)
	{
		this.msg = msg;
		this.idTypeSupport = idTypeSupport;
	}

	String getIdentityVisualValue(IdentityParam identity)
	{
		try
		{
			IdentityTypeDefinition idTypeDef = idTypeSupport.getTypeDefinition(identity.getTypeId());
			return idTypeDef.toHumanFriendlyString(msg, identity);
		} catch (IllegalArgumentException e)
		{
			return identity.getValue();
		}
	}
}
