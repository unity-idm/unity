/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import com.vaadin.data.Binder;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import io.imunity.webelements.tooltip.ComboBoxWithTooltip;
import io.imunity.webelements.tooltip.DescriptionTextFieldWithTooltip;
import io.imunity.webelements.tooltip.IntStepperWithTooltip;
import io.imunity.webelements.tooltip.TextFieldWithTooltip;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.webui.common.ListOfElements;

/**
 * Authentication realm editor.
 * 
 * @author P.Piernik
 *
 */
class AuthenticationRealmEditor extends CustomComponent
{
	private Binder<AuthenticationRealm> binder;
	private TextFieldWithTooltip name;
	private DescriptionTextFieldWithTooltip description;
	private IntStepperWithTooltip blockFor;
	private IntStepperWithTooltip blockAfterUnsuccessfulLogins;
	private IntStepperWithTooltip maxInactivity;
	private IntStepperWithTooltip allowForRememberMeDays;
	private ComboBoxWithTooltip<RememberMePolicy> rememberMePolicy;

	AuthenticationRealmEditor(UnityMessageSource msg, AuthenticationRealmEntry toEdit)
	{
		name = new TextFieldWithTooltip(
				msg.getMessage("AuthenticationRealm.name"),
				msg.getMessage("AuthenticationRealm.name.tooltip"));
		name.setWidth(100, Unit.PERCENTAGE);
		
		description = new DescriptionTextFieldWithTooltip(
				msg.getMessage("ServiceEditorBase.description"),
				msg.getMessage("AuthenticationRealm.description.tooltip"));

		blockAfterUnsuccessfulLogins = new IntStepperWithTooltip(
				msg.getMessage("AuthenticationRealm.blockAfterUnsuccessfulLogins"),
				msg.getMessage("AuthenticationRealm.blockAfterUnsuccessfulLogins.tooltip"));
		blockAfterUnsuccessfulLogins.setMinValue(1);
		blockAfterUnsuccessfulLogins.setMaxValue(999);
		blockAfterUnsuccessfulLogins.setWidth(5, Unit.EM);
		
		blockFor = new IntStepperWithTooltip(
				msg.getMessage("AuthenticationRealm.blockFor"),
				msg.getMessage("AuthenticationRealm.blockFor.tooltip"));
		blockFor.setMinValue(1);
		blockFor.setMaxValue(999);
		blockFor.setWidth(5, Unit.EM);

		rememberMePolicy = new ComboBoxWithTooltip<>(
				msg.getMessage("AuthenticationRealm.rememberMePolicy"),
				msg.getMessage("AuthenticationRealm.rememberMePolicy.tooltip"));
		rememberMePolicy.setItems(RememberMePolicy.values());
		rememberMePolicy.setEmptySelectionAllowed(false);
		rememberMePolicy.setWidth(100, Unit.PERCENTAGE);
		
		allowForRememberMeDays = new IntStepperWithTooltip(
				msg.getMessage("AuthenticationRealm.allowForRememberMeDays"),
				msg.getMessage("AuthenticationRealm.allowForRememberMeDays.tooltip"));
		allowForRememberMeDays.setMinValue(1);
		allowForRememberMeDays.setMaxValue(999);
		allowForRememberMeDays.setWidth(5, Unit.EM);

		maxInactivity = new IntStepperWithTooltip(
				msg.getMessage("AuthenticationRealm.maxInactivity"),
				msg.getMessage("AuthenticationRealm.maxInactivity.tooltip"));
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
		binder.setBean(toEdit.realm);
		FormLayout mainLayout = new FormLayout();
		mainLayout.setMargin(false);

		mainLayout.addComponents(name, description, blockAfterUnsuccessfulLogins, blockFor, 
				rememberMePolicy, allowForRememberMeDays, maxInactivity);
		if (!toEdit.endpoints.isEmpty())
		{
			ListOfElements<String> endpoints = new ListOfElements<>(toEdit.endpoints, t -> new Label(t));
			endpoints.setCaption(msg.getMessage("AuthenticationRealm.endpoints"));
			mainLayout.addComponent(endpoints);
		}
		setCompositionRoot(mainLayout);
		setWidth(45, Unit.EM);
	}

	void editMode()
	{
		name.setReadOnly(true);
	}

	boolean hasErrors()
	{
		return binder.validate().hasErrors();
	}

	AuthenticationRealm getAuthenticationRealm()
	{
		return binder.getBean();
	}
}
