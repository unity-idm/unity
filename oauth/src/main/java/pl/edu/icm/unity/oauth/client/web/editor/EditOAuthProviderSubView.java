/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.web.editor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.webadmin.tprofile.TranslationRulesPresenter;
import io.imunity.webconsole.utils.tprofile.EditInputTranslationProfileSubViewHelper;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientHttpMethod;
import pl.edu.icm.unity.oauth.client.config.DropboxProviderProperties;
import pl.edu.icm.unity.oauth.client.config.FacebookProviderProperties;
import pl.edu.icm.unity.oauth.client.config.GitHubProviderProperties;
import pl.edu.icm.unity.oauth.client.config.GoogleProviderProperties;
import pl.edu.icm.unity.oauth.client.config.IntuitProviderProperties;
import pl.edu.icm.unity.oauth.client.config.LinkedInProviderProperties;
import pl.edu.icm.unity.oauth.client.config.MicrosoftAzureV2ProviderProperties;
import pl.edu.icm.unity.oauth.client.config.MicrosoftLiveProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties.Providers;
import pl.edu.icm.unity.oauth.client.config.OrcidProviderProperties;
import pl.edu.icm.unity.oauth.client.config.UnityProviderProperties;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

/**
 * SubView for editing oauth authenticator provider
 * @author P.Piernik
 *
 */
public class EditOAuthProviderSubView extends CustomComponent implements UnitySubView
{
	private static final int LINK_FIELD_WIDTH = 50;
	private static final String TMP_CONFIG_KEY = "tmp.";

	private UnityMessageSource msg;
	private PKIManagement pkiMan;
	private Map<String, CustomProviderProperties> templates;

	private Binder<OAuthProviderConfiguration> binder;
	private ComboBox<String> templateCombo;

	private boolean editMode = false;

	public EditOAuthProviderSubView(UnityMessageSource msg, PKIManagement pkiMan,
			EditInputTranslationProfileSubViewHelper profileHelper, OAuthProviderConfiguration toEdit,
			Set<String> providersIds, SubViewSwitcher subViewSwitcher, List<String> registrationForms,
			Set<String> validators, Consumer<OAuthProviderConfiguration> onConfirm, Runnable onCancel)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
	
		editMode = toEdit != null;

		binder = new Binder<>(OAuthProviderConfiguration.class);

		FormLayout header = buildHeaderSection(providersIds);

		TranslationRulesPresenter profileRulesViewer = profileHelper.getRulesPresenterInstance();
		CollapsibleLayout remoteDataMapping = profileHelper.buildRemoteDataMappingEditorSection(subViewSwitcher, profileRulesViewer,
				p -> binder.getBean().setTranslationProfile(p), () -> binder.getBean().getTranslationProfile());

		CollapsibleLayout advanced = buildAdvancedSection(validators, registrationForms);

		binder.setBean(editMode ? toEdit : new OAuthProviderConfiguration());
		profileRulesViewer.setInput(binder.getBean().getTranslationProfile().getRules());

		templateCombo.addValueChangeListener(e -> {
			OAuthProviderConfiguration config = new OAuthProviderConfiguration();
			config.fromTemplate(msg, templates.get(e.getValue()), e.getValue(),
					editMode ? binder.getBean().getId() : null);
			config.setType(e.getValue());
			binder.setBean(config);
			profileRulesViewer.setInput(binder.getBean().getTranslationProfile().getRules());
		});

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(remoteDataMapping);
		mainView.addComponent(advanced);

		Runnable onConfirmR = () -> {
			try
			{
				onConfirm.accept(getProvider());
			} catch (FormValidationException e)
			{
				// ok
			}
		};

