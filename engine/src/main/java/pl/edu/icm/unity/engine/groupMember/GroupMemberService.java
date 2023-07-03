/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.groupMember;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.groupMember.GroupMemberWithAttributes;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.store.types.StoredIdentity;

@Component
class GroupMemberService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, GroupMemberService.class);

	private final EntityDAO entityDAO;
	private final AttributeTypeDAO attributeTypeDAO;
	private final MembershipDAO membershipDAO;
	private final AttributeDAO attributeDAO;
	private final IdentityDAO identityDAO;

	GroupMemberService(EntityDAO entityDAO, AttributeTypeDAO attributeTypeDAO,
	                   MembershipDAO membershipDAO, AttributeDAO attributeDAO, IdentityDAO identityDAO) {
		this.entityDAO = entityDAO;
		this.attributeTypeDAO = attributeTypeDAO;
		this.membershipDAO = membershipDAO;
		this.attributeDAO = attributeDAO;
		this.identityDAO = identityDAO;
	}

	List<GroupMemberWithAttributes> getGroupMembersWithAttributes(String group, List<String> attributes) {
		Set<String> globalAttr = getGlobalAttributes(attributes);

		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Long, Map<String, Map<String, AttributeExt>>> groupedAttributes = getAttributes(List.of(group), attributes, globalAttr);
		log.debug("Attributes in groups retrieval: {}", stopwatch.toString());

		return getGroupMembersWithAttributes(group, globalAttr, groupedAttributes);
	}

	Map<String, List<GroupMemberWithAttributes>> getGroupMembersWithAttributes(List<String> groups, List<String> attributes)
	{
		Map<String, List<GroupMemberWithAttributes>> groupMembers = new HashMap<>();

		Set<String> globalAttr = getGlobalAttributes(attributes);

		Map<Long, Map<String, Map<String, AttributeExt>>> groupedAttributes = getAttributes(groups, attributes, globalAttr);

		for (String grp: groups)
		{
			groupMembers.put(grp, getGroupMembersWithAttributes(grp, globalAttr, groupedAttributes));
		}

		return groupMembers;
	}

	private Set<String> getGlobalAttributes(List<String> attributes)
	{
		Map<String, AttributeType> allAsMap = attributeTypeDAO.getAllAsMap();
		if(attributes.isEmpty())
			return getGlobalAttributes(allAsMap.values().stream());

		return getGlobalAttributes(attributes.stream()
				.map(allAsMap::get)
				.filter(Objects::nonNull)
		);
	}

	private Set<String> getGlobalAttributes(Stream<AttributeType> allAsMap)
	{
		return allAsMap
				.filter(AttributeType::isGlobal)
				.map(AttributeType::getName)
				.collect(Collectors.toSet());
	}

	private List<GroupMemberWithAttributes> getGroupMembersWithAttributes(String group, Set<String> globalAttr, Map<Long, Map<String, Map<String, AttributeExt>>> groupedAttributes)
	{
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Long, EntityInformation> entityInfo = getEntityInfo(group);
		log.debug("Entities data retrieval: {}", stopwatch.toString());

		stopwatch.reset();
		stopwatch.start();
		Map<Long, Set<String>> memberships = getMemberships(group);
		log.debug("Group membership data retrieval: {}", stopwatch.toString());

		stopwatch.reset();
		stopwatch.start();
		Map<Long, List<Identity>> identities = getIdentities(group);
		log.debug("Identities data retrieval: {}", stopwatch.toString());

		List<GroupMemberWithAttributes> ret = new ArrayList<>();
		for (Long memberId: memberships.keySet())
		{
			Map<String, Map<String, AttributeExt>> memberGroupsWithAttr = groupedAttributes.getOrDefault(memberId, Map.of());
			Collection<AttributeExt> groupAttributes = memberGroupsWithAttr.getOrDefault(group, Map.of()).values();
			List<AttributeExt> globalAttributes = memberGroupsWithAttr.getOrDefault("/", Map.of()).values().stream()
					.filter(attributeExt -> globalAttr.contains(attributeExt.getName()))
					.collect(toList());
			Collection<AttributeExt> values = Stream.concat(groupAttributes.stream(), globalAttributes.stream())
					.collect(toMap(Attribute::getName, identity(), (attr1, attr2) -> attr1.getGroupPath().equals("/") ? attr2 : attr1))
					.values();
			ret.add(new GroupMemberWithAttributes(entityInfo.get(memberId), identities.get(memberId), values));
		}

		return ret;
	}

	private Map<Long, EntityInformation> getEntityInfo(String group)
	{
		return entityDAO.getByGroup(group).stream()
			.collect(toMap(EntityInformation::getId, identity()));
	}

	private Map<Long, Set<String>> getMemberships(String group)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<GroupMembership> all = membershipDAO.getMembers(group);
		log.debug("getMemberships {}", w.toString());

		return all.stream()
				.collect(groupingBy(GroupMembership::getEntityId, mapping(GroupMembership::getGroup, toSet())));
	}

	private Map<Long, List<Identity>> getIdentities(String group)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<StoredIdentity> all = identityDAO.getByGroup(group);
		log.debug("getIdentities {}", w.toString());

		return all.stream()
				.collect(groupingBy(StoredIdentity::getEntityId, mapping(StoredIdentity::getIdentity, toList())));
	}

	private Map<Long, Map<String, Map<String, AttributeExt>>> getAttributes(List<String> groups,
	                                                                        List<String> attributes,
	                                                                        Set<String> globalAttributes)
	{
		List<StoredAttribute> groupAttr;
		List<StoredAttribute> globalAttr = new ArrayList<>();
		if(attributes != null && !attributes.isEmpty())
			groupAttr = attributeDAO.getAttributesOfGroupMembers(attributes, groups);
		else
			groupAttr = attributeDAO.getAttributesOfGroupMembers(groups);
		if(!globalAttributes.isEmpty())
			globalAttr = attributeDAO.getAttributesOfGroupMembers(new ArrayList<>(globalAttributes), List.of("/"));

		return Stream.concat(groupAttr.stream(), globalAttr.stream())
				.collect(
						groupingBy(StoredAttribute::getEntityId,
								groupingBy(attribute -> attribute.getAttribute().getGroupPath(),
										toMap(
												attribute -> attribute.getAttribute().getName(),
												StoredAttribute::getAttribute,
												(attr1, attr2) -> attr1.getGroupPath().equals("/") ? attr2 : attr1))
						)
				);
	}

}
