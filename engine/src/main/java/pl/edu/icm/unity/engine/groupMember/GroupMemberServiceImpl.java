/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.groupMember;

import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.groupMember.GroupMemberService;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.store.api.*;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

@Component
class GroupMemberServiceImpl implements GroupMemberService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_BULK_OPS, GroupMemberServiceImpl.class);

	private final EntityDAO entityDAO;
	private final GroupDAO groupDAO;
	private final AttributeTypeDAO attributeTypeDAO;
	private final MembershipDAO membershipDAO;
	private final AttributeDAO attributeDAO;
	private final IdentityDAO identityDAO;
	private final InternalAuthorizationManager authz;

	GroupMemberServiceImpl(EntityDAO entityDAO, GroupDAO groupDAO, AttributeTypeDAO attributeTypeDAO,
	                       MembershipDAO membershipDAO, AttributeDAO attributeDAO, IdentityDAO identityDAO,
	                       InternalAuthorizationManager authz) {
		this.entityDAO = entityDAO;
		this.groupDAO = groupDAO;
		this.attributeTypeDAO = attributeTypeDAO;
		this.membershipDAO = membershipDAO;
		this.attributeDAO = attributeDAO;
		this.identityDAO = identityDAO;
		this.authz = authz;
	}

	@Override
	@Transactional
	public Group getGroup(String group) throws AuthorizationException {
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		return groupDAO.get(group);
	}

	@Override
	@Transactional
	public List<SimpleGroupMember> getGroupMembers(String group, List<String> attributes) throws AuthorizationException {
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		Stopwatch watch = Stopwatch.createStarted();

		List<String> globalAttr = getGlobalAttributes(attributes);

		Map<Long, Map<String, Map<String, AttributeExt>>> groupedAttributes = getAttributes(List.of(group), attributes, globalAttr);

		List<SimpleGroupMember> groupMembers = getGroupMembers(group, globalAttr, groupedAttributes);

		log.debug("Group membership data retrieval: {}", watch.toString());

		return groupMembers;
	}

	@Override
	@Transactional
	public Map<String, List<SimpleGroupMember>> getGroupMembers(List<String> groups, List<String> attributes) throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		Map<String, List<SimpleGroupMember>> groupMembers = new HashMap<>();

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
		return attributes.stream()
				.map(allAsMap::get)
				.filter(Objects::nonNull)
				.filter(AttributeType::isGlobal)
				.map(AttributeType::getName)
				.collect(toList());
	}

	private List<SimpleGroupMember> getGroupMembers(String group, List<String> globalAttr, Map<Long, Map<String, Map<String, AttributeExt>>> groupedAttributes) throws AuthorizationException
	{
		Map<Long, EntityInformation> entityInfo = getEntityInfo(group);
		Map<Long, Set<String>> memberships = getMemberships(group);
		Map<Long, List<Identity>> identities = getIdentities(group);

		List<SimpleGroupMember> ret = new ArrayList<>();
		for (Long memberId: memberships.keySet())
		{
			Map<String, Map<String, AttributeExt>> memberGroupsWithAttr = groupedAttributes.getOrDefault(memberId, Map.of());
			Collection<AttributeExt> groupAttributes = memberGroupsWithAttr.getOrDefault(group, Map.of()).values();
			List<AttributeExt> globalAttributes = memberGroupsWithAttr.getOrDefault("/", Map.of()).values().stream()
					.filter(y -> globalAttr.contains(y.getName()))
					.collect(toList());
			ret.add(new SimpleGroupMember(entityInfo.get(memberId), identities.get(memberId), Stream.concat(groupAttributes.stream(), globalAttributes.stream()).collect(toList())));
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
