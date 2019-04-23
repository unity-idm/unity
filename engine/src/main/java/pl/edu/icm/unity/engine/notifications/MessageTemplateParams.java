/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.util.Collections;
import java.util.Map;

/**
 * Template id, and its parameters  
 */
public class MessageTemplateParams
{
	public final String templateId;
	public final Map<String, String> parameters;

	public MessageTemplateParams(String templateId, Map<String, String> parameters)
	{
		this.templateId = templateId;
		this.parameters = Collections.unmodifiableMap(parameters);
	}
}
