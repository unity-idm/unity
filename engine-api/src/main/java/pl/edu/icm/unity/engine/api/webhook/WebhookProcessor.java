/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.webhook;

import java.util.Map;

import org.apache.hc.core5.http.ClassicHttpResponse;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.integration.Webhook;

public interface WebhookProcessor
{
	ClassicHttpResponse trigger(Webhook webhook, Map<String, String> params) throws EngineException;
}
