/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.exception;

import java.util.Set;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;

public class OAuthExceptionMapper
{
	public static void installExceptionHandlers(Set<Object> ret)
	{
		ret.add(new EngineExceptionMapper());
		ret.add(new NPEExceptionMapper());
		ret.add(new IllegalArgumentExceptionMapper());
		ret.add(new InternalExceptionMapper());
		ret.add(new JSONParseExceptionMapper());
		ret.add(new JSONParsingExceptionMapper());
		ret.add(new JSONExceptionMapper());
	}

	public static ErrorObject makeError(ErrorObject baseError, String description)
	{
		return OAuth2Error.INVALID_CLIENT.appendDescription(description.isEmpty() ? "" : "; " + description);
	}

}
