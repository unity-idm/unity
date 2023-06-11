/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import java.util.List;

import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector;


/**
 * Representation of an authenticator instance, which is a composition of {@link CredentialRetrieval} and
 * {@link CredentialVerificator}, configured. It is based on authenticator definition which is created by user,
 * but is a blueprint for instances with potentially different retrievals and thus different supported bindings.
 * <p>
 * Authenticator instance can be local or remote, depending on the associated verificator type (local or remote).
 * <p>
 * Local authenticator is special as it has an associated local credential. Its verificator uses the associated 
 * credential's configuration internally, but it is not advertised to the outside world, via the
 * {@link AuthenticatorInstanceMetadata} interface.
 * <p>
 * @author K. Benedyczak
 */
public interface AuthenticatorInstance
{

	/**
	 * Updates the current configuration of the authenticator. For local
	 * verificators the verificator configuration is only set for the
	 * underlying verificator, it is not exposed in the instanceDescription.
	 */
	void updateConfiguration(String vConfiguration, String retrievalConfiguration, String localCredential);

	CredentialRetrieval getRetrieval();

	AuthenticatorInstanceMetadata getMetadata();

	void setRevision(long revision);

	long getRevision();
	
	CredentialVerificator getCredentialVerificator();
	
	List<AuthenticationOptionsSelector> getAuthnOptionSelectors();
	
	List<IdPInfo> extractIdPs();
}
