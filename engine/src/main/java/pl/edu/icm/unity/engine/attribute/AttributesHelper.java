/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvidersRegistry;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;

/**
 * Attributes and ACs related operations, intended for reuse between other classes.
 * No operation in this interface performs any authorization.
 * @author K. Benedyczak
 */
@Component
public class AttributesHelper
{
	private AttributeMetadataProvidersRegistry atMetaProvidersRegistry;
	private AttributeClassDB acDB;
	private AttributeClassUtil acUtil;
	private IdentityDAO identityDAO;
	private EntityDAO entityDAO;
	private EntityResolver idResolver;
	private AttributeTypeDAO attributeTypeDAO;
	private AttributeDAO attributeDAO;
	private MembershipDAO membershipDAO;
	private AttributeStatementProcessor statementsHelper;
	private AttributeTypeHelper atHelper;
	private GroupDAO groupDAO;
	
	
	
	@Autowired
	public AttributesHelper(AttributeMetadataProvidersRegistry atMetaProvidersRegistry,
			AttributeClassDB acDB, IdentityDAO identityDAO,
			EntityDAO entityDAO, EntityResolver idResolver,
			AttributeTypeDAO attributeTypeDAO, AttributeDAO attributeDAO,
			MembershipDAO membershipDAO, AttributeStatementProcessor statementsHelper,
			AttributeTypeHelper atHelper, AttributeClassUtil acUtil,
			GroupDAO groupDAO)
	{
		this.atMetaProvidersRegistry = atMetaProvidersRegistry;
		this.acDB = acDB;
		this.identityDAO = identityDAO;
		this.entityDAO = entityDAO;
		this.idResolver = idResolver;
		this.attributeTypeDAO = attributeTypeDAO;
		this.attributeDAO = attributeDAO;
		this.membershipDAO = membershipDAO;
		this.statementsHelper = statementsHelper;
		this.atHelper = atHelper;
		this.acUtil = acUtil;
		this.groupDAO = groupDAO;
	}

	/**
	 * See {@link #getAllAttributes(long, String, String, SqlSession)}, the only difference is that the result
	 * is returned in a map indexed with groups (1st key) and attribute names (submap key).
	 * @param entityId
	 * @param groupPath
	 * @param attributeTypeName
	 * @return
	 * @throws EngineException 
	 * @throws WrongArgumentException 
	 */
	public Map<String, Map<String, AttributeExt>> getAllAttributesAsMap(long entityId, String groupPath, 
			boolean effective, String attributeTypeName) 
			throws EngineException
	{
		Map<String, Map<String, AttributeExt>> directAttributesByGroup = getAllEntityAttributesMap(entityId);
		if (!effective)
		{
			filterMap(directAttributesByGroup, groupPath, attributeTypeName);
			return directAttributesByGroup;
		}
		Set<String> allGroups = membershipDAO.getEntityMembershipSimple(entityId);
		List<String> groups = groupPath == null ? new ArrayList<>(allGroups) : Lists.newArrayList(groupPath);
		Map<String, Map<String, AttributeExt>> ret = new HashMap<>();
		
		Map<String, AttributesClass> allClasses = acDB.getAllAsMap();
		
		List<Identity> identities = identityDAO.getByEntity(entityId);
		for (String group: groups)
		{
			Map<String, AttributeExt> inGroup = statementsHelper.getEffectiveAttributes(identities, 
					group, attributeTypeName, allGroups, directAttributesByGroup, allClasses,
					groupDAO::get, attributeTypeDAO::get);
			ret.put(group, inGroup);
		}
		return ret;
	}

	public Map<String, AttributeExt> getAllAttributesAsMapOneGroup(long entityId, String groupPath) 
			throws EngineException
	{
		if (groupPath == null)
			throw new IllegalArgumentException("For this method group must be specified");
		Map<String, Map<String, AttributeExt>> asMap = getAllAttributesAsMap(entityId, groupPath, true, null);
		return asMap.get(groupPath);
	}

