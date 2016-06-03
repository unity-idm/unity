/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.engine.api.internal.IdentityResolver;
import pl.edu.icm.unity.types.DescribedObject;

/**
 * Implementations allow for verification of the provided credential. It is assumed that credential is 
 * provided via {@link CredentialExchange} interfaces. The actual interaction might be arbitrary complex.
 * <p>
 * Implementations must be thread safe. 
 * 
 * @author K. Benedyczak
 */
public interface CredentialVerificator extends CredentialExchange, DescribedObject
{
	void setIdentityResolver(IdentityResolver identityResolver);
	
	/**
	 * Instance name is the configured name of the authenticator to which this credential verificator belongs.
	 * @param name
	 */
	void setInstanceName(String name);
	
	/**
	 * @return serialized configuration of this verificator
	 */
	ObjectNode getSerializedConfiguration();
	
	/**
	 * sets configuration of this verificator
	 * @param json
	 */
	void setSerializedConfiguration(ObjectNode json);
}
