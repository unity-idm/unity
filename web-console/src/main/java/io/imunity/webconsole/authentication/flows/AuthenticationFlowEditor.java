/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;

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
	
	public AuthenticationFlowEditor(UnityMessageSource msg, AuthenticationFlowEntry toEdit, List<String> authenticators)
	{
		name = new TextField(msg.getMessage("AuthenticationFlow.name"));
		name.setWidth(100, Unit.PERCENTAGE);
		
		name.setValue(toEdit.flow.getName());
	
		firstFactorAuthenticators = new ChipsWithDropdown<>(s -> s, true);
		firstFactorAuthenticators.setCaption(msg.getMessage("AuthenticationFlow.firstFactorAuthenticators"));
		firstFactorAuthenticators.setItems(authenticators);
		if (toEdit.flow.getFirstFactorAuthenticators() != null) 
		{
			firstFactorAuthenticators.setSelectedItems(toEdit.flow.getFirstFactorAuthenticators().stream().collect(Collectors.toList()));
		}
		secondFactorAuthenticators = new ChipsWithDropdown<>(s -> s, true);
		secondFactorAuthenticators.setCaption(msg.getMessage("AuthenticationFlow.secondFactorAuthenticators"));
		secondFactorAuthenticators.setItems(authenticators);
		if (toEdit.flow.getSecondFactorAuthenticators() != null) 
		{
			secondFactorAuthenticators.setSelectedItems(toEdit.flow.getSecondFactorAuthenticators().stream().collect(Collectors.toList()));
		}
		
		policy = new ComboBox<>(
				msg.getMessage("AuthenticationFlow.policy"));
		policy.setItems(Policy.values());
		policy.setValue(Policy.REQUIRE);
		policy.setEmptySelectionAllowed(false);
			
		binder = new Binder<>(AuthenticationFlowDefinition.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.forField(policy).bind("policy");
		binder.setBean(toEdit.flow);
	
		FormLayout mainLayout = new FormLayout();
		mainLayout.setMargin(false);

		Label endpoints = new Label();
		endpoints.setCaption(msg.getMessage("AuthenticationRealm.endpoints"));
		endpoints.setWidth(100, Unit.PERCENTAGE);
		endpoints.setValue(String.join(", ", toEdit.endpoints));
		
		
		mainLayout.addComponents(name, policy, firstFactorAuthenticators, secondFactorAuthenticators);
		if (!toEdit.endpoints.isEmpty())
		{
			mainLayout.addComponent(endpoints);
		}
		
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

	public AuthenticationFlowDefinition getAuthenticationFlow()
	{		
		AuthenticationFlowDefinition def = binder.getBean();
		def.setFirstFactorAuthenticators(firstFactorAuthenticators.getSelectedItems().stream().collect(Collectors.toSet()));
		def.setSecondFactorAuthenticators(secondFactorAuthenticators.getSelectedItems());
		return def;
	}
}
