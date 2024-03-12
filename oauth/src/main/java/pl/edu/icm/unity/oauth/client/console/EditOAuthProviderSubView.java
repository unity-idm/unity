/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;
import io.imunity.vaadin.auth.binding.NameValuePairBinding;
import io.imunity.vaadin.auth.binding.ToggleWithDefault;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.NoSpaceValidator;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.EditableGrid;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;
import io.imunity.vaadin.endpoint.common.file.FileField;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.oauth.client.config.*;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientHttpMethod;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties.Providers;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.*;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.EDIT_VIEW_ACTION_BUTTONS_LAYOUT;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class EditOAuthProviderSubView extends VerticalLayout implements UnitySubView
{
	private static final String TMP_CONFIG_KEY = "tmp.";

	private final MessageSource msg;
	private final PKIManagement pkiMan;
	private final UnityServerConfiguration serverConfig;
	private Map<String, CustomProviderProperties> templates;

	private final Binder<OAuthProviderConfiguration> configBinder;
	private Select<String> templateCombo;

	private boolean editMode;

	EditOAuthProviderSubView(MessageSource msg, PKIManagement pkiMan, NotificationPresenter notificationPresenter,
			VaadinLogoImageLoader imageAccessService,
			InputTranslationProfileFieldFactory profileFieldFactory,
			OAuthProviderConfiguration toEdit, Set<String> providersIds, SubViewSwitcher subViewSwitcher,
			Set<String> registrationForms, Set<String> validators,
			UnityServerConfiguration serverConfig,
			Consumer<OAuthProviderConfiguration> onConfirm, Runnable onCancel)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.serverConfig = serverConfig;
		editMode = toEdit != null;

		configBinder = new Binder<>(OAuthProviderConfiguration.class);

		FormLayout header = buildHeaderSection(providersIds);
		AccordionPanel remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher,
				configBinder, "translationProfile");
		remoteDataMapping.setWidthFull();
		AccordionPanel advanced = buildAdvancedSection(validators, registrationForms);
		advanced.setWidthFull();

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
		mainView.add(header);
		mainView.add(remoteDataMapping, advanced);

		Button cancelButton = new Button(msg.getMessage("cancel"), event -> onCancel.run());
		cancelButton.setWidthFull();
		Button updateButton = new Button(editMode ? msg.getMessage("update") :  msg.getMessage("create"), event ->
		{
			try
			{
				onConfirm.accept(getProvider());
			} catch (FormValidationException e)
			{
				notificationPresenter.showError(
						msg.getMessage("EditOAuthProviderSubView.invalidConfiguration"), e.getMessage());
			}
		});
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, updateButton);
		buttonsLayout.setClassName(EDIT_VIEW_ACTION_BUTTONS_LAYOUT.getName());
		mainView.add(buttonsLayout);

		add(mainView);
	}

	private OAuthProviderConfiguration getProvider() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		return configBinder.getBean();
	}

	private FormLayout buildHeaderSection(Set<String> providersIds)
	{
		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		loadTemplates();

		templateCombo = new Select<>();
		templateCombo.setItems(templates.keySet().stream().sorted().toList());
		configBinder.forField(templateCombo).asRequired(msg.getMessage("fieldRequired"))
				.bind(OAuthProviderConfiguration::getType, OAuthProviderConfiguration::setType);
		if (!editMode)
			header.addFormItem(templateCombo, msg.getMessage("EditOAuthProviderSubView.template"));


		TextField id = new TextField();
		configBinder.forField(id).asRequired(msg.getMessage("fieldRequired")).withValidator((s, c) -> {
			if (providersIds.contains(s))
				return ValidationResult.error(msg.getMessage("EditOAuthProviderSubView.idExists"));
			else
				return ValidationResult.ok();
		}).withValidator(new NoSpaceValidator(msg::getMessage))
				.bind(OAuthProviderConfiguration::getId, OAuthProviderConfiguration::setId);
		id.setReadOnly(editMode);
		header.addFormItem(id, msg.getMessage("EditOAuthProviderSubView.id"));

		LocalizedTextFieldDetails name = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		configBinder.forField(name)
				.asRequired(msg.getMessage("fieldRequired"))
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.withValidator(item -> !item.getMap().isEmpty(), msg.getMessage("fieldRequired"))
				.bind(OAuthProviderConfiguration::getName, OAuthProviderConfiguration::setName);
		header.addFormItem(name, msg.getMessage("EditOAuthProviderSubView.name"));

		TextField clientId = new TextField();
		clientId.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(clientId).asRequired(msg.getMessage("fieldRequired"))
				.bind(OAuthProviderConfiguration::getClientId, OAuthProviderConfiguration::setClientId);
		header.addFormItem(clientId, msg.getMessage("EditOAuthProviderSubView.clientId"));

		TextField clientSecret = new TextField();
		clientSecret.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(clientSecret).asRequired(msg.getMessage("fieldRequired"))
				.bind(OAuthProviderConfiguration::getClientSecret, OAuthProviderConfiguration::setClientSecret);
		header.addFormItem(clientSecret, msg.getMessage("EditOAuthProviderSubView.clientSecret"));

		MultiSelectComboBox<String> requestedScopes = new CustomValuesMultiSelectComboBox();
		requestedScopes.setWidth(TEXT_FIELD_BIG.value());
		requestedScopes.setPlaceholder(msg.getMessage("typeAndConfirm"));
		header.addFormItem(requestedScopes, msg.getMessage("EditOAuthProviderSubView.requestedScopes"));
		configBinder.forField(requestedScopes)
				.withConverter(List::copyOf, HashSet::new)
				.bind(OAuthProviderConfiguration::getRequestedScopes, OAuthProviderConfiguration::setRequestedScopes);
			
		FileField logo = new FileField(msg, "image/*", "logo.jpg", serverConfig.getFileSizeLimit());
		configBinder.forField(logo)
				.bind(OAuthProviderConfiguration::getLogo, OAuthProviderConfiguration::setLogo);
		header.addFormItem(logo, msg.getMessage("EditOAuthProviderSubView.logo"));
		
		Checkbox openIdConnect = new Checkbox(msg.getMessage("EditOAuthProviderSubView.openIdConnect"));
		configBinder.forField(openIdConnect)
				.bind(OAuthProviderConfiguration::isOpenIdConnect, OAuthProviderConfiguration::setOpenIdConnect);
		header.addFormItem(openIdConnect, "");

		TextField openIdDiscovery = new TextField();
		openIdDiscovery.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(openIdDiscovery).asRequired(getOpenIdFieldValidator(openIdConnect, true))
				.bind(OAuthProviderConfiguration::getOpenIdDiscoverEndpoint, OAuthProviderConfiguration::setOpenIdDiscoverEndpoint);
		openIdDiscovery.setRequiredIndicatorVisible(false);
		header.addFormItem(openIdDiscovery, msg.getMessage("EditOAuthProviderSubView.openIdDiscoverEndpoint"))
				.setVisible(false);

		TextField authenticationEndpoint = new TextField();
		authenticationEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(authenticationEndpoint).asRequired(getOpenIdFieldValidator(openIdConnect, false))
				.bind(OAuthProviderConfiguration::getAuthenticationEndpoint, OAuthProviderConfiguration::setAuthenticationEndpoint);
		header.addFormItem(authenticationEndpoint, msg.getMessage("EditOAuthProviderSubView.authenticationEndpoint"));

		TextField accessTokenEndpoint = new TextField();
		accessTokenEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(accessTokenEndpoint).asRequired(getOpenIdFieldValidator(openIdConnect, false))
				.bind(OAuthProviderConfiguration::getAccessTokenEndpoint, OAuthProviderConfiguration::setAccessTokenEndpoint);
		header.addFormItem(accessTokenEndpoint, msg.getMessage("EditOAuthProviderSubView.accessTokenEndpoint"));

		openIdConnect.addValueChangeListener(e ->
		{
			authenticationEndpoint.setRequiredIndicatorVisible(!e.getValue());
			accessTokenEndpoint.setRequiredIndicatorVisible(!e.getValue());
			openIdDiscovery.setRequiredIndicatorVisible(e.getValue());
			openIdDiscovery.getParent().get().setVisible(e.getValue());
		});

		TextField profileEndpoint = new TextField();
		profileEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(profileEndpoint)
				.bind(OAuthProviderConfiguration::getProfileEndpoint, OAuthProviderConfiguration::setProfileEndpoint);
		header.addFormItem(profileEndpoint, msg.getMessage("EditOAuthProviderSubView.profileEndpoint"));

		return header;
	}

	private AccordionPanel buildAdvancedSection(Set<String> validators, Set<String> registrationForms)
	{
		FormLayout advanced = new FormLayout();
		advanced.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		advanced.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		Select<String> registrationForm = new Select<>();
		registrationForm.setWidth(TEXT_FIELD_MEDIUM.value());
		registrationForm.setItems(registrationForms);
		registrationForm.setEmptySelectionAllowed(true);
		configBinder.forField(registrationForm)
				.bind(OAuthProviderConfiguration::getRegistrationForm, OAuthProviderConfiguration::setRegistrationForm);
		advanced.addFormItem(registrationForm, msg.getMessage("EditOAuthProviderSubView.registrationForm"));

		Select<AccessTokenFormat> accessTokenFormat = new Select<>();
		accessTokenFormat.setItems(AccessTokenFormat.values());
		configBinder.forField(accessTokenFormat)
				.bind(OAuthProviderConfiguration::getAccessTokenFormat, OAuthProviderConfiguration::setAccessTokenFormat);
		advanced.addFormItem(accessTokenFormat, msg.getMessage("EditOAuthProviderSubView.accessTokenFormat"));

		Select<ClientAuthnMode> clientAuthenticationMode = new Select<>();
		clientAuthenticationMode.setItems(ClientAuthnMode.values());
		configBinder.forField(clientAuthenticationMode)
				.bind(OAuthProviderConfiguration::getClientAuthenticationMode, OAuthProviderConfiguration::setClientAuthenticationMode);
		advanced.addFormItem(clientAuthenticationMode, msg.getMessage("EditOAuthProviderSubView.clientAuthenticationMode"));

		Select<ClientAuthnMode> clientAuthenticationModeForProfile = new Select<>();
		clientAuthenticationModeForProfile.setItems(ClientAuthnMode.values());
		configBinder.forField(clientAuthenticationModeForProfile)
				.bind(OAuthProviderConfiguration::getClientAuthenticationModeForProfile, OAuthProviderConfiguration::setClientAuthenticationModeForProfile);
		advanced.addFormItem(clientAuthenticationModeForProfile, msg.getMessage("EditOAuthProviderSubView.clientAuthenticationModeForProfile"));

		Select<ToggleWithDefault> accountAssociation = new Select<>();
		accountAssociation.setItemLabelGenerator(item -> msg.getMessage("EnableDisableCombo." + item));
		accountAssociation.setItems(ToggleWithDefault.values());
		accountAssociation.setValue(ToggleWithDefault.bydefault);
		configBinder.forField(accountAssociation)
				.bind(OAuthProviderConfiguration::getAccountAssociation, OAuthProviderConfiguration::setAccountAssociation);
		advanced.addFormItem(accountAssociation, msg.getMessage("EditOAuthProviderSubView.accountAssociation"));
		
		EditableGrid<NameValuePairBinding> extraAuthorizationParameters = new EditableGrid<>(msg::getMessage, NameValuePairBinding::new);
		extraAuthorizationParameters.setWidth(TEXT_FIELD_BIG.value());
		extraAuthorizationParameters.setHeight("20em");
		advanced.addFormItem(extraAuthorizationParameters, msg.getMessage("EditOAuthProviderSubView.extraAuthorizationParameters"));
		extraAuthorizationParameters.addColumn(NameValuePairBinding::getName, NameValuePairBinding::setName, true)
				.setHeader(msg.getMessage("EditOAuthProviderSubView.extraAuthorizationParameter.name"))
				.setAutoWidth(true);
		extraAuthorizationParameters.addColumn(NameValuePairBinding::getValue, NameValuePairBinding::setValue, true)
				.setHeader(msg.getMessage("EditOAuthProviderSubView.extraAuthorizationParameter.value"))
				.setAutoWidth(true);

		configBinder.forField(extraAuthorizationParameters)
				.bind(OAuthProviderConfiguration::getExtraAuthorizationParameters, OAuthProviderConfiguration::setExtraAuthorizationParameters);
		
		Select<ServerHostnameCheckingMode> clientHostnameChecking = new Select<>();
		clientHostnameChecking.setItems(ServerHostnameCheckingMode.values());
		clientHostnameChecking.setEmptySelectionAllowed(false);
		configBinder.forField(clientHostnameChecking)
				.bind(OAuthProviderConfiguration::getClientHostnameChecking, OAuthProviderConfiguration::setClientHostnameChecking);
		advanced.addFormItem(clientHostnameChecking, msg.getMessage("EditOAuthProviderSubView.clientHostnameChecking"));

		Select<String> clientTrustStore = new Select<>();
		clientTrustStore.setItems(validators);
		clientTrustStore.setWidth(TEXT_FIELD_MEDIUM.value());
		clientTrustStore.setEmptySelectionAllowed(true);
		clientTrustStore.setEmptySelectionCaption(msg.getMessage("TrustStore.default"));
		configBinder.forField(clientTrustStore)
				.bind(OAuthProviderConfiguration::getClientTrustStore, OAuthProviderConfiguration::setClientTrustStore);
		advanced.addFormItem(clientTrustStore, msg.getMessage("EditOAuthProviderSubView.clientTrustStore"));

		Select<ClientHttpMethod> clientHttpMethodForProfileAccess = new Select<>();
		clientHttpMethodForProfileAccess.setItems(ClientHttpMethod.values());
		configBinder.forField(clientHttpMethodForProfileAccess)
				.bind(OAuthProviderConfiguration::getClientHttpMethodForProfileAccess, OAuthProviderConfiguration::setClientHttpMethodForProfileAccess);
		advanced.addFormItem(clientHttpMethodForProfileAccess, msg.getMessage("EditOAuthProviderSubView.clientHttpMethodForProfileAccess"));

		return new AccordionPanel(msg.getMessage("EditOAuthProviderSubView.advanced"), advanced);
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

	private Validator<String> getOpenIdFieldValidator(Checkbox openIdConnect, boolean toCheck)
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
	public List<String> getBreadcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditOAuthProviderSubView.provider"),
					configBinder.getBean().getId());
		else
			return Arrays.asList(msg.getMessage("EditOAuthProviderSubView.newProvider"));
	}
}
