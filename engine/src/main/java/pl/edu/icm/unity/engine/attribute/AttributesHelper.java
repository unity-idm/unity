/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static pl.edu.icm.unity.types.basic.audit.AuditEventTag.AUTHN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import pl.edu.icm.unity.base.capacityLimit.CapacityLimitName;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvidersRegistry;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.audit.AuditEventTrigger;
import pl.edu.icm.unity.engine.audit.AuditEventTrigger.AuditEventTriggerBuilder;
import pl.edu.icm.unity.engine.audit.AuditPublisher;
import pl.edu.icm.unity.engine.capacityLimits.InternalCapacityLimitVerificator;
import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
import pl.edu.icm.unity.exceptions.CapacityLimitReachedException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
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
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupsChain;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;
import pl.edu.icm.unity.types.basic.audit.AuditEventAction;
import pl.edu.icm.unity.types.basic.audit.AuditEventTag;
import pl.edu.icm.unity.types.basic.audit.AuditEventType;
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
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE,	AttributesHelper.class);

	
	private final AttributeMetadataProvidersRegistry atMetaProvidersRegistry;
	private final AttributeClassDB acDB;
	private final AttributeClassUtil acUtil;
	private final IdentityDAO identityDAO;
	private final EntityDAO entityDAO;
	private final EntityResolver idResolver;
	private final AttributeTypeDAO attributeTypeDAO;
	private final AttributeDAO attributeDAO;
	private final MembershipDAO membershipDAO;
	private final AttributeStatementProcessor statementsHelper;
	private final AttributeTypeHelper atHelper;
	private final GroupDAO groupDAO;
	private final AuditPublisher audit;
	private final InternalCapacityLimitVerificator capacityLimitVerificator;
	private final PublicAttributeRegistry attrRegistry;
	
	@Autowired
	public AttributesHelper(AttributeMetadataProvidersRegistry atMetaProvidersRegistry,
			AttributeClassDB acDB, IdentityDAO identityDAO,
			EntityDAO entityDAO, EntityResolver idResolver,
			AttributeTypeDAO attributeTypeDAO, AttributeDAO attributeDAO,
			MembershipDAO membershipDAO, AttributeStatementProcessor statementsHelper,
			AttributeTypeHelper atHelper, AttributeClassUtil acUtil,
			GroupDAO groupDAO, AuditPublisher audit,
			InternalCapacityLimitVerificator capacityLimitVerificator)
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
		this.audit = audit;
		this.capacityLimitVerificator = capacityLimitVerificator;
		this.attrRegistry = new PublicAttributeRegistry(attributeDAO, atHelper);
	}

	/**
	 * See {@link #getAllAttributes(long, String, String, SqlSession)}, the only difference is that the result
	 * is returned in a map indexed with groups (1st key) and attribute names (submap key).
	 */
	public Map<String, Map<String, AttributeExt>> getAllAttributesAsMap(long entityId, String groupPath, 
			boolean effective, String attributeTypeName) 
			throws EngineException
	{
		List<String> groupsPaths = groupPath != null ? singletonList(groupPath) : emptyList();
		return getAllAttributesAsMap(entityId, groupsPaths, effective, attributeTypeName);
	}

	/**
	 * See {@link #getAllAttributes(long, String, String, SqlSession)}, the only difference is that the result
	 * is returned in a map indexed with groups (1st key) and attribute names (submap key).
	 */
	private Map<String, Map<String, AttributeExt>> getAllAttributesAsMap(long entityId, List<String> groupsPaths,
			boolean effective, String attributeTypeName) throws EngineException
	{
		Map<String, Map<String, AttributeExt>> directAttributesByGroup = getAllEntityAttributesMap(entityId);
		if (!effective)
		{
			groupsPaths.forEach(g -> filterMap(directAttributesByGroup, g, attributeTypeName));
			return directAttributesByGroup;
		}
		Map<String, Group> allGroups = groupDAO.getAll().stream().collect(Collectors.toMap(g -> g.getPathEncoded(), g -> g));
		
		Set<String> allUserGroups = membershipDAO.getEntityMembershipSimple(entityId);
		List<String> groups = groupsPaths != null && groupsPaths.isEmpty() ? new ArrayList<>(allUserGroups) : groupsPaths;
		Map<String, Map<String, AttributeExt>> ret = new HashMap<>();

		Map<String, AttributesClass> allClasses = acDB.getAllAsMap();

		List<Identity> identities = identityDAO.getByEntity(entityId);
		for (String group: groups)
		{
			Map<String, AttributeExt> inGroup = statementsHelper.getEffectiveAttributes(identities, group,
					attributeTypeName, allUserGroups.stream().map(allGroups::get).collect(Collectors.toList()),
					directAttributesByGroup, allClasses, allGroups::get, attributeTypeDAO::get, g -> new GroupsChain(new Group(g).getPathsChain().stream().map(p -> allGroups.get(p)).collect(Collectors.toList())));
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
	 * @return map indexed with groups. Values are maps of all attributes in all groups, indexed with their names.
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
		Collection<AttributeExt> ret = getAllAttributesInternal(entityId, true, 
				group, at.getName(), true);
		return ret.size() == 1 ? ret.iterator().next() : null; 
	}

	public String getAttributeValueByMetadata(EntityParam entity, String group, String metadataId)
			throws EngineException
	{
		AttributeExt attribute = getAttributeByMetadata(entity, group, metadataId);
		if (attribute == null)
			return null;
		List<?> values = attribute.getValues();
		if (values.isEmpty())
			return null;
		return values.get(0).toString();
	}

	
	/**
	 * Sets ACs of a given entity. Pure business logic - no authZ and transaction management.
	 */
	public void setAttributeClasses(long entityId, String group, Collection<String> classes) 
			throws EngineException
	{
		AttributeClassHelper acHelper = acUtil.getACHelper(group, classes);
		
		List<AttributeExt> attributes = attributeDAO.getEntityAttributes(entityId, null, group);
		Collection<String> attributeNames = attributes.stream().
				map(Attribute::getName).
				collect(toList());
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
		List<String> groupsPaths = groupPath != null ? singletonList(groupPath) : emptyList();
		return getAllAttributesInternal(entityId, effective, groupsPaths, attributeTypeName, allowDisabled);
	}

	public Collection<AttributeExt> getAllAttributesInternal(long entityId,
			boolean effective, List<String> groupsPaths, String attributeTypeName,
			boolean allowDisabled) throws EngineException
	{
		if (!allowDisabled)
		{
			EntityInformation entityInformation = entityDAO.getByKey(entityId);
			if (entityInformation.getEntityState() == EntityState.disabled)
				throw new IllegalIdentityValueException("The entity is disabled");
		}
		if (groupsPaths != null)
		{
			Set<String> allGroups = membershipDAO.getEntityMembershipSimple(entityId);
			if (!allGroups.containsAll(groupsPaths))
				throw new IllegalGroupValueException("The entity is not a member of the group "
					+ groupsPaths);
		}
		return getAllAttributes(entityId, groupsPaths, effective, attributeTypeName);
	}

	public Collection<AttributeExt> getAllAttributes(long entityId, String groupPath, boolean effective, 
			String attributeTypeName) throws EngineException
	{
		List<String> groupsPaths = groupPath != null ? singletonList(groupPath) : emptyList();
		return getAllAttributes(entityId, groupsPaths, effective, attributeTypeName);
	}

	public Collection<AttributeExt> getAllAttributes(long entityId, List<String> groupsPaths, boolean effective,
	                                                 String attributeTypeName) throws EngineException
	{
		Map<String, Map<String, AttributeExt>> asMap = getAllAttributesAsMap(entityId, groupsPaths, effective,
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
	 * 
	 * @param honorInitialConfirmation if true then operation is run by privileged user, 
	 * otherwise it is modification of self possessed attribute and verification status must be set to unverified.
	 */
	public void addAttribute(long entityId, Attribute attribute, AttributeType at, boolean update,
			boolean honorInitialConfirmation) throws EngineException
	{
		if (attribute == null)
			throw new IllegalArgumentException("Trying to add null attribute for " + entityId);
		if (at == null)
			throw new IllegalArgumentException("Trying to add attribute " + attribute.getName() + " without type");
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
			checkAttributeCapacityLimit(at, aExt);	
			long createdAttrId = attributeDAO.create(param);
			attrRegistry.registerAttributeInfo(attribute, createdAttrId);
			audit.log(getAttrAudit(entityId, attribute, AuditEventAction.ADD));
		} else
		{
			if (!update)
				throw new IllegalAttributeValueException("The attribute already exists");
			AttributeExt updated = existing.get(0);
			param.getAttribute().setCreationTs(updated.getCreationTs());
			checkAttributeCapacityLimit(at, aExt);
			attributeDAO.updateAttribute(param);
			audit.log(getAttrAudit(entityId, attribute, AuditEventAction.UPDATE));
		}
	}

	private void checkAttributeCapacityLimit(AttributeType at, Attribute attr) throws CapacityLimitReachedException
	{

		if (isSystemAttribute(at))
			return;

		capacityLimitVerificator.assertInSystemLimitForSingleAdd(CapacityLimitName.AttributesCount,
				() -> attributeDAO.getCountWithoutType(attributeTypeDAO.getAllAsMap().values().stream()
						.filter(t -> isSystemAttribute(t)).map(t -> t.getName())
						.collect(toList())));
		capacityLimitVerificator.assertInSystemLimit(CapacityLimitName.AttributeValuesCount,
				() -> Long.valueOf(attr.getValues().size()));
		capacityLimitVerificator.assertInSystemLimit(CapacityLimitName.AttributeCumulativeValuesSize,
				() -> Long.valueOf(attr.getValues().stream().filter(v -> v != null)
						.mapToInt(String::length).sum()));

		for (String v : attr.getValues())
		{
			if (v != null)
				capacityLimitVerificator.assertInSystemLimit(CapacityLimitName.AttributeValueSize,
						() -> Long.valueOf(v.length()));
		}

	}
	
	private boolean isSystemAttribute(AttributeType at)
	{
		return at.isInstanceImmutable() || at.isTypeImmutable();
	}
	
	private AuditEventTriggerBuilder getAttrAudit(long entityId, Attribute attribute, AuditEventAction action)
	{
		return AuditEventTrigger.builder()
				.type(AuditEventType.ATTRIBUTE)
				.action(action)
				.name(attribute.getName())
				.subject(entityId)
				.details(ImmutableMap.of("group", attribute.getGroupPath(),
						"value", getTrimmedFirstValue(attribute)))
				.tags(AuditEventTag.USERS);
	}
	
	private String getTrimmedFirstValue(Attribute attr)
	{
		if (attr.getValues().isEmpty())
			return "-NONE-";
		
		final int showLength = 30;
		String fValue = attr.getValues().get(0);
		AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntax(attr.getValueSyntax());
		String deserialized = internalValueToExternal(syntax, fValue);
		return deserialized.length() > showLength ? deserialized.substring(0, showLength-3) + "..." : deserialized;
	}
	
	private <T> String internalValueToExternal(AttributeValueSyntax<T> syntax, String internalValue)
	{
		T deserialized = syntax.convertFromString(internalValue);
		return syntax.serializeSimple(deserialized);
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
	 */
	public void createOrUpdateAttribute(Attribute toCreate, long entityId)
	{
		StoredAttribute sAttr = toStoredAttribute(toCreate, entityId);
		List<AttributeExt> existing = attributeDAO.getEntityAttributes(entityId, toCreate.getName(), 
				toCreate.getGroupPath());
		if (existing.isEmpty())
		{
			attributeDAO.create(sAttr);
			if (toCreate.getName().startsWith(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX))
			{
				audit.log(AuditEventTrigger.builder()
						.type(AuditEventType.CREDENTIALS)
						.action(AuditEventAction.ADD)
						.name(toCreate.getName())
						.subject(entityId)
						.tags(AUTHN));
			}
		} else
		{
			sAttr.getAttribute().setCreationTs(existing.get(0).getCreationTs());
			attributeDAO.updateAttribute(sAttr);
			if (toCreate.getName().startsWith(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX))
			{
				audit.log(AuditEventTrigger.builder()
						.type(AuditEventType.CREDENTIALS)
						.action(AuditEventAction.UPDATE)
						.name(toCreate.getName())
						.subject(entityId)
						.tags(AUTHN));
			}
		}
	}
	
	/**
	 * Creates or updates an attribute. No schema checking is performed.
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
	
	public Optional<VerifiableElementBase> getFirstVerifiableAttributeValueFilteredByMeta(String metadataId,
			Collection<Attribute> list) throws EngineException
	{
		Optional<String> attrName = getAttributeName(metadataId);
		if (!attrName.isPresent())
			return Optional.empty();
		return convertToVerifiableAttributeValue(attrName.get(),
				getFirstValueOfAttributeFilteredByName(attrName.get(), list));
	}
	
	private Optional<String> getAttributeName(String metadata) throws EngineException
	{
		AttributeType attrType = getAttributeTypeWithSingeltonMetadata(metadata);
		if (attrType == null)
			return Optional.empty();

		return Optional.of(attrType.getName());
	}
	
	private Optional<VerifiableElementBase> convertToVerifiableAttributeValue(String attributeName, Optional<String> value)
	{
		if (!value.isPresent())
		{
			return Optional.empty();
		}
		
		AttributeValueSyntax<?> attributeSyntax = getAttributeSyntaxNotThrowing(attributeName);
		
		if (attributeSyntax != null && attributeSyntax.isEmailVerifiable())
		{
			return Optional.of((VerifiableElementBase) attributeSyntax.convertFromString(value.get()));
		}else
		{
			return Optional.of(new VerifiableElementBase(value.get()));
		}
	}
	
	private AttributeValueSyntax<?> getAttributeSyntaxNotThrowing(String attributeName)
	{
		try
		{
			return atHelper.getUnconfiguredSyntaxForAttributeName(attributeName);
		} catch (Exception e)
		{
			// ok
			log.debug("Can not get attribute syntax for attribute " + attributeName);
			return null;
		}
	}

	public Optional<String> getFirstValueOfAttributeFilteredByMeta(String metadataId, Collection<Attribute> list) throws EngineException
	{
		Optional<String> attrName = getAttributeName(metadataId);
		if (!attrName.isPresent())
			return Optional.empty();

		return getFirstValueOfAttributeFilteredByName(attrName.get(), list);
	}
	
	private Optional<String> getFirstValueOfAttributeFilteredByName(String attrName, Collection<Attribute> list) throws EngineException
	{
		for (Attribute attr : list)
		{
			if (attr.getName().equals(attrName) && attr.getValues() != null && !attr.getValues().isEmpty())
			{
				return Optional.ofNullable(attr.getValues().get(0));
			}
		}
		return Optional.empty();
		
	}
	
}
