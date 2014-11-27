/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import pl.edu.icm.unity.saml.SamlProperties.Binding;

/**
 * Stores information about an SAML endpoint.
 * @author K. Benedyczak
 */
public class SAMLEndpointDefinition
{
	private Binding binding;
	private String url;
	private String returnUrl;

	public SAMLEndpointDefinition()
	{
	}
	
	public SAMLEndpointDefinition(Binding binding, String url, String returnUrl)
	{
		super();
		this.binding = binding;
		this.url = url;
		this.returnUrl = returnUrl;
	}

	public Binding getBinding()
	{
		return binding;
	}

	public void setBinding(Binding binding)
	{
		this.binding = binding;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getReturnUrl()
	{
		return returnUrl;
	}

	public void setReturnUrl(String returnUrl)
	{
		this.returnUrl = returnUrl;
	}
}
