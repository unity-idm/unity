/**
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.session;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.json.TokenWithJsonContentsSerializer;


/**
 * Map session token contents to JsonNode
 * @author P.Piernik
 *
 */
@Component
public class SessionTokenJsonSerializer extends TokenWithJsonContentsSerializer
{
	public SessionTokenJsonSerializer()
	{
		super(SessionManagementImpl.SESSION_TOKEN_TYPE, "Session token JSON formatter");
	}
}
