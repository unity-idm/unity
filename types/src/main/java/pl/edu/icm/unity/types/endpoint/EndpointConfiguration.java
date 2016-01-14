/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.endpoint;

import java.util.List;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

/**
 * Base endpoint configuration. Useful when deploying a new endpoint and when updating it. 
 * @author Krzysztof Benedyczak
 */
public class EndpointConfiguration
{
	private I18nString displayedName;
	private String description;
	private List<AuthenticationOptionDescription> authenticationOptions;
	private String configuration;
	private String realm;
	
	public EndpointConfiguration(I18nString displayedName, String description,
			List<AuthenticationOptionDescription> authn, String configuration,
			String realm)
	{
		super();
		this.displayedName = displayedName;
		this.description = description;
		this.authenticationOptions = authn;
		this.configuration = configuration;
		this.realm = realm;
	}

	public I18nString getDisplayedName()
	{
		return displayedName;
	}

	public String getDescription()
	{
		return description;
	}

	public List<AuthenticationOptionDescription> getAuthenticationOptions()
	{
		return authenticationOptions;
	}

	public String getConfiguration()
	{
		return configuration;
	}

	public String getRealm()
	{
		return realm;
	}
}
