/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import io.imunity.upman.rest.UpmanRestEndpointProperties;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.RESTEndpointProperties;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class UpmanRestServiceConfiguration
{
	private GroupWithIndentIndicator rootGroup;
	private GroupWithIndentIndicator authorizationGroup;
	private List<String> allowedCORSheaders;
	private List<String> allowedCORSorigins;

	UpmanRestServiceConfiguration()
	{
		this.allowedCORSheaders = new ArrayList<>();
		this.allowedCORSorigins = new ArrayList<>();
	}

	public GroupWithIndentIndicator getRootGroup()
	{
		return rootGroup;
	}

	public void setRootGroup(GroupWithIndentIndicator rootGroup)
	{
		this.rootGroup = rootGroup;
	}

	public GroupWithIndentIndicator getAuthorizationGroup()
	{
		return authorizationGroup;
	}

	public void setAuthorizationGroup(GroupWithIndentIndicator authorizationGroup)
	{
		this.authorizationGroup = authorizationGroup;
	}

	public List<String> getAllowedCORSheaders()
	{
		return allowedCORSheaders;
	}

	public void setAllowedCORSheaders(List<String> allowedCORSheaders)
	{
		this.allowedCORSheaders = allowedCORSheaders;
	}

	public List<String> getAllowedCORSorigins()
	{
		return allowedCORSorigins;
	}

	public void setAllowedCORSorigins(List<String> allowedCORSorigins)
	{
		this.allowedCORSorigins = allowedCORSorigins;
	}

	public String toProperties()
	{
		Properties raw = new Properties();

		raw.put(UpmanRestEndpointProperties.PREFIX + UpmanRestEndpointProperties.ROOT_GROUP, rootGroup.group.getPathEncoded());
		raw.put(UpmanRestEndpointProperties.PREFIX + UpmanRestEndpointProperties.AUTHORIZATION_GROUP, authorizationGroup.group.getPathEncoded());

		allowedCORSheaders.forEach(cors -> {

			int i = allowedCORSheaders.indexOf(cors) + 1;
			raw.put(RESTEndpointProperties.PREFIX + UpmanRestEndpointProperties.ENABLED_CORS_HEADERS + i, cors);
		});

		allowedCORSorigins.forEach(cors -> {

			int i = allowedCORSorigins.indexOf(cors) + 1;
			raw.put(RESTEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_ORIGINS + i, cors);
		});

		UpmanRestEndpointProperties prop = new UpmanRestEndpointProperties(raw);
		return prop.getAsString();
	}

	public void fromProperties(String properties, MessageSource msg)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the rest admin service", e);
		}

		UpmanRestEndpointProperties restAdminProp = new UpmanRestEndpointProperties(raw);
		String rootGroupPath = restAdminProp.getValue(UpmanRestEndpointProperties.ROOT_GROUP);
		this.rootGroup = new GroupWithIndentIndicator(new Group(rootGroupPath),
			false);
		String authorizationGroupPath = restAdminProp.getValue(UpmanRestEndpointProperties.AUTHORIZATION_GROUP);
		this.authorizationGroup = new GroupWithIndentIndicator(new Group(authorizationGroupPath),
			false);
		allowedCORSheaders = restAdminProp.getListOfValues(UpmanRestEndpointProperties.ENABLED_CORS_HEADERS);
		allowedCORSorigins = restAdminProp.getListOfValues(UpmanRestEndpointProperties.ENABLED_CORS_ORIGINS);
	}

}