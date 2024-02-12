/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console.v8;

import java.util.List;

import io.imunity.attr.introspection.config.AttributePolicy;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;
import pl.edu.icm.unity.webui.common.ListOfDnDCollapsableElements;

class AttributePolicyConfigurationList extends ListOfDnDCollapsableElements<AttributePolicy>
{
	AttributePolicyConfigurationList(MessageSource msg, List<IdPInfo> idps)
	{
		super(msg, () -> new AttributePolicyConfigurationEditor(msg, idps), "",
				msg.getMessage("AttributePolicyConfigurationList.defaultPolicy"));
		
	}

	@Override
	protected AttributePolicy makeNewInstance()
	{
		return new AttributePolicy();
	}
}
