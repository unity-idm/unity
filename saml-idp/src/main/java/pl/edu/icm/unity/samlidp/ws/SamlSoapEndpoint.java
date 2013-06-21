/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.ws;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.Properties;

import eu.unicore.samly2.webservice.SAMLAuthnInterface;
import eu.unicore.samly2.webservice.SAMLQueryInterface;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.samlidp.SamlProperties;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.ws.CXFEndpoint;

/**
 * Endpoint exposing SAML SOAP binding.
 * 
 * @author K. Benedyczak
 */
public class SamlSoapEndpoint extends CXFEndpoint
{
	protected SamlProperties samlProperties;
	protected IdentitiesManagement identitiesMan;
	protected AttributesManagement attributesMan;
	protected PreferencesManagement preferencesMan;
	
	public SamlSoapEndpoint(UnityMessageSource msg, EndpointTypeDescription type,
			String servletPath, 
			IdentitiesManagement identitiesMan, AttributesManagement attributesMan,
			PreferencesManagement preferencesMan)
	{
		super(msg, type, servletPath);
		this.identitiesMan = identitiesMan;
		this.attributesMan = attributesMan;
		this.preferencesMan = preferencesMan;
	}

	@Override
	public void setSerializedConfiguration(String config)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new ByteArrayInputStream(config.getBytes()));
			samlProperties = new SamlProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the SAML SOAP" +
					" IdP endpoint's configuration", e);
		}
	}

	@Override
	public String getSerializedConfiguration()
	{
		CharArrayWriter writer = new CharArrayWriter();
		try
		{
			Properties properties = samlProperties.getProperties();
			properties.store(writer, "");
		} catch (IOException e)
		{
			throw new IllegalStateException("Can not serialize endpoint's configuration", e);
		}
		return writer.toString();
	}

	@Override
	protected void configureServices()
	{
		String endpointURL = getServletUrl(servletPath);
		SAMLAssertionQueryImpl assertionQueryImpl = new SAMLAssertionQueryImpl(samlProperties, 
				endpointURL, attributesMan, identitiesMan, preferencesMan);
		addWebservice(SAMLQueryInterface.class, assertionQueryImpl);
		SAMLAuthnImpl authnImpl = new SAMLAuthnImpl(samlProperties, endpointURL, 
				identitiesMan, attributesMan, preferencesMan);
		addWebservice(SAMLAuthnInterface.class, authnImpl);		
	}
}




