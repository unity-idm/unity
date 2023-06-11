/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.endpoint;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;

@Component
public class EndpointFileConfigurationManagementImpl implements EndpointFileConfigurationManagement
{
	private final UnityServerConfiguration config;
	private final ServerManagement serverMan;
	private final MessageSource msg;

	@Autowired
	public EndpointFileConfigurationManagementImpl(UnityServerConfiguration config, ServerManagement serverMan,
			MessageSource msg)
	{
		this.config = config;
		this.serverMan = serverMan;
		this.msg = msg;
	}

	public EndpointConfiguration getEndpointConfig(String name) throws EngineException
	{
		String endpointKey = getEndpointConfigKey(name)
				.orElseThrow(() -> new EngineException("File configuration for endpoint " + name + " does not exists"));

		String description = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_DESCRIPTION);
		List<String> authn = config.getEndpointAuth(endpointKey);

		String realm = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_REALM);
		I18nString displayedName = config.getLocalizedString(msg,
				endpointKey + UnityServerConfiguration.ENDPOINT_DISPLAYED_NAME);
		if (displayedName.isEmpty())
			displayedName.setDefaultValue(name);
		String jsonConfig;
		try
		{
			jsonConfig = serverMan.loadConfigurationFile(
					config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_CONFIGURATION));
		} catch (Exception e)
		{
			throw new EngineException("Can not read configuration from file for endpoint " + name);
		}

		return new EndpointConfiguration(displayedName, description, authn, jsonConfig, realm);
	}

	public Optional<String> getEndpointConfigKey(String endpointName)
	{
		for (String endpoint : config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS))
		{
			String cname = config.getValue(endpoint + UnityServerConfiguration.ENDPOINT_NAME);
			if (endpointName.equals(cname))
			{
				return Optional.of(endpoint);
			}
		}
		return Optional.empty();
	}
}
