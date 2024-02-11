/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.function.Supplier;

import io.imunity.scim.console.mapping.AttributeDefinitionWithMappingBean;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.api.services.idp.CollapsableGrid;
import pl.edu.icm.unity.base.message.MessageSource;

class AttributeDefinitionConfigurationList extends CollapsableGrid<AttributeDefinitionWithMappingBean>
{
	AttributeDefinitionConfigurationList(MessageSource msg, HtmlTooltipFactory tooltipFactory, String addText,
			AttributeEditContext context, AttributeEditorData editorData)
	{
		super(msg,
				() -> new AttributeDefinitionWithMappingConfigurationEditor(msg, tooltipFactory, () -> context,
						editorData),
				"", () -> new AttributeDefinitionWithMappingBean(), addText,
				!context.attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
	}

	AttributeDefinitionConfigurationList(MessageSource msg, HtmlTooltipFactory tooltipFactory, String addText,
			Supplier<AttributeEditContext> contextSupplier, AttributeEditorData editorData)
	{
		super(msg,
				() -> new AttributeDefinitionWithMappingConfigurationEditor(msg, tooltipFactory, contextSupplier,
						editorData),
				"", () -> new AttributeDefinitionWithMappingBean(), addText,
				!contextSupplier.get().attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
	}

	void refreshEditors()
	{
		gridListDataView.getItems()
				.forEach(e -> ((AttributeDefinitionWithMappingConfigurationEditor) e).refresh());
	}
}
