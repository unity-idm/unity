/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.integration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message type integration event configuration
 * 
 * @author P.Piernik
 *
 */
public class Message implements IntegrationEventConfiguration
{
	public final String messageTemplate;
	public final List<Long> singleRecipients;
	public final List<String> groupsRecipients;

	public Message(@JsonProperty("messageTemplate") String messageTemplate,
			@JsonProperty("singleRecipients") List<Long> singleRecipients,
			@JsonProperty("groupsRecipients") List<String> groupsRecipients)
	{
		this.messageTemplate = messageTemplate;
		this.singleRecipients = singleRecipients;
		this.groupsRecipients = groupsRecipients;
	}

}
