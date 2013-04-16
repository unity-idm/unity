/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.security.cert.X509Certificate;

import eu.emi.security.authn.x509.impl.X500NameUtils;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.authn.AbstractLocalVerificator;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

/**
 * Trivial verificator of certificates. It is assumed that the certificate was previously authenticated.
 * Therefore the only operation is resolving of the certificate user.
 * <p>
 * There is no local credential associated with this verificator. Therefore it always returns 
 * the correct credential state. Storing credential of this type makes no sense, but works (empty string is stored).
 * 
 * @author K. Benedyczak
 */
public class CertificateVerificator extends AbstractLocalVerificator implements CertificateExchange
{ 	
	private static final String[] IDENTITY_TYPES = {X500Identity.ID};

	public CertificateVerificator(String name, String description)
	{
		super(name, description, PasswordExchange.ID);
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
	}

	@Override
	public String prepareCredential(String rawCredential, String currentCredential)
			throws IllegalCredentialException
	{
		return "";
	}

	@Override
	public LocalCredentialState checkCredentialState(String currentCredential)
	{
		return LocalCredentialState.correct;
	}

	@Override
	public AuthenticatedEntity checkCertificate(X509Certificate[] chain)
			throws IllegalIdentityValueException
	{
		String identity = chain[0].getSubjectX500Principal().getName();
		EntityWithCredential resolved = identityResolver.resolveIdentity(identity, 
				IDENTITY_TYPES, credentialName);
		return new AuthenticatedEntity(resolved.getEntityId(), resolved.getLocalAuthnState(),
				X500NameUtils.getReadableForm(identity));
	}
}





