/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import java.util.List;
import java.util.stream.Collectors;

import io.imunity.attr.introspection.config.AttributePolicy;
import io.imunity.vaadin.auth.services.idp.CollapsableGrid;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;

class AttributePolicyConfigurationList extends CollapsableGrid<AttributePolicy>
{

	AttributePolicyConfigurationList(MessageSource msg, List<IdPInfo> idps)
	{
		super(msg, () -> new AttributePolicyConfigurationEditor(msg, idps), "", () -> new AttributePolicy(),
				msg.getMessage("AttributePolicyConfigurationList.defaultPolicy"), false);
		addValueChangeListener(e -> setNamesSupplier());
	}

	private List<AttributePolicyConfigurationEditor> getAttributePolicyConfigurationEditors()
	{

		return super.getEditors().stream()
				.map(e -> (AttributePolicyConfigurationEditor) e)
				.toList();
	}

	private void setNamesSupplier()
	{
		getAttributePolicyConfigurationEditors()
				.forEach(e -> e.setUsedNamesProvider(() -> getAttributePolicyConfigurationEditors().stream()
						.filter(ed -> ed != e)
						.map(ed -> ed.getName())
						.collect(Collectors.toSet())));
	}

}