	/**
	 * @param entityId
	 * @param atMapper
	 * @param gMapper
	 * @return map indexed with groups. Values are maps of all attributes in all groups, indexed with their names.
	 * @throws IllegalTypeException
	 * @throws IllegalGroupValueException
	 */
	private Map<String, Map<String, AttributeExt>> getAllEntityAttributesMap(long entityId) 
			throws IllegalTypeException, IllegalGroupValueException
	{
		List<StoredAttribute> attributes = attributeDAO.getAttributes(null, entityId, null);
		Map<String, Map<String, AttributeExt>> ret = new HashMap<>();
		for (StoredAttribute attribute: attributes)
		{
			Map<String, AttributeExt> attrsInGroup = ret.get(attribute.getAttribute().getGroupPath());
			if (attrsInGroup == null)
			{
				attrsInGroup = new HashMap<>();
				ret.put(attribute.getAttribute().getGroupPath(), attrsInGroup);
			}
			attrsInGroup.put(attribute.getAttribute().getName(), attribute.getAttribute());
		}
		return ret;
	}
	
	private void filterMap(Map<String, Map<String, AttributeExt>> directAttributesByGroup,
			String groupPath, String attributeTypeName)
	{
		if (groupPath != null)
		{
			Map<String, AttributeExt> v = directAttributesByGroup.get(groupPath); 
			directAttributesByGroup.clear();
			if (v != null)
				directAttributesByGroup.put(groupPath, v);
		}
		
		if (attributeTypeName != null)
		{
			for (Map<String, AttributeExt> e: directAttributesByGroup.values())
			{
				AttributeExt at = e.get(attributeTypeName);
				e.clear();
				if (at != null)
					e.put(attributeTypeName, at);
			}
		}
	}
	

	/**
	 * Returns {@link AttributeType} which has the given metadata set. The metadata used as parameter must be
	 * singleton, i.e. it is guaranteed that there is at maximum only one type with it.
	 * 
	 * @param metadataId
	 * @return
	 * @throws EngineException
	 */
	public AttributeType getAttributeTypeWithSingeltonMetadata(String metadataId)
			throws EngineException
	{
		AttributeMetadataProvider provider = atMetaProvidersRegistry.getByName(metadataId);
		if (!provider.isSingleton())
			throw new IllegalArgumentException("Metadata for this call must be singleton.");
		Collection<AttributeType> existingAts = attributeTypeDAO.getAll();
		AttributeType ret = null;
		for (AttributeType at: existingAts)
			if (at.getMetadata().containsKey(metadataId))
				ret = at;
		return ret;
	}
	
	public AttributeExt getAttributeByMetadata(EntityParam entity, String group,
			String metadataId) throws EngineException
	{
		AttributeType at = getAttributeTypeWithSingeltonMetadata(metadataId);
		if (at == null)
			return null;
		long entityId = idResolver.getEntityId(entity);
		Collection<AttributeExt> ret = getAllAttributesInternal(entityId, false, 
				group, at.getName(), true);
		return ret.size() == 1 ? ret.iterator().next() : null; 
	}

	
	/**
	 * Sets ACs of a given entity. Pure business logic - no authZ and transaction management.
	 * @param entityId
	 * @param group
	 * @param classes
	 * @param sql
	 * @throws EngineException
	 */
	public void setAttributeClasses(long entityId, String group, Collection<String> classes) 
			throws EngineException
	{
		AttributeClassHelper acHelper = acUtil.getACHelper(group, classes);
		
		List<AttributeExt> attributes = attributeDAO.getEntityAttributes(entityId, null, group);
		Collection<String> attributeNames = attributes.stream().
				map(a -> a.getName()).
				collect(Collectors.toList());
		Map<String, AttributeType> allTypes = attributeTypeDAO.getAllAsMap();
		acHelper.checkAttribtues(attributeNames, allTypes);

		Attribute classAttr = StringAttribute.of(AttributeClassTypeProvider.ATTRIBUTE_CLASSES_ATTRIBUTE, 
				group, new ArrayList<>(classes));
		createOrUpdateAttribute(classAttr, entityId);
	}
	
	
	public Collection<AttributeExt> getAllAttributesInternal(long entityId, 
			boolean effective, String groupPath, String attributeTypeName, 
			boolean allowDisabled) throws EngineException
	{
		if (!allowDisabled)
		{
			EntityInformation entityInformation = entityDAO.getByKey(entityId);
			if (entityInformation.getEntityState() == EntityState.disabled)
				throw new IllegalIdentityValueException("The entity is disabled");
		}
		if (groupPath != null)
		{
			Set<String> allGroups = membershipDAO.getEntityMembershipSimple(entityId);
			if (!allGroups.contains(groupPath))
				throw new IllegalGroupValueException("The entity is not a member of the group " 
						+ groupPath);
		}
		return getAllAttributes(entityId, groupPath, effective, attributeTypeName);
	}
	
