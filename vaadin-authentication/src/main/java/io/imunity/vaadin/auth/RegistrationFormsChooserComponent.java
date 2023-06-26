/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.LinkButton;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;

import java.util.List;

/**
 * Simple component which only displays the given registration forms.
 */
class RegistrationFormsChooserComponent extends VerticalLayout
{
	RegistrationFormsChooserComponent(List<RegistrationForm> forms, RegistrationFormsChooserListener listener,
	                                  Runnable finishHandler, MessageSource msg)
	{
		setPadding(false);
		setAlignItems(Alignment.CENTER);
		setJustifyContentMode(JustifyContentMode.CENTER);
		setSizeFull();

		H2 formName = new H2(msg.getMessage("RegistrationFormChooserDialog.selectForm"));
		formName.addClassName("u-reg-title");
		add(formName);

		for (RegistrationForm form : forms)
		{
			LinkButton button = new LinkButton(
				form.getDisplayedName().getValue(msg),
				event -> listener.formSelected(form)
			);
			add(button);
		}
		Button cancel = new Button(msg.getMessage("cancel"));
		cancel.addClickListener(event -> finishHandler.run());
		add(cancel);
	}

	interface RegistrationFormsChooserListener
	{
		void formSelected(RegistrationForm form);
	}
}
