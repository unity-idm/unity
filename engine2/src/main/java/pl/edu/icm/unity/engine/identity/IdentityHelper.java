/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.types.basic.Attribute;
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
	private IdentityTypeHelper idTypeHelper;
	private AttributesHelper attributeHelper;
	private GroupHelper groupHelper;
	private EntityCredentialsHelper credentialHelper;

	
	@Autowired
	public IdentityHelper(IdentityTypesRegistry idTypesRegistry, EntityDAO entityDAO,
			IdentityDAO identityDAO, IdentityTypeHelper idTypeHelper,
			AttributesHelper attributeHelper, GroupHelper groupHelper,
			EntityCredentialsHelper credentialHelper)
	{
		this.idTypesRegistry = idTypesRegistry;
		this.entityDAO = entityDAO;
		this.identityDAO = identityDAO;
		this.idTypeHelper = idTypeHelper;
		this.attributeHelper = attributeHelper;
		this.groupHelper = groupHelper;
		this.credentialHelper = credentialHelper;
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
		return ret;
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
			extracted.setGroupPath("/");
			try
			{
				attributeHelper.createAttribute(extracted, entityId);
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
		Identity identity = idTypeHelper.upcastIdentityParam(toAdd);
		identity.setCreationTs(ts);
		identity.setUpdateTs(ts);
		identityDAO.create(identity);
		
		return identity;
	}
}
