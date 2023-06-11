/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.endpoint;

import java.util.Optional;

import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.exceptions.EngineException;

public interface EndpointFileConfigurationManagement
{

	EndpointConfiguration getEndpointConfig(String name) throws EngineException;

	Optional<String> getEndpointConfigKey(String endpointName);

}