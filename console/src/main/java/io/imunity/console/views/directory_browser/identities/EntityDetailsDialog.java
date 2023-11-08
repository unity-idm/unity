/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import pl.edu.icm.unity.base.message.MessageSource;

class EntityDetailsDialog extends ConfirmDialog
{
	EntityDetailsDialog(MessageSource msg, EntityDetailsPanel contents)
	{
		setHeader(msg.getMessage("IdentityDetails.entityDetailsCaption"));
		setConfirmButton(new Button(msg.getMessage("close")));
		add(contents);
		setWidth("40em");
	}
}
