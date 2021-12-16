/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import java.util.List;
import java.util.function.Supplier;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.attr.introspection.config.AttrIntrospectionAttributePoliciesConfiguration;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.authn.IdPInfo;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;

class AttributePoliciesTab extends CustomComponent implements EditorTab
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
		setIcon(Images.attributes.getResource());
		setCaption(msg.getMessage("AttributePoliciesTab.attributePolicies"));
		VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.setMargin(false);

		AttributesGrid defaultPolicyAttrs = new AttributesGrid(msg);
		CollapsibleLayout defaultPolicyLayout = new CollapsibleLayout(
				msg.getMessage("AttributePoliciesTab.defaultPolicy"), defaultPolicyAttrs);
		defaultPolicyLayout.expand();
		attrPoliciesConfigBinder.forField(defaultPolicyAttrs).bind("defaultPolicyAttributes");
		mainWrapper.addComponent(defaultPolicyLayout);

		AttributePolicyConfigurationList customPoliciesList = new AttributePolicyConfigurationList(msg, providersSupplier.get());
		CollapsibleLayout customPoliciesLayout = new CollapsibleLayout(
				msg.getMessage("AttributePoliciesTab.customPolicies"), customPoliciesList);
		attrPoliciesConfigBinder.forField(customPoliciesList).bind("customPolicies");
		mainWrapper.addComponent(customPoliciesLayout);

		setCompositionRoot(mainWrapper);
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
}
