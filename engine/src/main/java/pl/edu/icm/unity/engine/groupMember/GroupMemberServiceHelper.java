/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.groupMember;

import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.groupMember.GroupMemberWithAttributes;
import pl.edu.icm.unity.store.api.*;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

@Component
class GroupMemberServiceHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, GroupMemberServiceHelper.class);

	private final EntityDAO entityDAO;
	private final AttributeTypeDAO attributeTypeDAO;
	private final MembershipDAO membershipDAO;
	private final AttributeDAO attributeDAO;
	private final IdentityDAO identityDAO;

	GroupMemberServiceHelper(EntityDAO entityDAO, AttributeTypeDAO attributeTypeDAO,
	                         MembershipDAO membershipDAO, AttributeDAO attributeDAO, IdentityDAO identityDAO) {
		this.entityDAO = entityDAO;
		this.attributeTypeDAO = attributeTypeDAO;
		this.membershipDAO = membershipDAO;
		this.attributeDAO = attributeDAO;
		this.identityDAO = identityDAO;
	}

	public List<GroupMemberWithAttributes> getGroupMembers(String group, List<String> attributes) {
		List<String> globalAttr = getGlobalAttributes(attributes);

		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Long, Map<String, Map<String, AttributeExt>>> groupedAttributes = getAttributes(List.of(group), attributes, globalAttr);
		log.info("Attributes in groups retrieval: {}", stopwatch.toString());

		return getGroupMembers(group, globalAttr, groupedAttributes);
	}

	public Map<String, List<GroupMemberWithAttributes>> getGroupMembers(List<String> groups, List<String> attributes)
	{
		Map<String, List<GroupMemberWithAttributes>> groupMembers = new HashMap<>();

		Stopwatch watch = Stopwatch.createStarted();

		List<String> globalAttr = getGlobalAttributes(attributes);

		Map<Long, Map<String, Map<String, AttributeExt>>> groupedAttributes = getAttributes(groups, attributes, globalAttr);

		for (String grp: groups)
		{
			groupMembers.put(grp, getGroupMembers(grp, globalAttr, groupedAttributes));
		}

		log.debug("Group membership data retrieval: {}", watch.toString());

		return groupMembers;
	}

	private List<String> getGlobalAttributes(List<String> attributes)
	{
		Map<String, AttributeType> allAsMap = attributeTypeDAO.getAllAsMap();
		if(attributes.isEmpty())
			return allAsMap.values().stream()
					.filter(AttributeType::isGlobal)
					.map(AttributeType::getName)
					.collect(toList());

		return attributes.stream()
				.map(allAsMap::get)
				.filter(Objects::nonNull)
				.filter(AttributeType::isGlobal)
				.map(AttributeType::getName)
				.collect(toList());
	}

	private List<GroupMemberWithAttributes> getGroupMembers(String group, List<String> globalAttr, Map<Long, Map<String, Map<String, AttributeExt>>> groupedAttributes)
	{
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Long, EntityInformation> entityInfo = getEntityInfo(group);
		log.info("Entities data retrieval: {}", stopwatch.toString());

		stopwatch.reset();
		stopwatch.start();
		Map<Long, Set<String>> memberships = getMemberships(group);
		log.info("Group membership data retrieval: {}", stopwatch.toString());

		stopwatch.reset();
		stopwatch.start();
		Map<Long, List<Identity>> identities = getIdentities(group);
		log.info("Identities data retrieval: {}", stopwatch.toString());

		List<GroupMemberWithAttributes> ret = new ArrayList<>();
		for (Long memberId: memberships.keySet())
		{
			Map<String, Map<String, AttributeExt>> memberGroupsWithAttr = groupedAttributes.getOrDefault(memberId, Map.of());
			Collection<AttributeExt> groupAttributes = memberGroupsWithAttr.getOrDefault(group, Map.of()).values();
			List<AttributeExt> globalAttributes = memberGroupsWithAttr.getOrDefault("/", Map.of()).values().stream()
					.filter(y -> globalAttr.contains(y.getName()))
					.collect(toList());
			ret.add(new GroupMemberWithAttributes(entityInfo.get(memberId), identities.get(memberId), Stream.concat(groupAttributes.stream(), globalAttributes.stream()).collect(toList())));
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
	                                                                        List<String> globalAttributes)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<StoredAttribute> all = attributeDAO.getAttributesOfGroupMembers(attributes, groups, globalAttributes);
		log.debug("getAttrs {}", w.toString());

		return all.stream()
				.collect(
						groupingBy(StoredAttribute::getEntityId,
								groupingBy(attribute -> attribute.getAttribute().getGroupPath(),
										toMap(attribute -> attribute.getAttribute().getName(), StoredAttribute::getAttribute)))
				);
	}

}
