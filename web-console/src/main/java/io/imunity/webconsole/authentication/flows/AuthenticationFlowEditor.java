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

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.DynamicPolicyConfigurationMVELContextKey;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.mvel.MVELExpressionField;
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
	private Binder<AuthenticationFlowDefinitionForBinder> binder;
	private ComboBox<Policy> policy;

	AuthenticationFlowEditor(MessageSource msg, AuthenticationFlowEntry toEdit, List<String> authenticators)
	{
		name = new TextField(msg.getMessage("AuthenticationFlow.name"));
		name.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
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

		MVELExpressionField policyConfig = new MVELExpressionField(msg, msg.getMessage("AuthenticationFlow.policyConfiguration"),
				msg.getMessage("MVELExpressionField.conditionDesc"), MVELExpressionContext.builder()
						.withTitleKey("AuthenticationFlow.policyConfigurationTitle")
						.withEvalToKey("MVELExpressionField.evalToBoolean")
						.withVars(DynamicPolicyConfigurationMVELContextKey.toMap())
						.build());
		policyConfig.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		
		policyConfig.setVisible(false);
		
		binder = new Binder<>(AuthenticationFlowDefinitionForBinder.class);
		binder.forField(name)
				.withValidator(new NoSpaceValidator(msg))
				.asRequired(msg.getMessage("fieldRequired"))
				.bind("name");
		binder.forField(firstFactorAuthenticators)
				.withNullRepresentation(Collections.emptyList())
				.withConverter(l -> l != null ? l.stream()
						.collect(Collectors.toSet()) : null, s -> s != null
								? s.stream()
										.collect(Collectors.toList())
								: null)
				.asRequired()
				.bind("firstFactorAuthenticators");
		binder.forField(secondFactorAuthenticators)
				.bind("secondFactorAuthenticators");
		binder.forField(policy)
				.bind("policy");
		binder.forField(policyConfig).bind("policyConfiguration");
		
		policy.addValueChangeListener(v ->
		{
			policyConfig.setVisible(v.getValue()
					.equals(Policy.DYNAMIC_EXPRESSION));
			if (!v.getValue()
					.equals(Policy.DYNAMIC_EXPRESSION))
			{
				policyConfig.clear();
			}
		});
		
		
		
		binder.setBean(toEdit.flow);

		FormLayout mainLayout = new FormLayout();
		mainLayout.setMargin(false);

		mainLayout.addComponents(name, policy, policyConfig, firstFactorAuthenticators, secondFactorAuthenticators);
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
		return binder.validate()
				.hasErrors();
	}

	AuthenticationFlowDefinitionForBinder getAuthenticationFlow()
	{
		return binder.getBean();
	}
}
