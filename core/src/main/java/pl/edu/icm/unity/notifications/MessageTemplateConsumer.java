/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.notifications;

import java.util.Map;

/**
 * Message template consumer
 * 
 * @author P. Piernik
 */
public interface MessageTemplateConsumer
{
	public String getDescription();

	public String getName();

	public Map<String, String> getVariables();
}
