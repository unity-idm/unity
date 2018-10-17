/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.List;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Simple component which only displays the given registration forms.
 */
class RegistrationFormsChooserComponent extends CustomComponent
{
	RegistrationFormsChooserComponent(List<RegistrationForm> forms, RegistrationFormsChooserListener listener,
			Runnable finishHandler, UnityMessageSource msg)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(true);
		
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSizeFull();
		wrapper.addComponent(main);
		wrapper.setComponentAlignment(main, Alignment.MIDDLE_CENTER);
		setSizeFull();
		setCompositionRoot(wrapper);
		
		Label formName = new Label(msg.getMessage("RegistrationFormChooserDialog.selectForm"));
		formName.addStyleName(Styles.vLabelH1.toString());
		formName.addStyleName("u-reg-title");
		main.addComponent(formName);
		main.setComponentAlignment(formName, Alignment.MIDDLE_CENTER);

		for (RegistrationForm form : forms)
		{
			Button button = new Button(form.getDisplayedName().getValue(msg));
			button.setStyleName(Styles.vButtonLink.toString());
			button.addStyleName(Styles.RegistrationLink.toString());
			button.addClickListener(event -> listener.formSelected(form));
			main.addComponent(button);
			main.setComponentAlignment(button, Alignment.MIDDLE_CENTER);
		}
		Button cancel = new Button(msg.getMessage("cancel"));
		cancel.addStyleName(Styles.margin.toString());
		cancel.addClickListener(event -> finishHandler.run());
		main.addComponent(cancel);
		main.setComponentAlignment(cancel, Alignment.MIDDLE_CENTER);
	}

	interface RegistrationFormsChooserListener
	{
		void formSelected(RegistrationForm form);
	}
}
