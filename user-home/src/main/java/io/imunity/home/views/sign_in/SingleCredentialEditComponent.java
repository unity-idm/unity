/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home.views.sign_in;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Wraps type-specific editor of a credential, adds confirmation and cancel buttons.
 */
class SingleCredentialEditComponent extends VerticalLayout
{
	static final int WIDTH = 19;
	private final ComponentsContainer editorComponents;

	SingleCredentialEditComponent(MessageSource msg, ComponentsContainer editorComponents, Runnable onUpdate,
								  Runnable onCancel)
	{
		this.editorComponents = editorComponents;
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.add(editorComponents.getComponents());
		Button update = new Button(msg.getMessage("CredentialChangeDialog.update"));
		update.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		update.addClickListener(e -> onUpdate.run());
		update.setWidthFull();
		main.add(update);
		
		LinkButton cancel = new LinkButton(msg.getMessage("cancel"), e -> onCancel.run());
		main.add(cancel);

		add(main);
		setWidth(WIDTH, Unit.EM);
	}
	
	void focusEditor()
	{
		for (Component component: editorComponents.getComponents())
			if (component instanceof Focusable)
			{
				((Focusable<?>) component).focus();
				break;
			}
	}
}
