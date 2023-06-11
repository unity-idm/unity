/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.integration;

import java.util.Map;

import com.vaadin.data.HasValue;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventConfiguration;

public interface IntegrationEventConfigurationEditor extends Component, HasValue<IntegrationEventConfiguration>
{
	void setTrigger(String trigger);
	Component test(Map<String, String> params) throws EngineException;
}
