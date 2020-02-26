/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.integration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds information about integration event
 * 
 * @author P.Piernik
 *
 */
public class IntegrationEvent
{
	public enum EventType
	{
		MESSAGE, WEBHOOK
	}

	public final String name;
	public final String trigger;
	public final EventType type;
	public final IntegrationEventConfiguration configuration;

	@JsonCreator
	public IntegrationEvent(@JsonProperty("name") String name, @JsonProperty("trigger") String trigger,
			@JsonProperty("type") EventType type,
			@JsonProperty("configuration") IntegrationEventConfiguration configuration)
	{
		this.name = name;
		this.trigger = trigger;
		this.type = type;
		this.configuration = configuration;
	}

	public IntegrationEvent(String name, EventType type)
	{
		this.name = name;
		this.trigger = null;
		this.type = type;
		this.configuration = null;
	}

}
