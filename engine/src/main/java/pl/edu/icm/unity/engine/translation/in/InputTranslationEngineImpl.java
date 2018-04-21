/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.translation.in.EntityChange;
import pl.edu.icm.unity.engine.api.translation.in.GroupEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.translation.in.MappedAttribute;
import pl.edu.icm.unity.engine.api.translation.in.MappedGroup;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.translation.ExecutionBreakException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Applies all mappings which were recorded by profile's actions, taking into account the overall profile's settings.
 * <p>
 * Important: the instance is running without authorization, the object can not be exposed to direct operation.
 * @author K. Benedyczak
 */
@Component
public class InputTranslationEngineImpl implements InputTranslationEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, InputTranslationEngineImpl.class);
	private EntityManagement idsMan;
	private AttributesManagement attrMan;
	private GroupsManagement groupsMan;
	private AttributeTypeHelper attrTypeHelper;
	
	@Autowired
	public InputTranslationEngineImpl(@Qualifier("insecure") EntityManagement idsMan, 
			@Qualifier("insecure") AttributesManagement attrMan,
			@Qualifier("insecure") GroupsManagement groupsMan,
			AttributeTypeHelper attrTypeHelper)
	{
		this.idsMan = idsMan;
		this.attrMan = attrMan;
		this.groupsMan = groupsMan;
		this.attrTypeHelper = attrTypeHelper;
	}

	
	/**
	 * Entry point.
	 * @param result
	 * @throws EngineException
	 */
	@Override
	public void process(MappingResult result) throws EngineException
	{
		Set<Attribute> processedAttributes = new HashSet<>();
		EntityParam entity = processIdentities(result, processedAttributes);
		result.setMappedAtExistingEntity(entity);
		if (entity == null)
		{
			log.info("The mapped identity does not exist in database and was not created. "
					+ "The creation of groups and attributes is skipped, the mapped groups and attributes "
					+ "will be available for the registration form (if any)");
			return;
		}
		processGroups(result, entity, processedAttributes);
		processAttributes(result, entity, processedAttributes);
		processEntityChanges(result, entity);
	}
	
	/**
	 * Merges the information obtained after execution of an input translation profile with a manually specified
	 * entity.
	 * @param result
	 * @param baseEntity
	 * @throws EngineException 
	 */
	@Override
	public void mergeWithExisting(MappingResult result, EntityParam baseEntity) throws EngineException
	{
		result.setCleanStaleAttributes(false);
		result.setCleanStaleIdentities(false);
		result.setCleanStaleGroups(false);
		
		Set<Attribute> processedAttributes = new HashSet<>();
		processIdentitiesForMerge(result, baseEntity);
		processGroups(result, baseEntity, processedAttributes);
		processAttributes(result, baseEntity, processedAttributes);
		processEntityChanges(result, baseEntity);
	}
	
	/**
	 * 
	 * @param result
	 * @return true only if no one of mapped identities is present in db.
	 */
	@Override
	public boolean identitiesNotPresentInDb(MappingResult result)
	{
		try
		{
			getIdentitiesToCreate(result);
			return true;
		} catch (EngineException e)
		{
			return false;
		}
	}

	@Override
	public Entity resolveMappedIdentity(MappedIdentity checked) throws EngineException
	{
		return idsMan.getEntity(new EntityParam(checked.getIdentity()));
	}
	
	@Override
	public MappedIdentity getExistingIdentity(MappingResult result)
	{
		for (MappedIdentity checked: result.getIdentities())
		{
			try
			{
				idsMan.getEntity(new EntityParam(checked.getIdentity()));
				return checked;
			} catch (IllegalArgumentException e)
			{
				//OK
			} catch (EngineException e)
			{
				log.error("Can't check the entity status, shouldn't happen", e);
			}			
		}
		return null;
	}
	
	/**
	 * performs identities mapping
	 * @param result
	 * @return an mapped identity - previously existing or newly created; or null if identity mapping was not successful
	 * @throws EngineException
	 */
	private EntityParam processIdentities(MappingResult result, Set<Attribute> processedAttributes) throws EngineException
	{
		List<MappedIdentity> mappedMissingIdentitiesToCreate = new ArrayList<>();
		List<MappedIdentity> mappedMissingIdentities = new ArrayList<>();
		List<MappedIdentity> mappedMissingCreateOrUpdateIdentities = new ArrayList<>();
		Entity existing = null;
		for (MappedIdentity checked: result.getIdentities())
		{
			try
			{
				Entity found = idsMan.getEntity(new EntityParam(checked.getIdentity()));
				if (existing != null && !existing.getId().equals(found.getId()))
				{
					log.warn("Identity was mapped to two different entities: " + 
							existing + " and " + found);
					throw new ExecutionBreakException();
				}
				existing = found;
				result.addAuthenticatedWith(checked.getIdentity().getValue());
			} catch (IllegalArgumentException e)
			{
				log.trace("Identity " + checked + " not found in DB, details of exception follows", e);
				if (checked.getMode() == IdentityEffectMode.REQUIRE_MATCH)
				{
					log.info("The identity " + checked.getIdentity() + " doesn't exists "
							+ "in local database, but the profile requires so.");
					throw new ExecutionBreakException();
				} else if (checked.getMode() == IdentityEffectMode.CREATE_OR_MATCH)
				{
					mappedMissingIdentitiesToCreate.add(checked);
				} else if (checked.getMode() == IdentityEffectMode.MATCH)
				{
					mappedMissingIdentities.add(checked);
				} else
				{
					mappedMissingCreateOrUpdateIdentities.add(checked);
				}
			}			
		}
		
		if (existing != null)
		{
			mappedMissingIdentitiesToCreate.addAll(mappedMissingCreateOrUpdateIdentities);
			if (result.isCleanStaleIdentities())
				removeStaleIdentities(existing, result.getIdentities());
		} else
		{
			mappedMissingIdentities.addAll(mappedMissingCreateOrUpdateIdentities);
		}
		if (mappedMissingIdentitiesToCreate.isEmpty() && mappedMissingIdentities.isEmpty() && existing == null)
		{
			log.info("The translation profile didn't return any identity of the principal. "
					+ "We can't authenticate such anonymous principal.");
			throw new ExecutionBreakException();
		}
		
		if (mappedMissingIdentitiesToCreate.isEmpty())
		{
			log.debug("No identity needs to be added");
			return existing != null ? new EntityParam(existing.getId()) : null;
		}
		
		if (existing != null)
		{
			addEquivalents(mappedMissingIdentitiesToCreate, new EntityParam(existing.getId()), result);
			return new EntityParam(existing.getId());
		} else
		{
			Identity created = createNewEntity(result, mappedMissingIdentitiesToCreate,
					processedAttributes);
			return new EntityParam(created.getEntityId());
		}

	}

	/**
	 * Identities created from this profile and idp which are not present are removed.
	 * @param existing
	 * @param allMapped
	 */
	private void removeStaleIdentities(Entity existing, List<MappedIdentity> allMapped)
	{
		IdentityParam exampleMapped = allMapped.get(0).getIdentity();
		String idp = exampleMapped.getRemoteIdp();
		if (idp == null)
			idp = "_____MISSING";
		String profile = exampleMapped.getTranslationProfile();
		if (profile == null)
			profile = "_____MISSING";
		for (Identity id: existing.getIdentities())
		{
			if (idp.equals(id.getRemoteIdp()) && profile.equals(id.getTranslationProfile()))
			{
				boolean has = allMapped.stream().anyMatch(mi -> 
					id.getValue().equals(mi.getIdentity().getValue()) &&
						id.getTypeId().equals(mi.getIdentity().getTypeId())
				);
				if (!has)
				{
					try
					{
						idsMan.removeIdentity(id);
					} catch (EngineException e)
					{
						log.error("Can not remove stale identity " + id, e);
					}
				}
			}
		}
	}
	
	/**
	 * Besides returning the list of identities to be created also ensures that no one from mapped
	 * identities does exist in the db.
	 * @param result
	 * @return list of mapped identities which should be created. 
	 * @throws EngineException
	 */
	private List<MappedIdentity> getIdentitiesToCreate(MappingResult result) throws EngineException
	{
		List<MappedIdentity> mappedMissingIdentitiesToCreate = new ArrayList<>();
		for (MappedIdentity checked: result.getIdentities())
		{
			try
			{
				idsMan.getEntity(new EntityParam(checked.getIdentity()));
				log.debug("Identity was mapped to existing identity.");
				throw new ExecutionBreakException();
			} catch (IllegalArgumentException e)
			{
				log.trace("Identity " + checked + " not found in DB, details of exception follows", e);
				if (checked.getMode() == IdentityEffectMode.REQUIRE_MATCH)
				{
					log.info("The identity " + checked.getIdentity() + " doesn't exist "
							+ "in local database, but the profile requires so.");
					throw new ExecutionBreakException();
				} else
				{
					mappedMissingIdentitiesToCreate.add(checked);
				}
			}			
		}
		return mappedMissingIdentitiesToCreate;
	}
	
	private void processIdentitiesForMerge(MappingResult result, EntityParam baseEntity) throws EngineException
	{
		List<MappedIdentity> mappedMissingIdentitiesToCreate = getIdentitiesToCreate(result);
		if (mappedMissingIdentitiesToCreate.isEmpty())
		{
			log.info("The translation profile didn't return any identity of the principal. "
					+ "We can't merge such anonymous principal.");
			throw new ExecutionBreakException();
		}
		
		addEquivalents(mappedMissingIdentitiesToCreate, baseEntity, result);
	}

	
	private void addEquivalents(Collection<MappedIdentity> toAdd, EntityParam parentEntity, MappingResult result) 
			throws EngineException
	{
		for (MappedIdentity mi: toAdd)
		{
			idsMan.addIdentity(mi.getIdentity(), parentEntity, false);
			result.addAuthenticatedWith(mi.getIdentity().getValue());
		}
	}
	
	private Identity createNewEntity(MappingResult result,
			List<MappedIdentity> mappedMissingIdentities, 
			Set<Attribute> processedAttributes) throws EngineException
	{
		MappedIdentity first = mappedMissingIdentities.remove(0);
		
		Identity added;
		List<Attribute> attributes = getAttributesInGroup("/", result);
		log.info("Adding entity " + first.getIdentity() + " to the local DB");
		added = idsMan.addEntity(first.getIdentity(), first.getCredentialRequirement(), 
				EntityState.valid, false, attributes);
		result.addAuthenticatedWith(first.getIdentity().getValue());
		processedAttributes.addAll(attributes);
		
		addEquivalents(mappedMissingIdentities, new EntityParam(added), result);
		return added;
	}

	private Map<String, List<Attribute>> getAttributesByGroup(MappingResult result)
	{
		Map<String, List<Attribute>> ret = new HashMap<>();
		
		ret.put("/", getAttributesInGroup("/", result));
		for (MappedGroup mg: result.getGroups())
		{
			if ("/".equals(mg.getGroup()))
				continue;
			ret.put(mg.getGroup(), getAttributesInGroup(mg.getGroup(), result));
		}
		return ret;
	}

	private List<Attribute> getAttributesInGroup(String group, MappingResult result)
	{
		List<Attribute> ret = new ArrayList<>();
		for (MappedAttribute mappedAttr: result.getAttributes())
		{
			if (mappedAttr.getAttribute().getGroupPath().equals(group))
				ret.add(mappedAttr.getAttribute());
		}
		return ret;
	}
	
	private void processGroups(MappingResult result, EntityParam principal,
			Set<Attribute> processedAttributes) throws EngineException
	{
		Map<String, List<Attribute>> attributesByGroup = getAttributesByGroup(result);
		Map<String, GroupMembership> currentGroups = idsMan.getGroups(principal);
        	Set<String> currentSimple = new HashSet<>(currentGroups.keySet());
		for (MappedGroup gm: result.getGroups())
		{
		        if (!currentGroups.containsKey(gm.getGroup()))
			{
				Deque<String> missingGroups = 
						Group.getMissingGroups(gm.getGroup(), currentSimple);
				log.info("Adding to group " + gm);
				addToGroupRecursive(principal, missingGroups, currentSimple, gm.getIdp(), 
						gm.getProfile(), gm.getCreateIfMissing(),
						attributesByGroup, processedAttributes);
			} else
			{
				log.debug("Entity already in the group " + gm + ", skipping");
			}
		}
		if (result.isCleanStaleGroups())
			removeStaleMemberships(currentGroups, result, principal);
		
	}
	
	private void removeStaleMemberships(Map<String, GroupMembership> currentGroups, MappingResult result,
			EntityParam principal)
	{
		IdentityParam exampleMapped = result.getIdentities().get(0).getIdentity();
		String idp = exampleMapped.getRemoteIdp();
		if (idp == null)
			idp = "_____MISSING";
		String profile = exampleMapped.getTranslationProfile();
		if (profile == null)
			profile = "_____MISSING";
		List<MappedGroup> mappedGroups = result.getGroups();
		for (GroupMembership membership: currentGroups.values())
		{
			if (!membership.getGroup().equals("/") &&
				idp.equals(membership.getRemoteIdp()) && profile.equals(membership.getTranslationProfile()))
			{
				if (!mappedGroups.stream().anyMatch(gm -> membership.getGroup().equals(gm.getGroup())))
					try
					{
						groupsMan.removeMember(membership.getGroup(), principal);
					} catch (Exception e)
					{
						log.error("Can not remove stale group membership in " 
								+ membership.getGroup(), e);
					}
			}
		}
	}
	
	private void addToGroupRecursive(EntityParam who, Deque<String> missingGroups, 
			Set<String> currentGroups, String idp, String profile, 
			GroupEffectMode createMissingGroups, Map<String, List<Attribute>> attributesByGroup,
			Set<Attribute> processedAttributes) 
					throws EngineException
	{
		String group = missingGroups.pollLast();
		List<Attribute> attributes = attributesByGroup.get(group);
		if (attributes == null)
			attributes = new ArrayList<>();
		
		boolean present = groupsMan.isPresent(group);
		if (!present)
		{
			if (createMissingGroups == GroupEffectMode.CREATE_GROUP_IF_MISSING)
			{
				log.info("Group " + group + " doesn't exist, "
						+ "will be created to fullfil translation profile rule");
				groupsMan.addGroup(new Group(group));
				groupsMan.addMemberFromParent(group, who, attributes, idp, profile);
				processedAttributes.addAll(attributes);
			} else if (createMissingGroups == GroupEffectMode.REQUIRE_EXISTING_GROUP)
			{
				log.debug("Entity should be added to a group " + group + " which is missing, failing.");
				throw new ExecutionBreakException();
			} else
			{
				log.debug("Entity should be added to a group " + group + " which is missing, ignoring.");
				return;
			}
		} else
		{
			groupsMan.addMemberFromParent(group, who, attributes, idp, profile);
			processedAttributes.addAll(attributes);
		}
		
		currentGroups.add(group);
		if (!missingGroups.isEmpty())
		{
			addToGroupRecursive(who, missingGroups, currentGroups, 
					idp, profile, createMissingGroups, attributesByGroup, 
					processedAttributes);
		}
	}
	
	private void processAttributes(MappingResult result, EntityParam principal, 
			Set<Attribute> alreadyProcessedAttributes) throws EngineException
	{
		Map<String, AttributeExt> existingAttributes = new HashMap<>();
		Collection<AttributeExt> existingAttrs = attrMan.getAllAttributes(principal, 
				false, null, null, false);
		for (AttributeExt a: existingAttrs)
			existingAttributes.put(a.getGroupPath()+"///" + a.getName(), a);

		List<MappedAttribute> attrs = result.getAttributes();
		
		for (MappedAttribute attr: attrs)
		{
			Attribute att = attr.getAttribute();
			if (alreadyProcessedAttributes.contains(att))
				continue;
			AttributeExt existing = existingAttributes.get(att.getGroupPath()+"///"+att.getName());
			switch (attr.getMode())
			{
			case CREATE_ONLY:
				if (existing == null)
				{
					log.info("Creating attribute " + att);
					attrMan.createAttribute(principal, att);					
				} else
				{
					log.debug("Skipping attribute which is already present: " + att);
				}
				break;
			case CREATE_OR_UPDATE:
				updateExistingAttribute(att, principal, existing);
				break;
			case UPDATE_ONLY:
				if (existing != null)
				{
					updateExistingAttribute(att, principal, existing);					
				} else
				{
					log.debug("Skipping attribute to be updated as there is no one defined: " + att);
				}
				break;
			}
		}
		
		if (result.isCleanStaleAttributes())
			removeStaleAttributes(result, existingAttrs, principal);
	}
	
	private void updateExistingAttribute(Attribute att, EntityParam principal, AttributeExt existing) 
			throws EngineException
	{
		if (existing != null && attributesEqual(att, existing))
		{
			log.debug("Attribute {} in DB is up to date, skiping update", att);
			return;
		}
		log.info("Updating attribute {}", att);
		attrMan.setAttribute(principal, att);
	}
	
	private boolean attributesEqual(Attribute attribute, AttributeExt fromDB)
	{
		if (!attribute.getValueSyntax().equals(fromDB.getValueSyntax()))
			return false;
		List<String> values = attribute.getValues();
		if (values.size() != fromDB.getValues().size())
			return false;
		AttributeValueSyntax<?> attrSyntax = attrTypeHelper.getUnconfiguredSyntax(
				attribute.getValueSyntax());
		
		for (int i=0; i<values.size(); i++)
			if (!attrSyntax.areEqualStringValue(values.get(i), fromDB.getValues().get(i)))
				return false;
		return true;
	}
	
	private void removeStaleAttributes(MappingResult result, Collection<AttributeExt> existingAttrs,
			EntityParam principal)
	{
		IdentityParam exampleMapped = result.getIdentities().get(0).getIdentity();
		String idp = exampleMapped.getRemoteIdp();
		if (idp == null)
			idp = "_____MISSING";
		String profile = exampleMapped.getTranslationProfile();
		if (profile == null)
			profile = "_____MISSING";
		List<MappedAttribute> mappedAttributes = result.getAttributes();
		for (AttributeExt a: existingAttrs)
		{
			if (idp.equals(a.getRemoteIdp()) && profile.equals(a.getTranslationProfile()))
			{
				if (!mappedAttributes.stream().anyMatch(ma -> 
						a.getName().equals(ma.getAttribute().getName()) &&
						a.getGroupPath().equals(ma.getAttribute().getGroupPath())))
					try
					{
						attrMan.removeAttribute(principal, a.getGroupPath(), a.getName());
					} catch (Exception e)
					{
						log.error("Can not remove stale attribute " + a, e);
					}
			}
		}
	}
	
	private void processEntityChanges(MappingResult result, EntityParam principal) throws EngineException
	{
		List<EntityChange> changes = result.getEntityChanges();
		
		for (EntityChange change: changes)
		{
			if (change.getScheduledOperation() != null)
				log.info("Changing entity scheduled operation to " + 
					change.getScheduledOperation() + " on " + change.getScheduledTime());
			else
				log.info("Clearing entity scheduled change operation");
			idsMan.scheduleEntityChange(principal, change.getScheduledTime(), 
					change.getScheduledOperation());
		}
	}
}
