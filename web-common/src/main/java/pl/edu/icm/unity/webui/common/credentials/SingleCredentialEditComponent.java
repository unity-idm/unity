/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Wraps type-specific editor of a credential, adds confirmation and cancel buttons.
 */
class SingleCredentialEditComponent extends CustomComponent
{
	static final int WIDTH = 19;
	private ComponentsContainer editorComponents;

	SingleCredentialEditComponent(MessageSource msg, ComponentsContainer editorComponents, Runnable onUpdate, 
			Runnable onCancel)
	{
		this.editorComponents = editorComponents;
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponents(editorComponents.getComponents());
		Button update = new Button(msg.getMessage("CredentialChangeDialog.update"));
		update.addStyleName(Styles.buttonAction.toString());
		update.addClickListener(e -> onUpdate.run());
		update.setWidthFull();
		main.addComponent(update);
		
		Button cancel = new Button(msg.getMessage("cancel"));
		cancel.addStyleName(Styles.vButtonLink.toString());
		cancel.addClickListener(e -> onCancel.run());
		main.addComponent(cancel);
		main.setComponentAlignment(cancel, Alignment.MIDDLE_RIGHT);

		setCompositionRoot(main);
		setWidth(WIDTH, Unit.EM);
	}
	
	boolean isEmpty()
	{
		return editorComponents.getComponents().length == 0;
	}
	
	void focusEditor()
	{
		for (Component component: editorComponents.getComponents())
			if (component instanceof Focusable)
			{
				((Focusable) component).focus();
				break;
			}
	}
}
