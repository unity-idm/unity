/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.security.Security;

import javax.net.ssl.KeyManagerFactory;

import org.apache.directory.server.ldap.LdapServer;

import eu.emi.security.authn.x509.X509Credential;

/**
 * Minimal extension of the Apachee {@link LdapServer}, allowing us to use Unity credential. 
 * Original LdapServer forces us to load JKS from disk.  
 * 
 * @author K. Benedyczak
 */
class UnityLdapServer extends LdapServer
{
	private X509Credential credential;
	private KeyManagerFactory unityKeyManagerFactory;
	
	UnityLdapServer(X509Credential credential)
	{
		this.credential = credential;
	}

	@Override
	public void loadKeyStore() throws Exception
	{
		String algorithm = Security.getProperty( "ssl.KeyManagerFactory.algorithm" );
		if ( algorithm == null )
			algorithm = KeyManagerFactory.getDefaultAlgorithm();
		unityKeyManagerFactory = KeyManagerFactory.getInstance(algorithm);
		unityKeyManagerFactory.init(credential.getKeyStore(), credential.getKeyPassword());
	}
	
	@Override
	public KeyManagerFactory getKeyManagerFactory()
	{
		return unityKeyManagerFactory;
	}
}
