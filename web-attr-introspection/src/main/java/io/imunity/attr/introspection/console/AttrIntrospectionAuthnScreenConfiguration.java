/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.PREFIX;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutColumnConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutPropertiesParser;

public class AttrIntrospectionAuthnScreenConfiguration
{
	private AuthnLayoutConfiguration authnLayoutConfiguration;
	private boolean enableSearch;

	AttrIntrospectionAuthnScreenConfiguration()
	{
		authnLayoutConfiguration = new AuthnLayoutConfiguration(
				Arrays.asList(new AuthnLayoutColumnConfiguration(new I18nString(),
						(int)VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH,
						Lists.newArrayList())),
				Lists.newArrayList());
	}
	
	Properties toProperties(MessageSource msg)
	{
		Properties raw = new Properties();
		raw.put(PREFIX + VaadinEndpointProperties.AUTHN_SHOW_SEARCH, String.valueOf(enableSearch));

		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		raw.putAll(parser.toProperties(authnLayoutConfiguration));

		return raw;
	}

	void fromProperties(String vaadinProperties, MessageSource msg)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(vaadinProperties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the attribute introspection service", e);
		}

		VaadinEndpointProperties vProperties = new VaadinEndpointProperties(raw);
		fromProperties(vProperties, msg);
	}

	private void fromProperties(VaadinEndpointProperties vaadinProperties, MessageSource msg)
	{
		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		authnLayoutConfiguration = parser.fromProperties(vaadinProperties);
		enableSearch = vaadinProperties.getBooleanValue(VaadinEndpointProperties.AUTHN_SHOW_SEARCH);
	}

	public AuthnLayoutConfiguration getauthnLayoutConfiguration()
	{
		return authnLayoutConfiguration;
	}

	public void setauthnLayoutConfiguration(AuthnLayoutConfiguration authnLayoutConfiguration)
	{
		this.authnLayoutConfiguration = authnLayoutConfiguration;
	}

	public boolean isEnableSearch()
	{
		return enableSearch;
	}

	public void setEnableSearch(boolean enableSearch)
	{
		this.enableSearch = enableSearch;
	}

}
