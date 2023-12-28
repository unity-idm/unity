/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities.credentials;

import static io.imunity.vaadin.elements.CssClassNames.SMALL_GAP;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import pl.edu.icm.unity.base.message.MessageSource;

class SingleCredentialEditComponent extends VerticalLayout
{
	private final ComponentsContainer editorComponents;
	static final int WIDTH = 19;

	SingleCredentialEditComponent(MessageSource msg, ComponentsContainer editorComponents, Runnable onUpdate,
			Runnable onCancel)
	{
		this.editorComponents = editorComponents;
		setPadding(false);
		addClassName(SMALL_GAP.getName());
		add(editorComponents.getComponents());
		Button update = new Button(msg.getMessage("CredentialChangeDialog.update"));
		update.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		update.addClickListener(e -> onUpdate.run());
		update.setWidthFull();
		add(update);

		LinkButton cancel = new LinkButton(msg.getMessage("cancel"), e -> onCancel.run());
		add(cancel);
		cancel.getStyle().set("align-self", "end");

		setWidth(WIDTH, Unit.EM);
	}

	boolean isEmpty()
	{
		return editorComponents.getComponents().length == 0;
	}

	void focusEditor()
	{
		for (Component component : editorComponents.getComponents())
			if (component instanceof Focusable)
			{
				((Focusable<?>) component).focus();
				break;
			}
	}
}
