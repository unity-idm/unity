/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance.audit_log;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;

class EntityDetailsDialog extends Dialog
{
	EntityDetailsDialog(Component component)
	{
		add(component);
		getFooter().add(new Button("cancel", e -> close()));
		setWidth("50em");
	}
}
