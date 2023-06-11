/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityGroupAttributes;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.bulk.GroupsWithMembers;
import pl.edu.icm.unity.engine.api.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.engine.attribute.AttributeStatementProcessor;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.credential.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.forms.enquiry.EnquiryTargetCondEvaluator;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

@Component
@Primary
class BulkQueryServiceImpl implements BulkGroupQueryService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_BULK_OPS, BulkQueryServiceImpl.class);
	
	private final AttributeStatementProcessor statementsHelper;
	private final EntityCredentialsHelper credentialsHelper;
	private final LocalCredentialsRegistry localCredReg;
	private final CompositeEntitiesInfoProvider dataProvider;
	private final InternalAuthorizationManager authz;
	private final TransactionalRunner tx;
	private final ForkJoinPool pool = ForkJoinPool.commonPool();
	
	@Autowired
	public BulkQueryServiceImpl(AttributeStatementProcessor statementsHelper,
			EntityCredentialsHelper credentialsHelper,
			LocalCredentialsRegistry localCredReg,
			CompositeEntitiesInfoProvider dataProvider,
			InternalAuthorizationManager authz,
			TransactionalRunner tx)
	{
		this.statementsHelper = statementsHelper;
		this.credentialsHelper = credentialsHelper;
		this.localCredReg = localCredReg;
		this.dataProvider = dataProvider;
		this.authz = authz;
		this.tx = tx;
	}


	@Override
	public GroupsWithMembers getMembersWithAttributeForAllGroups(String rootGroup, Set<String> groupFilter)
	{
		Stopwatch watch = Stopwatch.createStarted();
		if (groupFilter.stream().filter(grp -> !Group.isChildOrSame(grp, rootGroup)).findAny().isPresent())
			throw new IllegalArgumentException("All filter groups must be child of the rootGroup");
		MultiGroupMembershipData data = getMultiGroupMembershipData(rootGroup, groupFilter);
		GroupsWithMembers ret = assembleGroupsWithAttributes(data);
		log.debug("Bulk multi-group membership data retrieval of {} groups: {}", 
				ret.membersByGroup.keySet().size(), watch.toString());
		return ret; 
	}

	private MultiGroupMembershipData getMultiGroupMembershipData(String rootGroup, Set<String> groupFilter)
	{
		try
		{
			return tx.runInTransactionRetThrowing(() -> 
			{
				authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
				return dataProvider.getCompositeMultiGroupContents(rootGroup, groupFilter);
			});
		} catch (EngineException e)
		{
			throw new RuntimeEngineException(e);
		}
	}

	private GroupsWithMembers assembleGroupsWithAttributes(MultiGroupMembershipData data)
	{
		Map<Long, Entity> entities = getGroupEntitiesNoContext(false, data.entitiesData, data.globalSystemData);
		
		List<TaskWithGroup> tasks = new ArrayList<>(data.groups.size());
		for (String group: data.groups)
			tasks.add(new TaskWithGroup(
					pool.submit(() -> getGroupEntityAttribtues(group, data)), 
					group));

		Map<String, List<EntityGroupAttributes>> attributes = new HashMap<>(); 
		for (TaskWithGroup task: tasks)
			try
			{
				attributes.put(task.group, task.task.get());
			} catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while waiting for tasks", e);
			} catch (ExecutionException e)
			{
				throw new RuntimeException("Error in concurrent task", e);
			}
		return new GroupsWithMembers(entities, attributes);
	}
	
	private static class TaskWithGroup
	{
		private final ForkJoinTask<List<EntityGroupAttributes>> task;
		private final String group;
		
		TaskWithGroup(ForkJoinTask<List<EntityGroupAttributes>> task, String group)
		{
			this.task = task;
			this.group = group;
		}
	}
	
	private List<EntityGroupAttributes> getGroupEntityAttribtues(String group, MultiGroupMembershipData data)
	{
		Map<Long, Map<String, AttributeExt>> groupUsersAttributes = 
				getGroupUsersAttributes(group, data.entitiesData, data.globalSystemData);
		List<EntityGroupAttributes> groupEntityAttributes = new ArrayList<>(groupUsersAttributes.size());
		for (Map.Entry<Long, Map<String, AttributeExt>> entry: groupUsersAttributes.entrySet())
			groupEntityAttributes.add(new EntityGroupAttributes(entry.getKey(), entry.getValue()));
		return groupEntityAttributes;
	}
	
	@Transactional
	@Override
	public GroupMembershipData getBulkMembershipData(String group, Set<Long> filter) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		return dataProvider.getCompositeGroupContents(group, Optional.ofNullable(filter));
	}
	
	@Transactional
	@Override
	public GroupMembershipData getBulkMembershipData(String group) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		return dataProvider.getCompositeGroupContents(group, Optional.empty());
	}

	@Transactional
	@Override
	public GroupStructuralData getBulkStructuralData(String group) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		return dataProvider.getGroupStructuralContents(group);
	}
	
	/**
	 * @return all effective attributes of all entities in the group (including disabled ones)
	 */
	@Override
	public Map<Long, Map<String, AttributeExt>> getGroupUsersAttributes(String group, GroupMembershipData dataO)
	{
		GroupMembershipDataImpl data = (GroupMembershipDataImpl) dataO;
		return getGroupUsersAttributes(group, data.entitiesData, data.globalSystemData);
	}
	
	private Map<Long, Map<String, AttributeExt>> getGroupUsersAttributes(String group, EntitiesData entitiesData, 
			GlobalSystemData globalSystemData)
	{
		Stopwatch watch = Stopwatch.createStarted();
		Map<Long, Map<String, AttributeExt>> ret = new HashMap<>();
		for (Long entityId: entitiesData.getEntityInfo().keySet())
		{
			Set<String> memberships = entitiesData.getMemberships().get(entityId);
			if (memberships != null && memberships.contains(group))
				ret.put(entityId, getAllAttributesAsMap(entityId, group, entitiesData, globalSystemData));
		}
		log.debug("Bulk attributes assembly of {}: {}", group, watch.toString());
		return ret;
	}
	
	@Override
	public Map<Long, EntityInGroupData> getMembershipInfo(GroupMembershipData dataO)
	{
		Stopwatch watch = Stopwatch.createStarted();
		GroupMembershipDataImpl data = (GroupMembershipDataImpl) dataO;
		Map<Long, Set<String>> memberships = data.entitiesData.getMemberships();
		Map<Long, EntityInGroupData> ret = new HashMap<>();

		for (Long e : memberships.keySet())
		{
			CredentialInfo credentialInfo = getCredentialInfo(e, data.entitiesData, data.globalSystemData);
			Entity entity = assembleEntity(e, false, data.entitiesData, data.globalSystemData);
			Map<String, AttributeExt> groupAttributesAsMap = getAllAttributesAsMap(e, data.group, 
					data.entitiesData, data.globalSystemData);
			Map<String, AttributeExt> rootAttributesAsMap = data.group.equals("/") ? 
					groupAttributesAsMap : 
					getAllAttributesAsMap(e, "/", data.entitiesData, data.globalSystemData);
			ret.put(e, new EntityInGroupData(
					entity,
					data.group,
					memberships.get(e), 
					groupAttributesAsMap,
					rootAttributesAsMap,
					getEnquiryForms(e, data, credentialInfo)));
		}

		log.debug("Bulk members with groups: {}", watch.toString());
		return ret;
	}

	private Set<String> getEnquiryForms(Long e, GroupMembershipDataImpl data, CredentialInfo credentialInfo)
	{
		Set<String> forms = new HashSet<>();

		for (EnquiryForm enqForm : data.globalSystemData.getEnquiryForms().values())
		{
			if (EnquiryTargetCondEvaluator.evaluateTargetCondition(enqForm,
							data.entitiesData.getIdentities().get(e),
							data.entitiesData.getEntityInfo().get(e).getEntityState().toString(),
							credentialInfo,
							data.entitiesData.getMemberships().get(e),
							data.entitiesData.getDirectAttributes().get(e).get("/").values()))
				forms.add(enqForm.getName());
		}
		return forms;
	}
	
	@Override
	public Map<Long, Entity> getGroupEntitiesNoContextWithTargeted(GroupMembershipData dataO)
	{
		GroupMembershipDataImpl data = (GroupMembershipDataImpl) dataO;
		return getGroupEntitiesNoContext(true, data.entitiesData, data.globalSystemData);
	}

	@Override
	public Map<Long, Entity> getGroupEntitiesNoContextWithoutTargeted(GroupMembershipData dataO)
	{
		GroupMembershipDataImpl data = (GroupMembershipDataImpl) dataO;
		return getGroupEntitiesNoContext(false, data.entitiesData, data.globalSystemData);
	}
	

	@Override
	public Map<String, GroupContents> getGroupAndSubgroups(GroupStructuralData dataO)
	{
		Stopwatch watch = Stopwatch.createStarted();
		Map<String, GroupContents> ret = new HashMap<>();
		GroupStructuralDataImpl data = (GroupStructuralDataImpl) dataO;
		Set<String> allGroups = data.getGroups().keySet();
		for (Group group: data.getGroups().values())
		{
			if (!Group.isChildOrSame(group.toString(), data.getGroup()))
				continue;
			GroupContents entry = new GroupContents();
			entry.setGroup(group);
			entry.setSubGroups(getDirectSubGroups(group.toString(), allGroups));
			ret.put(group.toString(), entry);
		}
		log.debug("Bulk group and subgroups resolve: {}", watch.toString());
		return ret;
	}
	
	@Override
	public Map<String, GroupContents> getGroupAndSubgroups(GroupStructuralData dataO, String subGroup)
	{
		GroupStructuralDataImpl data = (GroupStructuralDataImpl) dataO;
		if (!Group.isChildOrSame(subGroup, data.getGroup()))
		{
			throw new IllegalArgumentException(
					"Group " + subGroup + " is not child of group structural data root group " + data.getGroup());
		}
		Stopwatch watch = Stopwatch.createStarted();
		Map<String, GroupContents> ret = new HashMap<>();

		Set<String> allGroups = data.getGroups().keySet();
		for (Group group : data.getGroups().values())
		{
			if (!Group.isChildOrSame(group.toString(), subGroup))
				continue;
			GroupContents entry = new GroupContents();
			entry.setGroup(group);
			entry.setSubGroups(getDirectSubGroups(group.toString(), allGroups));
			ret.put(group.toString(), entry);
		}
		log.debug("Bulk group and subgroups resolve: {}", watch.toString());
		return ret;
	}
	
	private List<String> getDirectSubGroups(String root, Set<String> allGroups)
	{
		int prefix = root.length() + 1;
		return allGroups.stream().
				filter(g -> Group.isChild(g, root)).
				filter(g -> !g.substring(prefix).contains("/")).
				collect(Collectors.toList());
	}
	
	private Map<Long, Entity> getGroupEntitiesNoContext(boolean includeTargeted, 
			EntitiesData entitiesData, GlobalSystemData globalSystemData)
	{
		Stopwatch watch = Stopwatch.createStarted();
		Map<Long, Entity> ret = new HashMap<>();
		for (Long entityId: entitiesData.getEntityInfo().keySet())
			ret.put(entityId, assembleEntity(entityId, includeTargeted, entitiesData, globalSystemData));
		log.debug("Bulk entities assembly: {}", watch.toString());
		return ret;
	}
	
	private Entity assembleEntity(long entityId, boolean includeTargeted, EntitiesData entitiesData, 
			GlobalSystemData globalSystemData)
	{
		CredentialInfo credInfo = getCredentialInfo(entityId, entitiesData, globalSystemData);
		List<Identity> identitites = entitiesData.getIdentities().get(entityId);
		if (!includeTargeted)
			identitites = filterTargetedIdentitites(identitites);
		return new Entity(identitites, entitiesData.getEntityInfo().get(entityId), credInfo);
	}
	
	private List<Identity> filterTargetedIdentitites(List<Identity> all)
	{
		return all.stream().filter(id -> id.getTarget() == null).collect(Collectors.toList());
	}
	
	private Map<String, AttributeExt> getAllAttributesAsMap(long entityId, String group, 
			EntitiesData entitiesData, GlobalSystemData globalSystemData) 
	{
		Map<String, Map<String, AttributeExt>> directAttributesByGroup = entitiesData.getDirectAttributes().get(entityId);
		List<Group> allGroups = entitiesData.getMemberships().get(entityId)
				.stream()
				.map(g -> globalSystemData.getGroups().get(g))
				.collect(Collectors.toList());
		List<Identity> identities = entitiesData.getIdentities().get(entityId);
		return statementsHelper.getEffectiveAttributes(identities, 
				group, null, allGroups, directAttributesByGroup, 
				globalSystemData.getAttributeClasses(),
				g -> globalSystemData.getGroups().get(g),
				globalSystemData.getAttributeTypes()::get,
				g -> globalSystemData.getCachingMVELGroupProvider().get(g));
	}
	
	private CredentialInfo getCredentialInfo(long entityId, EntitiesData entitiesData, GlobalSystemData globalSystemData)
	{
		Map<String, AttributeExt> attributes = entitiesData.getDirectAttributes().get(entityId).get("/");
		String credentialRequirementId = credentialsHelper.getCredentialReqFromAttribute(attributes);
		
		CredentialRequirementsHolder credReq;
		try
		{
			credReq = new CredentialRequirementsHolder(localCredReg, 
					globalSystemData.getCredentialRequirements().get(credentialRequirementId), 
					globalSystemData.getCredentials());
		} catch (IllegalCredentialException e)
		{
			throw new InternalException("Unknown credential assigned to entity", e);
		}
		return credentialsHelper.getCredentialInfoNoQuery(entityId, attributes, credReq, credentialRequirementId);
	}
}
