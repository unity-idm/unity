/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.endpoint;

import java.util.Optional;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;

public interface EndpointFileConfigurationManagement
{

	EndpointConfiguration getEndpointConfig(String name) throws EngineException;

	Optional<String> getEndpointConfigKey(String endpointName);

}