/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import static java.util.function.Function.identity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.engine.credential.CredentialReqRepository;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.store.types.StoredIdentity;

@Component
class CompositeEntitiesInfoProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_BULK_OPS, CompositeEntitiesInfoProvider.class);
	static int USERS_THRESHOLD_ALLOWING_EFFECTIVE_GET_OF_SELECTED_USERS = 500;
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
	@Autowired
	private EnquiryFormDB enquiryDB;

	public GroupMembershipData getCompositeGroupContents(String group, Optional<Set<Long>> filter) throws EngineException
	{
		Stopwatch watch = Stopwatch.createStarted();
		Set<Long> filterSet = (filter == null || !filter.isPresent() ? null : filter.get());
		Set<Long> members = (filterSet == null ? getMembers(group)
				: getMembers(group).stream().filter(i -> filterSet.contains(i))
						.collect(Collectors.toSet()));

		GlobalSystemData globalData = loadGlobalData();
		EntitiesData entitiesData = getEntitiesDataOfSingleGroup(group, gm -> members.contains(gm.getEntityId()));
		
		GroupMembershipDataImpl ret = new GroupMembershipDataImpl(group, globalData, entitiesData);
		log.debug("Bulk group membership data retrieval: {}", watch.toString());
		return ret;
	}

	private EntitiesData getEntitiesDataOfSingleGroup(String group, Predicate<GroupMembership> groupMembershipFilter)
	{
		Map<Long, EntityInformation> entityInfo = getGroupEntitiesInfo(group);
		Map<Long, Set<String>> filteredMemberships;
		if(entityInfo.size() < USERS_THRESHOLD_ALLOWING_EFFECTIVE_GET_OF_SELECTED_USERS)
			filteredMemberships = getMemberships(entityInfo.keySet());
		else
			filteredMemberships = getFilteredMemberships(groupMembershipFilter);
		return EntitiesData.builder()
				.withMemberships(filteredMemberships)
				.withEntityInfo(entityInfo)
				.withIdentities(getIdentities(group))
				.withDirectAttributes(getAttributes(group))				
				.build();
	}
	
	public MultiGroupMembershipData getCompositeMultiGroupContents(String rootGroup, Set<String> groupFilter) throws EngineException
	{
		Stopwatch watch = Stopwatch.createStarted();

		GlobalSystemData globalData = loadGlobalData();
		
		Predicate<String> groupTester = groupFilter.isEmpty() ? 
				grp -> Group.isChildOrSame(grp, rootGroup) :
				grp -> groupFilter.contains(grp);
		Set<String> acceptedGroups = globalData.getGroups().keySet().stream()
				.filter(groupTester)
				.collect(Collectors.toSet());
		
		Map<Long, Set<String>> allMemberships = getFilteredMemberships(a -> true);
		Set<Long> relevantMembers = allMemberships.entrySet().stream()
			.filter(entry -> !Sets.intersection(entry.getValue(), acceptedGroups).isEmpty())
			.map(entry -> entry.getKey())
			.collect(Collectors.toSet());
		Predicate<Long> entityTester = entityId -> relevantMembers.contains(entityId);
		Map<Long, Set<String>> memberships = allMemberships.entrySet().stream()
				.filter(entry -> entityTester.test(entry.getKey()))
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
		EntitiesData entitiesData = EntitiesData.builder()
				.withMemberships(memberships)
				.withEntityInfo(getEntityInfo(entityTester))
				.withIdentities(getIdentities(entityTester))
				.withDirectAttributes(getAllAttributes(entityTester))				
				.build();
		MultiGroupMembershipData ret = new MultiGroupMembershipData(acceptedGroups, globalData, entitiesData);
		log.debug("Bulk multi-group membership data retrieval: {}", watch.toString());
		return ret;
	}
	
	public GroupStructuralData getGroupStructuralContents(String group) throws EngineException
	{
		Stopwatch watch = Stopwatch.createStarted();
		GroupStructuralDataImpl ret = GroupStructuralDataImpl.builder()
			.withGroup(group)
			.withGroups(groupDAO.getAllAsMap())
			.build();
		log.debug("Bulk group structural data retrieval: {}", watch.toString());
		return ret;
	}
	
	private GlobalSystemData loadGlobalData() throws EngineException
	{
		Stopwatch watch = Stopwatch.createStarted();
		GlobalSystemData ret = GlobalSystemData.builder()
				.withAttributeTypes(attributeTypeDAO.getAllAsMap())
				.withAttributeClasses(acDB.getAllAsMap())
				.withGroups(groupDAO.getAllAsMap())
				.withCredentials(credentialRepository.getCredentialDefinitions())
				.withCredentialRequirements(getCredentialRequirements())
				.withEnquiryForms(enquiryDB.getAllAsMap())
				.build();
		log.debug("loading global data {}", watch.toString());
		return ret;
	}
	
	private Map<String, CredentialRequirements> getCredentialRequirements() throws EngineException
	{
		return credentialReqRepository.getCredentialRequirements().stream()
			.collect(Collectors.toMap(cr -> cr.getName(), cr -> cr));
	}

	private Map<Long, Map<String, Map<String, AttributeExt>>> getAttributes(String group)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<StoredAttribute> all = attributeDAO.getAttributesOfGroupMembers(group);
		log.debug("getAttrs {}", w.toString());
		return mapAttributesByEntities(all.stream());
	}

	private Map<Long, Map<String, Map<String, AttributeExt>>> getAllAttributes(Predicate<Long> entityTester)
	{
		Stopwatch w = Stopwatch.createStarted();
		Stream<StoredAttribute> all = attributeDAO.getAll().stream()
				.filter(sa -> entityTester.test(sa.getEntityId()));
		log.debug("getAllAttrs {}", w.toString());
		return mapAttributesByEntities(all);
	}
	
	
	private Map<Long, Map<String, Map<String, AttributeExt>>> mapAttributesByEntities(Stream<StoredAttribute> all)
	{
		Map<Long, Map<String, Map<String, AttributeExt>>> ret = new HashMap<>();
		all.forEach(sa -> 
		{
			Map<String, Map<String, AttributeExt>> entityAttrs = ret.computeIfAbsent(
					sa.getEntityId(), key -> new HashMap<>());
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
	
	private Map<Long, EntityInformation> getGroupEntitiesInfo(String group)
	{
		return entityDAO.getByGroup(group).stream()
			.collect(Collectors.toMap(EntityInformation::getId, identity()));
	}

	private Map<Long, EntityInformation> getEntityInfo(Predicate<Long> entityTester)
	{
		return entityDAO.getAll().stream()
				.filter(ei -> entityTester.test(ei.getId()))
				.collect(Collectors.toMap(entity -> entity.getId(), entity->entity));
	}
	
	private Map<Long, List<Identity>> getIdentities(String group)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<StoredIdentity> all = identityDAO.getByGroup(group);
		log.debug("getIdentities {}", w.toString());
		return mapIdentitiesByEntity(all.stream());
	}

	private Map<Long, List<Identity>> getIdentities(Predicate<Long> entityTester)
	{
		Stopwatch w = Stopwatch.createStarted();
		Stream<StoredIdentity> all = identityDAO.getAll().stream().filter(si -> entityTester.test(si.getEntityId()));
		log.debug("getAllIdentities {}", w.toString());
		return mapIdentitiesByEntity(all);
	}
	
	private Map<Long, List<Identity>> mapIdentitiesByEntity(Stream<StoredIdentity> all)
	{
		Map<Long, List<Identity>> ret = new HashMap<>();
		all.forEach(storedIdentity -> 
				ret.computeIfAbsent(storedIdentity.getEntityId(), key -> new ArrayList<>())
					.add(storedIdentity.getIdentity()));
		return ret;
	}
	
	private Set<Long> getMembers(String group)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<GroupMembership> all = membershipDAO.getMembers(group);
		log.debug("getMembers {}", w.toString());
		Set<Long> ret = new HashSet<>();
		for (GroupMembership gm: all)
			ret.add(gm.getEntityId());
		return ret;
	}
	
	private Map<Long, Set<String>> getFilteredMemberships(Predicate<GroupMembership> filter)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<GroupMembership> all = membershipDAO.getAll();
		log.debug("getMemberships {}", w.toString());
		Map<Long, Set<String>> ret = new HashMap<>();
		all.stream()
			.filter(filter)
			.forEach(membership -> ret
					.computeIfAbsent(membership.getEntityId(), key -> new HashSet<>())
					.add(membership.getGroup()));
		return ret;
	}

	private Map<Long, Set<String>> getMemberships(Set<Long> entityIds)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<GroupMembership> all = entityIds.isEmpty() ? List.of() : membershipDAO.getEntityMemberships(entityIds);
		log.debug("getMemberships {}", w.toString());
		Map<Long, Set<String>> ret = new HashMap<>();
		for (GroupMembership membership : all)
		{
			ret.computeIfAbsent(membership.getEntityId(), key -> new HashSet<>())
				.add(membership.getGroup());
		}
		return ret;
	}
}
