/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

class LegacyRequestedOAuthScopeDeserializer extends JsonDeserializer<RequestedOAuthScope>
{
	@Override
	public RequestedOAuthScope deserialize(JsonParser parser, DeserializationContext context) throws IOException
	{
		if (parser.currentToken() == JsonToken.VALUE_STRING)
		{
			String scope = parser.getValueAsString();
			return new RequestedOAuthScope(scope, ActiveOAuthScopeDefinition.builder().withName(scope)
					.withAttributes(null).build(), false);
		}
		return parser.getCodec().readValue(parser, RequestedOAuthScope.class);
	}
}
