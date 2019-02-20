/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.identity.IdentityHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Implements attributes operations.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
@Transactional
public class AttributeClassManagementImpl implements AttributeClassManagement
{
	private AttributeClassDB acDB;
	private AttributeDAO dbAttributes;
	private IdentityHelper identityHelper;
	private GroupDAO dbGroups;
	private EntityResolver idResolver;
	private InternalAuthorizationManager authz;
	private AttributesHelper attributesHelper;

	
	@Autowired
	public AttributeClassManagementImpl(AttributeClassDB acDB, AttributeDAO dbAttributes,
			IdentityHelper identityHelper, GroupDAO dbGroups, EntityResolver idResolver,
			InternalAuthorizationManager authz, AttributesHelper attributesHelper)
	{
		this.acDB = acDB;
		this.dbAttributes = dbAttributes;
		this.identityHelper = identityHelper;
		this.dbGroups = dbGroups;
		this.idResolver = idResolver;
		this.authz = authz;
		this.attributesHelper = attributesHelper;
	}

	@Override
	public void addAttributeClass(AttributesClass clazz) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		String addedName = clazz.getName();
		Set<String> missingParents = new HashSet<>(clazz.getParentClasses());
		Map<String, AttributesClass> allClasses = acDB.getAllAsMap();
		for (String c: allClasses.keySet())
		{
			if (addedName.equals(c))
				throw new WrongArgumentException("The attribute class " + addedName + " already exists");
			missingParents.remove(c);
		}
		if (!missingParents.isEmpty())
			throw new WrongArgumentException("The attribute class parent(s): " + missingParents + 
					" do(es) not exist");

		AttributeClassHelper.cleanupClass(clazz, allClasses);
		acDB.create(clazz);
	}

	@Override
	public void removeAttributeClass(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		Map<String, AttributesClass> allClasses = acDB.getAllAsMap();
		for (AttributesClass ac: allClasses.values())
		{
			if (ac.getParentClasses().contains(id))
				throw new SchemaConsistencyException("Can not remove attribute class " + id + 
						" as it is a parent of the attribute class " + ac.getName());
		}
		Set<Long> entities = identityHelper.getEntitiesWithStringAttribute(
				AttributeClassTypeProvider.ATTRIBUTE_CLASSES_ATTRIBUTE, id);
		if (entities.size() > 0)
		{
			String info = String.valueOf(entities.iterator().next());
			if (entities.size() > 1)
				info += " and " + (entities.size()-1) + " more";
			throw new SchemaConsistencyException("The attribute class " + id + 
					" can not be removed as there are entities with this class set. " +
					"Ids of entities: " + info);
		}

		Set<String> groupsUsing = getGroupsUsingAc(id);
		if (groupsUsing.size() > 0)
			throw new SchemaConsistencyException("The attribute class " + id + 
					" can not be removed as there are groups with have this class set: " +
					groupsUsing.toString());

		acDB.delete(id);
	}

	@Override
	public void updateAttributeClass(AttributesClass updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		Map<String, AttributesClass> allClasses = acDB.getAllAsMap();
		AttributesClass original = allClasses.get(updated.getName());
		if (original == null)
			throw new WrongArgumentException("There is no attribute class '" + 
					updated.getName() + "'");
		String acName = original.getName();

		AttributeClassHelper originalEffective = new AttributeClassHelper(allClasses, 
				Collections.singleton(acName));

		allClasses.put(acName, updated);
		AttributeClassHelper updatedEffective = new AttributeClassHelper(allClasses, 
				Collections.singleton(acName));

		boolean restrictiveChange = updatedEffective.isRestricting(originalEffective);
		if (restrictiveChange)
			checkIfUnused(acName, allClasses);

		acDB.update(updated);
	}
	
	@Override
	public Map<String, AttributesClass> getAttributeClasses() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return acDB.getAllAsMap();
	}

	@Override
	public void setEntityAttributeClasses(EntityParam entity, String group, Collection<String> classes)
			throws EngineException
	{
		entity.validateInitialization();
		authz.checkAuthorization(group, AuthzCapability.attributeModify);
		long entityId = idResolver.getEntityId(entity);
		attributesHelper.setAttributeClasses(entityId, group, classes);
	}
	

	@Override
	public Collection<AttributesClass> getEntityAttributeClasses(EntityParam entity,
			String group) throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		authz.checkAuthorization(group, AuthzCapability.read);
		Collection<AttributeExt> attrs = dbAttributes.getEntityAttributes(entityId,
				AttributeClassTypeProvider.ATTRIBUTE_CLASSES_ATTRIBUTE, group);
		if (attrs.size() == 0)
			return Collections.emptySet();
		AttributeExt attr = attrs.iterator().next();
		List<String> classes = attr.getValues();

		Map<String, AttributesClass> allClasses = acDB.getAllAsMap();
		Set<AttributesClass> ret = new HashSet<>(classes.size());
		for (String clazz: classes)
		{
			AttributesClass ac = allClasses.get(clazz);
			if (ac != null)
				ret.add(ac);
		}
		return ret;
	}
	
	/**
	 * @return set of all groups which use the given {@link AttributesClass}
	 */
	private Set<String> getGroupsUsingAc(String acName)
	{
		List<Group> allGroups = dbGroups.getAll();
		Set<String> ret = new HashSet<>();
		for (Group g: allGroups)
		{
			if (g.getAttributesClasses().contains(acName))
				ret.add(g.getName());
		}
		return ret;
	}
	
	private void checkIfUnused(String acName, Map<String, AttributesClass> allClasses) 
			throws EngineException
	{
		for (AttributesClass ac: allClasses.values())
		{
			if (ac.getParentClasses().contains(acName))
				throw new SchemaConsistencyException("Can not perform a restrictive "
						+ "change of an attribute class " + acName + 
						" as it is a parent of another attribute class " + ac.getName()
						+ ". Only non restrictive changes are allowed for used classes.");
		}
		Set<Long> entities = identityHelper.getEntitiesWithStringAttribute(
				AttributeClassTypeProvider.ATTRIBUTE_CLASSES_ATTRIBUTE, acName);
		if (entities.size() > 0)
		{
			String info = String.valueOf(entities.iterator().next());
			if (entities.size() > 1)
				info += " and " + (entities.size()-1) + " more";
			throw new SchemaConsistencyException("Can not perform a restrictive change of an "
					+ "attribute class " + acName + 
					" as there are entities with this class set. " +
					"Ids of entities: " + info + 
					". Only non restrictive changes are allowed for used classes.");
		}
		
		Set<String> groupsUsing = getGroupsUsingAc(acName);
		if (groupsUsing.size() > 0)
			throw new SchemaConsistencyException("Can not perform a restrictive change of an "
					+ "attribute class " + acName + 
					" as there are groups with have this class set: " +
					groupsUsing.toString() + 
					". Only non restrictive changes are allowed for used classes.");
	}
}
