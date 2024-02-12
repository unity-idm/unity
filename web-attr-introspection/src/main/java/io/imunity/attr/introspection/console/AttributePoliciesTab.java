/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import java.util.List;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.attr.introspection.config.AttrIntrospectionAttributePoliciesConfiguration;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorBase.EditorTab;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;

class AttributePoliciesTab extends VerticalLayout implements EditorTab
{
	private final MessageSource msg;
	private final Supplier<List<IdPInfo>> providersSupplier;

	AttributePoliciesTab(MessageSource msg, Supplier<List<IdPInfo>> providersSupplier)
	{
		this.msg = msg;
		this.providersSupplier = providersSupplier;
	}

	void initUI(Binder<AttrIntrospectionAttributePoliciesConfiguration> attrPoliciesConfigBinder)
	{
		VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.setMargin(false);
		mainWrapper.setPadding(false);
		mainWrapper.setSizeFull();

		AttributesGrid defaultPolicyAttrs = new AttributesGrid(msg);
		AccordionPanel defaultPolicyLayout = new AccordionPanel(msg.getMessage("AttributePoliciesTab.defaultPolicy"),
				defaultPolicyAttrs);
		defaultPolicyLayout.setWidthFull();
		defaultPolicyLayout.setOpened(true);
		attrPoliciesConfigBinder.forField(defaultPolicyAttrs)
				.bind("defaultPolicyAttributes");
		mainWrapper.add(defaultPolicyLayout);

		AttributePolicyConfigurationList customPoliciesList = new AttributePolicyConfigurationList(msg,
				providersSupplier.get());
		AccordionPanel customPoliciesLayout = new AccordionPanel(msg.getMessage("AttributePoliciesTab.customPolicies"),
				customPoliciesList);
		customPoliciesLayout.setWidthFull();

		attrPoliciesConfigBinder.forField(customPoliciesList)
				.bind("customPolicies");
		mainWrapper.add(customPoliciesLayout);

		add(mainWrapper);
		setMargin(false);
		setSizeFull();
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.OTHER.toString();
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.TAGS;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("AttributePoliciesTab.attributePolicies");
	}
}
