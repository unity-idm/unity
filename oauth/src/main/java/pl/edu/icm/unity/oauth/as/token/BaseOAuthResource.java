/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.server.api.internal.Token;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;

/**
 * Common code inherited by OAuth resources
 * 
 * @author K. Benedyczak
 */
public class BaseOAuthResource
{
	protected String getResponseContent(com.nimbusds.oauth2.sdk.Response oauthResponse)
	{
		try
		{
			return oauthResponse.toHTTPResponse().getContent();
		} catch (SerializeException e)
		{
			throw new InternalException("Can not serialize OAuth success response", e);
		}
	}
	
	protected URI toURI(String raw)
	{
		try
		{
			return new URI(raw);
		} catch (URISyntaxException e)
		{
			throw new InternalException("uri can not be reparsed" + raw, e);
		}
	}
	
	protected JWT decodeIDToken(OAuthToken internalToken)
	{
		try
		{
			return internalToken.getOpenidInfo() == null ? null : 
				SignedJWT.parse(internalToken.getOpenidInfo());
		} catch (ParseException e)
		{
			throw new InternalException("Can not parse the internal id token", e);
		}
	}
	
	protected OAuthToken parseInternalToken(Token codeToken)
	{
		try
		{
			return OAuthToken.getInstanceFromJson(codeToken.getContents());
		} catch (Exception e)
		{
			throw new InternalException("Can not parse the internal code token", e);
		}
	}
	
	protected Response makeError(ErrorObject baseError, String description)
	{
		if (description != null)
			baseError = baseError.appendDescription("; " + description);
		TokenErrorResponse eResp = new TokenErrorResponse(baseError);
		HTTPResponse httpResp = eResp.toHTTPResponse();
		return toResponse(Response.status(httpResp.getStatusCode()).entity(httpResp.getContent()));
	}
	
	protected Response makeBearerError(BearerTokenError error)
	{
		String header = error.toWWWAuthenticateHeader();
		return toResponse(Response.status(error.getHTTPStatusCode()).header("WWW-Authenticate", header));
	}
	
	protected Response makeBearerError(BearerTokenError error, String description)
	{
		error.appendDescription(" " + description);
		return makeBearerError(error);
	}

	protected Response toResponse(ResponseBuilder respBuilder)
	{
		return respBuilder.header("Pragma", "no-cache").header("Cache-Control", "no-store").build();
	}
}
