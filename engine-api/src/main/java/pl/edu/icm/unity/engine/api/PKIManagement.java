/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Provides access to PKI related stores: credentials, certificates and truststores (validators).
 * <p>
 * Currently it is read only and implementation is based on FS stored data. In future it will be enhanced
 * to support DB-stored data with possibility to add/remove contents.
 * <p>
 * Single exception are certificates. It is possible to managed (add/remove) them at runtime, however the 
 * current implementation is in memory only, i.e. all changes are lost after restart. Again this will be
 * refactored in future.
 * @author K. Benedyczak
 */
public interface PKIManagement
{
	/**
	 * @return set with available credential names
	 * @throws EngineException
	 */
	Set<String> getCredentialNames() throws EngineException;
	
	/**
	 * @param name 
	 * @return credential by name
	 * @throws EngineException
	 */
	X509Credential getCredential(String name) throws EngineException;
	
	/**
	 * @return set of available validators
	 * @throws EngineException
	 */
	Set<String> getValidatorNames() throws EngineException;
	
	/**
	 * @param name
	 * @return validator by name
	 * @throws EngineException
	 */
	X509CertChainValidatorExt getValidator(String name) throws EngineException; 
	
	/**
	 * @return method allows to quickly get {@link IAuthnAndTrustConfiguration} of the main server.
	 */
	IAuthnAndTrustConfiguration getMainAuthnAndTrust();
		
	/**
	 * @return set with available certificate names
	 * @throws EngineException
	 */
	Set<String> getAllCertificateNames() throws EngineException;
	
	/**
	 * @param name 
	 * @return certificate by name
	 * @throws EngineException
	 */
	NamedCertificate getCertificate(String name) throws EngineException;
	
	/**
	 * Adds a new volatile certificate
	 * @param name
	 * @param updated
	 * @throws EngineException
	 */
	void addVolatileCertificate(String name, X509Certificate updated) throws EngineException;

	/**
	 * 
	 * @return set with available volatile certificates 
	 * @throws EngineException
	 */
	List<NamedCertificate> getVolatileCertificates() throws EngineException;
	
	
	/**
	 * Adds a new persisted certificate
	 * @param toAdd
	 * @throws EngineException
	 */
	void addPersistedCertificate(NamedCertificate toAdd) throws EngineException;

	/**
	 * 
	 * @return set with available persisted certificates 
	 * @throws EngineException
	 */
	List<NamedCertificate> getPersistedCertificates() throws EngineException;

	/**
	 * Removes a given certificate
	 * @param toRemove
	 * @throws EngineException
	 */
	void removeCertificate(String toRemove) throws EngineException;

	
	/**
	 * Updates a given certificate
	 * @param toRemove
	 * @throws EngineException
	 */
	void updateCertificate(NamedCertificate toUpdate) throws EngineException;
	
	/**
	 * 
	 * @param config
	 */
	void loadCertificatesFromConfigFile();
	
}
