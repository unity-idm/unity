/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.bulk.CompositeGroupContents;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.engine.credential.CredentialReqRepository;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;

@Component
class CompositeEntitiesInfoProvider
{
	@Autowired
	private AttributeTypeDAO attributeTypeDAO;
	@Autowired
	private AttributeDAO attributeDAO;
	@Autowired
	private MembershipDAO membershipDAO;
	@Autowired
	private AttributeClassDB acDB;
	@Autowired
	private GroupDAO groupDAO;
	@Autowired
	private EntityDAO entityDAO;
	@Autowired
	private IdentityDAO identityDAO;
	@Autowired
	private CredentialRepository credentialRepository;
	@Autowired
	private CredentialReqRepository credentialReqRepository;

	
	public CompositeGroupContents getCompositeGroupContents(String group) throws EngineException
	{
		Map<Long, Set<String>> memberships = getMemberships(group);
		return CompositeGroupContentsImpl.builder()
			.withAttributeTypes(attributeTypeDAO.getAllAsMap())
			.withAttributeClasses(acDB.getAllAsMap())
			.withGroups(groupDAO.getAllAsMap())
			.withMemberships(memberships)
			.withEntityInfo(getEntityInfo(memberships.keySet()))
			.withIdentities(getIdentities(memberships.keySet()))
			.withDirectAttributes(getAttributes(memberships.keySet()))
			.withCredentials(credentialRepository.getCredentialDefinitions())
			.withCredentialRequirements(getCredentialRequirements())
			.build();
	}
	
	private Map<String, CredentialRequirements> getCredentialRequirements() throws EngineException
	{
		return credentialReqRepository.getCredentialRequirements().stream()
			.collect(Collectors.toMap(cr -> cr.getName(), cr -> cr));
	}

	private Map<Long, Map<String, Map<String, AttributeExt>>> getAttributes(Set<Long> entities)
	{
		List<StoredAttribute> all = attributeDAO.getAll(); //TODO improve, get by group from DB
		Map<Long, Map<String, Map<String, AttributeExt>>> ret = new HashMap<>();
		for (Long member: entities)
			ret.put(member, new HashMap<>());
		all.stream() 
			.filter(sa -> entities.contains(sa.getEntityId()))
			.forEach(sa -> 
			{
				Map<String, Map<String, AttributeExt>> entityAttrs = ret.get(sa.getEntityId());
				Map<String, AttributeExt> attrsInGroup = entityAttrs.get(sa.getAttribute().getGroupPath());
				if (attrsInGroup == null)
				{
					attrsInGroup = new HashMap<>();
					entityAttrs.put(sa.getAttribute().getGroupPath(), attrsInGroup);
				}
				attrsInGroup.put(sa.getAttribute().getName(), sa.getAttribute());
			});
		return ret;
	}

	private Map<Long, EntityInformation> getEntityInfo(Set<Long> entities)
	{
		return entityDAO.getAll().stream() //TODO improve, get by group from DB
			.filter(entity -> entities.contains(entity.getId()))
			.collect(Collectors.toMap(entity -> entity.getId(), entity->entity));
	}

	private Map<Long, List<Identity>> getIdentities(Set<Long> entities)
	{
		List<StoredIdentity> all = identityDAO.getAll(); //TODO improve, get by group from DB
		Map<Long, List<Identity>> ret = new HashMap<>();
		for (Long member: entities)
			ret.put(member, new ArrayList<>());
		all.stream() 
			.filter(storedIdentity -> entities.contains(storedIdentity.getEntityId()))
			.forEach(storedIdentity -> ret.get(storedIdentity.getEntityId()).add(storedIdentity.getIdentity()));
		return ret;
	}

	private Map<Long, Set<String>> getMemberships(String group)
	{
		List<GroupMembership> all = membershipDAO.getAll(); //TODO: improve, getBygroup from DB
		Set<Long> members = all.stream()
				.filter(g -> g.getGroup().equals(group))
				.map(g -> g.getEntityId())
				.collect(Collectors.toSet());
		Map<Long, Set<String>> ret = new HashMap<>();
		for (Long member: members)
			ret.put(member, new HashSet<>());
		all.stream()
			.filter(membership -> members.contains(membership.getEntityId()))
			.forEach(membership -> ret.get(membership.getEntityId()).add(membership.getGroup()));
		return ret;
	}
}
