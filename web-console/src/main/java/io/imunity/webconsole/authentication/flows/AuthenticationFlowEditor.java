/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.validators.NoSpaceValidator;

/**
 * Authentication realm editor.
 * 
 * @author P.Piernik
 *
 */
class AuthenticationFlowEditor extends CustomComponent
{
	private TextField name;
	private ChipsWithDropdown<String> firstFactorAuthenticators;
	private ChipsWithDropdown<String> secondFactorAuthenticators;
	private Binder<AuthenticationFlowDefinition> binder;
	private ComboBox<Policy> policy;

	AuthenticationFlowEditor(MessageSource msg, AuthenticationFlowEntry toEdit, List<String> authenticators)
	{
		name = new TextField(msg.getMessage("AuthenticationFlow.name"));
		name.setWidth(100, Unit.PERCENTAGE);
		name.setValue(toEdit.flow.getName());

		firstFactorAuthenticators = new ChipsWithDropdown<>(s -> s, true);
		firstFactorAuthenticators.setCaption(msg.getMessage("AuthenticationFlow.firstFactorAuthenticators"));
		firstFactorAuthenticators.setItems(authenticators);

		secondFactorAuthenticators = new ChipsWithDropdown<>(s -> s, true);
		secondFactorAuthenticators.setCaption(msg.getMessage("AuthenticationFlow.secondFactorAuthenticators"));
		secondFactorAuthenticators.setItems(authenticators);

		policy = new ComboBox<>(msg.getMessage("AuthenticationFlow.policy"));
		policy.setItems(Policy.values());
		policy.setValue(Policy.REQUIRE);
		policy.setEmptySelectionAllowed(false);

		binder = new Binder<>(AuthenticationFlowDefinition.class);
		binder.forField(name).withValidator(new NoSpaceValidator(msg)).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.forField(firstFactorAuthenticators).withNullRepresentation(Collections.emptyList())
				.withConverter(l -> l != null ? l.stream().collect(Collectors.toSet()) : null,
						s -> s != null ? s.stream().collect(Collectors.toList()) : null)
				.asRequired().bind("firstFactorAuthenticators");
		binder.forField(secondFactorAuthenticators).bind("secondFactorAuthenticators");
		binder.forField(policy).bind("policy");
		binder.setBean(toEdit.flow);

		FormLayout mainLayout = new FormLayout();
		mainLayout.setMargin(false);

		mainLayout.addComponents(name, policy, firstFactorAuthenticators, secondFactorAuthenticators);
		if (!toEdit.endpoints.isEmpty())
		{
			ListOfElements<String> endpoints = new ListOfElements<>(toEdit.endpoints, t -> new Label(t));
			endpoints.setCaption(msg.getMessage("AuthenticationFlow.endpoints"));
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

	AuthenticationFlowDefinition getAuthenticationFlow()
	{
		return binder.getBean();
	}
}
