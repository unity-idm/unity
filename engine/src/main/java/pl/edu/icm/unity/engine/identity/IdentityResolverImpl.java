/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
import pl.edu.icm.unity.engine.credential.CredentialReqRepository;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Default implementation of the identity resolver. Immutable.
 * @author K. Benedyczak
 */
@Component
public class IdentityResolverImpl implements IdentityResolver
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, IdentityResolverImpl.class);
	private IdentityTypeHelper idTypeHelper;
	private EntityDAO dbIdentities;
	private EntityResolver dbResolver;
	private AttributesHelper attributeHelper;
	private CredentialReqRepository credReqRepository;
	
	
	@Autowired
	public IdentityResolverImpl(IdentityTypeHelper idTypeHelper, EntityDAO dbIdentities,
			EntityResolver dbResolver, AttributesHelper attributeHelper,
			CredentialReqRepository credReqRepository)
	{
		this.idTypeHelper = idTypeHelper;
		this.dbIdentities = dbIdentities;
		this.dbResolver = dbResolver;
		this.attributeHelper = attributeHelper;
		this.credReqRepository = credReqRepository;
	}

	@Override
	@Transactional
	public EntityWithCredential resolveIdentity(String identity, String[] identityTypes,
			String credentialName) throws EngineException
	{
		long entityId = getEntity(identity, identityTypes, null, null, true);
		if (!isEntityEnabled(entityId))
			throw new IllegalIdentityValueException("Authentication is disabled for this entity");		
		EntityWithCredential ret = new EntityWithCredential();
		if (credentialName != null)
		{
			CredentialRequirements credentialRequirements = resolveCredentialRequirements(
					entityId);
			if (credentialRequirements.getRequiredCredentials().contains(credentialName))
			{
				Collection<AttributeExt> credAttributes = attributeHelper.getAllAttributes(
						entityId, "/", true, 
						CredentialAttributeTypeProvider.CREDENTIAL_PREFIX+credentialName);
				if (credAttributes.size() > 0)
				{
					Attribute a = credAttributes.iterator().next();
					ret.setCredentialValue((String)a.getValues().get(0));
				}
			}
			ret.setCredentialName(credentialName);
		}
		ret.setEntityId(entityId);
		return ret;
	}
	
	private CredentialRequirements resolveCredentialRequirements(long entityId) 
			throws EngineException
	{
		Collection<AttributeExt> credReqAttrs = attributeHelper.getAllAttributes(
				entityId, "/", true,
				CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS);
		Attribute cra = credReqAttrs.iterator().next();
		String cr = (String) cra.getValues().get(0);
		return credReqRepository.get(cr);
	}
	
	@Override
	@Transactional
	public long resolveIdentity(String identity, String[] identityTypes, String target, String realm) 
			throws IllegalIdentityValueException
	{
		return getEntity(identity, identityTypes, target, realm, false);
	}
	
	private long getEntity(String identity, String[] identityTypes, String target, String realm, 
			boolean requireConfirmed) 
			throws IllegalIdentityValueException
	{
		for (String identityType: identityTypes)
		{
			IdentityTaV tav = new IdentityTaV(identityType, identity, target, realm);
			try
			{
				Identity found = dbResolver.getFullIdentity(tav);
				if (!requireConfirmed || isIdentityConfirmed(found))
				{
					return found.getEntityId();
				} else
				{
					log.debug("Identity " + identity + " was found but is not confirmed, "
							+ "not returning it for loggin in");
				}
			} catch (Exception e)
			{
				log.trace("Got exception searching identity, likely it simply does not exist", e);
			}
		}
		throw new IllegalIdentityValueException("No identity with value " + identity);
	}
	
	private boolean isIdentityConfirmed(Identity identity)
	{
		IdentityTypeDefinition typeDefinition = idTypeHelper.getTypeDefinition(identity.getTypeId());
		if (!typeDefinition.isEmailVerifiable())
			return true;
		return identity.isConfirmed();
	}

	@Override
	public boolean isEntityEnabled(long entity)
	{
		EntityState entityState = dbIdentities.getByKey(entity).getEntityState();
		return entityState != EntityState.authenticationDisabled && entityState != EntityState.disabled;
	}
	
}
