/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import io.imunity.vaadin.elements.DialogWithActionFooter;
import pl.edu.icm.unity.base.message.MessageSource;

class EntityDetailsDialog extends DialogWithActionFooter
{
	EntityDetailsDialog(MessageSource msg, EntityDetailsPanel contents)
	{
		super(msg::getMessage);
		setHeaderTitle(msg.getMessage("IdentityDetails.entityDetailsCaption"));
		setActionButton(msg.getMessage("close"), this::close);
		setCancelButtonVisible(false);
		add(contents);
		setWidth("40em");
	}
}
