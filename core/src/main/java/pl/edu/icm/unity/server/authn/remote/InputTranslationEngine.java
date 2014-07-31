/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.translation.ExecutionBreakException;
import pl.edu.icm.unity.server.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.server.translation.in.MappedAttribute;
import pl.edu.icm.unity.server.translation.in.MappedIdentity;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.GroupUtils;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Applies all mappings which were recorded by profile's actions, taking into account the overall profile's settings.
 * <p>
 * Important: the instance is running without authorization, the object can not be exposed to direct operation.
 * @author K. Benedyczak
 */
@Component
public class InputTranslationEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, InputTranslationEngine.class);
	private IdentitiesManagement idsMan;
	private AttributesManagement attrMan;
	private GroupsManagement groupsMan;
	
	@Autowired
	public InputTranslationEngine(@Qualifier("insecure") IdentitiesManagement idsMan, 
			@Qualifier("insecure") AttributesManagement attrMan,
			@Qualifier("insecure") GroupsManagement groupsMan)
	{
		this.idsMan = idsMan;
		this.attrMan = attrMan;
		this.groupsMan = groupsMan;
	}

	
	/**
	 * Entry point.
	 * @param result
	 * @throws EngineException
	 */
	public void process(MappingResult result) throws EngineException
	{
		Identity principal = processIdentities(result);
		if (principal == null)
		{
			log.info("The mapped identity does not exist in database and was not created. "
					+ "The creation of groups and attributes is skipped, the mapped groups and attributes "
					+ "will be available for the registration form (if any)");
			return;
		}
		
		processGroups(result, principal);
		processAttributes(result, principal);
	}
	
	private Identity processIdentities(MappingResult result) throws EngineException
	{
		List<MappedIdentity> mappedMissingIdentitiesToCreate = new ArrayList<>();
		List<MappedIdentity> mappedMissingIdentities = new ArrayList<>();
		Entity existing = null;
		for (MappedIdentity checked: result.getIdentities())
		{
			try
			{
				Entity found = idsMan.getEntity(new EntityParam(checked.getIdentity()));
				if (existing != null && !existing.getId().equals(found.getId()))
				{
					log.warn("Identity was mapped to two different entities: " + 
							existing + " and " + found + ". Skipping.");
					throw new ExecutionBreakException();
				}
				existing = found;
			} catch (IllegalIdentityValueException e)
			{
				if (checked.getMode() == IdentityEffectMode.REQUIRE_MATCH)
				{
					log.info("The identity " + checked.getIdentity() + " doesn't exists "
							+ "in local database, but the profile requires so. Skipping.");
					throw new ExecutionBreakException();
				} else if (checked.getMode() == IdentityEffectMode.CREATE_OR_MATCH)
				{
					mappedMissingIdentitiesToCreate.add(checked);
				} else
				{
					mappedMissingIdentities.add(checked);
				}
			}			
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
			return existing != null ? existing.getIdentities()[0] : null;
		}
		
		if (existing != null)
		{
			addEquivalents(mappedMissingIdentitiesToCreate, new EntityParam(existing.getId()));
			return existing.getIdentities()[0];
		} else
		{
			return createNewEntity(result, mappedMissingIdentitiesToCreate);
		}

	}
	
	private void addEquivalents(Collection<MappedIdentity> toAdd, EntityParam parentEntity) 
			throws EngineException
	{
		for (MappedIdentity mi: toAdd)
		{
			idsMan.addIdentity(mi.getIdentity(), parentEntity, false);
		}
	}
	
	private Identity createNewEntity(MappingResult result,
			List<MappedIdentity> mappedMissingIdentities) throws EngineException
	{
		MappedIdentity first = mappedMissingIdentities.remove(0);
		
		Identity added;
		List<Attribute<?>> attributes = getRootGroupAttributes(result);
		log.info("Adding entity " + first.getIdentity() + " to the local DB");
		added = idsMan.addEntity(first.getIdentity(), first.getCredentialRequirement(), 
				EntityState.valid, false, attributes);
		
		addEquivalents(mappedMissingIdentities, new EntityParam(added));
		return added;
	}

	private List<Attribute<?>> getRootGroupAttributes(MappingResult result) throws EngineException
	{
		List<Attribute<?>> ret = new ArrayList<>();
		for (MappedAttribute mappedAttr: result.getAttributes())
		{
			if (mappedAttr.getAttribute().getGroupPath().equals("/"))
				ret.add(mappedAttr.getAttribute());
		}
		return ret;
	}
	
	private void processGroups(MappingResult result, Identity principal) throws EngineException
	{
		EntityParam entity = new EntityParam(principal);
		Set<String> currentGroups = new HashSet<String>(idsMan.getGroups(entity));
		for (String gm: result.getGroups())
		{
			if (!currentGroups.contains(gm))
			{
				Deque<String> missingGroups = GroupUtils.getMissingGroups(gm, currentGroups);
				log.info("Adding to group " + gm);
				addToGroupRecursive(entity, missingGroups, currentGroups);
			} else
			{
				log.debug("Entity already in the group " + gm + ", skipping");
			}
		}
	}
	
	private void addToGroupRecursive(EntityParam who, Deque<String> missingGroups, Set<String> currentGroups) 
			throws EngineException
	{
		String group = missingGroups.pollLast();
		groupsMan.addMemberFromParent(group, who);
		currentGroups.add(group);
		if (!missingGroups.isEmpty())
		{
			addToGroupRecursive(who, missingGroups, currentGroups);
		}
	}
	
	private void processAttributes(MappingResult result, Identity principal) throws EngineException
	{
		EntityParam entity = new EntityParam(principal);
		
		Set<String> existingANames = new HashSet<>();
		Collection<AttributeExt<?>> existingAttrs = attrMan.getAllAttributes(entity, 
				false, null, null, false);
		for (AttributeExt<?> a: existingAttrs)
			existingANames.add(a.getGroupPath()+"///" + a.getName());

		List<MappedAttribute> attrs = result.getAttributes();
		
		for (MappedAttribute attr: attrs)
		{
			Attribute<?> att = attr.getAttribute();
			switch (attr.getMode())
			{
			case CREATE_ONLY:
				if (!existingANames.contains(att.getGroupPath()+"///"+att.getName()))
				{
					log.info("Creating attribute " + att);
					attrMan.setAttribute(entity, att, false);					
				} else
				{
					log.debug("Skipping attribute which is already present: " + att);
				}
				break;
			case CREATE_OR_UPDATE:
				log.info("Updating attribute " + att);
				attrMan.setAttribute(entity, att, true);
				break;
			case UPDATE_ONLY:
				if (existingANames.contains(att.getGroupPath()+"///"+att.getName()))
				{
					log.info("Updating attribute " + att);
					attrMan.setAttribute(entity, att, true);					
				} else
				{
					log.debug("Skipping attribute to be updated as there is no one defined: " + att);
				}
				break;
			}
		}
	}
}
