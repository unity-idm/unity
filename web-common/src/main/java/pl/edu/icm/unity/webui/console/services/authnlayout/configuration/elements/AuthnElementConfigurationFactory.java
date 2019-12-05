/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;

public interface AuthnElementConfigurationFactory
{
	Optional<AuthnElementConfiguration> getConfigurationElement(UnityMessageSource msg,
			VaadinEndpointProperties properties, String specEntry);
}