	public Collection<AttributeExt> getAllAttributes(long entityId, String groupPath, boolean effective, 
			String attributeTypeName) throws EngineException
	{
		Map<String, Map<String, AttributeExt>> asMap = getAllAttributesAsMap(entityId, groupPath, effective, 
				attributeTypeName);
		List<AttributeExt> ret = new ArrayList<>();
		for (Map<String, AttributeExt> entry: asMap.values())
			ret.addAll(entry.values());
		return ret;
	}

	/**
	 * As {@link #addAttribute(long, Attribute, AttributeType, boolean, boolean)} but the attribute type 
	 * is automatically resolved
	 */
	public void addAttribute(long entityId, Attribute attribute, boolean update, boolean honorInitialConfirmation) 
			throws EngineException
	{
		AttributeType at = attributeTypeDAO.get(attribute.getName());
		addAttribute(entityId, attribute, at, update, honorInitialConfirmation);
	}
	
	/**
	 * Adds an attribute. This method performs engine level checks: whether the attribute type is not immutable,
	 * and properly sets unverified state if attribute is added by ordinary user (not an admin).
	 * <p>
	 * 
	 * @param entityId
	 * @param update
	 * @param at
	 * @param honorInitialConfirmation if true then operation is run by privileged user, 
	 * otherwise it is modification of self 
	 * possessed attribute and verification status must be set to unverified.
	 * @param attribute
	 * @throws EngineException
	 */
	public void addAttribute(long entityId, Attribute attribute, AttributeType at, boolean update,
			boolean honorInitialConfirmation) throws EngineException
	{
		if (at.isInstanceImmutable())
			throw new SchemaConsistencyException("The attribute with name " + at.getName() + 
					" can not be manually modified");
		addAttributeInternal(entityId, attribute, at, update, honorInitialConfirmation);
	}

	/**
	 * As {@link #addSystemAttribute(long, Attribute, AttributeType, boolean)} but the attribute type 
	 * is automatically resolved
	 */
	public void addSystemAttribute(long entityId, Attribute attribute, boolean update) 
			throws EngineException
	{
		AttributeType at = attributeTypeDAO.get(attribute.getName());
		addSystemAttribute(entityId, attribute, at, update);
	}
	
	/**
	 * Adds a system attribute. Use only internally.
	 * 
	 * @param entityId
	 * @param update
	 * @param at
	 * @param attribute
	 * @throws EngineException
	 */
	public void addSystemAttribute(long entityId, Attribute attribute, AttributeType at, boolean update) 
			throws EngineException
	{
		addAttributeInternal(entityId, attribute, at, update, true);
	}
	
	private void addAttributeInternal(long entityId, Attribute attribute, AttributeType at, boolean update,
			boolean honorInitialConfirmation) throws EngineException
	{
		if (attribute.getValueSyntax() == null)
			attribute.setValueSyntax(at.getValueSyntax());
		enforceCorrectConfirmationState(entityId, update, attribute, honorInitialConfirmation);
		validate(attribute, at);
		
		AttributeExt aExt = new AttributeExt(attribute, true);
		StoredAttribute param = new StoredAttribute(aExt, entityId);
		List<AttributeExt> existing = attributeDAO.getEntityAttributes(entityId, attribute.getName(), 
				attribute.getGroupPath());
		if (existing.isEmpty())
		{
			if (!membershipDAO.isMember(entityId, attribute.getGroupPath()))
				throw new IllegalGroupValueException("The entity is not a member "
						+ "of the group specified in the attribute");
			attributeDAO.create(param);
		} else
		{
			if (!update)
				throw new IllegalAttributeValueException("The attribute already exists");
			AttributeExt updated = existing.get(0);
			param.getAttribute().setCreationTs(updated.getCreationTs());
			attributeDAO.updateAttribute(param);
		}
	}
	
