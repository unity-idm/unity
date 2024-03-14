/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import io.imunity.upman.rest.UpmanRestEndpointProperties;
import io.imunity.vaadin.auth.services.idp.GroupWithIndentIndicator;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.rest.RESTEndpointProperties;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

public class UpmanRestServiceConfiguration
{
	private GroupWithIndentIndicator rootGroup;
	private GroupWithIndentIndicator authorizationGroup;
	private List<String> allowedCORSheaders;
	private List<String> allowedCORSorigins;
	private List<String> rootGroupAttributes;

	
	UpmanRestServiceConfiguration(String attribute)
	{
		this.allowedCORSheaders = new ArrayList<>();
		this.allowedCORSorigins = new ArrayList<>();
		this.rootGroupAttributes = new ArrayList<>(List.of(attribute));
	}
	
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

	public List<String> getRootGroupAttributes()
	{
		return rootGroupAttributes;
	}

	public void setRootGroupAttributes(List<String> rootGroupAttributes)
	{
		this.rootGroupAttributes = rootGroupAttributes;
	}
	
	public String toProperties()
	{
		Properties raw = new Properties();

		raw.put(UpmanRestEndpointProperties.PREFIX + UpmanRestEndpointProperties.ROOT_GROUP, rootGroup.group().getPathEncoded());
		raw.put(UpmanRestEndpointProperties.PREFIX + UpmanRestEndpointProperties.AUTHORIZATION_GROUP, authorizationGroup.group().getPathEncoded());

		if (!CollectionUtils.isEmpty(rootGroupAttributes))
		{
			for (int i = 0; i < rootGroupAttributes.size(); i++)
			{
				raw.put(UpmanRestEndpointProperties.PREFIX + UpmanRestEndpointProperties.ROOT_GROUP_ATTRIBUTES
						+ (i + 1), rootGroupAttributes.get(i));
			}
		}
	
		allowedCORSheaders.forEach(cors -> {

			int i = allowedCORSheaders.indexOf(cors) + 1;
			raw.put(UpmanRestEndpointProperties.PREFIX + UpmanRestEndpointProperties.ENABLED_CORS_HEADERS + i, cors);
		});

		allowedCORSorigins.forEach(cors -> {

			int i = allowedCORSorigins.indexOf(cors) + 1;
			raw.put(UpmanRestEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_ORIGINS + i, cors);
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
		rootGroupAttributes = restAdminProp.getListOfValues(UpmanRestEndpointProperties.ROOT_GROUP_ATTRIBUTES);
	}
	
	@Component
	public static class UpmanRestServiceConfigurationProvider
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_REST, UpmanRestServiceConfigurationProvider.class);

		private final AttributeSupport attributeSupport;

		@Autowired
		public UpmanRestServiceConfigurationProvider(AttributeSupport attributeSupport)
		{
			this.attributeSupport = attributeSupport;
		}

		UpmanRestServiceConfiguration getNewConfig() 
		{
			AttributeType nameAttr = null;
			try
			{
				nameAttr = attributeSupport.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
			} catch (EngineException e)
			{
				log.error("Can not get name attribute", e);
			}
			return nameAttr == null ? new UpmanRestServiceConfiguration() : new UpmanRestServiceConfiguration(nameAttr.getName());
		}	
	}
}