/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.tabs;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;

import io.imunity.vaadin.endpoint.common.api.services.DefaultServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorBase.EditorTab;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;


/**
 * Service base authentication editor tab
 * 
 * @author P.Piernik
 *
 */
public class AuthenticationTab extends VerticalLayout implements EditorTab
{
	private MessageSource msg;
	private final List<String> allRealms;
	private final List<String> flows;
	private final List<String> authenticators;

	public AuthenticationTab(MessageSource msg, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, List<String> allRealms, String binding)

	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.flows = WebServiceAuthenticationTab.filterBindingCompatibleAuthenticationFlow(flows, authenticators, binding);
		this.authenticators = authenticators.stream().filter(a -> a.getSupportedBindings().contains(binding))
				.map(a -> a.getId()).collect(Collectors.toList());
	}

	public void initUI(Binder<DefaultServiceDefinition> binder)
	{	
		FormLayout mainAuthenticationLayout = new FormLayout();
		mainAuthenticationLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		mainAuthenticationLayout.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());

		ComboBox<String> realm = new ComboBox<>();
		realm.setItems(allRealms);
		binder.forField(realm)
				.asRequired()
				.bind("realm");
		mainAuthenticationLayout.addFormItem(realm, msg.getMessage("ServiceEditorBase.realm"));

		Map<String, List<String>> labels = new HashMap<>();
		labels.put(msg.getMessage("ServiceEditorBase.flows"), flows);
		labels.put(msg.getMessage("ServiceEditorBase.authenticators"), authenticators);
		GroupedValuesChipsWithDropdown authAndFlows = new GroupedValuesChipsWithDropdown(labels);
		authAndFlows.setWidth(50, Unit.EM);	
		binder.forField(authAndFlows)
		.withConverter(List::copyOf, l -> new HashSet<>(l == null ? new ArrayList<>() : l))
		.withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();

		}).bind("authenticationOptions");
		authAndFlows.setRequiredIndicatorVisible(true);
		mainAuthenticationLayout.addFormItem(authAndFlows, msg.getMessage("ServiceEditorBase.authenticatorsAndFlows"));
		add(mainAuthenticationLayout);
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.AUTHENTICATION.toString();
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.SIGN_IN;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("ServiceEditorBase.authentication");
	}
}
