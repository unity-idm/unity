/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.web;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.webadmin.tprofile.TranslationRulesPresenter;
import io.imunity.webconsole.utils.tprofile.EditInputTranslationProfileSubViewHelper;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientHttpMethod;
import pl.edu.icm.unity.oauth.client.web.authnEditor.OAuthBaseConfiguration;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties.VerificationProtocol;
import pl.edu.icm.unity.oauth.rp.verificator.BearerTokenVerificator;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * OAuthRP authenticator editor
 * 
 * @author P.Piernik
 *
 */
public class OAuthRPAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private static final int LINK_FIELD_WIDTH = 50;

	private TokensManagement tokenMan;
	private PKIManagement pkiMan;
	private EditInputTranslationProfileSubViewHelper profileHelper;

	private Set<String> validators;
	private Binder<OAuthRPConfiguration> configBinder;

	public OAuthRPAuthenticatorEditor(UnityMessageSource msg, TokensManagement tokenMan, PKIManagement pkiMan,
			EditInputTranslationProfileSubViewHelper profileHelper) throws EngineException
	{
		super(msg);
		this.tokenMan = tokenMan;
		this.pkiMan = pkiMan;
		this.profileHelper = profileHelper;
		validators = pkiMan.getValidatorNames();
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("OAuthRPAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(OAuthRPConfiguration.class);

		FormLayout header = buildHeaderSection();

		TranslationRulesPresenter profileRulesViewer = profileHelper.getRulesPresenterInstance();
		CollapsibleLayout remoteDataMapping = profileHelper.buildRemoteDataMappingEditorSection(subViewSwitcher,
				profileRulesViewer, p -> configBinder.getBean().setTranslationProfile(p),
				() -> configBinder.getBean().getTranslationProfile());
		CollapsibleLayout advanced = buildAdvancedSection();

		OAuthRPConfiguration config = new OAuthRPConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration);
		}

		configBinder.setBean(config);
		profileRulesViewer.setInput(configBinder.getBean().getTranslationProfile().getRules());

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(remoteDataMapping);
		mainView.addComponent(advanced);

		return mainView;
	}

	private FormLayout buildHeaderSection()
	{

		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);

		TextField clientId = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.clientId"));
		clientId.setWidth(30, Unit.EM);
		configBinder.forField(clientId).asRequired(msg.getMessage("fieldRequired")).bind("clientId");
		header.addComponent(clientId);

		TextField clientSecret = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.clientSecret"));
		clientSecret.setWidth(30, Unit.EM);
		configBinder.forField(clientSecret).asRequired(msg.getMessage("fieldRequired")).bind("clientSecret");
		header.addComponent(clientSecret);

		TextField requiredScopes = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.requiredScopes"));
		requiredScopes.setWidth(30, Unit.EM);
		configBinder.forField(requiredScopes).bind("requiredScopes");
		header.addComponent(requiredScopes);

		ComboBox<VerificationProtocol> verificationProtocol = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.verificationProtocol"));
		verificationProtocol.setItems(VerificationProtocol.values());
		verificationProtocol.setValue(VerificationProtocol.unity);
		configBinder.forField(verificationProtocol).bind("verificationProtocol");
		header.addComponent(verificationProtocol);

		TextField verificationEndpoint = new TextField(
				msg.getMessage("OAuthRPAuthenticatorEditor.verificationEndpoint"));
		verificationEndpoint.setWidth(LINK_FIELD_WIDTH, Unit.EM);
		verificationEndpoint.setRequiredIndicatorVisible(false);
		configBinder.forField(verificationEndpoint).asRequired((v, c) -> {

			if (verificationProtocol.getValue() != VerificationProtocol.internal && v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			} else
			{
				return ValidationResult.ok();
			}
		}).bind("verificationEndpoint");
		header.addComponent(verificationEndpoint);

		TextField profileEndpoint = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.profileEndpoint"));
		profileEndpoint.setWidth(LINK_FIELD_WIDTH, Unit.EM);
		configBinder.forField(profileEndpoint).bind("profileEndpoint");
		header.addComponent(profileEndpoint);

		return header;
	}

	private CollapsibleLayout buildAdvancedSection()
	{
		FormLayoutWithFixedCaptionWidth advanced = new FormLayoutWithFixedCaptionWidth();
		advanced.setMargin(false);

		TextField cacheTime = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.cacheTime"));
		configBinder.forField(cacheTime)
				.withConverter(new StringToIntegerConverter(
						msg.getMessage("OAuthRPAuthenticatorEditor.cacheTime.notANumber")))
				.bind("cacheTime");

		advanced.addComponent(cacheTime);

		ComboBox<ServerHostnameCheckingMode> clientHostnameChecking = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.clientHostnameChecking"));
		clientHostnameChecking.setItems(ServerHostnameCheckingMode.values());
		clientHostnameChecking.setValue(ServerHostnameCheckingMode.FAIL);
		configBinder.forField(clientHostnameChecking).bind("clientHostnameChecking");
		advanced.addComponent(clientHostnameChecking);

		ComboBox<String> clientTrustStore = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.clientTrustStore"));
		clientTrustStore.setItems(validators);

		configBinder.forField(clientTrustStore).bind("clientTrustStore");
		advanced.addComponent(clientTrustStore);

		ComboBox<ClientHttpMethod> clientHttpMethodForProfileAccess = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.clientHttpMethodForProfileAccess"));
		clientHttpMethodForProfileAccess.setItems(ClientHttpMethod.values());
		clientHttpMethodForProfileAccess.setValue(ClientHttpMethod.get);
		configBinder.forField(clientHttpMethodForProfileAccess).bind("clientHttpMethodForProfileAccess");
		advanced.addComponent(clientHttpMethodForProfileAccess);

		return new CollapsibleLayout(msg.getMessage("OAuthRPAuthenticatorEditor.advanced"), advanced);
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
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

	public class OAuthRPConfiguration extends OAuthBaseConfiguration
	{
		private int cacheTime;
		private VerificationProtocol verificationProtocol;
		private String verificationEndpoint;
		private boolean openIdMode;
		private String requiredScopes;

		public OAuthRPConfiguration()
		{
			super();
		}

		public void fromProperties(String source)
		{
			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(source));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the oauth-rp verificator", e);
			}

			OAuthRPProperties oauthRPprop = new OAuthRPProperties(raw, pkiMan, tokenMan);

			setCacheTime(oauthRPprop.getIntValue(OAuthRPProperties.CACHE_TIME));
			setVerificationProtocol(oauthRPprop.getEnumValue(OAuthRPProperties.VERIFICATION_PROTOCOL,
					VerificationProtocol.class));
			setVerificationEndpoint(oauthRPprop.getValue(OAuthRPProperties.VERIFICATION_ENDPOINT));
			setProfileEndpoint(oauthRPprop.getValue(OAuthRPProperties.PROFILE_ENDPOINT));
			setClientAuthenticationMode(oauthRPprop.getEnumValue(OAuthRPProperties.CLIENT_AUTHN_MODE,
					ClientAuthnMode.class));
			setClientAuthenticationModeForProfile(oauthRPprop.getEnumValue(
					OAuthRPProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS, ClientAuthnMode.class));
			setClientHttpMethodForProfileAccess(oauthRPprop.getEnumValue(
					OAuthRPProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
					ClientHttpMethod.class));
			setRequiredScopes(oauthRPprop.getValue(OAuthRPProperties.REQUIRED_SCOPES));
			setClientId(oauthRPprop.getValue(OAuthRPProperties.CLIENT_ID));
			setClientSecret(oauthRPprop.getValue(OAuthRPProperties.CLIENT_SECRET));
			setOpenIdMode(oauthRPprop.getBooleanValue(OAuthRPProperties.OPENID_MODE));

			setClientHostnameChecking(oauthRPprop.getEnumValue(OAuthRPProperties.CLIENT_HOSTNAME_CHECKING,
					ServerHostnameCheckingMode.class));
			setClientTrustStore(oauthRPprop.getValue(OAuthRPProperties.CLIENT_TRUSTSTORE));

			if (oauthRPprop.isSet(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE))
			{
				setTranslationProfile(TranslationProfileGenerator.getProfileFromString(oauthRPprop
						.getValue(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE)));

			} else
			{
				setTranslationProfile(TranslationProfileGenerator.generateIncludeInputProfile(
						oauthRPprop.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE)));
			}
		}

		public String toProperties() throws ConfigurationException
		{
			Properties raw = new Properties();
			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_ID, getClientId());
			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_SECRET, getClientSecret());

			if (getRequiredScopes() != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.REQUIRED_SCOPES,
						getRequiredScopes());
			}

			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.OPENID_MODE, String.valueOf(openIdMode));

			if (getVerificationEndpoint() != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.VERIFICATION_ENDPOINT,
						getVerificationEndpoint());
			}

			if (verificationProtocol != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.VERIFICATION_PROTOCOL,
						verificationProtocol.toString());
			}

			if (getProfileEndpoint() != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.PROFILE_ENDPOINT,
						getProfileEndpoint());
			}

			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CACHE_TIME, String.valueOf(cacheTime));

			if (getClientAuthenticationMode() != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_AUTHN_MODE,
						getClientAuthenticationMode().toString());
			}

			if (getClientAuthenticationModeForProfile() != null)
			{
				raw.put(OAuthRPProperties.PREFIX
						+ OAuthRPProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS,
						getClientAuthenticationModeForProfile().toString());
			}

			if (getClientHttpMethodForProfileAccess() != null)
			{
				raw.put(OAuthRPProperties.PREFIX
						+ OAuthRPProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
						getClientHttpMethodForProfileAccess().toString());
			}

			try
			{
				raw.put(OAuthRPProperties.PREFIX
						+ CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE,
						Constants.MAPPER.writeValueAsString(
								getTranslationProfile().toJsonObject()));
			} catch (Exception e)
			{
				throw new InternalException("Can't serialize authenticator translation profile to JSON",
						e);
			}

			if (getClientHostnameChecking() != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_HOSTNAME_CHECKING,
						getClientHostnameChecking().toString());
			}

			if (getClientTrustStore() != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_TRUSTSTORE,
						getClientTrustStore());
			}

			OAuthRPProperties prop = new OAuthRPProperties(raw, pkiMan, tokenMan);
			return prop.getAsString();

		}

		public int getCacheTime()
		{
			return cacheTime;
		}

		public void setCacheTime(int cacheTime)
		{
			this.cacheTime = cacheTime;
		}

		public VerificationProtocol getVerificationProtocol()
		{
			return verificationProtocol;
		}

		public void setVerificationProtocol(VerificationProtocol verificationProtocol)
		{
			this.verificationProtocol = verificationProtocol;
		}

		public String getVerificationEndpoint()
		{
			return verificationEndpoint;
		}

		public void setVerificationEndpoint(String verificationEndpoint)
		{
			this.verificationEndpoint = verificationEndpoint;
		}

		public boolean isOpenIdMode()
		{
			return openIdMode;
		}

		public void setOpenIdMode(boolean openIdMode)
		{
			this.openIdMode = openIdMode;
		}

		public String getRequiredScopes()
		{
			return requiredScopes;
		}

		public void setRequiredScopes(String requiredScopes)
		{
			this.requiredScopes = requiredScopes;
		}
	}
}
