/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Set;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Provides access to PKI related stores: credentials and truststores (validators).
 * <p>
 * Currently it is read only and implementation is based on FS stored data. In future it will be enhanced
 * to support DB-stored data with possibility to add/remove contents.
 * @author K. Benedyczak
 */
public interface PKIManagement
{
	/**
	 * @return set with available credential names
	 * @throws EngineException
	 */
	public Set<String> getCredentialNames() throws EngineException;
	
	/**
	 * @param name 
	 * @return credential by name
	 * @throws EngineException
	 */
	public X509Credential getCredential(String name) throws EngineException;
	
	/**
	 * @return set of available validators
	 * @throws EngineException
	 */
	public Set<String> getValidatorNames() throws EngineException;
	
	/**
	 * @param name
	 * @return validator by name
	 * @throws EngineException
	 */
	public X509CertChainValidatorExt getValidator(String name) throws EngineException; 
	
	/**
	 * @return method allows to quickly get {@link IAuthnAndTrustConfiguration} of the main server.
	 */
	public IAuthnAndTrustConfiguration getMainAuthnAndTrust();
}
