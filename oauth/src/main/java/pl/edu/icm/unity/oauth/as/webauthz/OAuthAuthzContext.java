/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.Date;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;

/**
 * Context stored in HTTP session maintaining authorization token. 
 * @author K. Benedyczak
 */
public class OAuthAuthzContext
{
	public static final long AUTHN_TIMEOUT = 900000;
	private AuthorizationRequest request;
	private Date timestamp;
	private String returnURI;

	public OAuthAuthzContext(AuthorizationRequest request)
	{
		super();
		this.request = request;
		this.timestamp = new Date();
	}

	public AuthorizationRequest getRequest()
	{
		return request;
	}
	
	public boolean isExpired()
	{
		return System.currentTimeMillis() > AUTHN_TIMEOUT+timestamp.getTime();
	}

	public String getReturnURI()
	{
		return returnURI;
	}

	public void setReturnURI(String returnURI)
	{
		this.returnURI = returnURI;
	}
}
