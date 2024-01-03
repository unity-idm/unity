/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console.v8;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.ui.*;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.oauth.client.config.*;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientHttpMethod;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties.Providers;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.common.*;
import pl.edu.icm.unity.webui.common.binding.NameValuePairBinding;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.file.ImageField;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.validators.NoSpaceValidator;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

import java.util.*;
import java.util.function.Consumer;

/**
 * SubView for editing oauth authenticator provider
 * 
 * @author P.Piernik
 *
 */
class EditOAuthProviderSubView extends CustomComponent implements UnitySubView
{
	private static final String TMP_CONFIG_KEY = "tmp.";

	private MessageSource msg;
	private PKIManagement pkiMan;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;
	private Map<String, CustomProviderProperties> templates;

	private Binder<OAuthProviderConfiguration> configBinder;
	private ComboBox<String> templateCombo;

	private boolean editMode = false;

	EditOAuthProviderSubView(MessageSource msg, UnityServerConfiguration serverConfig, PKIManagement pkiMan, 
			URIAccessService uriAccessService, ImageAccessService imageAccessService,
			InputTranslationProfileFieldFactory profileFieldFactory,
			OAuthProviderConfiguration toEdit, Set<String> providersIds, SubViewSwitcher subViewSwitcher,
			Set<String> registrationForms, Set<String> validators,
			Consumer<OAuthProviderConfiguration> onConfirm, Runnable onCancel)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		
		editMode = toEdit != null;

		configBinder = new Binder<>(OAuthProviderConfiguration.class);

