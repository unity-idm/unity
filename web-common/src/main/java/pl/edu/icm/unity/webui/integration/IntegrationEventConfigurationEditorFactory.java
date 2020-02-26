/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.integration;

public interface IntegrationEventConfigurationEditorFactory
{
	String supportedType();
	IntegrationEventConfigurationEditor getEditor(String trigger);
}
