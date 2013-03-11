/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

/**
 * Verificator of local credentials. The only difference is that such verificators must have 
 * local credential name set.
 * @author K. Benedyczak
 */
public interface LocalCredentialVerificator extends CredentialVerificator
{
	public String getCredentialName();
	public void setCredentialName(String credential);
}
