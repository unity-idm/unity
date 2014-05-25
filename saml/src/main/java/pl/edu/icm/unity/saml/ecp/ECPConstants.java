/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

/**
 * ECP profile constants.
 * @author K. Benedyczak
 */
public class ECPConstants
{
	public static final String ECP_CONTENT_TYPE = "application/vnd.paos+xml";
	public static final String PAOS_VERSION = "ver=\"urn:liberty:paos:2003-08\"";
	public static final String ECP_PROFILE = "urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp";
	public static final String CHANNEL_BINDING = "\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp:2.0:cb\"";
	public static final String HOK = "\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp:2.0:hok\"";
	
	public static final String ECP_NS = ECP_PROFILE;
}
