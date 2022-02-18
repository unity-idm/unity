/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.Collections;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.ListOfDnDCollapsableElements;

class AttributeDefinitionConfigurationList extends ListOfDnDCollapsableElements<AttributeDefinitionWithMappingBean>
{
	AttributeDefinitionConfigurationList(MessageSource msg, String addText, boolean disableComplexAndMulti,
			boolean onlyMappingEdit)
	{
		super(msg, () -> new AttributeDefinitionWithMappingConfigurationEditor(msg, disableComplexAndMulti, onlyMappingEdit), "",
				Collections.emptyList(), addText, onlyMappingEdit);
	}

	@Override
	protected AttributeDefinitionWithMappingBean makeNewInstance()
	{
		return new AttributeDefinitionWithMappingBean();
	}
}
