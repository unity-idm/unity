/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientHttpMethod;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties.VerificationProtocol;
import pl.edu.icm.unity.oauth.rp.verificator.BearerTokenVerificator;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class OAuthRPAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{

	private final OAuthAccessTokenRepository tokenMan;
	private final PKIManagement pkiMan;
	private final InputTranslationProfileFieldFactory profileFieldFactory;
	private final Set<String> validatorNames;

	private Binder<OAuthRPConfiguration> configBinder;

	OAuthRPAuthenticatorEditor(MessageSource msg, OAuthAccessTokenRepository tokenMan, PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory) throws EngineException
	{
		super(msg);
		this.tokenMan = tokenMan;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		validatorNames = pkiMan.getValidatorNames();
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("OAuthRPAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(OAuthRPConfiguration.class);

		FormLayout header = buildHeaderSection();

		AccordionPanel remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher,
				configBinder, "translationProfile");
		remoteDataMapping.setWidthFull();
		AccordionPanel advanced = buildAdvancedSection();
		advanced.setWidthFull();

		OAuthRPConfiguration config = new OAuthRPConfiguration(pkiMan, tokenMan);
		if (editMode)
		{
			config.fromProperties(toEdit.configuration);
		}

		configBinder.setBean(config);

		VerticalLayout mainView = new VerticalLayout();
		mainView.setPadding(false);
		mainView.add(header);
		mainView.add(remoteDataMapping, advanced);

		return mainView;
	}

	private FormLayout buildHeaderSection()
	{
		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));

		TextField clientId = new TextField();
		clientId.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(clientId).asRequired(msg.getMessage("fieldRequired"))
				.bind(OAuthRPConfiguration::getClientId, OAuthRPConfiguration::setClientId);
		header.addFormItem(clientId, msg.getMessage("OAuthRPAuthenticatorEditor.clientId"));

		TextField clientSecret = new TextField();
		clientSecret.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(clientSecret).asRequired(msg.getMessage("fieldRequired"))
				.bind(OAuthRPConfiguration::getClientSecret, OAuthRPConfiguration::setClientSecret);
		header.addFormItem(clientSecret, msg.getMessage("OAuthRPAuthenticatorEditor.clientSecret"));
		
		MultiSelectComboBox<String> requiredScopes = new CustomValuesMultiSelectComboBox();
		requiredScopes.setPlaceholder(msg.getMessage("typeAndConfirm"));
		requiredScopes.setWidth(TEXT_FIELD_BIG.value());
		header.addFormItem(requiredScopes, msg.getMessage("OAuthRPAuthenticatorEditor.requiredScopes"));
		configBinder.forField(requiredScopes)
				.withConverter(List::copyOf, HashSet::new)
				.bind(OAuthRPConfiguration::getRequiredScopes, OAuthRPConfiguration::setRequiredScopes);
		
		Select<VerificationProtocol> verificationProtocol = new Select<>();
		verificationProtocol.setItems(VerificationProtocol.values());
		configBinder.forField(verificationProtocol)
				.bind(OAuthRPConfiguration::getVerificationProtocol, OAuthRPConfiguration::setVerificationProtocol);
		header.addFormItem(verificationProtocol, msg.getMessage("OAuthRPAuthenticatorEditor.verificationProtocol"));

		TextField verificationEndpoint = new TextField();
		verificationEndpoint.setWidth(TEXT_FIELD_BIG.value());
		verificationEndpoint.setRequiredIndicatorVisible(false);
		configBinder.forField(verificationEndpoint).asRequired((v, c) -> {

			if (verificationProtocol.getValue() != VerificationProtocol.internal && v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			} else
			{
				return ValidationResult.ok();
			}
		}).bind(OAuthRPConfiguration::getVerificationEndpoint, OAuthRPConfiguration::setVerificationEndpoint);
		header.addFormItem(verificationEndpoint, msg.getMessage("OAuthRPAuthenticatorEditor.verificationEndpoint"));

		TextField profileEndpoint = new TextField();
		profileEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(profileEndpoint)
				.bind(OAuthRPConfiguration::getProfileEndpoint, OAuthRPConfiguration::setProfileEndpoint);
		header.addFormItem(profileEndpoint, msg.getMessage("OAuthRPAuthenticatorEditor.profileEndpoint"));

		return header;
	}

	private AccordionPanel buildAdvancedSection()
	{
		FormLayout advanced = new FormLayout();
		advanced.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		advanced.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		IntegerField cacheTime = new IntegerField();
		configBinder.forField(cacheTime)
				.bind(OAuthRPConfiguration::getCacheTime, OAuthRPConfiguration::setCacheTime);
		advanced.addFormItem(cacheTime, msg.getMessage("OAuthRPAuthenticatorEditor.cacheTime"));

		Select<ClientAuthnMode> clientAuthenticationMode = new Select<>();
		clientAuthenticationMode.setItems(ClientAuthnMode.values());
		configBinder.forField(clientAuthenticationMode)
				.bind(OAuthRPConfiguration::getClientAuthenticationMode, OAuthRPConfiguration::setClientAuthenticationMode);
		advanced.addFormItem(clientAuthenticationMode, msg.getMessage("OAuthRPAuthenticatorEditor.clientAuthenticationMode"));

		Select<ClientAuthnMode> clientAuthenticationModeForProfile = new Select<>();
		clientAuthenticationModeForProfile.setItems(ClientAuthnMode.values());
		clientAuthenticationModeForProfile.setEmptySelectionAllowed(false);
		configBinder.forField(clientAuthenticationModeForProfile)
				.bind(OAuthRPConfiguration::getClientAuthenticationModeForProfile, OAuthRPConfiguration::setClientAuthenticationModeForProfile);
		advanced.addFormItem(clientAuthenticationModeForProfile, msg.getMessage("OAuthRPAuthenticatorEditor.clientAuthenticationModeForProfile"));

		Select<ServerHostnameCheckingMode> clientHostnameChecking = new Select<>();
		clientHostnameChecking.setItems(ServerHostnameCheckingMode.values());
		clientHostnameChecking.setEmptySelectionAllowed(false);
		configBinder.forField(clientHostnameChecking)
				.bind(OAuthRPConfiguration::getClientHostnameChecking, OAuthRPConfiguration::setClientHostnameChecking);
		advanced.addFormItem(clientHostnameChecking, msg.getMessage("OAuthRPAuthenticatorEditor.clientHostnameChecking"));

		Select<String> clientTrustStore = new Select<>();
		clientTrustStore.setItems(validatorNames);
		clientTrustStore.setEmptySelectionAllowed(true);

		configBinder.forField(clientTrustStore)
				.bind(OAuthRPConfiguration::getClientTrustStore, OAuthRPConfiguration::setClientTrustStore);
		advanced.addFormItem(clientTrustStore, msg.getMessage("OAuthRPAuthenticatorEditor.clientTrustStore"));

		Select<ClientHttpMethod> clientHttpMethodForProfileAccess = new Select<>();
		clientHttpMethodForProfileAccess.setItems(ClientHttpMethod.values());
		clientHttpMethodForProfileAccess.setEmptySelectionAllowed(false);
		configBinder.forField(clientHttpMethodForProfileAccess)
				.bind(OAuthRPConfiguration::getClientHttpMethodForProfileAccess, OAuthRPConfiguration::setClientHttpMethodForProfileAccess);
		advanced.addFormItem(clientHttpMethodForProfileAccess, msg.getMessage("OAuthRPAuthenticatorEditor.clientHttpMethodForProfileAccess"));

		Checkbox openIdMode = new Checkbox(msg.getMessage("OAuthRPAuthenticatorEditor.openIdMode"));
		configBinder.forField(openIdMode)
				.bind(OAuthRPConfiguration::isOpenIdMode, OAuthRPConfiguration::setOpenIdMode);
		advanced.addFormItem(openIdMode, "");

		return new AccordionPanel(msg.getMessage("OAuthRPAuthenticatorEditor.advanced"), advanced);
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), BearerTokenVerificator.NAME, getConfiguration(), null);
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
