/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

/**
 * Common interface implemented by all SAML endpoints
 * @author K. Benedyczak
 */
public interface SamlEndpoint
{
	public SamlIdpProperties getSamlProperties();
}
