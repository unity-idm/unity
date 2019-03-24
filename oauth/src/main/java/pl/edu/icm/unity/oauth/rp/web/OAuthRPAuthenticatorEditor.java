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
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties.VerificationProtocol;
import pl.edu.icm.unity.oauth.rp.verificator.BearerTokenVerificator;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * OAuthRP authenticator editor
 * @author P.Piernik
 *
 */
public class OAuthRPAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private static final int LINK_FIELD_WIDTH = 50;

	private TokensManagement tokenMan;
	private PKIManagement pkiMan;
	private Set<String> validators;
	private boolean editMode;
	private Binder<OAuthRPConfiguration> binder;
	private EditInputTranslationProfileSubViewHelper profileHelper;

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
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher, boolean forceNameEditable)
	{
		editMode = toEdit != null;
		setName(editMode ? toEdit.id : msg.getMessage("OAuthRPAuthenticatorEditor.defaultName"));
		setNameReadOnly(editMode && !forceNameEditable);

		binder = new Binder<>(OAuthRPConfiguration.class);

		FormLayout header = buildHeaderSection();

		TranslationRulesPresenter profileRulesViewer = profileHelper.getRulesPresenterInstance();
		CollapsibleLayout remoteDataMapping = profileHelper.buildRemoteDataMappingEditorSection(subViewSwitcher,
				profileRulesViewer, p -> binder.getBean().setTranslationProfile(p),
				() -> binder.getBean().getTranslationProfile());
		CollapsibleLayout advanced = buildAdvancedSection();

		OAuthRPConfiguration config = new OAuthRPConfiguration();
		if (toEdit != null)
		{
			config.fromProperties(toEdit.configuration);
		}

		binder.setBean(config);
		profileRulesViewer.setInput(binder.getBean().getTranslationProfile().getRules());

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
		binder.forField(clientId).asRequired(msg.getMessage("fieldRequired")).bind("clientId");
		header.addComponent(clientId);

		TextField clientSecret = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.clientSecret"));
		clientSecret.setWidth(30, Unit.EM);
		binder.forField(clientSecret).asRequired(msg.getMessage("fieldRequired")).bind("clientSecret");
		header.addComponent(clientSecret);

		TextField requiredScopes = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.requiredScopes"));
		requiredScopes.setWidth(30, Unit.EM);
		binder.forField(requiredScopes).bind("requiredScopes");
		header.addComponent(requiredScopes);

		ComboBox<VerificationProtocol> verificationProtocol = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.verificationProtocol"));
		verificationProtocol.setItems(VerificationProtocol.values());
		verificationProtocol.setValue(VerificationProtocol.unity);
		binder.forField(verificationProtocol).bind("verificationProtocol");
		header.addComponent(verificationProtocol);

		TextField verificationEndpoint = new TextField(
				msg.getMessage("OAuthRPAuthenticatorEditor.verificationEndpoint"));
		verificationEndpoint.setWidth(LINK_FIELD_WIDTH, Unit.EM);
		verificationEndpoint.setRequiredIndicatorVisible(false);
		binder.forField(verificationEndpoint).asRequired((v, c) -> {

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
		binder.forField(profileEndpoint).bind("profileEndpoint");
		header.addComponent(profileEndpoint);

		return header;
	}

	private CollapsibleLayout buildAdvancedSection()
	{
		FormLayoutWithFixedCaptionWidth advanced = new FormLayoutWithFixedCaptionWidth();
		advanced.setMargin(false);
		
		TextField cacheTime = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.cacheTime"));
		binder.forField(cacheTime)
				.withConverter(new StringToIntegerConverter(
						msg.getMessage("OAuthRPAuthenticatorEditor.cacheTime.notANumber")))
				.bind("cacheTime");

		advanced.addComponent(cacheTime);

		ComboBox<ServerHostnameCheckingMode> clientHostnameChecking = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.clientHostnameChecking"));
		clientHostnameChecking.setItems(ServerHostnameCheckingMode.values());
		clientHostnameChecking.setValue(ServerHostnameCheckingMode.FAIL);
		binder.forField(clientHostnameChecking).bind("clientHostnameChecking");
		advanced.addComponent(clientHostnameChecking);

		ComboBox<String> clientTrustStore = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.clientTrustStore"));
		clientTrustStore.setItems(validators);

		binder.forField(clientTrustStore).bind("clientTrustStore");
		advanced.addComponent(clientTrustStore);

		ComboBox<ClientHttpMethod> clientHttpMethodForProfileAccess = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.clientHttpMethodForProfileAccess"));
		clientHttpMethodForProfileAccess.setItems(ClientHttpMethod.values());
		clientHttpMethodForProfileAccess.setValue(ClientHttpMethod.get);
		binder.forField(clientHttpMethodForProfileAccess).bind("clientHttpMethodForProfileAccess");
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
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		return binder.getBean().toProperties();
	}

	public class OAuthRPConfiguration
	{
		private int cacheTime;
		private VerificationProtocol verificationProtocol;
		private String verificationEndpoint;
		private String profileEndpoint;
		private ClientAuthnMode clientAuthenticationMode;
		private ClientAuthnMode clientAuthenticationModeForProfile;
		private ClientHttpMethod clientHttpMethodForProfileAccess;
		private String clientId;
		private String clientSecret;
		private String requiredScopes;
		private boolean openIdMode;
		private ServerHostnameCheckingMode clientHostnameChecking;
		private String clientTrustStore;
		private TranslationProfile translationProfile;

		public OAuthRPConfiguration()
		{
			translationProfile = TranslationProfileGenerator.generateEmptyInputProfile();
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

			cacheTime = oauthRPprop.getIntValue(OAuthRPProperties.CACHE_TIME);
			verificationProtocol = oauthRPprop.getEnumValue(OAuthRPProperties.VERIFICATION_PROTOCOL,
					VerificationProtocol.class);
			verificationEndpoint = oauthRPprop.getValue(OAuthRPProperties.VERIFICATION_ENDPOINT);
			profileEndpoint = oauthRPprop.getValue(OAuthRPProperties.PROFILE_ENDPOINT);

			clientAuthenticationMode = oauthRPprop.getEnumValue(OAuthRPProperties.CLIENT_AUTHN_MODE,
					ClientAuthnMode.class);
			clientAuthenticationModeForProfile = oauthRPprop.getEnumValue(
					OAuthRPProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS, ClientAuthnMode.class);
			clientHttpMethodForProfileAccess = oauthRPprop.getEnumValue(
					OAuthRPProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
					ClientHttpMethod.class);
			requiredScopes = oauthRPprop.getValue(OAuthRPProperties.REQUIRED_SCOPES);

			clientId = oauthRPprop.getValue(OAuthRPProperties.CLIENT_ID);
			clientSecret = oauthRPprop.getValue(OAuthRPProperties.CLIENT_SECRET);
			openIdMode = oauthRPprop.getBooleanValue(OAuthRPProperties.OPENID_MODE);

			clientHostnameChecking = oauthRPprop.getEnumValue(OAuthRPProperties.CLIENT_HOSTNAME_CHECKING,
					ServerHostnameCheckingMode.class);

			clientTrustStore = oauthRPprop.getValue(OAuthRPProperties.CLIENT_TRUSTSTORE);

			if (oauthRPprop.isSet(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE))
			{
				translationProfile = TranslationProfileGenerator.getProfileFromString(oauthRPprop
						.getValue(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE));

			} else
			{
				translationProfile = TranslationProfileGenerator.generateIncludeInputProfile(
						oauthRPprop.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE));
			}
		}

		public String toProperties()
		{

			Properties raw = new Properties();

			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_ID, clientId);

			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_SECRET, clientSecret);

			if (requiredScopes != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.REQUIRED_SCOPES, requiredScopes);
			}

			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.OPENID_MODE, String.valueOf(openIdMode));

			if (verificationEndpoint != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.VERIFICATION_ENDPOINT,
						verificationEndpoint);
			}

			if (verificationProtocol != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.VERIFICATION_PROTOCOL,
						verificationProtocol.toString());
			}

			if (profileEndpoint != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.PROFILE_ENDPOINT, profileEndpoint);
			}

			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CACHE_TIME, String.valueOf(cacheTime));

			if (clientAuthenticationMode != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_AUTHN_MODE,
						clientAuthenticationMode.toString());
			}

			if (clientAuthenticationModeForProfile != null)
			{
				raw.put(OAuthRPProperties.PREFIX
						+ OAuthRPProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS,
						clientAuthenticationModeForProfile.toString());
			}

			if (clientHttpMethodForProfileAccess != null)
			{
				raw.put(OAuthRPProperties.PREFIX
						+ OAuthRPProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
						clientHttpMethodForProfileAccess.toString());
			}

			try
			{
				raw.put(OAuthRPProperties.PREFIX
						+ CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE,
						Constants.MAPPER.writeValueAsString(translationProfile.toJsonObject()));
			} catch (Exception e)
			{
				throw new InternalException("Can't serialize authenticator translation profile to JSON",
						e);
			}

			if (clientHostnameChecking != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_HOSTNAME_CHECKING,
						clientHostnameChecking.toString());
			}

			if (clientTrustStore != null)
			{
				raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_TRUSTSTORE,
						clientTrustStore);
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

		public ClientAuthnMode getClientAuthenticationMode()
		{
			return clientAuthenticationMode;
		}

		public void setClientAuthenticationMode(ClientAuthnMode clientAuthenticationMode)
		{
			this.clientAuthenticationMode = clientAuthenticationMode;
		}

		public ClientAuthnMode getClientAuthenticationModeForProfile()
		{
			return clientAuthenticationModeForProfile;
		}

		public void setClientAuthenticationModeForProfile(ClientAuthnMode clientAuthenticationModeForProfile)
		{
			this.clientAuthenticationModeForProfile = clientAuthenticationModeForProfile;
		}

		public ClientHttpMethod getClientHttpMethodForProfileAccess()
		{
			return clientHttpMethodForProfileAccess;
		}

		public void setClientHttpMethodForProfileAccess(ClientHttpMethod clientHttpMethodForProfileAccess)
		{
			this.clientHttpMethodForProfileAccess = clientHttpMethodForProfileAccess;
		}

		public String getClientId()
		{
			return clientId;
		}

		public void setClientId(String clientId)
		{
			this.clientId = clientId;
		}

		public String getClientSecret()
		{
			return clientSecret;
		}

		public void setClientSecret(String clientSecret)
		{
			this.clientSecret = clientSecret;
		}

		public String getRequiredScopes()
		{
			return requiredScopes;
		}

		public void setRequiredScopes(String requiredScopes)
		{
			this.requiredScopes = requiredScopes;
		}

		public boolean isOpenIdMode()
		{
			return openIdMode;
		}

		public void setOpenIdMode(boolean openIdMode)
		{
			this.openIdMode = openIdMode;
		}

		public ServerHostnameCheckingMode getClientHostnameChecking()
		{
			return clientHostnameChecking;
		}

		public void setClientHostnameChecking(ServerHostnameCheckingMode clientHostnameChecking)
		{
			this.clientHostnameChecking = clientHostnameChecking;
		}

		public String getClientTrustStore()
		{
			return clientTrustStore;
		}

		public void setClientTrustStore(String clientTrustStore)
		{
			this.clientTrustStore = clientTrustStore;
		}

		public TranslationProfile getTranslationProfile()
		{
			return translationProfile;
		}

		public void setTranslationProfile(TranslationProfile translationProfile)
		{
			this.translationProfile = translationProfile;
		}

		public String getProfileEndpoint()
		{
			return profileEndpoint;
		}

		public void setProfileEndpoint(String profileEndpoint)
		{
			this.profileEndpoint = profileEndpoint;
		}
	}
}
