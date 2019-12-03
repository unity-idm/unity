/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.ThemeConstans;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.ImageUtils;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutColumnConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutPropertiesParser;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.ExpandConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.LastUsedConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SeparatorConfig;

/**
 * 
 * @author P.Piernik
 *
 */
public class ServiceWebConfiguration
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
	private String defaultMainTheme;
	private String defaultAuthnTheme;
	private String webContentDir;
	private boolean productionMode;
	private String template;
	private boolean compactCredentialReset;

	private AuthnLayoutConfiguration authenticationLayoutConfiguration;
	private List<AuthnElementConfiguration> retUserLayoutConfiguration;

	public ServiceWebConfiguration(List<String> regForms)
	{
		this();
		registrationForms.addAll(regForms);
	}

	public ServiceWebConfiguration()
	{
		registrationForms = new ArrayList<>();

		authenticationLayoutConfiguration = new AuthnLayoutConfiguration(
				Arrays.asList(new AuthnLayoutColumnConfiguration(new I18nString(),
						new Float(VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH).intValue(),
						Lists.newArrayList())),
				Lists.newArrayList());

		retUserLayoutConfiguration = Arrays.asList(new LastUsedConfig(),
				new SeparatorConfig(new I18nString()),
				new ExpandConfig());

		defaultMainTheme = ThemeConstans.sidebarTheme;
		defaultAuthnTheme = ThemeConstans.unityTheme;
		productionMode = true;
		template = VaadinEndpointProperties.DEFAULT_TEMPLATE;
		compactCredentialReset = true;
	}

	public ServiceWebConfiguration(String defaultMainTheme)
	{
		this();
		this.defaultMainTheme = defaultMainTheme;
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

		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.SHOW_REGISTRATION_FORMS_IN_HEADER,
				String.valueOf(showRegistrationFormsInHeader));

		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.PRODUCTION_MODE,
				String.valueOf(productionMode));
		if (webContentDir != null)
		{
			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.WEB_CONTENT_PATH,
					webContentDir);
		}

		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.TEMPLATE, template);

		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.CRED_RESET_COMPACT,
				String.valueOf(compactCredentialReset));

		if (externalRegistrationURL != null)
		{
			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.EXTERNAL_REGISTRATION_URL,
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
			title.toProperties(raw, VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_TITLE,
					msg);
		}

		if (logo != null)
		{
			FileFieldUtils.saveInProperties(getLogo(),
					VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_LOGO, raw,
					fileStorageService, StandardOwner.SERVICE.toString(), serviceName);
		} else
		{
			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_LOGO, "");
		}

		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.THEME, defaultMainTheme);
		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_THEME, defaultAuthnTheme);

		raw.putAll(AuthnLayoutPropertiesParser.toProperties(msg, authenticationLayoutConfiguration));
		raw.putAll(AuthnLayoutPropertiesParser.returningUserColumnElementToProperties(msg,
				retUserLayoutConfiguration));

		return raw;
	}

	public void fromProperties(String vaadinProperties, UnityMessageSource msg, URIAccessService uriAccessService)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(vaadinProperties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the home ui service", e);
		}

		VaadinEndpointProperties vProperties = new VaadinEndpointProperties(raw);
		fromProperties(vProperties, msg, uriAccessService);
	}

	public void fromProperties(VaadinEndpointProperties vaadinProperties, UnityMessageSource msg,
			URIAccessService uriAccessService)
	{

		if (vaadinProperties.isSet(VaadinEndpointProperties.WEB_CONTENT_PATH))
		{
			webContentDir = vaadinProperties.getValue(VaadinEndpointProperties.WEB_CONTENT_PATH);
		}
		productionMode = vaadinProperties.getBooleanValue(VaadinEndpointProperties.PRODUCTION_MODE);
		template = vaadinProperties.getValue(VaadinEndpointProperties.TEMPLATE);
		compactCredentialReset = vaadinProperties.getBooleanValue(VaadinEndpointProperties.CRED_RESET_COMPACT);
		showSearch = vaadinProperties.getBooleanValue(VaadinEndpointProperties.AUTHN_SHOW_SEARCH);
		addAllAuthnOptions = vaadinProperties.getBooleanValue(VaadinEndpointProperties.AUTHN_ADD_ALL);
		showCancel = vaadinProperties.getBooleanValue(VaadinEndpointProperties.AUTHN_SHOW_CANCEL);
		showLastUsedAuthnOption = vaadinProperties
				.getBooleanValue(VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY);
		autoLogin = vaadinProperties.getBooleanValue(VaadinEndpointProperties.AUTO_LOGIN);

		enableRegistration = vaadinProperties.getBooleanValue(VaadinEndpointProperties.ENABLE_REGISTRATION);

		showRegistrationFormsInHeader = vaadinProperties
				.getBooleanValue(VaadinEndpointProperties.SHOW_REGISTRATION_FORMS_IN_HEADER);

		externalRegistrationURL = vaadinProperties.getValue(VaadinEndpointProperties.EXTERNAL_REGISTRATION_URL);
		registrationForms = vaadinProperties
				.getListOfValues(VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS);

		String logoUri = vaadinProperties.getValue(VaadinEndpointProperties.AUTHN_LOGO);
		logo = ImageUtils.getImageFromUriSave(logoUri, uriAccessService);

		title = vaadinProperties.getLocalizedStringWithoutFallbackToDefault(msg,
				VaadinEndpointProperties.AUTHN_TITLE);

		authenticationLayoutConfiguration = AuthnLayoutPropertiesParser.fromProperties(vaadinProperties,
				msg);
		retUserLayoutConfiguration = AuthnLayoutPropertiesParser.getReturingUserColumnElementsFromProperties(vaadinProperties,
				msg);

		if (vaadinProperties.isSet(VaadinEndpointProperties.THEME))
		{
			defaultMainTheme = vaadinProperties.getValue(VaadinEndpointProperties.THEME);
		}
		if (vaadinProperties.isSet(VaadinEndpointProperties.AUTHN_THEME))
		{
			defaultAuthnTheme = vaadinProperties.getValue(VaadinEndpointProperties.AUTHN_THEME);
		}
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

	public String getWebContentDir()
	{
		return webContentDir;
	}

	public void setWebContentDir(String webContentDir)
	{
		this.webContentDir = webContentDir;
	}

	public boolean isProductionMode()
	{
		return productionMode;
	}

	public void setProductionMode(boolean productionMode)
	{
		this.productionMode = productionMode;
	}

	public String getTemplate()
	{
		return template;
	}

	public void setTemplate(String template)
	{
		this.template = template;
	}

	public boolean isCompactCredentialReset()
	{
		return compactCredentialReset;
	}

	public void setCompactCredentialReset(boolean compactCredentialReset)
	{
		this.compactCredentialReset = compactCredentialReset;
	}

	public AuthnLayoutConfiguration getAuthenticationLayoutConfiguration()
	{
		return authenticationLayoutConfiguration;
	}

	public void setAuthenticationLayoutConfiguration(
			AuthnLayoutConfiguration authenticationLayoutConfiguration)
	{
		this.authenticationLayoutConfiguration = authenticationLayoutConfiguration;
	}

	public List<AuthnElementConfiguration> getRetUserLayoutConfiguration()
	{
		return retUserLayoutConfiguration;
	}

	public void setRetUserLayoutConfiguration(
			List<AuthnElementConfiguration> retUserLayoutConfiguration)
	{
		this.retUserLayoutConfiguration = retUserLayoutConfiguration;
	}
}