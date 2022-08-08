/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local.web;

import java.util.Collection;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.rp.local.AccessTokenAndPasswordVerificator;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * OAuthRP authenticator editor
 * 
 * @author P.Piernik
 *
 */
class LocalOAuthRPAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{

	private Binder<LocalOAuthRPConfiguration> configBinder;
	private Collection<CredentialDefinition> credentialDefinitions;

	LocalOAuthRPAuthenticatorEditor(MessageSource msg, Collection<CredentialDefinition> credentialDefinitions)
			throws EngineException
	{
		super(msg);
		this.credentialDefinitions = credentialDefinitions;
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
		{
			config.fromProperties(toEdit.configuration);
		}

		configBinder.setBean(config);

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);

		return mainView;
	}

	private FormLayout buildHeaderSection()
	{

		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);

		ComboBox<String> localCredential = new ComboBox<>();
		localCredential
				.setItems(credentialDefinitions.stream().filter(c -> c.getTypeId().equals(PasswordVerificator.NAME))
						.map(c -> c.getName()).collect(Collectors.toList()));
		localCredential.setCaption(msg.getMessage("LocalOAuthRPAuthenticatorEditor.localCredential"));
		configBinder.forField(localCredential).asRequired().bind("credential");
		header.addComponent(localCredential);

		ChipsWithTextfield requiredScopes = new ChipsWithTextfield(msg);
		requiredScopes.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		requiredScopes.setCaption(msg.getMessage("LocalOAuthRPAuthenticatorEditor.requiredScopes"));
		header.addComponent(requiredScopes);
		configBinder.forField(requiredScopes).bind("requiredScopes");

		return header;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), AccessTokenAndPasswordVerificator.NAME, getConfiguration(), null);
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
			throw new FormValidationException("Invalid configuration of the oauth-rp with password verificator", e);
		}
	}

}
