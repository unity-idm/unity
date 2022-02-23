/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;

@Component
public class SAMLSPConfigurationParser
{
	private final PKIManagement pkiMan;
	
	public SAMLSPConfigurationParser(@Qualifier("insecure") PKIManagement pkiMan)
	{
		this.pkiMan = pkiMan;
	}

	public SAMLSPConfiguration parse(String source)
	{
		SAMLSPProperties samlProperties = loadAsProperties(source);
		return fromProperties(samlProperties);
	}

	private SAMLSPProperties loadAsProperties(String source)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			return new SAMLSPProperties(properties, pkiMan);
		} catch (ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator(?)", e);
		}
	}
	
	private SAMLSPConfiguration fromProperties(SAMLSPProperties samlProperties)
	{
		//TODO
		throw new UnsupportedOperationException("TODO");
	}
	
	public String serialize(SAMLSPConfiguration config)
	{
		SAMLSPProperties asProperties = toProperties(config);
		return serializeToString(asProperties);
	}
	
	private SAMLSPProperties toProperties(SAMLSPConfiguration config)
	{
		//TODO
		throw new UnsupportedOperationException("TODO");
	}
	
	private String serializeToString(SAMLSPProperties samlProperties)
	{
		StringWriter sbw = new StringWriter();
		try
		{
			samlProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize SAML verificator configuration", e);
		}
		return sbw.toString();
	}
}
