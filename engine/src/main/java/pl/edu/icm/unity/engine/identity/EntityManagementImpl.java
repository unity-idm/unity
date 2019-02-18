/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.msgtemplates.UserNotificationTemplateDef;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.attribute.AttributeClassUtil;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.credential.SystemAllCredentialRequirements;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.MergeConflictException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Implementation of identities management. Responsible for top level transaction handling,
 * proper error logging and authorization.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class EntityManagementImpl implements EntityManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER,	EntityManagementImpl.class);
	private IdentityTypeDAO idTypeDAO;
	private IdentityTypeHelper idTypeHelper;
	private IdentityDAO idDAO;
	private EntityDAO entityDAO;
	private GroupDAO groupDAO;
	private AttributeTypeDAO attributeTypeDAO;
	private MembershipDAO membershipDAO;
	private EntityCredentialsHelper credentialsHelper;
	private GroupHelper groupHelper;
	private SheduledOperationHelper scheduledOperationHelper;
	private AttributesHelper attributesHelper;
	private IdentityHelper identityHelper;
	private EntityResolver idResolver;
	private InternalAuthorizationManager authz;
	private IdentityTypesRegistry idTypesRegistry;
	private EmailConfirmationManager confirmationManager;
	private AttributeClassUtil acUtil;
	private TransactionalRunner tx;
	private UnityServerConfiguration cfg;
	private NotificationProducer notificationProducer;
	
	@Autowired
	public EntityManagementImpl(IdentityTypeDAO idTypeDAO, IdentityTypeHelper idTypeHelper,
			IdentityDAO idDAO, EntityDAO entityDAO, GroupDAO groupDAO,
			AttributeTypeDAO attributeTypeDAO, MembershipDAO membershipDAO,
			EntityCredentialsHelper credentialsHelper, GroupHelper groupHelper,
			SheduledOperationHelper scheduledOperationHelper,
			AttributesHelper attributesHelper, IdentityHelper identityHelper,
			EntityResolver idResolver, InternalAuthorizationManager authz,
			IdentityTypesRegistry idTypesRegistry,
			EmailConfirmationManager confirmationManager, AttributeClassUtil acUtil,
			TransactionalRunner tx,
			UnityServerConfiguration cfg, NotificationProducer notificationProducer)
	{
		this.idTypeDAO = idTypeDAO;
		this.idTypeHelper = idTypeHelper;
		this.idDAO = idDAO;
		this.entityDAO = entityDAO;
		this.groupDAO = groupDAO;
		this.attributeTypeDAO = attributeTypeDAO;
		this.membershipDAO = membershipDAO;
		this.credentialsHelper = credentialsHelper;
		this.groupHelper = groupHelper;
		this.scheduledOperationHelper = scheduledOperationHelper;
		this.attributesHelper = attributesHelper;
		this.identityHelper = identityHelper;
		this.idResolver = idResolver;
		this.authz = authz;
		this.idTypesRegistry = idTypesRegistry;
		this.confirmationManager = confirmationManager;
		this.acUtil = acUtil;
		this.tx = tx;
		this.cfg = cfg;
		this.notificationProducer = notificationProducer;
	}

	@Override
	public Identity addEntity(IdentityParam toAdd, String credReqId, EntityState initialState,
			boolean extractAttributes) throws EngineException
	{
		return addEntity(toAdd, credReqId, initialState, extractAttributes, null);
	}
	
	@Override
	public Identity addEntity(IdentityParam toAdd, EntityState initialState,
			boolean extractAttributes) throws EngineException
	{
		return addEntity(toAdd, SystemAllCredentialRequirements.NAME, initialState, extractAttributes, null);
	}

	@Override
	public Identity addEntity(IdentityParam toAdd, String credReqId,
			EntityState initialState, boolean extractAttributes,
			List<Attribute> attributesP) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.identityModify);
		List<Attribute> attributes = attributesP == null ? Collections.emptyList() : attributesP;
		
		Identity ret = tx.runInTransactionRetThrowing(() -> {
			return identityHelper.addEntity(toAdd, credReqId, initialState, 
					extractAttributes, attributes, true);
		}); 
		return ret;
	}
	
	@Override
	public Identity addEntity(IdentityParam toAdd,
			EntityState initialState, boolean extractAttributes,
			List<Attribute> attributesP) throws EngineException
	{
		return addEntity(toAdd, SystemAllCredentialRequirements.NAME, initialState, extractAttributes, attributesP);
	}
	
	private static class IdentityWithAuthzInfo
	{
		private Identity identity;
		private boolean fullAuthz;

		IdentityWithAuthzInfo(Identity identity, boolean fullAuthz)
		{
			this.identity = identity;
			this.fullAuthz = fullAuthz;
		}
	}
	
	@Override
	public Identity addIdentity(IdentityParam toAdd, EntityParam parentEntity, boolean extractAttributes)
			throws EngineException
	{
		IdentityWithAuthzInfo ret = tx.runInTransactionRetThrowing(() -> {
			long entityId = idResolver.getEntityId(parentEntity);
			IdentityType identityType = idTypeDAO.get(toAdd.getTypeId());
			
			boolean fullAuthz = authorizeIdentityChange(entityId, Sets.newHashSet(toAdd), 
					identityType.isSelfModificable());
			if (!fullAuthz)
				toAdd.setConfirmationInfo(new ConfirmationInfo(false));
			List<Identity> identities = idDAO.getByEntity(entityId);
			if (!fullAuthz && getIdentityCountOfType(identities, identityType.getIdentityTypeProvider()) 
					>= identityType.getMaxInstances())
				throw new SchemaConsistencyException("Can not add another identity of this type as "
						+ "the configured maximum number of instances was reached.");
			Identity toCreate = idTypeHelper.upcastIdentityParam(toAdd, entityId);
			idDAO.create(new StoredIdentity(toCreate));
			if (extractAttributes && fullAuthz)
				identityHelper.addExtractedAttributes(toCreate);
			return new IdentityWithAuthzInfo(toCreate, fullAuthz);
		});
		
		if (!ret.fullAuthz)
		{
			tx.runInTransactionThrowing(() -> {
				confirmationManager.sendVerificationNoTx(new EntityParam(
					ret.identity.getEntityId()), ret.identity, false);
			});
		}
		return ret.identity;
	}

	private int getIdentityCountOfType(List<Identity> identities, String type)
	{
		int ret = 0;
		for (Identity id: identities)
			if (id.getTypeId().equals(type))
				ret++;
		return ret;
	}

	private void checkVerifiedMinCountForRemoval(List<Identity> identities, IdentityTaV toRemove,
			IdentityType type) throws SchemaConsistencyException, IllegalIdentityValueException
	{
		IdentityTypeDefinition typeDef = idTypeHelper.getTypeDefinition(type);
		if (!typeDef.isEmailVerifiable())
			return;
		int existing = 0;
		String comparableValue = typeDef.getComparableValue(toRemove.getValue(), 
				toRemove.getRealm(), toRemove.getTarget());
		for (Identity id: identities)
		{
			if (id.getTypeId().equals(toRemove.getTypeId()))
			{
				if (comparableValue.equals(id.getComparableValue()) && !id.isConfirmed())
					return;
				if (id.isConfirmed())
					existing++;
			}
		}
		if (existing <= type.getMinVerifiedInstances())
			throw new SchemaConsistencyException("Can not remove the verified identity as "
					+ "the configured minimum number of verified instances was reached.");
	}
	
	/**
	 * Checks if identityModify capability is granted. If it is only in self access context the
	 * confirmation status is forced to be unconfirmed.
	 * @throws AuthorizationException
	 * @returns true if full authZ is set or false if limited only. 
	 */
	private boolean authorizeIdentityChange(long entityId, Collection<? extends IdentityParam> toAdd, 
			boolean selfModifiable) throws AuthorizationException
	{
		boolean fullAuthz = authz.getCapabilities(false, "/").contains(AuthzCapability.identityModify);
		if (!fullAuthz)
		{
			authz.checkAuthorization(selfModifiable && authz.isSelf(entityId), 
					AuthzCapability.identityModify);
			for (IdentityParam idP: toAdd)
				idP.setConfirmationInfo(new ConfirmationInfo());
			return false;
		}
		return true;
	}
	
	@Transactional
	@Override
	public void removeIdentity(IdentityTaV toRemove) throws EngineException
	{
		long entityId = idResolver.getEntityId(new EntityParam(toRemove));
		IdentityType identityType = idTypeDAO.get(toRemove.getTypeId());
		List<Identity> identities = idDAO.getByEntity(entityId);
		String type = identityType.getIdentityTypeProvider();
		boolean fullAuthz = authorizeIdentityChange(entityId, new ArrayList<>(), 
				identityType.isSelfModificable());

		if (identities.size() == 1)
			throw new SchemaConsistencyException("Can not remove the last identity, "
					+ "it is only possible to perform the full removeal by deleting "
					+ "its entity now.");
		
		if (!fullAuthz)
		{
			if (getIdentityCountOfType(identities, type) <= identityType.getMinInstances())
				throw new SchemaConsistencyException("Can not remove the identity as "
						+ "the configured minimum number of instances was reached.");
			checkVerifiedMinCountForRemoval(identities, toRemove, identityType);
		}
		IdentityTypeDefinition typeDefinition = idTypeHelper.getTypeDefinition(identityType);
		String cmpValue = typeDefinition.getComparableValue(toRemove.getValue(), toRemove.getRealm(), 
				toRemove.getTarget()); 
		idDAO.delete(StoredIdentity.toInDBIdentityValue(identityType.getName(), cmpValue));
	}

	@Override
	@Transactional
	public void updateIdentity(IdentityTaV original, IdentityParam updated) throws EngineException
	{
		if (!Objects.equals(updated.getTypeId(), original.getTypeId()))
			throw new IllegalArgumentException("Identity type can not be changed");
		authz.checkAuthorization(AuthzCapability.identityModify);
		
		IdentityType identityType = idTypeDAO.get(original.getTypeId());
		IdentityTypeDefinition typeDefinition = idTypeHelper.getTypeDefinition(identityType);
		String cmpValue = typeDefinition.getComparableValue(original.getValue(), original.getRealm(), 
				original.getTarget());
		String updatedCmpValue = typeDefinition.getComparableValue(updated.getValue(), updated.getRealm(), 
				updated.getTarget());
		if (!Objects.equals(cmpValue, updatedCmpValue))
			throw new IllegalArgumentException("Identity change can not effect in comparable "
					+ "value change of existing identity");
		
		String inDBKey = StoredIdentity.toInDBIdentityValue(identityType.getName(), cmpValue);
		long entityId = idResolver.getEntityId(new EntityParam(original));
		Identity updatedFull = idTypeHelper.upcastIdentityParam(updated, entityId);
		idDAO.updateByName(inDBKey, new StoredIdentity(updatedFull));
	}

	@Override
	@Transactional
	public void setIdentities(EntityParam entity, Collection<String> updatedTypes,
			Collection<? extends IdentityParam> newIdentities) throws EngineException
	{
		List<IdentityWithAuthzInfo> ret = tx.runInTransactionRetThrowing(() -> {
			entity.validateInitialization();
			ensureNoDynamicIdentityType(updatedTypes);
			ensureIdentitiesAreOfSpecifiedTypes(updatedTypes, newIdentities);

			long entityId = idResolver.getEntityId(entity);
			Map<String, IdentityType> identityTypes = idTypeDAO.getAllAsMap();
			boolean selfModifiable = areAllTypesSelfModifiable(updatedTypes, identityTypes);
			boolean fullAuthz = authorizeIdentityChange(entityId, newIdentities, selfModifiable);
			List<Identity> identities = idDAO.getByEntity(entityId);
			Map<String, Set<Identity>> currentIdentitiesByType = 
					getCurrentIdentitiesByType(updatedTypes, identities);
			Map<String, Set<IdentityParam>> requestedIdentitiesByType = 
					getRequestedIdentitiesByType(updatedTypes, newIdentities);
			List<IdentityWithAuthzInfo> created = new ArrayList<>();
			for (String type: updatedTypes)
			{
				List<Identity> createdOfType = setIdentitiesOfType(
						identityTypes.get(type), entityId, currentIdentitiesByType.get(type), 
						requestedIdentitiesByType.get(type), fullAuthz);
				createdOfType.stream()
					.map(id -> new IdentityWithAuthzInfo(id, fullAuthz))
					.forEach(arg -> created.add(arg));
			}
			return created;
		});
		for (IdentityWithAuthzInfo id: ret)
			if (!id.fullAuthz)
			{
				tx.runInTransactionThrowing(() -> {
					confirmationManager.sendVerificationNoTx(new EntityParam(
							id.identity.getEntityId()), id.identity, false);
				});
			}
	}

	private List<Identity> setIdentitiesOfType(IdentityType type, long entityId, 
			Set<Identity> existing, Set<IdentityParam> requested, boolean fullAuthz) throws EngineException
	{
		Set<IdentityParam> toRemove = substractIdentitySets(type, existing, requested);
		Set<IdentityParam> toAdd = substractIdentitySets(type, requested, existing);
		verifyLimitsOfIdentities(type, existing, requested, toRemove, toAdd, fullAuthz);
		List<Identity> created = new ArrayList<>();
		for (IdentityParam add: toAdd)
		{
			if (!fullAuthz)
				add.setConfirmationInfo(new ConfirmationInfo(false));
			created.add(identityHelper.insertIdentity(add, entityId, false));
		}
		for (IdentityParam remove: toRemove)
		{
			String comparableValue = idTypeHelper.upcastIdentityParam(remove, entityId).
					getComparableValue();
			idDAO.delete(StoredIdentity.toInDBIdentityValue(remove.getTypeId(), comparableValue));
		}
		return created;
	}
	
	private void verifyLimitsOfIdentities(IdentityType type, Set<Identity> existing, Set<IdentityParam> requested, 
			Set<IdentityParam> toRemove, Set<IdentityParam> toAdd, boolean fullAuthz) 
			throws SchemaConsistencyException
	{
		if (fullAuthz)
			return;
		
		int newCount = requested.size();
		if (newCount < type.getMinInstances() && existing.size() >= type.getMaxInstances())
			throw new SchemaConsistencyException("The operation can not be completed as in effect "
					+ "the configured minimum number of instances would be violated "
					+ "for the identity type " + type.getIdentityTypeProvider());
		if (newCount > type.getMaxInstances() && existing.size() <= type.getMaxInstances())
			throw new SchemaConsistencyException("The operation can not be completed as in effect "
					+ "the configured maximum number of instances would be violated "
					+ "for the identity type " + type.getIdentityTypeProvider());
		IdentityTypeDefinition typeDefinition = idTypeHelper.getTypeDefinition(type);
		if (typeDefinition.isEmailVerifiable())
		{
			int newConfirmedCount = 0;
			int currentConfirmedCount = 0;
			for (IdentityParam ni: existing)
				if (ni.isConfirmed())
					newConfirmedCount++;
			currentConfirmedCount = newConfirmedCount;
			for (IdentityParam ni: toRemove)
				if (ni.isConfirmed())
					newConfirmedCount--;
			for (IdentityParam ni: toAdd)
				if (ni.isConfirmed())
					newConfirmedCount++;
			
			if (newConfirmedCount < type.getMinVerifiedInstances() && 
					currentConfirmedCount >= type.getMinVerifiedInstances())
				throw new SchemaConsistencyException("The operation can not be completed as in effect "
					+ "the configured minimum number of confirmed identities would be violated "
					+ "for the identity type " + type.getIdentityTypeProvider());
		}
		
	}
	
	private Set<IdentityParam> substractIdentitySets(IdentityType type, Set<? extends IdentityParam> from, 
			Set<? extends IdentityParam> what) throws IllegalIdentityValueException
	{
		IdentityTypeDefinition typeDefinition = idTypeHelper.getTypeDefinition(type);
		Set<IdentityParam> ret = new HashSet<>();
		for (IdentityParam idParam: from)
		{
			String idParamCmp = typeDefinition.getComparableValue(idParam.getValue(), 
					idParam.getRealm(), idParam.getTarget());
			boolean found = false;
			for (IdentityParam removed: what)
			{
				String removedCmp = typeDefinition.getComparableValue(removed.getValue(), 
						removed.getRealm(), removed.getTarget());
				
				if (idParamCmp.equals(removedCmp))
				{
					found = true;
					break;
				}
			}
			if (!found)
				ret.add(idParam);
		}
		return ret;
	}
	

	private Map<String, Set<IdentityParam>> getRequestedIdentitiesByType(Collection<String> updatedTypes, 
			Collection<? extends IdentityParam> identities)
	{
		Map<String, Set<IdentityParam>> ret = new HashMap<>();
		for (String type: updatedTypes)
			ret.put(type, new HashSet<>());
		for (IdentityParam id: identities)
		{
			ret.get(id.getTypeId()).add(id);
		}
		return ret;
	}
	
	private Map<String, Set<Identity>> getCurrentIdentitiesByType(Collection<String> updatedTypes, 
			List<Identity> identities)
	{
		Map<String, Set<Identity>> ret = new HashMap<>();
		for (String type: updatedTypes)
			ret.put(type, new HashSet<>());
		for (Identity id: identities)
		{
			if (!updatedTypes.contains(id.getTypeId()))
				continue;
			ret.get(id.getTypeId()).add(id);
		}
		return ret;
	}
	
	private boolean areAllTypesSelfModifiable(Collection<String> updatedTypes, 
			Map<String, IdentityType> identityTypes)
	{
		for (String type: updatedTypes)
		{
			IdentityType idType = identityTypes.get(type);
			if (!idType.isSelfModificable())
			{
				return false;
			}
		}
		return true;
	}
	
	private void ensureIdentitiesAreOfSpecifiedTypes(Collection<String> updatedTypes,
			Collection<? extends IdentityParam> newIdentities) throws IllegalIdentityValueException
	{
		for (IdentityParam id: newIdentities)
		{
			if (!updatedTypes.contains(id.getTypeId()))
				throw new IllegalArgumentException("All new identities must be "
						+ "of types specified as the first argument");
			
		}		
	}
	
	private void ensureNoDynamicIdentityType(Collection<String> updatedTypes) 
			throws IllegalTypeException, IllegalIdentityValueException
	{
		for (String type: updatedTypes)
		{
			IdentityTypeDefinition idType = idTypesRegistry.getByName(type);
			if (idType.isDynamic())
				throw new IllegalIdentityValueException("Identity type " + type + 
						" is dynamic and can not be manually set");

		}		
	}
	
	
	@Transactional
	@Override
	public void resetIdentity(EntityParam toReset, String typeIdToReset,
			String realm, String target) throws EngineException
	{
		toReset.validateInitialization();
		if (typeIdToReset == null)
			throw new IllegalIdentityValueException("Identity type can not be null");
		IdentityTypeDefinition idType = idTypesRegistry.getByName(typeIdToReset);
		if (!idType.isDynamic())
			throw new IllegalIdentityValueException("Identity type " + typeIdToReset + 
					" is not dynamic and can not be reset");
		long entityId = idResolver.getEntityId(toReset);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
		resetIdentityForEntity(entityId, typeIdToReset, realm, target);
	}

	
	@Transactional
	@Override
	public void removeEntity(EntityParam toRemove) throws EngineException
	{
		toRemove.validateInitialization();
		long entityId = idResolver.getEntityId(toRemove);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
		sendNotification(entityId, cfg.getValue(UnityServerConfiguration.ACCOUNT_REMOVED_NOTIFICATION));
		entityDAO.deleteByKey(entityId);
	}
	
	@Override
	@Transactional
	public void setEntityStatus(EntityParam toChange, EntityState status)
			throws EngineException
	{
		toChange.validateInitialization();
		if (status == EntityState.onlyLoginPermitted)
			throw new IllegalArgumentException("The new entity status 'only login permitted' "
					+ "can be only set as a side effect of scheduling an account "
					+ "removal with a grace period.");
		long entityId = idResolver.getEntityId(toChange);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
		EntityInformation current = entityDAO.getByKey(entityId);
		if (current.getEntityState() == status)
			return;
		
		String notificationToSend = null;
		if (current.getEntityState() == EntityState.valid 
				&& (status == EntityState.authenticationDisabled 
				|| status == EntityState.disabled))
			notificationToSend = cfg.getValue(UnityServerConfiguration.ACCOUNT_DISABLED_NOTIFICATION);
		if (status == EntityState.valid 
				&& (current.getEntityState() == EntityState.authenticationDisabled 
				|| current.getEntityState() == EntityState.disabled))
			notificationToSend = cfg.getValue(UnityServerConfiguration.ACCOUNT_ACTIVATED_NOTIFICATION);
		
		current.setEntityState(status);
		entityDAO.updateByKey(entityId, current);
		sendNotification(entityId, notificationToSend);
	}

	@Transactional
	@Override
	public Entity getEntity(EntityParam entity) throws EngineException
	{
		return getEntity(entity, null, true, "/");
	}
	
	@Transactional
	@Override
	public Entity getEntityNoContext(EntityParam entity, String group) throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		Entity ret;
		try
		{
			authz.checkAuthorization(authz.isSelf(entityId), group, AuthzCapability.readHidden);
			List<Identity> identities = idDAO.getByEntity(entityId);
			ret = assembleEntity(entityId, identities);
		} catch (AuthorizationException e)
		{
			ret = resolveEntityBasic(entityId, null, false, group);
		}
		return ret;
	}
	
	@Transactional
	@Override
	public Entity getEntity(EntityParam entity, String target, boolean allowCreate, String group)
			throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		return resolveEntityBasic(entityId, target, allowCreate, group);
	}

	@Transactional
	@Override
	public String getEntityLabel(EntityParam entity) throws EngineException
	{
		entity.validateInitialization();
		AttributeExt attribute = attributesHelper.getAttributeByMetadata(entity, "/", 
				EntityNameMetadataProvider.NAME);
		if (attribute == null)
			return null;
		List<?> values = attribute.getValues();
		if (values.isEmpty())
			return null;
		return values.get(0).toString();
	}
	
	
	/**
	 * Checks if read cap is set and resolved the entity: identities and credential with respect to the
	 * given target.
	 * @param entityId
	 * @param target
	 * @param allowCreate
	 * @param sqlMap
	 * @return
	 * @throws EngineException
	 */
	private Entity resolveEntityBasic(long entityId, String target, boolean allowCreate, String group) 
			throws EngineException
	{
		authz.checkAuthorization(authz.isSelf(entityId), group, AuthzCapability.read);
		List<Identity> identities = getIdentitiesForEntity(entityId, target, allowCreate); 
		return assembleEntity(entityId, identities);
	}
	
	/**
	 * assembles the final entity by adding the credential and state info.
	 * @param entityId
	 * @param identities
	 * @param sqlMap
	 * @return
	 * @throws EngineException
	 */
	private Entity assembleEntity(long entityId, List<Identity> identities) throws EngineException
	{
		CredentialInfo credInfo = credentialsHelper.getCredentialInfo(entityId);
		EntityInformation theState = entityDAO.getByKey(entityId);
		return new Entity(identities, theState, credInfo);
	}
	
	@Override
	@Transactional
	public Map<String, GroupMembership> getGroups(EntityParam entity) throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.read);
		return getEntityMembershipAsMap(entityId);
	}

	private Map<String, GroupMembership> getEntityMembershipAsMap(long entityId) throws EngineException
	{
		List<GroupMembership> entityMembership = membershipDAO.getEntityMembership(entityId);
		Map<String, GroupMembership> ret = new HashMap<>();
		for (GroupMembership memberhip: entityMembership)
			ret.put(memberhip.getGroup(), memberhip);
		return ret;
	}

	@Override
	@Transactional
	public Collection<Group> getGroupsForPresentation(EntityParam entity)
			throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.read);
		return getEntityGroupsLimited(entityId);
	}

	private Set<Group> getEntityGroupsLimited(long entityId)
	{
		List<GroupMembership> entityMembership = membershipDAO.getEntityMembership(entityId);
		Map<String, Group> allAsMap = groupDAO.getAllAsMap();
		Set<Group> ret = new HashSet<Group>();
		for (GroupMembership groupMem: entityMembership)
		{
			Group group = allAsMap.get(groupMem.getGroup());
			group.setAttributesClasses(Collections.emptySet());
			group.setAttributeStatements(new AttributeStatement[0]);
			ret.add(group);
		}
		return ret;
	}
	
	@Override
	@Transactional
	public void scheduleEntityChange(EntityParam toChange, Date changeTime,
			EntityScheduledOperation operation) throws EngineException
	{
		toChange.validateInitialization();
		long entityId = idResolver.getEntityId(toChange);

		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);

		if (operation != null && changeTime != null &&
				changeTime.getTime() <= System.currentTimeMillis())
			scheduledOperationHelper.performScheduledOperation(entityId, operation);
		else
			scheduledOperationHelper.setScheduledOperationByAdmin(entityId, changeTime, operation);
	}

	@Override
	@Transactional
	public void scheduleRemovalByUser(EntityParam toChange, Date changeTime)
			throws EngineException
	{
		toChange.validateInitialization();
		long entityId = idResolver.getEntityId(toChange);

		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.attributeModify);

		if (changeTime.getTime() <= System.currentTimeMillis())
			scheduledOperationHelper.performScheduledOperation(entityId, 
					EntityScheduledOperation.REMOVE);
		else
			scheduledOperationHelper.setScheduledRemovalByUser(entityId, changeTime);
	}
	
	@Override
	@Transactional
	public void mergeEntities(EntityParam target, EntityParam merged, boolean safeMode) throws EngineException
	{
		target.validateInitialization();
		merged.validateInitialization();
		authz.checkAuthorization(AuthzCapability.identityModify);
		long mergedId = idResolver.getEntityId(merged);
		long targetId = idResolver.getEntityId(target);

		mergeIdentities(mergedId, targetId, safeMode);

		mergeMemberships(mergedId, targetId);
		mergeAttributes(mergedId, targetId, safeMode);
		entityDAO.deleteByKey(mergedId);
	}

	private void mergeAttributes(long mergedId, long targetId, boolean safeMode) throws EngineException
	{
		Collection<AttributeExt> newAttributes = 
				attributesHelper.getAllAttributes(mergedId, null, false, null);
		Collection<AttributeExt> targetAttributes = 
				attributesHelper.getAllAttributes(targetId, null, false, null);
		Set<String> targetAttributesSet = new HashSet<>();
		for (AttributeExt attribute: targetAttributes)
			targetAttributesSet.add(getAttrKey(attribute));
		
		Map<String, AttributeType> attributeTypes = attributeTypeDAO.getAllAsMap();
		
		for (AttributeExt attribute: newAttributes)
		{
			AttributeType type = attributeTypes.get(attribute.getName());
			
			if (attribute.getName().startsWith(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX))
			{
				copyCredentialAttribute(attribute, targetAttributesSet, targetId, safeMode);
				continue;
			}
			
			if (type.isInstanceImmutable())
				continue;
			
			if (targetAttributesSet.contains(getAttrKey(attribute)))
			{
				if (safeMode)
					throw new MergeConflictException("Attribute " + attribute.getName() + 
							" in group " + attribute.getGroupPath() + " is in conflict");
				continue;
			}
			
			AttributeClassHelper acHelper = acUtil.getACHelper(targetId, 
					attribute.getGroupPath());
			if (!acHelper.isAllowed(attribute.getName()))
			{
				if (safeMode)
					throw new MergeConflictException("Attribute " + attribute.getName() + 
							" in group " + attribute.getGroupPath() + 
							" is in conflict with target's entity attribute classes");
				continue;
			}
			
			attributesHelper.createAttribute(attribute, targetId);
		}
	}
	
	private void copyCredentialAttribute(AttributeExt attribute, Set<String> targetAttributesSet,
			long targetId, boolean safeMode) throws EngineException
	{
		if (targetAttributesSet.contains(getAttrKey(attribute)))
		{
			if (safeMode)
				throw new MergeConflictException("Credential " + attribute.getName().
						substring(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX.length()) + 
						" is in conflict");
			return;
		}
		attributesHelper.createAttribute(attribute, targetId);
	}
	
	private String getAttrKey(Attribute a)
	{
		return a.getGroupPath() + "///" + a.getName();
	}
	
	private void mergeMemberships(long mergedId, long targetId) throws EngineException
	{
		Map<String, GroupMembership> currentGroups = getEntityMembershipAsMap(targetId);
		Map<String, GroupMembership> mergedGroups = getEntityMembershipAsMap(mergedId);
		EntityParam ep = new EntityParam(targetId);
		mergedGroups.keySet().removeAll(currentGroups.keySet());
		Map<String, GroupMembership> toAdd = new TreeMap<>(mergedGroups);
		for (Map.Entry<String, GroupMembership> groupM: toAdd.entrySet())
			groupHelper.addMemberFromParent(groupM.getKey(), ep, groupM.getValue().getRemoteIdp(), 
					groupM.getValue().getTranslationProfile(), 
					groupM.getValue().getCreationTs());
	}
	
	private void mergeIdentities(long mergedId, long targetId, boolean safeMode) 
			throws EngineException
	{
		List<Identity> mergedIdentities = idDAO.getByEntity(mergedId);
		List<Identity> targetIdentities = idDAO.getByEntity(targetId);
		Set<String> existingIdTypesPerTarget = new HashSet<>();
		for (Identity id: targetIdentities)
			existingIdTypesPerTarget.add(getIdTypeKeyWithTargetAndRealm(id));
		
		for (Identity id: mergedIdentities)
		{
			IdentityTypeDefinition identityTypeProvider = idTypesRegistry.getByName(id.getTypeId());
			if (!identityTypeProvider.isRemovable())
				continue;
			
			if (identityTypeProvider.isDynamic() &&
				existingIdTypesPerTarget.contains(getIdTypeKeyWithTargetAndRealm(id)))
			{
				if (safeMode)
					throw new MergeConflictException("There is conflicting dynamic identity: " +
							id);
				continue;
			}
			id.setEntityId(targetId);
			idDAO.update(new StoredIdentity(id));
		}
	}
	
	private String getIdTypeKeyWithTargetAndRealm(Identity id)
	{
		return id.getTypeId() + "__" + id.getTarget() + "__" + id.getRealm();
	}
	
	private void resetIdentityForEntity(long entityId, String type, String realm, String target) 
			throws IllegalTypeException
	{
		List<StoredIdentity> all = idDAO.getByEntityFull(entityId);
		IdentityType resolvedType = idTypeDAO.get(type);
		IdentityTypeDefinition idTypeImpl = idTypeHelper.getTypeDefinition(resolvedType); 
		if (!idTypeImpl.isDynamic())
			throw new IllegalTypeException("Reset is possible for "
					+ "dynamic identity types only");

		for (StoredIdentity sid: all)
		{
			Identity id = sid.getIdentity();
			if (id.getTypeId().equals(type))
			{
				if (realm != null && !realm.equals(id.getRealm()))
					continue;
				if (target != null && !target.equals(id.getTarget()))
					continue;
				idDAO.delete(sid.getName());
			}
		}
	}
	
	private List<Identity> getIdentitiesForEntity(long entityId, String target, boolean allowCreate) 
			throws IllegalTypeException
	{
		List<Identity> all = idDAO.getByEntity(entityId);
		List<Identity> ret = new ArrayList<>(all.size() + 4);
		for (Identity id: all)
			if (id.getTarget() == null || id.getTarget().equals(target))
				ret.add(id);
		Set<String> presentTypes = new HashSet<>();
		for (Identity id: ret)
			presentTypes.add(id.getTypeId());
		if (allowCreate)
			identityHelper.addDynamic(entityId, presentTypes, ret, target);
		return ret;
	}

	private void sendNotification(long entityId, String templateId)
	{
		if (templateId == null)
			return;
		try
		{
			EntityParam recipient = new EntityParam(entityId);
			String entityName = getEntityLabel(recipient);
			Map<String, String> params = new HashMap<>();
			params.put(UserNotificationTemplateDef.USER, entityName == null ? "" : entityName);
			notificationProducer.sendNotification(recipient, templateId, 
					params, cfg.getDefaultLocale().toString(), null, false);
		} catch (Exception e)
		{
			log.warn("Unable to send notification using template " + templateId 
					+ " to entity " + entityId, e);
		}
	}
}
