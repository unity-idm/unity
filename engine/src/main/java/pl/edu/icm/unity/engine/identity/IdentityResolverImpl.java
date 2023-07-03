/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
import pl.edu.icm.unity.engine.credential.CredentialReqRepository;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;

/**
 * Default implementation of the identity resolver. Immutable.
 * @author K. Benedyczak
 */
@Component
public class IdentityResolverImpl implements IdentityResolver
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, IdentityResolverImpl.class);
	private static final String[] HUMAN_READABLE_IDENTITY_TYPES = {UsernameIdentity.ID, EmailIdentity.ID, X500Identity.ID};

	private IdentityTypeHelper idTypeHelper;
	private IdentityHelper idHelper;
	private EntityDAO dbIdentities;
	private EntityResolver dbResolver;
	private AttributesHelper attributeHelper;
	private CredentialReqRepository credReqRepository;
	private final EntityManagement entityManagement;
	
	@Autowired
	public IdentityResolverImpl(IdentityTypeHelper idTypeHelper, EntityDAO dbIdentities,
			EntityResolver dbResolver, AttributesHelper attributeHelper,
			CredentialReqRepository credReqRepository, IdentityHelper idHelper,
			@Qualifier("insecure") EntityManagement entityManagement)
	{
		this.idTypeHelper = idTypeHelper;
		this.dbIdentities = dbIdentities;
		this.dbResolver = dbResolver;
		this.attributeHelper = attributeHelper;
		this.credReqRepository = credReqRepository;
		this.entityManagement = entityManagement;
		this.idHelper = idHelper;
	}

	@Override
	@Transactional
	public EntityWithCredential resolveIdentity(String identity, String[] identityTypes,
			String credentialName) throws EngineException
	{
		long entityId = getIdentity(identity, identityTypes, null, null, true).getEntityId();
		return resolveEntity(entityId, credentialName);
	}

	@Override
	@Transactional
	public EntityWithCredential resolveEntity(long entityId, String credentialName) throws EngineException
	{
		if (!isEntityEnabled(entityId))
			throw new IllegalIdentityValueException("Authentication is disabled for this entity");		
		EntityWithCredential ret = new EntityWithCredential();
		if (credentialName != null)
		{
			CredentialRequirements credentialRequirements = resolveCredentialRequirements(
					entityId);
			if (credentialRequirements.getRequiredCredentials().contains(credentialName))
			{
				AttributeExt credAttribute = attributeHelper.getEffectiveAttributeOneGroup(
						entityId, "/", 
						CredentialAttributeTypeProvider.CREDENTIAL_PREFIX+credentialName);
				if (credAttribute != null)
					ret.setCredentialValue((String)credAttribute.getValues().get(0));
			}
			ret.setCredentialName(credentialName);
		}
		ret.setEntityId(entityId);
		return ret;
	}
	
	private CredentialRequirements resolveCredentialRequirements(long entityId) 
			throws EngineException
	{
		AttributeExt credReqAttr = attributeHelper.getEffectiveAttributeOneGroup(
				entityId, "/", 
				CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS);
		String cr = (String) credReqAttr.getValues().get(0);
		return credReqRepository.get(cr);
	}
	
	@Override
	@Transactional
	public long resolveIdentity(String identity, String[] identityTypes, String target, String realm) 
			throws IllegalIdentityValueException
	{
		return getIdentity(identity, identityTypes, target, realm, false).getEntityId();
	}
	
	@Override
	@Transactional
	public EntityWithCredential resolveSubject(AuthenticationSubject subject, String[] identityTypes, String credentialName)
			throws IllegalIdentityValueException, IllegalTypeException, IllegalGroupValueException, EngineException
	{
		return subject.entityId == null ? 
				resolveIdentity(subject.identity, identityTypes, credentialName) : 
				resolveEntity(subject.entityId, credentialName);
	}
	
	private Identity getIdentity(String identity, String[] identityTypes, String target, String realm, 
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
					return found;
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
	
	private void assertEntityEnabled(long entity) throws IllegalIdentityValueException
	{
		if (!isEntityEnabled(entity))
			throw new IllegalIdentityValueException("Authentication is disabled for this entity");
	}
	@Override
	public String getDisplayedUserName(EntityParam entity) throws EngineException
	{
		String label = entityManagement.getEntityLabel(entity);
		if (label != null)
			return label;
		
		Entity resolved = entityManagement.getEntity(entity);
		Map<String, Identity> identitiesMap = resolved.getIdentities().stream()
				.collect(Collectors.toMap(id -> id.getTypeId(), id -> id));

		for (String master: HUMAN_READABLE_IDENTITY_TYPES)
			if (identitiesMap.containsKey(master))
				return identitiesMap.get(master).getValue();
		return null;
	}

	@Transactional
	@Override
	public List<Identity> getIdentitiesForEntity(EntityParam entity) throws IllegalIdentityValueException
	{
		return idHelper.getIdentitiesForEntity(dbResolver.getEntityId(entity), null);
	}

	@Transactional
	@Override
	public Identity insertIdentity(IdentityParam toAdd, EntityParam entity)
			throws IllegalIdentityValueException
	{
		return idHelper.insertIdentity(toAdd, dbResolver.getEntityId(entity), true);
	}

	@Transactional
	@Override
	public Identity resolveSubject(AuthenticationSubject subject, String identityType)
			throws IllegalIdentityValueException, IllegalTypeException, IllegalGroupValueException, EngineException
	{
		if (subject.entityId == null)
		{
			Identity id = getIdentity(subject.identity, new String[] { identityType }, null, null, false);
			assertEntityEnabled(id.getEntityId());
			return id;
		} else
		{
			List<Identity> identitiesWithSearchedType = getIdentitiesForEntity(new EntityParam(subject.entityId))
					.stream().filter(i -> i.getTypeId().equals(identityType)).collect(Collectors.toList());
			if (identitiesWithSearchedType.isEmpty())
				throw new IllegalIdentityValueException(
						"Entity " + subject.entityId + "  doesn't have identity of type " + identityType);
			if (identitiesWithSearchedType.size() > 1)
				throw new IllegalIdentityValueException(
						"Entity " + subject.entityId + " has more than one identity of type " + identityType);
			Identity id = identitiesWithSearchedType.get(0);
			assertEntityEnabled(id.getEntityId());
			return id;
		}
	}
}
