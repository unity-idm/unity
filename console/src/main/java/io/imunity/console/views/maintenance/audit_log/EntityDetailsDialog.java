/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance.audit_log;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;

class EntityDetailsDialog extends Dialog
{
	EntityDetailsDialog(Component component, String buttonName)
	{
		add(component);
		Button button = new Button(buttonName, e -> close());
		button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		getFooter().add(button);
		setWidth("50em");
	}
}
