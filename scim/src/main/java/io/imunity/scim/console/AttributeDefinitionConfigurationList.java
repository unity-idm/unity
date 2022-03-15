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
	AttributeDefinitionConfigurationList(MessageSource msg, String addText, AttributeEditContext context, AttributeEditorData editorData)
	{
		super(msg, () -> new AttributeDefinitionWithMappingConfigurationEditor(msg, context, editorData), "",
				Collections.emptyList(), addText, !context.attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
	}

	@Override
	protected AttributeDefinitionWithMappingBean makeNewInstance()
	{
		return new AttributeDefinitionWithMappingBean();
	}
}
