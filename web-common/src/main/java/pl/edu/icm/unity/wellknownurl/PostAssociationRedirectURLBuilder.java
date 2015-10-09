/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.wellknownurl;

import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;

/**
 * Builds URL to be used for redirection after account association.
 * @author K. Benedyczak
 */
public class PostAssociationRedirectURLBuilder
{
	public enum Status {success, error, cancelled}

	public static final String PARAM_STATUS = "status";
	public static final String PARAM_ERROR_CODE = "error_msg";
	public static final String PARAM_ASSOCIATION_OF = "association_of";
	public static final String PARAM_ASSOCIATION_FROM = "association_from_idp";
	public static final String PARAM_ASSOCIATED_WITH = "associated_with";

	private URIBuilder uriBuilder;
	
	public PostAssociationRedirectURLBuilder(String baseURL, Status status)
	{
		try
		{
			uriBuilder = new URIBuilder(baseURL);
		} catch (URISyntaxException e)
		{
			throw new IllegalStateException("Illegal redirect URI, shouldn't happen", e);
		}
		
		uriBuilder.addParameter(PARAM_STATUS, status.toString());
	}
	
	public PostAssociationRedirectURLBuilder setErrorCode(String error)
	{
		uriBuilder.addParameter(PARAM_ERROR_CODE, error);
		return this;
	}
	
	public PostAssociationRedirectURLBuilder setAssociatedInto(long associatedIntoEntity)
	{
		uriBuilder.addParameter(PARAM_ASSOCIATED_WITH, String.valueOf(associatedIntoEntity));
		return this;
	}

	public PostAssociationRedirectURLBuilder setAssociatedInfo(String associatedIdentity, String idp)
	{
		if (idp != null)
			uriBuilder.addParameter(PARAM_ASSOCIATION_FROM, idp);
		uriBuilder.addParameter(PARAM_ASSOCIATION_OF, associatedIdentity);
		return this;
	}

	@Override
	public String toString()
	{
		return uriBuilder.toString();
	}
}
