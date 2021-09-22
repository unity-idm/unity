/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.ldap;

import pl.edu.icm.unity.ldap.EmbeddedDirectoryServer;

/**
 * Usefull for manual otp-ldap authenticator tests
 * @author P.Piernik
 *
 */
public class EmbeddedDirectoryServerRunner
{
	public static void main(String... args) throws Exception 
	{
		EmbeddedDirectoryServer embeddedDirectoryServer = new EmbeddedDirectoryServer();
		embeddedDirectoryServer.startEmbeddedServer();
		String hostname = embeddedDirectoryServer.getPlainConnection().getConnectedAddress();
		String port = embeddedDirectoryServer.getPlainConnection().getConnectedPort()+"";
		String sslHostname = embeddedDirectoryServer.getSSLConnection().getConnectedAddress();
		String sslPort = embeddedDirectoryServer.getSSLConnection().getConnectedPort()+"";
		
		
		System.out.println("Hostname: " + hostname);
		System.out.println("Port: " + port);
		System.out.println("SSL Hostname: " + sslHostname);
		System.out.println("SSL Port: " + sslPort);	
	}
}
