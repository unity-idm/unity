/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import java.util.List;

import io.imunity.attr.introspection.config.AttributePolicy;
import io.imunity.vaadin.auth.services.idp.CollapsableGrid;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;

class AttributePolicyConfigurationList extends CollapsableGrid<AttributePolicy>
{
	AttributePolicyConfigurationList(MessageSource msg, List<IdPInfo> idps)
	{
		super(msg, () -> new AttributePolicyConfigurationEditor(msg, idps),"", () -> new AttributePolicy(),
				msg.getMessage("AttributePolicyConfigurationList.defaultPolicy"), false);
		
	}

	
}
