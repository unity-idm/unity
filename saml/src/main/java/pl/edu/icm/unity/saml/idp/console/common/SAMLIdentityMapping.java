/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console.common;

/**
 * Stores information about SAML and internal Unity identity connection
 * 
 * @author P.Piernik
 *
 */
public class SAMLIdentityMapping
{
	private String unityId;
	private String samlId;

	public SAMLIdentityMapping()
	{

	}

	public String getUnityId()
	{
		return unityId;
	}

	public void setUnityId(String unityId)
	{
		this.unityId = unityId;
	}

	public String getSamlId()
	{
		return samlId;
	}

	public void setSamlId(String samlId)
	{
		this.samlId = samlId;
	}
}