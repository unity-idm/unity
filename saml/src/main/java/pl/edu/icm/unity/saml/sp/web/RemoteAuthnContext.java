/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.Serializable;

import pl.edu.icm.unity.saml.sp.web.SAMLSPRetrievalProperties.Binding;

/**
 * Context of a remote SAML authentication. Stored in HTTP session.
 * @author K. Benedyczak
 */
public class RemoteAuthnContext implements Serializable
{
	private String request;
	private String idpUrl;
	private Binding binding;

	public String getRequest()
	{
		return request;
	}
	public void setRequest(String request)
	{
		this.request = request;
	}
	public Binding getBinding()
	{
		return binding;
	}
	public void setBinding(Binding binding)
	{
		this.binding = binding;
	}
	public String getIdpUrl()
	{
		return idpUrl;
	}
	public void setIdpUrl(String idpUrl)
	{
		this.idpUrl = idpUrl;
	}
}
