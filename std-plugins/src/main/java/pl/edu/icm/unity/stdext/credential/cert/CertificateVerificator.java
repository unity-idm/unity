/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.cert;

import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.DenyReason;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.identity.X500Identity;

/**
 * Trivial verificator of certificates. It is assumed that the certificate was previously authenticated.
 * Therefore the only operation is resolving of the certificate user.
 * <p>
 * There is no local credential associated with this verificator. Therefore it always returns 
 * the correct credential state. Storing credential of this type makes no sense, but works (empty string is stored).
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class CertificateVerificator extends AbstractLocalVerificator implements CertificateExchange
{ 	
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, CertificateVerificator.class);
	private static final String[] IDENTITY_TYPES = {X500Identity.ID};
	public static final String NAME = "certificate";
	public static final String DESC = "Verifies certificates";

	private EntityManagement idMan;
	
	@Autowired
	public CertificateVerificator(@Qualifier("insecure") EntityManagement idMan)
	{
		super(NAME, DESC, CertificateExchange.ID, false);
		this.idMan = idMan;
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
	public String prepareCredential(String rawCredential, String currentCredential, boolean verify)
			throws IllegalCredentialException
	{
		return "";
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential)
	{
		return new CredentialPublicInformation(LocalCredentialState.correct, "");
	}

	@Override
	public AuthenticationResult checkCertificate(X509Certificate[] chain, 
			String formForUnknown, boolean enableAssociation, AuthenticationTriggeringContext triggeringContext)
	{
		String identity = chain[0].getSubjectX500Principal().getName();
		try
		{
			EntityWithCredential resolved = identityResolver.resolveIdentity(identity, IDENTITY_TYPES, credentialName);
			AuthenticatedEntity entity = new AuthenticatedEntity(resolved.getEntityId(),
					X500NameUtils.getReadableForm(identity), null);
			return LocalAuthenticationResult.successful(entity, getAuthenticationMethod());
		} catch (IllegalIdentityValueException e)
		{
			log.warn("Checking certificate failed", e);
			return LocalAuthenticationResult.failed(e, DenyReason.undefinedCredential);
		}

		catch (Exception e)
		{
			log.warn("Checking certificate failed", e);
			return LocalAuthenticationResult.failed(e);
		}
	}

	@Override
	public String invalidate(String currentCredential)
	{
		throw new IllegalStateException("This credential doesn't support invalidation");
	}

	private boolean checkX500Id(EntityParam entity) throws EngineException
	{
		Entity entityRes = idMan.getEntity(entity);
		if (entityRes == null)
			return false;	
		List<Identity> ids = entityRes.getIdentities();

		for (Identity id : ids)
		{
			if (id.getTypeId().equals(X500Identity.ID))
				return true;
		}

		return false;
	}
	
	
	@Override
	public boolean isCredentialSet(EntityParam entity)
			throws EngineException
	{
		return checkX500Id(entity);
	}

	@Override
	public boolean isCredentialDefinitionChagneOutdatingCredentials(String newCredentialDefinition)
	{
		return false;
	}
	
	@Override
	public AuthenticationMethod getAuthenticationMethod()
	{
		return AuthenticationMethod.swk;
	}
	
	@Component
	public static class Factory extends AbstractLocalCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<CertificateVerificator> factory)
		{
			super(NAME, DESC, false, factory);
		}
	}

}