		FormLayout header = buildHeaderSection(providersIds);
		CollapsibleLayout remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher,
				configBinder, "translationProfile");
		CollapsibleLayout advanced = buildAdvancedSection(validators, registrationForms);

		configBinder.setBean(editMode ? toEdit.clone() : new OAuthProviderConfiguration());

		templateCombo.addValueChangeListener(e -> {
			OAuthProviderConfiguration config = new OAuthProviderConfiguration();
			config.fromTemplate(msg, imageAccessService, templates.get(e.getValue()), e.getValue(),
					editMode ? configBinder.getBean().getId() : null);
			config.setType(e.getValue());
			configBinder.setBean(config);
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
				NotificationPopup.showError(msg,
						msg.getMessage("EditOAuthProviderSubView.invalidConfiguration"), e);
			}
		};

		mainView.addComponent(editMode
				? StandardButtonsHelper.buildConfirmEditButtonsBar(msg, onConfirmR, onCancel)
				: StandardButtonsHelper.buildConfirmNewButtonsBar(msg, onConfirmR, onCancel));

		setCompositionRoot(mainView);
	}

	private OAuthProviderConfiguration getProvider() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		return configBinder.getBean();
	}

	private FormLayoutWithFixedCaptionWidth buildHeaderSection(Set<String> providersIds)
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);

		loadTemplates();

		templateCombo = new ComboBox<>();
		templateCombo.setCaption(msg.getMessage("EditOAuthProviderSubView.template"));
		templateCombo.setItems(templates.keySet().stream().sorted());
		templateCombo.setEmptySelectionAllowed(false);

		configBinder.forField(templateCombo).asRequired(msg.getMessage("fieldRequired")).bind("type");

		if (!editMode)
		{
			header.addComponent(templateCombo);
		}

		TextField id = new TextField(msg.getMessage("EditOAuthProviderSubView.id"));
		configBinder.forField(id).asRequired(msg.getMessage("fieldRequired")).withValidator((s, c) -> {
			if (providersIds.contains(s))
			{
				return ValidationResult.error(msg.getMessage("EditOAuthProviderSubView.idExists"));
			} else
			{
				return ValidationResult.ok();
			}

		}).withValidator(new NoSpaceValidator(msg)).bind("id");
		id.setReadOnly(editMode);
		header.addComponent(id);

		I18nTextField name = new I18nTextField(msg, msg.getMessage("EditOAuthProviderSubView.name"));
		configBinder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		header.addComponent(name);

		TextField clientId = new TextField(msg.getMessage("EditOAuthProviderSubView.clientId"));
		clientId.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(clientId).asRequired(msg.getMessage("fieldRequired")).bind("clientId");
		header.addComponent(clientId);

		TextField clientSecret = new TextField(msg.getMessage("EditOAuthProviderSubView.clientSecret"));
		clientSecret.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(clientSecret).asRequired(msg.getMessage("fieldRequired")).bind("clientSecret");
		header.addComponent(clientSecret);

		ChipsWithTextfield requestedScopes = new ChipsWithTextfield(msg);
		requestedScopes.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		requestedScopes.setCaption(msg.getMessage("EditOAuthProviderSubView.requestedScopes"));
		header.addComponent(requestedScopes);
		configBinder.forField(requestedScopes).bind("requestedScopes");
			
		ImageField logo = new ImageField(msg, uriAccessService, serverConfig.getFileSizeLimit());
		logo.setCaption(msg.getMessage("EditOAuthProviderSubView.logo"));
		logo.configureBinding(configBinder, "logo");
		header.addComponent(logo);
		
		CheckBox openIdConnect = new CheckBox(msg.getMessage("EditOAuthProviderSubView.openIdConnect"));
		configBinder.forField(openIdConnect).bind("openIdConnect");
		header.addComponent(openIdConnect);

		TextField openIdDiscovery = new TextField(
				msg.getMessage("EditOAuthProviderSubView.openIdDiscoverEndpoint"));
		openIdDiscovery.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(openIdDiscovery).asRequired(getOpenIdFieldValidator(openIdConnect, true))
				.bind("openIdDiscoverEndpoint");
		openIdDiscovery.setVisible(false);
		openIdDiscovery.setRequiredIndicatorVisible(false);
		header.addComponent(openIdDiscovery);

		openIdConnect.addValueChangeListener(e -> openIdDiscovery.setVisible(e.getValue()));

		TextField authenticationEndpoint = new TextField(
				msg.getMessage("EditOAuthProviderSubView.authenticationEndpoint"));
		configBinder.forField(authenticationEndpoint).asRequired(getOpenIdFieldValidator(openIdConnect, false))
				.bind("authenticationEndpoint");
		authenticationEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		authenticationEndpoint.setRequiredIndicatorVisible(false);
		header.addComponent(authenticationEndpoint);

		TextField accessTokenEndpoint = new TextField(
				msg.getMessage("EditOAuthProviderSubView.accessTokenEndpoint"));
		accessTokenEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(accessTokenEndpoint).asRequired(getOpenIdFieldValidator(openIdConnect, false))
				.bind("accessTokenEndpoint");
		accessTokenEndpoint.setRequiredIndicatorVisible(false);
		header.addComponent(accessTokenEndpoint);

		TextField profileEndpoint = new TextField(msg.getMessage("EditOAuthProviderSubView.profileEndpoint"));
		profileEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(profileEndpoint).bind("profileEndpoint");
		header.addComponent(profileEndpoint);

		return header;
	}

	private CollapsibleLayout buildAdvancedSection(Set<String> validators, Set<String> registrationForms)
	{
		FormLayoutWithFixedCaptionWidth advanced = new FormLayoutWithFixedCaptionWidth();
		advanced.setMargin(false);

		ComboBox<String> registrationForm = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.registrationForm"));
		registrationForm.setItems(registrationForms);
		configBinder.forField(registrationForm).bind("registrationForm");
		advanced.addComponent(registrationForm);

		ComboBox<AccessTokenFormat> accessTokenFormat = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.accessTokenFormat"));
		accessTokenFormat.setItems(AccessTokenFormat.values());
		accessTokenFormat.setEmptySelectionAllowed(false);
		configBinder.forField(accessTokenFormat).bind("accessTokenFormat");
		advanced.addComponent(accessTokenFormat);

		ComboBox<ClientAuthnMode> clientAuthenticationMode = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.clientAuthenticationMode"));
		clientAuthenticationMode.setItems(ClientAuthnMode.values());
		clientAuthenticationMode.setEmptySelectionAllowed(false);
		configBinder.forField(clientAuthenticationMode).bind("clientAuthenticationMode");
		advanced.addComponent(clientAuthenticationMode);

		ComboBox<ClientAuthnMode> clientAuthenticationModeForProfile = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.clientAuthenticationModeForProfile"));
		clientAuthenticationModeForProfile.setItems(ClientAuthnMode.values());
		clientAuthenticationModeForProfile.setEmptySelectionAllowed(false);
		configBinder.forField(clientAuthenticationModeForProfile).bind("clientAuthenticationModeForProfile");
		advanced.addComponent(clientAuthenticationModeForProfile);

		EnableDisableCombo accountAssociation = new EnableDisableCombo(
				msg.getMessage("EditOAuthProviderSubView.accountAssociation"), msg);
		configBinder.forField(accountAssociation).bind("accountAssociation");
		advanced.addComponent(accountAssociation);
		
		GridWithEditor<NameValuePairBinding> extraAuthorizationParameters = new GridWithEditor<>(msg, NameValuePairBinding.class);
		extraAuthorizationParameters.setCaption(msg.getMessage("EditOAuthProviderSubView.extraAuthorizationParameters"));
		advanced.addComponent(extraAuthorizationParameters);
		extraAuthorizationParameters.addTextColumn(s -> s.getName(), (t, v) -> t.setName(v),
				msg.getMessage("EditOAuthProviderSubView.extraAuthorizationParameter.name"), 30, true);

		extraAuthorizationParameters.addTextColumn(s -> s.getValue(), (t, v) -> t.setValue(v),
				msg.getMessage("EditOAuthProviderSubView.extraAuthorizationParameter.value"), 30, true);

		extraAuthorizationParameters.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(extraAuthorizationParameters).bind("extraAuthorizationParameters");
		
		ComboBox<ServerHostnameCheckingMode> clientHostnameChecking = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.clientHostnameChecking"));
		clientHostnameChecking.setItems(ServerHostnameCheckingMode.values());
		clientHostnameChecking.setEmptySelectionAllowed(false);
		configBinder.forField(clientHostnameChecking).bind("clientHostnameChecking");
		advanced.addComponent(clientHostnameChecking);

		ComboBox<String> clientTrustStore = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.clientTrustStore"));
		clientTrustStore.setItems(validators);
		clientTrustStore.setEmptySelectionCaption(msg.getMessage("TrustStore.default"));
		configBinder.forField(clientTrustStore).bind("clientTrustStore");
		advanced.addComponent(clientTrustStore);

		ComboBox<ClientHttpMethod> clientHttpMethodForProfileAccess = new ComboBox<>(
				msg.getMessage("EditOAuthProviderSubView.clientHttpMethodForProfileAccess"));
		clientHttpMethodForProfileAccess.setItems(ClientHttpMethod.values());
		clientHttpMethodForProfileAccess.setEmptySelectionAllowed(false);
		configBinder.forField(clientHttpMethodForProfileAccess).bind("clientHttpMethodForProfileAccess");
		advanced.addComponent(clientHttpMethodForProfileAccess);

		return new CollapsibleLayout(msg.getMessage("EditOAuthProviderSubView.advanced"), advanced);
	}

	private Properties addEmptyProviderConfig(Properties raw, String key)
	{
		raw.put(OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY + key, "");
		return raw;
	}

	private Properties getTemplateConfigProperties()
	{
		Properties p = new Properties();
		addEmptyProviderConfig(p, CustomProviderProperties.CLIENT_ID);
		addEmptyProviderConfig(p, CustomProviderProperties.CLIENT_SECRET);
		return p;
	}

	private void loadTemplates()
	{
		templates = new HashMap<>();

		templates.put(Providers.dropbox.toString(), new DropboxProviderProperties(getTemplateConfigProperties(),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.github.toString(), new GitHubProviderProperties(getTemplateConfigProperties(),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.google.toString(), new GoogleProviderProperties(getTemplateConfigProperties(),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.facebook.toString(),
				new FacebookProviderProperties(getTemplateConfigProperties(),
						OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.intuit.toString(), new IntuitProviderProperties(getTemplateConfigProperties(),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.linkedin.toString(),
				new LinkedInProviderProperties(getTemplateConfigProperties(),
						OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.microsoft.toString(),
				new MicrosoftLiveProviderProperties(getTemplateConfigProperties(),
						OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.microsoftAzureV2.toString(),
				new MicrosoftAzureV2ProviderProperties(getTemplateConfigProperties(),
						OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.orcid.toString(),
				new OrcidProviderProperties(
						addEmptyProviderConfig(getTemplateConfigProperties(),
								CommonWebAuthnProperties.TRANSLATION_PROFILE),
						OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.unity.toString(),
				new UnityProviderProperties(
						addEmptyProviderConfig(getTemplateConfigProperties(),
								CustomProviderProperties.OPENID_DISCOVERY),
						OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
		templates.put(Providers.custom.toString(), new CustomProviderProperties(
				addEmptyProviderConfig(addEmptyProviderConfig(addEmptyProviderConfig(
						addEmptyProviderConfig(getTemplateConfigProperties(),
								CustomProviderProperties.PROVIDER_LOCATION),
						CommonWebAuthnProperties.TRANSLATION_PROFILE),
						CustomProviderProperties.ACCESS_TOKEN_ENDPOINT),
						CustomProviderProperties.PROVIDER_NAME),
				OAuthClientProperties.PROVIDERS + TMP_CONFIG_KEY, pkiMan));
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

	@Override
	public List<String> getBredcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditOAuthProviderSubView.provider"),
					configBinder.getBean().getId());
		else
			return Arrays.asList(msg.getMessage("EditOAuthProviderSubView.newProvider"));
	}
}
