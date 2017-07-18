/**
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.session;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.json.JsonBytesFacility;


/**
 * Map session token contents to JsonNode
 * @author P.Piernik
 *
 */
@Component
public class JsonSessionTokenFacility extends JsonBytesFacility
{
	@Override
	public String getDescription()
	{
		return "Session token JSON formatter";
	}

	@Override
	public String getName()
	{
		return SessionManagementImpl.SESSION_TOKEN_TYPE;
	}
}
