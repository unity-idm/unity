/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.types.DescribedObject;
import pl.edu.icm.unity.types.authn.CredentialType;


/**
 * Produces {@link LocalCredentialHandler}s
 * @author K. Benedyczak
 */
public interface LocalCredentialHandlerFactory extends DescribedObject
{
	/**
	 * @return credential type supported by handlers produced by this factory
	 */
	public CredentialType getCredentialType(); 
	
	
	public LocalCredentialHandler newInstance();
}
