/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.integration;

import java.util.Map;

/**
 * Responsible for processing integration events
 * 
 * @author P.Piernik
 *
 */
public interface IntegrationEventProcessor
{
	void trigger(IntegrationEvent event, Map<String, String> params);
}
