/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.integration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Webhook type integration event configuration
 * 
 * @author P.Piernik
 *
 */
public class Webhook implements IntegrationEventConfiguration
{
	public enum WebhookHttpMethod
	{
		GET, POST
	}

	public final String url;
	public final WebhookHttpMethod httpMethod;
	public final String truststore;

	public Webhook(@JsonProperty("url") String url, @JsonProperty("httpMethod") WebhookHttpMethod httpMethod,
			@JsonProperty("truststore") String truststore)
	{
		this.url = url;
		this.httpMethod = httpMethod;
		this.truststore = truststore;
	}
}
