/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.types.authn.AuthenticatorInstance;


/**
 * Representation of an authenticator, which is a composition of {@link CredentialRetrieval} and
 * {@link CredentialVerificator}, configured.
 * <p>
 * Authenticator can be local or remote, depending on the associated verificator type (local or remote).
 * <p>
 * Local authenticator is special as it has an associated local credential. Its verificator uses the associated 
 * credential's configuration internally, but it is not advertised to the outside world, via the
 * {@link AuthenticatorInstance} interface.
 * <p>
 * @author K. Benedyczak
 */
public interface Authenticator
{

	/**
	 * Updates the current configuration of the authenticator. For local
	 * verificators the verificator configuration is only set for the
	 * underlying verificator, it is not exposed in the instanceDescription.
	 * 
	 * @param rConfiguration
	 * @param vConfiguration
	 */
	void updateConfiguration(String rConfiguration, String vConfiguration,
			String localCredential);

	/**
	 * Get authenticator retrieval
	 * 
	 * @return
	 */
	CredentialRetrieval getRetrieval();

	/**
	 * Get authenticator instance
	 * 
	 * @return
	 */
	AuthenticatorInstance getAuthenticatorInstance();

	/**
	 * Set authenticator revision
	 * 
	 * @param revision
	 *                to set
	 */
	void setRevision(long revision);

	/**
	 * Get authenticator revision
	 * 
	 * @return authenticator revision
	 */
	long getRevision();
}