	/**
	 * Makes sure that the initial confirmation state is correctly set. This works as follows:
	 * - if honorInitialConfirmation is true then we assume that admin is performing the modification
	 * and everything is left as originally requested.
	 * - otherwise it is assumed that ordinary user is the caller, and all values are set as unconfirmed,
	 * unless the operation is updating an existing attribute - then values which are equal to already existing
	 * preserve their confirmation state. 
	 * <p>
	 * What is more it is checked in case of attribute update, when there is no-admin mode if the attribute 
	 * being updated had at least one confirmed value. If yes, also at least one confirmed value must be preserved. 
	 * 
	 * @throws EngineException 
	 */
	@SuppressWarnings("unchecked")
	private void enforceCorrectConfirmationState(long entityId, boolean update,
			Attribute attribute, boolean honorInitialConfirmation) throws EngineException
	{
		@SuppressWarnings("rawtypes")
		AttributeValueSyntax syntax = atHelper.getUnconfiguredSyntax(attribute.getValueSyntax());
		if (!syntax.isEmailVerifiable() || honorInitialConfirmation)
			return;
		
		if (!update)
		{
			setUnconfirmed(attribute, syntax);
			return;
		}
		
		Collection<AttributeExt> attrs = attributeDAO.getEntityAttributes(entityId, attribute.getName(),
				attribute.getGroupPath());
		if (attrs.isEmpty())
		{
			setUnconfirmed(attribute, syntax);
			return;
		}
		
		AttributeExt updated = attrs.iterator().next();
		Set<Integer> preservedStateIndices = new HashSet<Integer>();
		boolean oneConfirmedValuePreserved = false;
		//first we find matching values where confirmation state should be preserved
		for (int i=0; i<attribute.getValues().size(); i++)
		{
			String newValue = attribute.getValues().get(i);
			for (String existingValue: updated.getValues())
			{
				Object newAsObject = syntax.convertFromString(newValue);
				Object existingAsObject = syntax.convertFromString(existingValue);
				if (syntax.areEqual(newAsObject, existingAsObject))
				{
					preservedStateIndices.add(i);
					VerifiableElement newValueCasted = (VerifiableElement) newAsObject;
					VerifiableElement existingValueCasted = (VerifiableElement) existingAsObject;
					newValueCasted.setConfirmationInfo(existingValueCasted.getConfirmationInfo());
					attribute.getValues().set(i, syntax.convertToString(newValueCasted));
					if (existingValueCasted.getConfirmationInfo().isConfirmed())
						oneConfirmedValuePreserved = true;
				}
			}
		}
		//and we reset remaining
		for (int i=0; i<attribute.getValues().size(); i++)
		{
			if (!preservedStateIndices.contains(i))
			{
				Object newAsObject = syntax.convertFromString(attribute.getValues().get(i));
				VerifiableElement val = (VerifiableElement) newAsObject;
				val.setConfirmationInfo(new ConfirmationInfo(0));
				attribute.getValues().set(i, syntax.convertToString(val));
			}
		}
		
		if (!oneConfirmedValuePreserved)
		{
			for (String existingValue: updated.getValues())
			{
				Object existingAsObject = syntax.convertFromString(existingValue);
				VerifiableElement existingValueCasted = (VerifiableElement) existingAsObject;
				if (existingValueCasted.getConfirmationInfo().isConfirmed())
					throw new IllegalAttributeValueException("At least " + 
							"one confirmed value must be preserved");
			}
		}
	}
	
	public static <T extends VerifiableElement> void setUnconfirmed(Attribute attribute, AttributeValueSyntax<T> syntax)
	{
		setConfirmationStatus(attribute, syntax, false);
	}

	public static <T extends VerifiableElement> void setConfirmed(Attribute attribute, AttributeValueSyntax<T> syntax)
	{
		setConfirmationStatus(attribute, syntax, true);
	}

	private static <T extends VerifiableElement> void setConfirmationStatus(Attribute attribute, 
			AttributeValueSyntax<T> syntax, boolean confirmed)
	{
		List<String> updated = new ArrayList<>(attribute.getValues().size());
		for (String v : attribute.getValues())
		{
			T val = syntax.convertFromString(v);
			val.setConfirmationInfo(new ConfirmationInfo(confirmed));
			updated.add(syntax.convertToString(val));
		}
		attribute.setValues(updated);
	}

	
	/**
	 * Checks if the given set of attributes fulfills rules of ACs of a specified group 
	 * @throws EngineException 
	 */
	public void checkGroupAttributeClassesConsistency(List<Attribute> attributes, String path) 
			throws EngineException
	{
		AttributeClassHelper helper = acUtil.getACHelper(path, new ArrayList<>(0));
		Set<String> attributeNames = new HashSet<>(attributes.size());
		for (Attribute a: attributes)
			attributeNames.add(a.getName());
		helper.checkAttribtues(attributeNames, null);
	}

