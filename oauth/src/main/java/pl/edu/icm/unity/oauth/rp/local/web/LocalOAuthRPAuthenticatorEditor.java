/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.data.binder.Binder;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.oauth.rp.local.LocalAccessTokenVerificator;

import java.util.HashSet;
import java.util.List;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

class LocalOAuthRPAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private Binder<LocalOAuthRPConfiguration> configBinder;

	LocalOAuthRPAuthenticatorEditor(MessageSource msg)
	{
		super(msg);
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("LocalOAuthRPAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(LocalOAuthRPConfiguration.class);

		FormLayout header = buildHeaderSection();

		LocalOAuthRPConfiguration config = new LocalOAuthRPConfiguration();
		if (editMode)
			config.fromProperties(toEdit.configuration);

		configBinder.setBean(config);

		VerticalLayout mainView = new VerticalLayout();
		mainView.setPadding(false);
		mainView.add(header);

		return mainView;
	}

	private FormLayout buildHeaderSection()
	{
		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));

		MultiSelectComboBox<String> requiredScopes = new CustomValuesMultiSelectComboBox();
		requiredScopes.setPlaceholder(msg.getMessage("typeAndConfirm"));
		requiredScopes.setWidth(TEXT_FIELD_BIG.value());
		header.addFormItem(requiredScopes, msg.getMessage("LocalOAuthRPAuthenticatorEditor.requiredScopes"));
		configBinder.forField(requiredScopes)
				.withConverter(List::copyOf, HashSet::new)
				.bind(LocalOAuthRPConfiguration::getRequiredScopes, LocalOAuthRPConfiguration::setRequiredScopes);

		return header;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), LocalAccessTokenVerificator.NAME, getConfiguration(), null);
	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();
		try
		{
			return configBinder.getBean().toProperties();
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the oauth-rp verificator", e);
		}
	}
}
