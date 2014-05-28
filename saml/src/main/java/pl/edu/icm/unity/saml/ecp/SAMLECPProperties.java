/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.util.Map;
import java.util.Properties;

import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.server.api.PKIManagement;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;

/**
 * Extension of SAML SP properties. Allows for specification of the settings required to generate 
 * JWT after completed ECP flow.
 *  
 * @author K. Benedyczak
 */
public class SAMLECPProperties extends SAMLSPProperties
{
	private static final String JWT_P = "jwt.";
	
	static
	{
		DocumentationCategory jwt = new DocumentationCategory(
				"JWT generation specific settings", "04");

		for (Map.Entry<String, PropertyMD> entry: JWTAuthenticationProperties.META.entrySet())
			META.put(JWT_P + entry.getKey(), entry.getValue().setCategory(jwt));
	}
	
	public SAMLECPProperties(Properties properties, PKIManagement pkiMan) throws ConfigurationException
	{
		super(properties, pkiMan);
	}
	
	public JWTAuthenticationProperties getJWTProperties()
	{
		return new JWTAuthenticationProperties(P + JWT_P, properties);
	}
}
