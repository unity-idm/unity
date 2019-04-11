/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications.script;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GroovyNotificationChannelConfig
{
	public final String scriptPath;
	public final boolean supportsTemplates;
	
	@JsonCreator
	public GroovyNotificationChannelConfig(@JsonProperty("scriptPath") String scriptPath, 
			@JsonProperty("supportsTemplates") boolean supportsTemplates)
	{
		this.scriptPath = scriptPath;
		this.supportsTemplates = supportsTemplates;
	}
}
