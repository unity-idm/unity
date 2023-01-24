/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.group;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.capacityLimit.CapacityLimitName;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.attribute.AttributeClassUtil;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.audit.AuditEventTrigger;
import pl.edu.icm.unity.engine.audit.AuditEventTrigger.AuditEventTriggerBuilder;
import pl.edu.icm.unity.engine.audit.AuditPublisher;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.capacityLimits.InternalCapacityLimitVerificator;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.exceptions.EntityNotFoundException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.GroupsChain;
import pl.edu.icm.unity.types.basic.audit.AuditEventAction;
import pl.edu.icm.unity.types.basic.audit.AuditEventType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static pl.edu.icm.unity.types.basic.audit.AuditEventTag.GROUPS;
import static pl.edu.icm.unity.types.basic.audit.AuditEventTag.MEMBERS;


/**
 * Implementation of groups management. Responsible for top level transaction handling,
 * proper error logging and authorization.
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class GroupsManagementImpl implements GroupsManagement
{
	private GroupDAO dbGroups;
	private MembershipDAO membershipDAO;
	private GroupHelper groupHelper;
	private AttributeDAO dbAttributes;
	private AttributeTypeDAO attributeTypeDAO;
	private AttributeClassDB acDB;
	private InternalAuthorizationManager authz;
	private AttributesHelper attributesHelper;
	private EntityResolver idResolver;
	private EmailConfirmationManager confirmationManager;
	private TransactionalRunner tx;
	private AttributeClassUtil acUtil;
	private MessageSource msg;
	private AuditPublisher audit;
	private InternalCapacityLimitVerificator capacityLimitVerificator;

	
	@Autowired
	public GroupsManagementImpl(GroupDAO dbGroups, MembershipDAO membershipDAO,
			GroupHelper groupHelper, AttributeDAO dbAttributes,
			AttributeTypeDAO attributeTypeDAO, AttributeClassDB acDB,
			InternalAuthorizationManager authz, AttributesHelper attributesHelper,
			EntityResolver idResolver, EmailConfirmationManager confirmationManager,
			AttributeClassUtil acUtil, TransactionalRunner tx, MessageSource msg,
			AuditPublisher audit, InternalCapacityLimitVerificator capacityLimitVerificator)
	{
		this.dbGroups = dbGroups;
		this.membershipDAO = membershipDAO;
		this.groupHelper = groupHelper;
		this.dbAttributes = dbAttributes;
		this.attributeTypeDAO = attributeTypeDAO;
		this.acDB = acDB;
		this.authz = authz;
		this.attributesHelper = attributesHelper;
		this.idResolver = idResolver;
		this.confirmationManager = confirmationManager;
		this.acUtil = acUtil;
		this.tx = tx;
		this.msg = msg;
		this.audit = audit;
		this.capacityLimitVerificator = capacityLimitVerificator;
	}

	@Override
	@Transactional
	public void addGroup(Group toAdd, boolean withParents) throws EngineException
	{
		authz.checkAuthorization(toAdd.getParentPath(), AuthzCapability.groupModify);
		capacityLimitVerificator.assertInSystemLimitForSingleAdd(CapacityLimitName.GroupsCount, () -> dbGroups.getCount());
		groupHelper.validateGroupStatements(toAdd);
		AttributeClassUtil.validateAttributeClasses(toAdd.getAttributesClasses(), acDB);

		boolean groupExists = dbGroups.exists(toAdd.getParentPath());
		if (!groupExists && withParents)
			addGroup(new Group(toAdd.getParentPath()), withParents);
		else if (!groupExists)
			throw new IllegalArgumentException("Parent group " + toAdd.getParentPath() + " does not exist");

		if (toAdd.isPublic())
		{
			assertParentIsPrivate(toAdd);
		}

		dbGroups.create(toAdd);
		audit.log(AuditEventTrigger.builder()
			.type(AuditEventType.GROUP)
			.action(AuditEventAction.ADD)
			.name(toAdd.getName())
			.tags(GROUPS));
	}
	
	@Override
	@Transactional
	public void addGroups(Set<Group> toAdd) throws EngineException
	{
		Set<Group> onlyParentGroups = Group.getRootsOfSet(toAdd);
		for (Group parent : onlyParentGroups)
		{
			authz.checkAuthorization(parent.getParentPath(), AuthzCapability.groupModify);
		}
		capacityLimitVerificator.assertInSystemLimit(CapacityLimitName.GroupsCount,
				() -> dbGroups.getCount() + toAdd.size());
		List<Group> groupsSortedByPath = toAdd.stream().sorted()
				.collect(Collectors.toList());

		for (Group groupToAdd : groupsSortedByPath)
		{
			if (!dbGroups.exists(groupToAdd.getParentPath()))
			{
				throw new IllegalArgumentException("Parent group " + groupToAdd.getParentPath() + " does not exist");
			}
				
			if (groupToAdd.isPublic())
			{
				assertParentIsPrivate(groupToAdd);
			}
			dbGroups.create(groupToAdd);
		}
		
		for (Group addedGroup : groupsSortedByPath)
		{
			audit.log(AuditEventTrigger.builder().type(AuditEventType.GROUP).action(AuditEventAction.ADD)
					.name(addedGroup.getName()).tags(GROUPS));
		}
		
	}

	@Override
	@Transactional
	public void removeGroup(String path, boolean recursive) throws EngineException
	{
		authz.checkAuthorization(path, AuthzCapability.groupModify);
		if ("/".equals(path))
			throw new IllegalGroupValueException("Removing the root group is forbidden");
		if (!recursive && !getSubGroups(path).isEmpty())
			throw new IllegalGroupValueException("The group contains subgroups");
		try
		{
			dbGroups.delete(path);
		}
		catch (EntityNotFoundException e)
		{
			throw new GroupNotFoundException(e.getMessage());
		}

		audit.log(AuditEventTrigger.builder()
				.type(AuditEventType.GROUP)
				.action(AuditEventAction.REMOVE)
				.name(path)
				.tags(GROUPS));
	}

	@Override
	public void addMemberFromParent(String path, EntityParam entity) throws EngineException
	{
		addMemberFromParent(path, entity, null);
	}
	
	@Override
	public void addMemberFromParent(String path, EntityParam entity,
			List<Attribute> attributes) throws EngineException
	{
		addMemberFromParent(path, entity, attributes, null, null);
	}

	@Override
	public void addMemberFromParent(String path, EntityParam entity,
			final List<Attribute> attributesP, String idp, String translationProfile) throws EngineException
	{
		entity.validateInitialization();
		List<Attribute> attributes = attributesP == null ? Collections.emptyList() : attributesP;

		tx.runInTransactionThrowing(() -> {
			long entityId = idResolver.getEntityId(entity);
			authz.checkAuthorization(authz.isSelf(entityId), path, AuthzCapability.groupModify);
			
			attributesHelper.checkGroupAttributeClassesConsistency(attributes, path);
			
			groupHelper.addMemberFromParent(path, entity, idp, translationProfile, new Date());

			attributesHelper.addAttributesList(attributes, entityId, true);
		}); 
		
		//careful - must be after the transaction is committed
		confirmationManager.sendVerificationsQuietNoTx(entity, attributes, false);	
	}

	@Override
	@Transactional
	public void removeMember(String path, EntityParam entity) throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), path, AuthzCapability.groupModify);
		
		if (path.equals("/"))
			throw new IllegalArgumentException("The entity can not be removed from the root group");
		
		Set<String> entityMembership = membershipDAO.getEntityMembershipSimple(entityId);
		if (!entityMembership.contains(path))
			throw new IllegalArgumentException("The entity is not a member of the group " + path);
		for (String group: entityMembership)
		{
			if (Group.isChildOrSame(group, path))
			{
				membershipDAO.deleteByKey(entityId, group);
				audit.log(AuditEventTrigger.builder()
						.type(AuditEventType.MEMBERSHIP)
						.action(AuditEventAction.REMOVE)
						.name(group)
						.subject(entityId)
						.tags(MEMBERS, GROUPS));
				dbAttributes.deleteAttributesInGroup(entityId, group);
			}
		}
	}

	@Override
	@Transactional
	public GroupContents getContents(String path, int filter) throws EngineException
	{
		try
		{
			authz.checkAuthorization(path, AuthzCapability.read);
		} catch (AuthorizationException e)
		{
			if (((GroupContents.GROUPS | GroupContents.METADATA) & filter) == 0)
				throw e;
			return getLimitedContents(path, filter);
		}
		return getCompleteContents(path, filter);
	}
	
	private GroupContents getCompleteContents(String path, int filter) throws IllegalGroupValueException
	{
		GroupContents ret = new GroupContents();
		if ((filter & GroupContents.GROUPS) != 0)
		{
			List<String> directSubGroups = getDirectSubGroups(path);
			ret.setSubGroups(directSubGroups);
		}
		if ((filter & GroupContents.MEMBERS) != 0)
		{
			List<GroupMembership> members = membershipDAO.getMembers(path);
			ret.setMembers(members);
		}
		if ((filter & GroupContents.METADATA) != 0)
		{
			Group fullGroup;
			try
			{
				fullGroup = dbGroups.get(path);
			}
			catch (EntityNotFoundException e)
			{
				throw new GroupNotFoundException(e.getMessage());
			}

			ret.setGroup(fullGroup);
		}
		return ret;
	}
	
	/**
	 * Invoked whenever getContents fails due to insufficient authZ. In such case still some subgroups
	 * of the given group can be returned, only if the requester is their member.
	 * @param path
	 * @param filter
	 * @return
	 * @throws EngineException
	 */
	private GroupContents getLimitedContents(String path, int filter) throws EngineException
	{
		long entity = InvocationContext.getCurrent().getLoginSession().getEntityId();
		Set<String> allGroups = tx.runInTransactionRetThrowing(() -> {
			authz.checkAuthorization(true, AuthzCapability.read);
			return membershipDAO.getEntityMembershipSimple(entity);
		});
		if (!allGroups.contains(path))
			throw new AuthorizationException("Access is denied. The operation "
					+ "getContents requires 'read' capability");

		GroupContents ret = new GroupContents();

		if ((filter & GroupContents.GROUPS) != 0)
		{
			List<String> directSubgroups = new ArrayList<String>();

			for (String g: allGroups)
			{
				Group potential = new Group(g);
				String parent = potential.getParentPath();
				if (parent != null && parent.equals(path))
					directSubgroups.add(g);
			}
			ret.setSubGroups(directSubgroups);
		}			
		if ((filter & GroupContents.MEMBERS) != 0)
		{
			ret.setMembers(new ArrayList<>());
		}
		if ((filter & GroupContents.METADATA) != 0)
		{
			Group gMetadata;
			try
			{
				gMetadata = dbGroups.get(path);
			}
			catch (EntityNotFoundException e)
			{
				throw new GroupNotFoundException(e.getMessage());
			}
			ret.setGroup(gMetadata);
		}
		return ret;
	}

	@Override
	@Transactional
	public void updateGroup(String path, Group group) throws EngineException
	{
		updateGroup(path, group, null, null);
	}
	
	@Override
	@Transactional
	public void updateGroup(String path, Group group, String changedProperty, String newValue) throws EngineException
	{
		authz.checkAuthorization(path, AuthzCapability.groupModify);
		if (!path.equals(group.toString()))
			throw new IllegalGroupValueException("Changing group name is currently "
					+ "unsupported. Only displayed name can be changed.");
		groupHelper.validateGroupStatements(group);
		AttributeClassUtil.validateAttributeClasses(group.getAttributesClasses(), acDB);
		List<GroupMembership> gc = membershipDAO.getMembers(path);
		Map<String, AttributeType> allTypes = attributeTypeDAO.getAllAsMap();

		for (GroupMembership membership : gc)
		{
			long entity = membership.getEntityId();
			AttributeClassHelper helper = acUtil.getACHelper(entity, path, group.getAttributesClasses());
			Collection<String> attributes = getEntityInGroupAttributeNames(entity, path);
			helper.checkAttribtues(attributes, allTypes);
		}

		Group actual;
		try
		{
			actual = dbGroups.get(path);
		}
		catch (EntityNotFoundException e)
		{
			throw new GroupNotFoundException(e.getMessage());
		}

		boolean changingAccessMode = actual.isPublic() != group.isPublic();
		if (changingAccessMode)
		{
			if (!group.isPublic())
			{
				assertChildrenArePublic(actual, getDirectSubGroups(path));
			} else
			{
				assertParentIsPrivate(group);
			}
		}
		
		dbGroups.updateByName(path, group);
		AuditEventTriggerBuilder auditEvent = AuditEventTrigger.builder()
				.type(AuditEventType.GROUP)
				.action(AuditEventAction.UPDATE)
				.name(group.getName())
				.tags(GROUPS);
		if (nonNull(changedProperty) && nonNull(newValue)) {
			auditEvent.details(ImmutableMap.of("action", changedProperty, "value", newValue));
		}
		audit.log(auditEvent);
	}
	
	
	private Collection<String> getEntityInGroupAttributeNames(long entity, String path)
	{
		List<AttributeExt> attributes = dbAttributes.getEntityAttributes(entity, null, path);
		return attributes.stream().
				map(a -> a.getName()).
				collect(Collectors.toSet());
	}

	@Transactional
	@Override
	public Set<String> getChildGroups(String root) throws EngineException
	{
		authz.checkAuthorization(root, AuthzCapability.read);
		return getSubGroupsInclusive(root);
	}

	@Transactional
	@Override
	public boolean isPresent(String group) throws AuthorizationException
	{
		authz.checkAuthorization(group, AuthzCapability.read);
		return dbGroups.exists(group);
	}

	@Transactional
	@Override
	public void addGroup(Group toAdd) throws EngineException
	{
		addGroup(toAdd, false);
	}

	@Transactional
	@Override
	public GroupsChain getGroupsChain(String path)
	{
		authz.checkAuthorizationRT("/", AuthzCapability.read);
		return new GroupsChain(dbGroups.getGroupChain(path));
	}
	
	@Transactional
	@Override
	public List<Group> getGroupsByWildcard(String pathWildcard)
	{
		authz.checkAuthorizationRT("/", AuthzCapability.read);
		List<Group> all = dbGroups.getAll();
		return GroupPatternMatcher.filterMatching(all, pathWildcard);
	}
	
	@Transactional
	@Override
	public Map<String, Group> getAllGroups()
	{
		authz.checkAuthorizationRT("/", AuthzCapability.read);
		return dbGroups.getAllAsMap();
	}
	
	private Set<String> getSubGroupsInclusive(String root)
	{
		Set<String> allGroups = dbGroups.getAllNames();
		return allGroups.stream().
				filter(g -> Group.isChildOrSame(g, root)).
				collect(Collectors.toSet());
	}

	private Set<String> getSubGroups(String root)
	{
		Set<String> allGroups = dbGroups.getAllNames();
		return allGroups.stream().
				filter(g -> Group.isChild(g, root)).
				collect(Collectors.toSet());
	}

	private List<String> getDirectSubGroups(String root)
	{
		Set<String> allGroups = dbGroups.getAllNames();
		int prefix = root.length() + 1;
		return allGroups.stream().
				filter(g -> Group.isChild(g, root)).
				filter(g -> !g.substring(prefix).contains("/")).
				collect(Collectors.toList());
	}
	
	private void assertChildrenArePublic(Group group, List<String> childs) throws EngineException
	{
		for (String child : childs)
		{
			Group childGroup = dbGroups.get(child);
			if (childGroup.isPublic())
			{
				throw new PublicChildGroupException(group.getDisplayedName().getValue(msg),
						childGroup.getDisplayedName().getValue(msg));

			}
		}
	}

	private void assertParentIsPrivate(Group group) throws EngineException
	{
		if (!group.isTopLevel())
		{
			Group parent = dbGroups.get(group.getParentPath());
			if (!parent.isPublic())
			{
				throw new ParentIsPrivateGroupException(parent.getDisplayedName().getValue(msg),
						group.getDisplayedName().getValue(msg));
			}
		}
	}
	
	public static class PublicChildGroupException extends InternalException
	{
		public PublicChildGroupException(String parent, String child)
		{
			super("Cannot set group " + parent + " to private mode, child group " + child + " is public");
		}
	}

	public static class ParentIsPrivateGroupException extends InternalException
	{
		public ParentIsPrivateGroupException(String parent, String child)
		{
			super("Cannot set group " + child + " to public mode, parent group " + parent + " is private");
		}
	}
}
