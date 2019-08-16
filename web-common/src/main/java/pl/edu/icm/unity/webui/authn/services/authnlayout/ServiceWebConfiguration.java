/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services.authnlayout;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.ImageUtils;

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

	private Properties authnScreenProperties;
	private Properties layoutForRetUserProperties;

	public ServiceWebConfiguration(List<String> regForms)
	{
		this();
		registrationForms.addAll(regForms);
	}

	public ServiceWebConfiguration()
	{
		registrationForms = new ArrayList<>();
		authnScreenProperties = new Properties();
		String columnKey = VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_COLUMNS_PFX + "1.";
		authnScreenProperties.put(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_TITLE, "");
		authnScreenProperties.put(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS, "");

		layoutForRetUserProperties = new Properties();
		authnScreenProperties.put(
				VaadinEndpointProperties.PREFIX
						+ VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT,
				VaadinEndpointProperties.DEFAULT_AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT_CONTENT);
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

		raw.putAll(authnScreenProperties);
		raw.putAll(layoutForRetUserProperties);

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

		authnScreenProperties = new Properties();
		Iterator<String> columnKeys = vaadinProperties
				.getStructuredListKeys(VaadinEndpointProperties.AUTHN_COLUMNS_PFX).iterator();
		while (columnKeys.hasNext())
		{
			String columnKey = columnKeys.next();

			if (vaadinProperties.isSet(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_TITLE))
			{

				authnScreenProperties.put(
						VaadinEndpointProperties.PREFIX + columnKey
								+ VaadinEndpointProperties.AUTHN_COLUMN_TITLE,
						vaadinProperties.getValue(columnKey
								+ VaadinEndpointProperties.AUTHN_COLUMN_TITLE));
			}
			if (vaadinProperties.isSet(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_SEPARATOR))
			{

				I18nString sep = vaadinProperties.getLocalizedStringWithoutFallbackToDefault(msg,
						columnKey + VaadinEndpointProperties.AUTHN_COLUMN_SEPARATOR);
				sep.toProperties(authnScreenProperties, VaadinEndpointProperties.PREFIX + columnKey
						+ VaadinEndpointProperties.AUTHN_COLUMN_SEPARATOR, msg);
			}
			if (vaadinProperties.isSet(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS))
			{
				authnScreenProperties.put(
						VaadinEndpointProperties.PREFIX + columnKey
								+ VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS,
						vaadinProperties.getValue(columnKey
								+ VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS));

			}

			if (vaadinProperties.isSet(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_WIDTH))
			{
				authnScreenProperties.put(
						VaadinEndpointProperties.PREFIX + columnKey
								+ VaadinEndpointProperties.AUTHN_COLUMN_WIDTH,
						vaadinProperties.getValue(columnKey
								+ VaadinEndpointProperties.AUTHN_COLUMN_WIDTH));

			}

		}

		layoutForRetUserProperties = new Properties();
		if (vaadinProperties.isSet(VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT))
		{
			layoutForRetUserProperties.put(
					VaadinEndpointProperties.PREFIX
							+ VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT,
					vaadinProperties.getValue(
							VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT));
		}

		Iterator<String> separatorsKeys = vaadinProperties
				.getStructuredListKeys(VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX).iterator();

		Properties labels = new Properties();
		while (separatorsKeys.hasNext())
		{
			String separatorKey = separatorsKeys.next();
			labels.put(VaadinEndpointProperties.PREFIX + separatorKey
					+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT,
					vaadinProperties.getValue(separatorKey
							+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT));

			I18nString val = vaadinProperties.getLocalizedStringWithoutFallbackToDefault(msg,
					separatorKey + VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT);

			val.toProperties(labels, VaadinEndpointProperties.PREFIX + separatorKey
					+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT, msg);

		}
		layoutForRetUserProperties.putAll(labels);
		authnScreenProperties.putAll(labels);

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

	public Properties getAuthnScreenProperties()
	{
		return authnScreenProperties;
	}

	public void setAuthnScreenProperties(Properties authnScreenProperties)
	{
		this.authnScreenProperties = authnScreenProperties;
	}

	public Properties getLayoutForRetUserProperties()
	{
		return layoutForRetUserProperties;
	}

	public void setLayoutForRetUserProperties(Properties layoutForRetUserProperties)
	{
		this.layoutForRetUserProperties = layoutForRetUserProperties;
	}
}