/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvidersRegistry;
import pl.edu.icm.unity.engine.api.attributes.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Implements attributes operations.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class AttributeTypeManagementImpl implements AttributeTypeManagement
{
	private AttributeSyntaxFactoriesRegistry attrValueTypesReg;
	private AttributeTypeDAO attributeTypeDAO;
	private AttributeDAO attributeDAO;
	private IdentityTypeDAO dbIdentities;
	private AttributeMetadataProvidersRegistry atMetaProvidersRegistry;
	private InternalAuthorizationManager authz;
	private AttributeTypeHelper atHelper;
	private AttributesHelper aHelper;


	@Autowired
	public AttributeTypeManagementImpl(AttributeSyntaxFactoriesRegistry attrValueTypesReg,
			AttributeTypeDAO attributeTypeDAO, AttributeDAO attributeDAO,
			IdentityTypeDAO dbIdentities,
			AttributeMetadataProvidersRegistry atMetaProvidersRegistry,
			InternalAuthorizationManager authz, AttributeTypeHelper atHelper,
			AttributesHelper aHelper)
	{
		this.attrValueTypesReg = attrValueTypesReg;
		this.attributeTypeDAO = attributeTypeDAO;
		this.attributeDAO = attributeDAO;
		this.dbIdentities = dbIdentities;
		this.atMetaProvidersRegistry = atMetaProvidersRegistry;
		this.authz = authz;
		this.atHelper = atHelper;
		this.aHelper = aHelper;
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
		atHelper.validateSyntax(toAdd);
		if (toAdd.getFlags() != 0)
			throw new IllegalAttributeTypeException("Custom attribute types must not have any flags set");
		authz.checkAuthorization(AuthzCapability.maintenance);
		atHelper.setDefaultSyntaxConfiguration(toAdd);
		Collection<AttributeType> existingAts = attributeTypeDAO.getAll();
		verifyATMetadata(toAdd, existingAts);
		attributeTypeDAO.create(toAdd);
	}

	
	@Override
	@Transactional
	public void updateAttributeType(AttributeType at) throws EngineException
	{
		at.validateInitialization();
		atHelper.validateSyntax(at);
		if (at.getFlags() != 0)
			throw new IllegalAttributeTypeException("Custom attribute types must not have any flags set");
		authz.checkAuthorization(AuthzCapability.maintenance);
		AttributeType atExisting = attributeTypeDAO.get(at.getName());
		if (((atExisting.getFlags() & AttributeType.TYPE_IMMUTABLE_FLAG) != 0) &&
				((atExisting.getFlags() & AttributeType.INSTANCES_IMMUTABLE_FLAG) != 0))
			throw new IllegalAttributeTypeException("Attribute type which has immutable type "
					+ "and values can not be modified in any way.");
		
		if ((atExisting.getFlags() & AttributeType.TYPE_IMMUTABLE_FLAG) != 0)
		{
			updateImmutableAttributeType(at, atExisting);
			return;
		}
		Collection<AttributeType> existingAts = attributeTypeDAO.getAll();
		verifyATMetadata(at, existingAts);
		verifyAttributesConsistencyWithUpdatedType(at);
		
		attributeTypeDAO.update(at);
		if (!at.getValueSyntax().equals(atExisting.getValueSyntax()))
			clearAttributeExtractionFromIdentities(at.getName());
	}

	private void verifyAttributesConsistencyWithUpdatedType(AttributeType at) throws IllegalAttributeTypeException
	{
		List<StoredAttribute> allAttributesOfType = attributeDAO.getAttributes(at.getName(), null, null);
		for (StoredAttribute sa: allAttributesOfType)
		{
			try
			{
				aHelper.validate(sa.getAttribute(), at);
			} catch (Exception e)
			{
				throw new IllegalAttributeTypeException("Can't update the attribute type as at least " +
						"one attribute instance will be in conflict with the new type. " +
						"The conflicting attribute which was found: " + sa.getAttribute(), e);
			}
		}
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

	@Override
	@Transactional
	public AttributeType getAttributeType(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return attributeTypeDAO.get(name);
	}
}
