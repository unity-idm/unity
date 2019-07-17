/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.endpoints;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.endpoints.authnlayout.WebServiceAuthnScreenLayoutEditor;
import pl.edu.icm.unity.webui.authn.endpoints.authnlayout.WebServiceReturningLayoutEditor;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.ImageField;
import pl.edu.icm.unity.webui.common.file.ImageUtils;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

/**
 * 
 * @author P.Piernik
 *
 */
public class WebServiceEditor extends ServiceEditorBase
{
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfiguration;
	private AuthenticatorSupportService authenticatorSupportService;

	private Binder<ServiceWebConfiguration> binder;

	private List<String> allRegistrationForms;
	private VaadinEndpointProperties properties;

	private WebServiceAuthnScreenLayoutEditor webScreenEditor;
	private WebServiceReturningLayoutEditor webRetUserScreenEditor;
	private CollapsibleLayout lyoutForRetUserSection;

	public WebServiceEditor(UnityMessageSource msg, URIAccessService uriAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			EndpointTypeDescription type, ServiceDefinition toEdit, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> allRegistrationForms, AuthenticatorSupportService authenticatorSupportService)
	{
		super(msg, type, toEdit, allRealms, flows, authenticators);
		this.allRegistrationForms = allRegistrationForms;
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
		this.serverConfiguration = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;

		ServiceWebConfiguration config = new ServiceWebConfiguration(allRegistrationForms);
		properties = loadVaadinProperties(
				toEdit != null && toEdit.getConfiguration() != null ? toEdit.getConfiguration()
						: new VaadinEndpointProperties(new Properties()).getAsString());
		config.fromProperties(properties, msg, uriAccessService);

		if (config.enableRegistration && (config.registrationForms.isEmpty()))
		{
			config.setRegistrationForms(allRegistrationForms);
		}

		binder = new Binder<>(ServiceWebConfiguration.class);
		addToAuthenticationTab(true, buildGeneralAuthnControls());
		addToAuthenticationTab(false, buildRegistrationSection());
		addToAuthenticationTab(false, buildPresentationSection());
		addToAuthenticationTab(false, buildMainLayoutSection());
		addToAuthenticationTab(false, buildLayoutForReturningUserSection());
		binder.setBean(config);

	}

	private Component[] buildGeneralAuthnControls()
	{
		CheckBox showSearch = new CheckBox();
		showSearch.setCaption(msg.getMessage("WebServiceEditorBase.showSearch"));
		binder.forField(showSearch).bind("showSearch");

		CheckBox addAllAuthnOptions = new CheckBox();
		addAllAuthnOptions.setCaption(msg.getMessage("WebServiceEditorBase.addAllAuthnOptions"));
		binder.forField(addAllAuthnOptions).bind("addAllAuthnOptions");

		CheckBox showCancel = new CheckBox();
		showCancel.setCaption(msg.getMessage("WebServiceEditorBase.showCancel"));
		binder.forField(showCancel).bind("showCancel");

		CheckBox showLastUsedAuthnOption = new CheckBox();
		showLastUsedAuthnOption.setCaption(msg.getMessage("WebServiceEditorBase.showLastUsedAuthnOption"));
		showLastUsedAuthnOption.addValueChangeListener(e -> {
			lyoutForRetUserSection.setVisible(e.getValue());
		});

		binder.forField(showLastUsedAuthnOption).bind("showLastUsedAuthnOption");

		CheckBox autoLogin = new CheckBox();
		autoLogin.setCaption(msg.getMessage("WebServiceEditorBase.autoLogin"));
		binder.forField(autoLogin).bind("autoLogin");

		return new Component[] { showSearch, addAllAuthnOptions, showCancel, showLastUsedAuthnOption,
				autoLogin };
	}

	private Component buildRegistrationSection()
	{
		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		CheckBox enableRegistration = new CheckBox();
		enableRegistration.setCaption(msg.getMessage("WebServiceEditorBase.enableRegistration"));
		binder.forField(enableRegistration).bind("enableRegistration");
		main.addComponent(enableRegistration);

		CheckBox showRegistrationFormsInHeader = new CheckBox();
		showRegistrationFormsInHeader
				.setCaption(msg.getMessage("WebServiceEditorBase.showRegistrationFormsInHeader"));
		showRegistrationFormsInHeader.setEnabled(false);
		binder.forField(showRegistrationFormsInHeader).bind("showRegistrationFormsInHeader");
		main.addComponent(showRegistrationFormsInHeader);

		TextField externalRegistrationURL = new TextField();
		externalRegistrationURL.setEnabled(false);
		externalRegistrationURL.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH,
				FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		externalRegistrationURL.setCaption(msg.getMessage("WebServiceEditorBase.externalRegistrationURL"));
		binder.forField(externalRegistrationURL).bind("externalRegistrationURL");
		main.addComponent(externalRegistrationURL);

		ChipsWithDropdown<String> regFormsCombo = new ChipsWithDropdown<>();
		regFormsCombo.setEnabled(false);
		regFormsCombo.setCaption(msg.getMessage("WebServiceEditorBase.registrationForms"));
		regFormsCombo.setItems(allRegistrationForms);
		binder.forField(regFormsCombo).bind("registrationForms");
		main.addComponent(regFormsCombo);

		enableRegistration.addValueChangeListener(e -> {
			boolean v = e.getValue();
			showRegistrationFormsInHeader.setEnabled(v);
			externalRegistrationURL.setEnabled(v);
			regFormsCombo.setEnabled(v);
		});

		CollapsibleLayout regSection = new CollapsibleLayout(
				msg.getMessage("WebServiceEditorBase.usersRegistration"), main);
		regSection.expand();
		return regSection;
	}

