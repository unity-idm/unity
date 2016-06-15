/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvidersRegistry;
import pl.edu.icm.unity.engine.api.attributes.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Implements attributes operations.
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class AttributeTypeManagementImpl implements AttributeTypeManagement
{
	private AttributeSyntaxFactoriesRegistry attrValueTypesReg;
	private AttributeTypeDAO attributeTypeDAO;
	private AttributeDAO attributeDAO;
	private IdentityTypeDAO dbIdentities;
	private AttributeMetadataProvidersRegistry atMetaProvidersRegistry;
	private AuthorizationManager authz;


	@Autowired
	public AttributeTypeManagementImpl(AttributeSyntaxFactoriesRegistry attrValueTypesReg,
			AttributeTypeDAO attributeTypeDAO, AttributeDAO attributeDAO,
			IdentityTypeDAO dbIdentities,
			AttributeMetadataProvidersRegistry atMetaProvidersRegistry,
			AuthorizationManager authz)
	{
		this.attrValueTypesReg = attrValueTypesReg;
		this.attributeTypeDAO = attributeTypeDAO;
		this.attributeDAO = attributeDAO;
		this.dbIdentities = dbIdentities;
		this.atMetaProvidersRegistry = atMetaProvidersRegistry;
		this.authz = authz;
	}

	@Override
	public String[] getSupportedAttributeValueTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		Collection<AttributeValueSyntaxFactory<?>> all = attrValueTypesReg.getAll();
		String[] ret = new String[all.size()];
		Iterator<AttributeValueSyntaxFactory<?>> it = all.iterator();
		for (int i=0; it.hasNext(); i++)
			ret[i] = it.next().getId();
		return ret;
	}

	@Override
	@Transactional
	public void addAttributeType(AttributeType toAdd) throws EngineException
	{
		toAdd.validateInitialization();
		if (toAdd.getFlags() != 0)
			throw new IllegalAttributeTypeException("Custom attribute types must not have any flags set");
		authz.checkAuthorization(AuthzCapability.maintenance);
		Collection<AttributeType> existingAts = attributeTypeDAO.getAll();
		verifyATMetadata(toAdd, existingAts);
		attributeTypeDAO.create(toAdd);
	}

	@Override
	@Transactional
	public void updateAttributeType(AttributeType at) throws EngineException
	{
		at.validateInitialization();
		if (at.getFlags() != 0)
			throw new IllegalAttributeTypeException("Custom attribute types must not have any flags set");
		authz.checkAuthorization(AuthzCapability.maintenance);
		AttributeType atExisting = attributeTypeDAO.get(at.getName());
		if ((atExisting.getFlags() & AttributeType.TYPE_IMMUTABLE_FLAG) != 0)
		{
			updateImmutableAttributeType(at, atExisting);
			return;
		}
		Collection<AttributeType> existingAts = attributeTypeDAO.getAll();
		verifyATMetadata(at, existingAts);

		attributeTypeDAO.update(at);
		if (!at.getValueSyntax().equals(atExisting.getValueSyntax()))
			clearAttributeExtractionFromIdentities(at.getName());
	}

	/**
	 * Sets all user-controlled (mutable) elements of attribute type which has immutable type.
	 * @param at
	 * @param existing
	 */
	public static void setModifiableSettingsOfImmutableAT(AttributeType at, AttributeType existing)
	{
		existing.setDisplayedName(at.getDisplayedName());
		existing.setDescription(at.getDescription());
		existing.setSelfModificable(at.isSelfModificable());
	}
	
	/**
	 * Attribute types marked as immutable can have only displayed name, description, visibility and selfModifiable
	 * settings modified. Also metadata is not set on them.
	 * @param at
	 * @throws EngineException 
	 */
	private void updateImmutableAttributeType(AttributeType at, AttributeType existing) 
			throws EngineException
	{
		setModifiableSettingsOfImmutableAT(at, existing);
		attributeTypeDAO.update(existing);
	}
	
	private void verifyATMetadata(AttributeType at, Collection<AttributeType> existingAts) 
			throws IllegalAttributeTypeException
	{
		Map<String, String> meta = at.getMetadata();
		if (meta.isEmpty())
			return;
		Map<String, String> existing = new HashMap<String, String>();
		for (AttributeType eat: existingAts)
			if (!eat.getName().equals(at.getName()))
			{
				for (String eatm: eat.getMetadata().keySet())
					existing.put(eatm, eat.getName());
			}
		
		for (Map.Entry<String, String> metaE: meta.entrySet())
		{
			AttributeMetadataProvider provider = atMetaProvidersRegistry.getByName(metaE.getKey());
			if (provider.isSingleton())
			{
				if (existing.containsKey(metaE.getKey()) && !existing.get(
						metaE.getKey()).equals(at.getName()))
					throw new IllegalAttributeTypeException("The attribute type metadata " + 
							metaE.getKey() + " can be assigned to a single " +
							"attribute type only" +
							" and it is already assigned to attribute type " + 
							existing.get(metaE.getKey()));
			}
			
			provider.verify(metaE.getValue(), at);
		}
	}
	
	@Override
	@Transactional
	public void removeAttributeType(String id, boolean deleteInstances) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		AttributeType at = attributeTypeDAO.get(id);
		if ((at.getFlags() & (AttributeType.TYPE_IMMUTABLE_FLAG | 
				AttributeType.INSTANCES_IMMUTABLE_FLAG)) != 0)
			throw new IllegalAttributeTypeException("The attribute type with name " + id + 
					" can not be manually removed");
		if (!deleteInstances && !attributeDAO.getAttributes(id, null, null).isEmpty())
			throw new IllegalAttributeTypeException("The attribute type " + id + " has instances");
		
		attributeTypeDAO.delete(id);
		clearAttributeExtractionFromIdentities(id);
	}

	private void clearAttributeExtractionFromIdentities(String id)
	{
		Collection<IdentityType> identityTypes = dbIdentities.getAll();
		for (IdentityType idType: identityTypes)
		{
			Map<String, String> extractedMap = idType.getExtractedAttributes();
			Iterator<Map.Entry<String, String>> entries = extractedMap.entrySet().iterator();
			boolean updateIdType = false;
			while (entries.hasNext())
			{
				Map.Entry<String, String> extracted = entries.next();
				if (extracted.getValue().equals(id))
				{
					entries.remove();
					updateIdType = true;
				}
			}
			if (updateIdType)
			{
				dbIdentities.update(idType);
			}
		}
	}
	
	@Override
	@Transactional
	public Collection<AttributeType> getAttributeTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return attributeTypeDAO.getAll();
	}

	@Override
	@Transactional
	public Map<String, AttributeType> getAttributeTypesAsMap() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return attributeTypeDAO.getAllAsMap();
	}
}
