/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.idp;

/**
 * Represent single user import configuration in idp service
 * 
 * @author P.Piernik
 *
 */
public class UserImportConfig
{
	private String importer;
	private String identityType;

	public UserImportConfig()
	{
	}

	public String getImporter()
	{
		return importer;
	}

	public void setImporter(String importer)
	{
		this.importer = importer;
	}

	public String getIdentityType()
	{
		return identityType;
	}

	public void setIdentityType(String identityType)
	{
		this.identityType = identityType;
	}
}