	private Component buildPresentationSection()
	{
		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		ImageField logo = new ImageField(msg, uriAccessService, serverConfiguration.getFileSizeLimit());
		logo.setCaption(msg.getMessage("WebServiceEditorBase.logo"));
		logo.configureBinding(binder, "logo");
		main.addComponent(logo);

		I18nTextField title = new I18nTextField(msg);
		title.setCaption(msg.getMessage("WebServiceEditorBase.title"));
		binder.forField(title).bind("title");
		main.addComponent(title);

		CollapsibleLayout presentationSection = new CollapsibleLayout(
				msg.getMessage("WebServiceEditorBase.presentation"), main);
		presentationSection.expand();
		return presentationSection;
	}

	private Component buildMainLayoutSection()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);

		webScreenEditor = new WebServiceAuthnScreenLayoutEditor(properties, msg, authenticatorSupportService,
				() -> authAndFlows.getSelectedItems());
		authAndFlows.addValueChangeListener(e -> webScreenEditor.refreshColumnsElements());

		main.addComponent(webScreenEditor);
		CollapsibleLayout mainLayoutSection = new CollapsibleLayout(
				msg.getMessage("WebServiceEditorBase.mainLayout"), main);
		mainLayoutSection.expand();
		return mainLayoutSection;
	}

	private Component buildLayoutForReturningUserSection()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);

		webRetUserScreenEditor = new WebServiceReturningLayoutEditor(properties, msg);
		main.addComponent(webRetUserScreenEditor);

		lyoutForRetUserSection = new CollapsibleLayout(
				msg.getMessage("WebServiceEditorBase.layoutForReturningUser"), main);
		lyoutForRetUserSection.expand();
		lyoutForRetUserSection.setVisible(false);
		return lyoutForRetUserSection;
	}

	@Override
	protected String getConfiguration(String serviceName) throws FormValidationException
	{
		validateConfiguration();
		Properties raw = binder.getBean().toProperties(msg, fileStorageService, serviceName);

		raw.putAll(webScreenEditor.getConfiguration());
		raw.putAll(webRetUserScreenEditor.getConfiguration());
		VaadinEndpointProperties vaadinEndpointProperties = new VaadinEndpointProperties(raw);
		return vaadinEndpointProperties.getAsString();
	}

	@Override
	protected void validateConfiguration() throws FormValidationException
	{
		if (binder.validate().hasErrors())
		{
			throw new FormValidationException();
		}
		webScreenEditor.validateConfiguration();
	}

	private VaadinEndpointProperties loadVaadinProperties(String properties)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the home ui service", e);
		}

		VaadinEndpointProperties vaadinProperties = new VaadinEndpointProperties(raw);
		return vaadinProperties;
	}

	public static class ServiceWebConfiguration
	{
		private boolean showSearch;
		private boolean addAllAuthnOptions;
		private boolean showCancel;
		private boolean showLastUsedAuthnOption;
		private boolean autoLogin;

		private boolean enableRegistration;
		private boolean showRegistrationFormsInHeader;
		private String externalRegistrationURL;
		private List<String> registrationForms;

		private LocalOrRemoteResource logo;
		private I18nString title;

		public ServiceWebConfiguration(List<String> regForms)
		{
			this();
			registrationForms.addAll(regForms);
		}

		public ServiceWebConfiguration()
		{
			registrationForms = new ArrayList<>();
		}

		public Properties toProperties(UnityMessageSource msg, FileStorageService fileStorageService,
				String serviceName)
		{
			Properties raw = new Properties();

			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_SHOW_SEARCH,
					String.valueOf(showSearch));
			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_ADD_ALL,
					String.valueOf(addAllAuthnOptions));
			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_SHOW_CANCEL,
					String.valueOf(showCancel));
			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY,
					String.valueOf(showLastUsedAuthnOption));
			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTO_LOGIN,
					String.valueOf(autoLogin));

			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.ENABLE_REGISTRATION,
					String.valueOf(enableRegistration));

			raw.put(VaadinEndpointProperties.PREFIX
					+ VaadinEndpointProperties.SHOW_REGISTRATION_FORMS_IN_HEADER,
					String.valueOf(showRegistrationFormsInHeader));

			if (externalRegistrationURL != null)
			{
				raw.put(VaadinEndpointProperties.PREFIX
						+ VaadinEndpointProperties.EXTERNAL_REGISTRATION_URL,
						externalRegistrationURL);
			}
			if (registrationForms != null && !registrationForms.isEmpty())
			{
				registrationForms.forEach(c -> raw.put(VaadinEndpointProperties.PREFIX
						+ VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS
						+ (registrationForms.indexOf(c) + 1), c));
			}

			if (title != null)
			{
				title.toProperties(raw,
						VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_TITLE,
						msg);
			}

			if (logo != null)
			{
				FileFieldUtils.saveInProperties(getLogo(),
						VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_LOGO,
						raw, fileStorageService, StandardOwner.SERVICE.toString(), serviceName);
			}

			return raw;
		}

		public void fromProperties(VaadinEndpointProperties vaadinProperties, UnityMessageSource msg,
				URIAccessService uriAccessService)
		{

			showSearch = vaadinProperties.getBooleanValue(VaadinEndpointProperties.AUTHN_SHOW_SEARCH);
			addAllAuthnOptions = vaadinProperties.getBooleanValue(VaadinEndpointProperties.AUTHN_ADD_ALL);
			showCancel = vaadinProperties.getBooleanValue(VaadinEndpointProperties.AUTHN_SHOW_CANCEL);
			showLastUsedAuthnOption = vaadinProperties
					.getBooleanValue(VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY);
			autoLogin = vaadinProperties.getBooleanValue(VaadinEndpointProperties.AUTO_LOGIN);

			enableRegistration = vaadinProperties
					.getBooleanValue(VaadinEndpointProperties.ENABLE_REGISTRATION);

			showRegistrationFormsInHeader = vaadinProperties
					.getBooleanValue(VaadinEndpointProperties.SHOW_REGISTRATION_FORMS_IN_HEADER);

			externalRegistrationURL = vaadinProperties
					.getValue(VaadinEndpointProperties.EXTERNAL_REGISTRATION_URL);
			registrationForms = vaadinProperties
					.getListOfValues(VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS);

			String logoUri = vaadinProperties.getValue(VaadinEndpointProperties.AUTHN_LOGO);
			logo = ImageUtils.getImageFromUriSave(logoUri, uriAccessService);

			title = vaadinProperties.getLocalizedStringWithoutFallbackToDefault(msg,
					VaadinEndpointProperties.AUTHN_TITLE);

		}

		public boolean isShowSearch()
		{
			return showSearch;
		}

		public void setShowSearch(boolean showSearch)
		{
			this.showSearch = showSearch;
		}

		public boolean isAddAllAuthnOptions()
		{
			return addAllAuthnOptions;
		}

		public void setAddAllAuthnOptions(boolean addAllAuthnOptions)
		{
			this.addAllAuthnOptions = addAllAuthnOptions;
		}

		public boolean isShowCancel()
		{
			return showCancel;
		}

		public void setShowCancel(boolean showCancel)
		{
			this.showCancel = showCancel;
		}

		public boolean isShowLastUsedAuthnOption()
		{
			return showLastUsedAuthnOption;
		}

		public void setShowLastUsedAuthnOption(boolean showLastUsedAuthnOption)
		{
			this.showLastUsedAuthnOption = showLastUsedAuthnOption;
		}

		public boolean isAutoLogin()
		{
			return autoLogin;
		}

		public void setAutoLogin(boolean autoLogin)
		{
			this.autoLogin = autoLogin;
		}

		public boolean isEnableRegistration()
		{
			return enableRegistration;
		}

		public void setEnableRegistration(boolean enableRegistration)
		{
			this.enableRegistration = enableRegistration;
		}

		public boolean isShowRegistrationFormsInHeader()
		{
			return showRegistrationFormsInHeader;
		}

		public void setShowRegistrationFormsInHeader(boolean showRegistrationFormsInHeader)
		{
			this.showRegistrationFormsInHeader = showRegistrationFormsInHeader;
		}

		public String getExternalRegistrationURL()
		{
			return externalRegistrationURL;
		}

		public void setExternalRegistrationURL(String externalRegistrationURL)
		{
			this.externalRegistrationURL = externalRegistrationURL;
		}

		public List<String> getRegistrationForms()
		{
			return registrationForms;
		}

		public void setRegistrationForms(List<String> registrationForms)
		{
			this.registrationForms = registrationForms;
		}

		public LocalOrRemoteResource getLogo()
		{
			return logo;
		}

		public void setLogo(LocalOrRemoteResource logo)
		{
			this.logo = logo;
		}

		public I18nString getTitle()
		{
			return title;
		}

		public void setTitle(I18nString title)
		{
			this.title = title;
		}

	}

}
