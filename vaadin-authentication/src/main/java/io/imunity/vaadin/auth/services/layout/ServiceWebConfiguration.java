/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout;

import com.google.common.collect.Lists;
import io.imunity.vaadin.auth.services.layout.configuration.AuthnLayoutColumnConfiguration;
import io.imunity.vaadin.auth.services.layout.configuration.AuthnLayoutConfiguration;
import io.imunity.vaadin.auth.services.layout.configuration.AuthnLayoutPropertiesParser;
import io.imunity.vaadin.auth.services.layout.configuration.elements.AuthnElementConfiguration;
import io.imunity.vaadin.auth.services.layout.configuration.elements.ExpandConfig;
import io.imunity.vaadin.auth.services.layout.configuration.elements.LastUsedConfig;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import io.imunity.vaadin.auth.services.layout.configuration.elements.SeparatorConfig;
import io.imunity.vaadin.endpoint.common.file.FileFieldUtils;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static io.imunity.vaadin.endpoint.common.VaadinEndpointProperties.PREFIX;


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

	public ServiceWebConfiguration()
	{
		this(null);
	}
	
	public ServiceWebConfiguration(String overridenMainTheme)
	{
		registrationForms = new ArrayList<>();

		authenticationLayoutConfiguration = new AuthnLayoutConfiguration(
				Arrays.asList(new AuthnLayoutColumnConfiguration(new I18nString(),
						(int) VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH,
						Lists.newArrayList())),
				Lists.newArrayList());

		retUserLayoutConfiguration = Arrays.asList(new LastUsedConfig(),
				new SeparatorConfig(new I18nString()),
				new ExpandConfig());

		defaultMainTheme = overridenMainTheme;
		productionMode = true;
		template = VaadinEndpointProperties.DEFAULT_TEMPLATE;
		compactCredentialReset = true;
	}

	public Properties toProperties(MessageSource msg, FileStorageService fileStorageService,
			String serviceName)
	{
		Properties raw = new Properties();

		raw.put(PREFIX + VaadinEndpointProperties.AUTHN_SHOW_SEARCH, String.valueOf(showSearch));
		raw.put(PREFIX + VaadinEndpointProperties.AUTHN_ADD_ALL, String.valueOf(addAllAuthnOptions));
		raw.put(PREFIX + VaadinEndpointProperties.AUTHN_SHOW_CANCEL, String.valueOf(showCancel));
		raw.put(PREFIX + VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY, String.valueOf(showLastUsedAuthnOption));
		raw.put(PREFIX + VaadinEndpointProperties.AUTO_LOGIN, String.valueOf(autoLogin));

		raw.put(PREFIX + VaadinEndpointProperties.ENABLE_REGISTRATION, String.valueOf(enableRegistration));

		raw.put(PREFIX + VaadinEndpointProperties.SHOW_REGISTRATION_FORMS_IN_HEADER,
				String.valueOf(showRegistrationFormsInHeader));

		raw.put(PREFIX + VaadinEndpointProperties.PRODUCTION_MODE, String.valueOf(productionMode));
		if (webContentDir != null)
			raw.put(PREFIX + VaadinEndpointProperties.WEB_CONTENT_PATH, webContentDir);

		raw.put(PREFIX + VaadinEndpointProperties.TEMPLATE, template);

		raw.put(PREFIX + VaadinEndpointProperties.CRED_RESET_COMPACT, String.valueOf(compactCredentialReset));

		if (externalRegistrationURL != null)
			raw.put(PREFIX + VaadinEndpointProperties.EXTERNAL_REGISTRATION_URL, externalRegistrationURL);
		
		if (registrationForms != null && !registrationForms.isEmpty())
		{
			registrationForms.forEach(c -> raw.put(PREFIX
					+ VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS
					+ (registrationForms.indexOf(c) + 1), c));
		}

		if (title != null)
			title.toProperties(raw, PREFIX + VaadinEndpointProperties.AUTHN_TITLE, msg);

		if (logo != null)
		{
			FileFieldUtils.saveInProperties(getLogo(),
					PREFIX + VaadinEndpointProperties.AUTHN_LOGO, raw,
					fileStorageService, StandardOwner.SERVICE.toString(), serviceName);
		} else
		{
			raw.put(PREFIX + VaadinEndpointProperties.AUTHN_LOGO, "");
		}

		if (defaultMainTheme != null)
			raw.put(PREFIX + VaadinEndpointProperties.THEME, defaultMainTheme);
		if (defaultAuthnTheme != null)
			raw.put(PREFIX + VaadinEndpointProperties.AUTHN_THEME, defaultAuthnTheme);
		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		raw.putAll(parser.toProperties(authenticationLayoutConfiguration));
		raw.putAll(parser.returningUserColumnElementToProperties(retUserLayoutConfiguration));

		return raw;
	}

	public void fromProperties(String vaadinProperties, MessageSource msg, VaadinLogoImageLoader imageAccessService,
			String systemDefaultTheme)
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
		fromProperties(vProperties, msg, imageAccessService, systemDefaultTheme);
	}

	private void fromProperties(VaadinEndpointProperties vaadinProperties, MessageSource msg,
			VaadinLogoImageLoader imageAccessService, String systemDefaultTheme)
	{
		//this is set for effective endpoint configuration at endpoint loading - we copy this here 
		//to have consistent setup. However this property is not persisted.
		if (systemDefaultTheme != null)
			vaadinProperties.setProperty(VaadinEndpointProperties.DEF_THEME, systemDefaultTheme);
		
		if (vaadinProperties.isSet(VaadinEndpointProperties.WEB_CONTENT_PATH))
			webContentDir = vaadinProperties.getValue(VaadinEndpointProperties.WEB_CONTENT_PATH);

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
		
		logo = imageAccessService.loadImageFromUri(logoUri).orElse(null);

		title = vaadinProperties.getLocalizedStringWithoutFallbackToDefault(msg,
				VaadinEndpointProperties.AUTHN_TITLE);
		if (title.isEmpty())
			title = null;

		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		authenticationLayoutConfiguration = parser.fromProperties(vaadinProperties);
		retUserLayoutConfiguration = parser.getReturingUserColumnElementsFromProperties(vaadinProperties);
		
		if (vaadinProperties.isSet(VaadinEndpointProperties.THEME))
			defaultMainTheme = vaadinProperties.getValue(VaadinEndpointProperties.THEME);
		if (vaadinProperties.isSet(VaadinEndpointProperties.AUTHN_THEME))
			defaultAuthnTheme = vaadinProperties.getValue(VaadinEndpointProperties.AUTHN_THEME);
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