/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.integration;

import java.util.Map;

/**
 * Integration event definition. The implementation defines the contract between the integration event and the 
 * code using the integration event. In particular all the variables which are supported by the code using the 
 * integration event are defined in the implementation. 
 * 
 * @author P. Piernik
 */
public interface IntegrationEventDefinition
{
	String getName();
	
	String getDescriptionKey();
	
	Map<String, IntegrationEventVariable> getVariables();
	
	String getGroup();
}
