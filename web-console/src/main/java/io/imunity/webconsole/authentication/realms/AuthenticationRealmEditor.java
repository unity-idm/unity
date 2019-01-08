/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;

/**
 * Authentication realm editor.
 * 
 * @author P.Piernik
 *
 */
public class AuthenticationRealmEditor extends CustomComponent
{

	private Binder<AuthenticationRealm> binder;
	private TextField name;
	private TextArea description;
	private IntStepper blockFor;
	private IntStepper blockAfterUnsuccessfulLogins;
	private IntStepper maxInactivity;
	private IntStepper allowForRememberMeDays;
	private ComboBox<RememberMePolicy> rememberMePolicy;

	public AuthenticationRealmEditor(UnityMessageSource msg, AuthenticationRealm toEdit)
	{
		name = new TextField(msg.getMessage("AuthenticationRealm.name"));
		name.setWidth(100, Unit.PERCENTAGE);
		
		description = new DescriptionTextArea(
				msg.getMessage("AuthenticationRealm.description"));

		blockAfterUnsuccessfulLogins = new IntStepper(
				msg.getMessage("AuthenticationRealm.blockAfterUnsuccessfulLogins"));
		blockAfterUnsuccessfulLogins.setMinValue(1);
		blockAfterUnsuccessfulLogins.setMaxValue(999);
		blockAfterUnsuccessfulLogins.setWidth(5, Unit.EM);
		
		blockFor = new IntStepper(msg.getMessage("AuthenticationRealm.blockFor"));
		blockFor.setMinValue(1);
		blockFor.setMaxValue(999);
		blockFor.setWidth(5, Unit.EM);

		rememberMePolicy = new ComboBox<>(
				msg.getMessage("AuthenticationRealm.rememberMePolicy"));
		rememberMePolicy.setItems(RememberMePolicy.values());
		rememberMePolicy.setEmptySelectionAllowed(false);
		rememberMePolicy.setWidth(100, Unit.PERCENTAGE);
		
		allowForRememberMeDays = new IntStepper(
				msg.getMessage("AuthenticationRealm.allowForRememberMeDays"));
		allowForRememberMeDays.setMinValue(1);
		allowForRememberMeDays.setMaxValue(999);
		allowForRememberMeDays.setWidth(5, Unit.EM);

		maxInactivity = new IntStepper(msg.getMessage("AuthenticationRealm.maxInactivity"));
		maxInactivity.setMinValue(1);
		maxInactivity.setMaxValue(99999);
		maxInactivity.setWidth(5, Unit.EM);

		binder = new Binder<>(AuthenticationRealm.class);

		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(description, "description");

		binder.forField(blockAfterUnsuccessfulLogins)
			.asRequired(msg.getMessage("fieldRequired"))
			.bind("blockAfterUnsuccessfulLogins");
		binder.forField(blockFor).asRequired(msg.getMessage("fieldRequired"))
				.bind("blockFor");

		binder.forField(rememberMePolicy).asRequired(msg.getMessage("fieldRequired"))
				.bind("rememberMePolicy");
		binder.forField(allowForRememberMeDays).asRequired(msg.getMessage("fieldRequired"))
				.bind("allowForRememberMeDays");

		binder.forField(maxInactivity).asRequired(msg.getMessage("fieldRequired"))
				.bind("maxInactivity");
		binder.setBean(toEdit);
		FormLayout mainLayout = new FormLayout();
		mainLayout.setMargin(false);

		mainLayout.addComponents(name, description, blockAfterUnsuccessfulLogins, blockFor, 
				rememberMePolicy, allowForRememberMeDays, maxInactivity);
		setCompositionRoot(mainLayout);
		setWidth(100, Unit.PERCENTAGE);
	}

	public void editMode()
	{
		name.setReadOnly(true);
	}

	public boolean hasErrors()
	{
		return binder.validate().hasErrors();
	}

	public AuthenticationRealm getAuthenticationRealm()
	{
		return binder.getBean();
	}
}
