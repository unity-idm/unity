/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.credential.SystemAllCredentialRequirements;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Shared code related to handling entities and identities
 * @author K. Benedyczak
 */
@Component
public class IdentityHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, IdentityHelper.class);
	
	private IdentityTypesRegistry idTypesRegistry;
	private EntityDAO entityDAO;
	private IdentityDAO identityDAO;
	private AttributeDAO attributeDAO;
	private IdentityTypeHelper idTypeHelper;
	private AttributesHelper attributeHelper;
	private GroupHelper groupHelper;
	private EntityCredentialsHelper credentialHelper;

	
	@Autowired
	public IdentityHelper(IdentityTypesRegistry idTypesRegistry, EntityDAO entityDAO,
			IdentityDAO identityDAO, AttributeDAO attributeDAO, IdentityTypeHelper idTypeHelper,
			AttributesHelper attributeHelper, GroupHelper groupHelper,
			EntityCredentialsHelper credentialHelper)
	{
		this.idTypesRegistry = idTypesRegistry;
		this.entityDAO = entityDAO;
		this.identityDAO = identityDAO;
		this.attributeDAO = attributeDAO;
		this.idTypeHelper = idTypeHelper;
		this.attributeHelper = attributeHelper;
		this.groupHelper = groupHelper;
		this.credentialHelper = credentialHelper;
	}

	/**
	 * As {@link #getEntitiesBySimpleAttribute(String, String, Set)} with in the '/' group.
	 * @param attribute
	 * @param values
	 * @return
	 * @throws IllegalTypeException
	 * @throws IllegalGroupValueException
	 */
	public Set<Long> getEntitiesByRootAttribute(String attribute, Set<String> values) 
			throws IllegalTypeException, IllegalGroupValueException
	{
		return getEntitiesBySimpleAttribute("/", attribute, values);
	}
	
	/**
	 * Returns all entities that have a given attribute defined in the given group, and that the attribute
	 * value is in the given set of values. This method considers only the first value
	 * of checked attribute. Also this method will work only for simple attributes, which has unencoded String
	 * values. 
	 * 
	 * @param groupPath
	 * @param attributeTypeName
	 * @param values
	 * @return
	 * @throws IllegalTypeException
	 * @throws IllegalGroupValueException
	 */
	public Set<Long> getEntitiesBySimpleAttribute(String groupPath, String attributeTypeName, 
			Set<String> values) 
			throws IllegalTypeException, IllegalGroupValueException
	{
		List<StoredAttribute> attributes = attributeDAO.getAttributes(attributeTypeName, null, groupPath);
		Set<Long> ret = new HashSet<Long>();
		for (StoredAttribute sa: attributes)
		{
			AttributeExt attribute = sa.getAttribute();
			if (attribute.getValues().isEmpty())
				continue;
			if (values.contains(attribute.getValues().get(0)))
				ret.add(sa.getEntityId());
		}
		return ret;
	}
	
	/**
	 * It is assumed that the attribute is mapped to string.
	 * Returned are all entities which have value of the attribute among values of the given attribute
	 * in any group.
	 * 
	 * @param attributeTypeName
	 * @param value
	 * @return
	 * @throws IllegalTypeException
	 * @throws IllegalGroupValueException
	 */
	public Set<Long> getEntitiesWithStringAttribute(String attributeTypeName, String value) 
			throws IllegalTypeException, IllegalGroupValueException
	{
		List<StoredAttribute> attributes = attributeDAO.getAttributes(attributeTypeName, null, null);
		Set<Long> ret = new HashSet<Long>();
		for (StoredAttribute sa: attributes)
		{
			AttributeExt attribute = sa.getAttribute();
			if (attribute.getValues().isEmpty())
				continue;
			if (attribute.getValues().contains(value))
				ret.add(sa.getEntityId());
		}
		return ret;
	}

	/**
	 * Adds an entity with all the complicated logic around it. Does not perform authorization and DB 
	 * transaction set up: pure business logic.
	 * Entity is created, initial identity is added to it. Membership in the root group is created,
	 * credential requirement is set as well as initial and extracted attributes.
	 * @param toAdd
	 * @param credReqId
	 * @param initialState
	 * @param extractAttributes
	 * @param attributes
	 * @param sqlMap
	 * @throws EngineException 
	 */
	public Identity addEntity(IdentityParam toAdd, String credReqId, EntityState initialState, 
			boolean extractAttributes, List<Attribute> attributes, boolean honorInitialConfirmation) 
					throws EngineException
	{
		attributeHelper.checkGroupAttributeClassesConsistency(attributes, "/");

		EntityInformation entity = new EntityInformation();
		entity.setEntityState(initialState);
		long entityId = entityDAO.create(entity);

		Identity ret = insertIdentity(toAdd, entityId, false);

		groupHelper.addMemberFromParent("/", new EntityParam(entityId), null, null, new Date());
		
		credentialHelper.setEntityCredentialRequirements(entityId, credReqId);
		
		attributeHelper.addAttributesList(attributes, entityId, honorInitialConfirmation);
		
		if (extractAttributes)
			addExtractedAttributes(ret);
		
		addDynamic(entityId, Sets.newHashSet(ret.getTypeId()), new ArrayList<>(), null);
		
		return ret;
	}
	
	/**
	 * As {@link #addEntity(IdentityParam, String, EntityState, boolean, List, boolean)} with default credential requirements.
	 */
	public Identity addEntity(IdentityParam toAdd, EntityState initialState, 
			boolean extractAttributes, List<Attribute> attributes, boolean honorInitialConfirmation) 
					throws EngineException
	{
		return addEntity(toAdd, SystemAllCredentialRequirements.NAME, initialState, extractAttributes, 
				attributes, honorInitialConfirmation);
	}
	
	/**
	 * Extracts attributes from {@link Identity} and adds them for the entity in the '/' group. 
	 * @param from
	 */
	public void addExtractedAttributes(Identity from)
	{
		IdentityType idType = idTypeHelper.getIdentityType(from.getTypeId());
		IdentityTypeDefinition typeProvider = idTypeHelper.getTypeDefinition(idType);
		Map<String, String> toExtract = idType.getExtractedAttributes();
		List<Attribute> extractedList = typeProvider.extractAttributes(from.getValue(), toExtract);
		if (extractedList == null)
			return;
		long entityId = from.getEntityId();
		for (Attribute extracted: extractedList)
		{
			if (RoleAttributeTypeProvider.AUTHORIZATION_ROLE.equals(extracted.getName()))
			{
				log.warn("This should never happen: skipping role attribute addition extracted from identity");
				continue;
			}
			extracted.setGroupPath("/");
			try
			{
				attributeHelper.addAttribute(entityId, extracted, false, true);
			} catch (Exception e)
			{
				log.warn("Can not add extracted attribute " + extracted.getName() 
						+ " for entity " + entityId + ": " + e.toString(), e);
			}
		}
	}
	
	/**
	 * Creates a given identity in database. Can create entity if needed. 
	 * @param toAdd
	 * @param entityId
	 * @param allowSystem
	 * @return
	 * @throws IllegalIdentityValueException
	 * @throws IllegalTypeException
	 * @throws WrongArgumentException
	 */
	public Identity insertIdentity(IdentityParam toAdd, long entityId, boolean allowSystem) 
			throws IllegalIdentityValueException, IllegalTypeException, WrongArgumentException
	{
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(toAdd.getTypeId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		if (idTypeDef.isDynamic() && !allowSystem)
			throw new IllegalIdentityValueException("The identity type " + idTypeDef.getId() + 
					" is created automatically and can not be added manually");
		idTypeDef.validate(toAdd.getValue());
		if (idTypeDef.isTargeted())
		{
			if (toAdd.getTarget() == null || toAdd.getRealm() == null)
				throw new IllegalIdentityValueException("The identity target and realm are required "
						+ "for identity type " + idTypeDef.getId());
		} else
		{
			if (toAdd.getTarget() != null || toAdd.getRealm() != null)
				throw new IllegalIdentityValueException("The identity target and realm must not be set "
						+ "for identity type " + idTypeDef.getId());
		}

		Date ts = new Date();
		Identity identity = idTypeHelper.upcastIdentityParam(toAdd, entityId);
		identity.setCreationTs(ts);
		identity.setUpdateTs(ts);
		try
		{
			identityDAO.create(new StoredIdentity(identity));
		} catch (Exception e)
		{
			throw new IllegalIdentityValueException("Can not add identity " + toAdd, e);
		}
		
		return identity;
	}
	
	
	/**
	 * Creates dynamic identities which are currently absent for the entity.
	 */
	void addDynamic(long entityId, Set<String> presentTypes, List<Identity> ret, String target)
	{
		for (IdentityTypeDefinition idType: idTypesRegistry.getDynamic())
		{
			if (presentTypes.contains(idType.getId()))
				continue;
			if (idType.isTargeted() && target == null)
				continue;
			Identity added = createDynamicIdentity(idType, entityId, target);
			if (added != null)
				ret.add(added);
		}
	}
	
	private Identity createDynamicIdentity(IdentityTypeDefinition idTypeImpl, long entityId, String target)
	{
		String realm = InvocationContext.safeGetRealm();
		if (idTypeImpl.isTargeted() && (realm == null || target == null))
			return null;
		Identity newId = idTypeImpl.createNewIdentity(realm, target, entityId);
		identityDAO.create(new StoredIdentity(newId));
		return newId;
	}
}