		mainView.addComponent(editMode
				? StandardButtonsHelper.buildConfirmEditButtonsBar(msg, onConfirmR, onCancel)
				: StandardButtonsHelper.buildConfirmNewButtonsBar(msg, onConfirmR, onCancel));

		
		setCompositionRoot(mainView);
	}

	private Validator<String> getOpenIdFieldValidator(CheckBox openIdConnect, boolean toCheck)
	{
		return (v, c) -> {
			if (openIdConnect.getValue() == toCheck && v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			} else
			{
				return ValidationResult.ok();
			}
		};
	}

	private void loadTemplates()
	{
		templates = new HashMap<>();

		templates.put(Providers.dropbox.toString(), new DropboxProviderProperties(getTempConfigProperties(),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.github.toString(), new GitHubProviderProperties(getTempConfigProperties(),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.google.toString(), new GoogleProviderProperties(getTempConfigProperties(),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.facebook.toString(), new FacebookProviderProperties(getTempConfigProperties(),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.intuit.toString(), new IntuitProviderProperties(getTempConfigProperties(),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.linkedin.toString(), new LinkedInProviderProperties(getTempConfigProperties(),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.microsoft.toString(), new MicrosoftLiveProviderProperties(
				getTempConfigProperties(), OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.microsoftAzureV2.toString(), new MicrosoftAzureV2ProviderProperties(
				getTempConfigProperties(), OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.orcid.toString(),
				new OrcidProviderProperties(
						addEmptyProviderConfig(getTempConfigProperties(),
								CommonWebAuthnProperties.TRANSLATION_PROFILE),
						OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.unity.toString(),
				new UnityProviderProperties(
						addEmptyProviderConfig(getTempConfigProperties(),
								CustomProviderProperties.OPENID_DISCOVERY),
						OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.custom.toString(), new CustomProviderProperties(
				addEmptyProviderConfig(addEmptyProviderConfig(
						addEmptyProviderConfig(addEmptyProviderConfig(getTempConfigProperties(),
								CustomProviderProperties.PROVIDER_LOCATION),
								CommonWebAuthnProperties.TRANSLATION_PROFILE),
						CustomProviderProperties.ACCESS_TOKEN_ENDPOINT),
						CustomProviderProperties.PROVIDER_NAME),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
	}

	private OAuthProviderConfiguration getProvider() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		return binder.getBean();
	}

	private Properties addEmptyProviderConfig(Properties raw, String key)
	{
		raw.put(OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY + key, "");
		return raw;
	}

	private Properties getTempConfigProperties()
	{
		Properties p = new Properties();
		addEmptyProviderConfig(p, CustomProviderProperties.CLIENT_ID);
		addEmptyProviderConfig(p, CustomProviderProperties.CLIENT_SECRET);
		return p;
	}

	private FormLayout buildHeaderSection(Set<String> providersIds)
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);

		loadTemplates();

		templateCombo = new ComboBox<>();
		templateCombo.setCaption(msg.getMessage("EditOAuthProviderSubView.template"));
		templateCombo.setItems(templates.keySet());

		binder.forField(templateCombo).asRequired(msg.getMessage("fieldRequired")).bind("type");

		if (!editMode)
		{
			header.addComponent(templateCombo);
		}

		TextField id = new TextField(msg.getMessage("EditOAuthProviderSubView.id"));
		binder.forField(id).asRequired(msg.getMessage("fieldRequired")).withValidator((s, c) -> {
			if (providersIds.contains(s))
			{
				return ValidationResult.error(msg.getMessage("EditOAuthProviderSubView.idExists"));
			} else
			{
				return ValidationResult.ok();
			}

		}).bind("id");
		id.setReadOnly(editMode);
		header.addComponent(id);

		I18nTextField name = new I18nTextField(msg, msg.getMessage("EditOAuthProviderSubView.name"));
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		header.addComponent(name);

		TextField clientId = new TextField(msg.getMessage("EditOAuthProviderSubView.clientId"));
		clientId.setWidth(30, Unit.EM);
		binder.forField(clientId).asRequired(msg.getMessage("fieldRequired")).bind("clientId");
		header.addComponent(clientId);

		TextField clientSecret = new TextField(msg.getMessage("EditOAuthProviderSubView.clientSecret"));
		clientSecret.setWidth(30, Unit.EM);
		binder.forField(clientSecret).asRequired(msg.getMessage("fieldRequired")).bind("clientSecret");
		header.addComponent(clientSecret);

		TextField requestedScopes = new TextField(msg.getMessage("EditOAuthProviderSubView.requestedScopes"));
		requestedScopes.setWidth(30, Unit.EM);
		binder.forField(requestedScopes).bind("requestedScopes");
		header.addComponent(requestedScopes);

		I18nTextField iconUrl = new I18nTextField(msg, msg.getMessage("EditOAuthProviderSubView.iconUrl"));
		iconUrl.setWidth(LINK_FIELD_WIDTH, Unit.EM);
		binder.forField(iconUrl).bind("iconUrl");
		header.addComponent(iconUrl);

		CheckBox openIdConnect = new CheckBox(msg.getMessage("EditOAuthProviderSubView.openIdConnect"));
		binder.forField(openIdConnect).bind("openIdConnect");
		header.addComponent(openIdConnect);

		TextField openIdDiscovery = new TextField(
				msg.getMessage("EditOAuthProviderSubView.openIdDiscoverEndpoint"));
		openIdDiscovery.setWidth(LINK_FIELD_WIDTH, Unit.EM);
		binder.forField(openIdDiscovery).asRequired(getOpenIdFieldValidator(openIdConnect, true))
				.bind("openIdDiscoverEndpoint");
		openIdDiscovery.setVisible(false);
		openIdDiscovery.setRequiredIndicatorVisible(false);
		header.addComponent(openIdDiscovery);

		openIdConnect.addValueChangeListener(e -> openIdDiscovery.setVisible(e.getValue()));

		TextField authenticationEndpoint = new TextField(
				msg.getMessage("EditOAuthProviderSubView.authenticationEndpoint"));
		binder.forField(authenticationEndpoint).asRequired(getOpenIdFieldValidator(openIdConnect, false))
				.bind("authenticationEndpoint");
		authenticationEndpoint.setWidth(LINK_FIELD_WIDTH, Unit.EM);
		authenticationEndpoint.setRequiredIndicatorVisible(false);
		header.addComponent(authenticationEndpoint);

		TextField accessTokenEndpoint = new TextField(
				msg.getMessage("EditOAuthProviderSubView.accessTokenEndpoint"));
		accessTokenEndpoint.setWidth(LINK_FIELD_WIDTH, Unit.EM);
		binder.forField(accessTokenEndpoint).asRequired(getOpenIdFieldValidator(openIdConnect, false))
				.bind("accessTokenEndpoint");
		accessTokenEndpoint.setRequiredIndicatorVisible(false);
		header.addComponent(accessTokenEndpoint);

		TextField profileEndpoint = new TextField(msg.getMessage("EditOAuthProviderSubView.profileEndpoint"));
		profileEndpoint.setWidth(LINK_FIELD_WIDTH, Unit.EM);
		binder.forField(profileEndpoint).bind("profileEndpoint");
		header.addComponent(profileEndpoint);

		return header;
	}

	private CollapsibleLayout buildAdvancedSection(Set<String> validators, List<String> registrationForms)
	{
		FormLayoutWithFixedCaptionWidth advanced = new FormLayoutWithFixedCaptionWidth();
		advanced.setMargin(false);

		ComboBox<String> registrationForm = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.registrationForm"));
		registrationForm.setItems(registrationForms);
		binder.forField(registrationForm).bind("registrationForm");
		advanced.addComponent(registrationForm);

		ComboBox<AccessTokenFormat> accessTokenFormat = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.accessTokenFormat"));
		accessTokenFormat.setItems(AccessTokenFormat.values());
		accessTokenFormat.setValue(AccessTokenFormat.standard);
		binder.forField(accessTokenFormat).bind("accessTokenFormat");
		advanced.addComponent(accessTokenFormat);

		ComboBox<ClientAuthnMode> clientAuthenticationMode = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.clientAuthenticationMode"));
		clientAuthenticationMode.setItems(ClientAuthnMode.values());
		clientAuthenticationMode.setValue(ClientAuthnMode.secretBasic);
		binder.forField(clientAuthenticationMode).bind("clientAuthenticationMode");
		advanced.addComponent(clientAuthenticationMode);

		ComboBox<ClientAuthnMode> clientAuthenticationModeForProfile = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.clientAuthenticationModeForProfile"));
		clientAuthenticationModeForProfile.setItems(ClientAuthnMode.values());
		clientAuthenticationModeForProfile.setValue(ClientAuthnMode.secretBasic);
		binder.forField(clientAuthenticationModeForProfile).bind("clientAuthenticationModeForProfile");
		advanced.addComponent(clientAuthenticationModeForProfile);

		CheckBox accountAssociation = new CheckBox(
				msg.getMessage("EditOAuthProviderSubView.accountAssociation"));
		binder.forField(accountAssociation).bind("accountAssociation");
		advanced.addComponent(accountAssociation);

		TextField extraAuthorizationParameters = new TextField(
				msg.getMessage("EditOAuthProviderSubView.extraAuthorizationParameters"));
		extraAuthorizationParameters.setWidth(30, Unit.EM);
		binder.forField(extraAuthorizationParameters).bind("extraAuthorizationParameters");
		advanced.addComponent(extraAuthorizationParameters);

		ComboBox<ServerHostnameCheckingMode> clientHostnameChecking = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.clientHostnameChecking"));
		clientHostnameChecking.setItems(ServerHostnameCheckingMode.values());
		clientHostnameChecking.setValue(ServerHostnameCheckingMode.FAIL);
		binder.forField(clientHostnameChecking).bind("clientHostnameChecking");
		advanced.addComponent(clientHostnameChecking);

		ComboBox<String> clientTrustStore = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.clientTrustStore"));
		clientTrustStore.setItems(validators);

		binder.forField(clientTrustStore).bind("clientTrustStore");
		advanced.addComponent(clientTrustStore);

		ComboBox<ClientHttpMethod> clientHttpMethodForProfileAccess = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.clientHttpMethodForProfileAccess"));
		clientHttpMethodForProfileAccess.setItems(ClientHttpMethod.values());
		clientHttpMethodForProfileAccess.setValue(ClientHttpMethod.get);
		binder.forField(clientHttpMethodForProfileAccess).bind("clientHttpMethodForProfileAccess");
		advanced.addComponent(clientHttpMethodForProfileAccess);

		return new CollapsibleLayout(msg.getMessage("EditOAuthProviderSubView.advanced"), advanced);
	}

	@Override
	public List<String> getBredcrumbs()
	{
		return Arrays.asList(msg.getMessage("EditOAuthProviderSubView.breadcrumbs"),
				editMode ? binder.getBean().getId() : msg.getMessage("new"));
	}

	public static class OAuthProviderConfiguration
	{
		private String type;
		private String id;
		private I18nString name;
		private String clientId;
		private String clientSecret;
		private ClientAuthnMode clientAuthenticationMode;
		private ClientHttpMethod clientHttpMethodForProfileAccess;
		private ClientAuthnMode clientAuthenticationModeForProfile;
		private String requestedScopes;
		private I18nString iconUrl;
		private boolean openIdConnect;
		private String openIdDiscoverEndpoint;
		private String authenticationEndpoint;
		private String accessTokenEndpoint;
		private String profileEndpoint;
		private String registrationForm;
		private AccessTokenFormat accessTokenFormat;
		private boolean accountAssociation;
		private ServerHostnameCheckingMode clientHostnameChecking;
		private String clientTrustStore;
		private TranslationProfile translationProfile;
		private String extraAuthorizationParameters;

		public OAuthProviderConfiguration()
		{
			type = Providers.custom.toString();
			translationProfile = TranslationProfileGenerator.generateEmptyInputProfile();
		}

		public String getType()
		{
			return type;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		public boolean isOpenIdConnect()
		{
			return openIdConnect;
		}

		public void setOpenIdConnect(boolean openIdConnect)
		{
			this.openIdConnect = openIdConnect;
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

		public String getRequestedScopes()
		{
			return requestedScopes;
		}

		public void setRequestedScopes(String requestedScopes)
		{
			this.requestedScopes = requestedScopes;
		}

		public I18nString getIconUrl()
		{
			return iconUrl;
		}

		public void setIconUrl(I18nString iconUrl)
		{
			this.iconUrl = iconUrl;
		}

		public String getOpenIdDiscoverEndpoint()
		{
			return openIdDiscoverEndpoint;
		}

		public void setOpenIdDiscoverEndpoint(String openIdDiscoverEndpoint)
		{
			this.openIdDiscoverEndpoint = openIdDiscoverEndpoint;
		}

		public String getAuthenticationEndpoint()
		{
			return authenticationEndpoint;
		}

		public void setAuthenticationEndpoint(String authenticationEndpoint)
		{
			this.authenticationEndpoint = authenticationEndpoint;
		}

		public String getAccessTokenEndpoint()
		{
			return accessTokenEndpoint;
		}

		public void setAccessTokenEndpoint(String accessTokenEndpoint)
		{
			this.accessTokenEndpoint = accessTokenEndpoint;
		}

		public String getProfileEndpoint()
		{
			return profileEndpoint;
		}

		public void setProfileEndpoint(String profileEndpoint)
		{
			this.profileEndpoint = profileEndpoint;
		}

		public String getRegistrationForm()
		{
			return registrationForm;
		}

		public void setRegistrationForm(String registrationForm)
		{
			this.registrationForm = registrationForm;
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

		public boolean isAccountAssociation()
		{
			return accountAssociation;
		}

		public void setAccountAssociation(boolean accountAssociation)
		{
			this.accountAssociation = accountAssociation;
		}

		public String getExtraAuthorizationParameters()
		{
			return extraAuthorizationParameters;
		}

		public void setExtraAuthorizationParameters(String extraAuthorizationParameters)
		{
			this.extraAuthorizationParameters = extraAuthorizationParameters;
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

		public void setClientTrustStore(String httpClientTrustStore)
		{
			this.clientTrustStore = httpClientTrustStore;
		}

		public ClientHttpMethod getClientHttpMethodForProfileAccess()
		{
			return clientHttpMethodForProfileAccess;
		}

		public void setClientHttpMethodForProfileAccess(ClientHttpMethod clientHttpMethodForProfileAccess)
		{
			this.clientHttpMethodForProfileAccess = clientHttpMethodForProfileAccess;
		}

		public TranslationProfile getTranslationProfile()
		{
			return translationProfile;
		}

		public void setTranslationProfile(TranslationProfile translationProfile)
		{
			this.translationProfile = translationProfile;
		}

		public I18nString getName()
		{
			return name;
		}

		public void setName(I18nString name)
		{
			this.name = name;
		}

		public AccessTokenFormat getAccessTokenFormat()
		{
			return accessTokenFormat;
		}

		public void setAccessTokenFormat(AccessTokenFormat accessTokenFormat)
		{
			this.accessTokenFormat = accessTokenFormat;
		}

		public String getId()
		{
			return id;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public void fromTemplate(UnityMessageSource msg, CustomProviderProperties source, String idFromTemplate,
				String orgId)
		{
			String profile = source.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE);
			if (profile != null && !profile.isEmpty())
			{

				translationProfile = TranslationProfileGenerator.generateIncludeInputProfile(
						source.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE));

			}
			fromProperties(msg, source, orgId != null ? orgId : idFromTemplate);
		}

		public void fromProperties(UnityMessageSource msg, CustomProviderProperties source, String id)
		{
			this.id = id;
			type = source.getValue(CustomProviderProperties.PROVIDER_TYPE);
			name = source.getLocalizedString(msg, CustomProviderProperties.PROVIDER_NAME);
			authenticationEndpoint = source.getValue(CustomProviderProperties.PROVIDER_LOCATION);

			accessTokenEndpoint = source.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT);
			profileEndpoint = source.getValue(CustomProviderProperties.PROFILE_ENDPOINT);
			iconUrl = source.getLocalizedString(msg, CustomProviderProperties.ICON_URL);

			clientId = source.getValue(CustomProviderProperties.CLIENT_ID);
			clientSecret = source.getValue(CustomProviderProperties.CLIENT_SECRET);

			clientAuthenticationMode = source.getEnumValue(CustomProviderProperties.CLIENT_AUTHN_MODE,
					ClientAuthnMode.class);
			clientAuthenticationModeForProfile = source.getEnumValue(
					CustomProviderProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS,
					ClientAuthnMode.class);
			clientHttpMethodForProfileAccess = source.getEnumValue(
					CustomProviderProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
					ClientHttpMethod.class);
			requestedScopes = source.getValue(CustomProviderProperties.SCOPES);

			accessTokenFormat = source.getEnumValue(CustomProviderProperties.ACCESS_TOKEN_FORMAT,
					AccessTokenFormat.class);
			openIdConnect = source.getBooleanValue(CustomProviderProperties.OPENID_CONNECT);
			openIdDiscoverEndpoint = source.getValue(CustomProviderProperties.OPENID_DISCOVERY);

			registrationForm = source.getValue(CommonWebAuthnProperties.REGISTRATION_FORM);

			if (source.isSet(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE))
			{
				translationProfile = TranslationProfileGenerator.getProfileFromString(
						source.getValue(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE));

			} else
			{
				translationProfile = TranslationProfileGenerator.generateIncludeInputProfile(
						source.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE));
			}

			if (source.isSet(CommonWebAuthnProperties.ENABLE_ASSOCIATION))
			{
				accountAssociation = source
						.getBooleanValue(CommonWebAuthnProperties.ENABLE_ASSOCIATION);
			}

			clientHostnameChecking = source.getEnumValue(CustomProviderProperties.CLIENT_HOSTNAME_CHECKING,
					ServerHostnameCheckingMode.class);

			clientTrustStore = source.getValue(CustomProviderProperties.CLIENT_TRUSTSTORE);
			extraAuthorizationParameters = source
					.getValue(CustomProviderProperties.ADDITIONAL_AUTHZ_PARAMS);

		}

		public void toProperties(Properties raw, UnityMessageSource msg)
		{
			String prefix = OAuthClientProperties.P + OAuthClientProperties.PROVIDERS + id + ".";

			raw.put(prefix + CustomProviderProperties.PROVIDER_TYPE, type);

			name.toProperties(raw, prefix + CustomProviderProperties.PROVIDER_NAME);		
			raw.put(prefix + CustomProviderProperties.PROVIDER_NAME, name.getValue(msg));
			
			
			if (authenticationEndpoint != null)
			{
				raw.put(prefix + CustomProviderProperties.PROVIDER_LOCATION, authenticationEndpoint);
			}

			if (accessTokenEndpoint != null)
			{
				raw.put(prefix + CustomProviderProperties.ACCESS_TOKEN_ENDPOINT, accessTokenEndpoint);
			}

			if (profileEndpoint != null)
			{
				raw.put(prefix + CustomProviderProperties.PROFILE_ENDPOINT, profileEndpoint);
			}

			if (iconUrl != null)
			{
				iconUrl.toProperties(raw, prefix + CustomProviderProperties.ICON_URL);
				raw.put(prefix + CustomProviderProperties.ICON_URL, iconUrl.getValue(msg));
			}

			raw.put(prefix + CustomProviderProperties.CLIENT_ID, clientId);

			raw.put(prefix + CustomProviderProperties.CLIENT_SECRET, clientSecret);

			if (clientAuthenticationMode != null)
			{
				raw.put(prefix + CustomProviderProperties.CLIENT_AUTHN_MODE,
						clientAuthenticationMode.toString());
			}

			if (clientAuthenticationModeForProfile != null)
			{
				raw.put(prefix + CustomProviderProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS,
						clientAuthenticationModeForProfile.toString());
			}

			if (clientHttpMethodForProfileAccess != null)
			{
				raw.put(prefix + CustomProviderProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
						clientHttpMethodForProfileAccess.toString());
			}

			if (requestedScopes != null)
			{
				raw.put(prefix + CustomProviderProperties.SCOPES, requestedScopes);
			}

			if (accessTokenFormat != null)
			{
				raw.put(prefix + CustomProviderProperties.ACCESS_TOKEN_FORMAT,
						accessTokenFormat.toString());
			}

			raw.put(prefix + CustomProviderProperties.OPENID_CONNECT, String.valueOf(openIdConnect));

			if (openIdDiscoverEndpoint != null)
			{
				raw.put(prefix + CustomProviderProperties.OPENID_DISCOVERY, openIdDiscoverEndpoint);
			}

			if (registrationForm != null)
			{
				raw.put(prefix + CommonWebAuthnProperties.REGISTRATION_FORM, registrationForm);
			}

			raw.put(prefix + CommonWebAuthnProperties.ENABLE_ASSOCIATION,
					String.valueOf(accountAssociation));

			try
			{
				raw.put(prefix + CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE,
						Constants.MAPPER.writeValueAsString(translationProfile.toJsonObject()));
			} catch (Exception e)
			{
				throw new InternalException("Can't serialize provider's translation profile to JSON",
						e);
			}

			if (clientHostnameChecking != null)
			{
				raw.put(prefix + CustomProviderProperties.CLIENT_HOSTNAME_CHECKING,
						clientHostnameChecking.toString());
			}

			if (clientTrustStore != null)
			{
				raw.put(prefix + CustomProviderProperties.CLIENT_TRUSTSTORE, clientTrustStore);
			}

			if (extraAuthorizationParameters != null)
			{
				raw.put(prefix + CustomProviderProperties.ADDITIONAL_AUTHZ_PARAMS,
						extraAuthorizationParameters);
			}
		}
	}

}