	/**
	 * Same as {@link #addAttribute(SqlSession, long, boolean, AttributeType, boolean, Attribute)}
	 * but for a whole list of attributes. It is assumed that attributes are always created. 
	 * Attribute type is automatically resolved.
	 *   
	 * @param attributes
	 * @param entityId
	 * @param honorInitialConfirmation
	 * @param sqlMap
	 * @throws EngineException
	 */
	public void addAttributesList(List<Attribute> attributes, long entityId, boolean honorInitialConfirmation) 
			throws EngineException
	{
		Map<String, AttributeType> typesMap = attributeTypeDAO.getAllAsMap();
		for (Attribute a: attributes)
			addAttribute(entityId, a, typesMap.get(a.getName()), true, honorInitialConfirmation);
	}
	
	/**
	 * Creates or updates an attribute. No schema checking is performed.
	 * @param toCreate
	 * @param entityId
	 */
	public void createOrUpdateAttribute(Attribute toCreate, long entityId)
	{
		StoredAttribute sAttr = toStoredAttribute(toCreate, entityId);
		List<AttributeExt> existing = attributeDAO.getEntityAttributes(entityId, toCreate.getName(), 
				toCreate.getGroupPath());
		if (existing.isEmpty())
			attributeDAO.create(sAttr);
		else
		{
			sAttr.getAttribute().setCreationTs(existing.get(0).getCreationTs());
			attributeDAO.updateAttribute(sAttr);
		}
	}
	
	/**
	 * Creates or updates an attribute. No schema checking is performed.
	 * @param toCreate
	 * @param entityId
	 */
	public void createAttribute(Attribute toCreate, long entityId)
	{
		StoredAttribute sAttr = toStoredAttribute(toCreate, entityId);
		attributeDAO.create(sAttr);
	}

	private StoredAttribute toStoredAttribute(Attribute toCreate, long entityId)
	{
		AttributeExt aExt = new AttributeExt(toCreate, true);
		return new StoredAttribute(aExt, entityId);
	}
	
	/**
	 * Checks if the given {@link Attribute} is valid wrt the {@link AttributeType} constraints
	 * @param attribute
	 * @param at
	 * @throws IllegalAttributeValueException
	 * @throws IllegalAttributeTypeException
	 */
	public void validate(Attribute attribute, AttributeType at) 
			throws IllegalAttributeValueException, IllegalAttributeTypeException
	{
		List<String> values = attribute.getValues();
		if (at.getMinElements() > values.size())
			throw new IllegalAttributeValueException("Attribute must have at least " + 
					at.getMinElements() + " values");
		if (at.getMaxElements() < values.size())
			throw new IllegalAttributeValueException("Attribute must have at most " + 
					at.getMaxElements() + " values");
		if (!attribute.getName().equals(at.getName()))
			throw new IllegalAttributeTypeException(
					"Attribute being checked has type " + 
					attribute.getName() + " while provided type is " + 
					at.getName());
		if (!attribute.getValueSyntax().equals(at.getValueSyntax()))
			throw new IllegalAttributeTypeException(
					"Attribute being checked has syntax " + 
					attribute.getValueSyntax() + " while provided type uses " + 
					at.getValueSyntax());
		
		AttributeValueSyntax<?> initializedValueSyntax = atHelper.getSyntax(at); 
		for (String val: values)
			initializedValueSyntax.validateStringValue(val);
		if (at.isUniqueValues())
		{
			for (int i=0; i<values.size(); i++)
				for (int j=i+1; j<values.size(); j++)
				{
					if (initializedValueSyntax.areEqualStringValue(values.get(i), values.get(j)))
						throw new IllegalAttributeValueException(
								"Duplicated values detected: " + (i+1) + " and " 
										+ (j+1));
				}
		}
	}
}
