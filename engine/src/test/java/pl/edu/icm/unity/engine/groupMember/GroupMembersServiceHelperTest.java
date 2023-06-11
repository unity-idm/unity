package pl.edu.icm.unity.engine.groupMember;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider.AUTHORIZATION_ROLE;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.groupMember.GroupMemberWithAttributes;
import pl.edu.icm.unity.engine.api.groupMember.GroupMembersService;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;

public class GroupMembersServiceHelperTest extends DBIntegrationTestBase
{
	@Autowired
	private GroupMembersService groupMembersService;

	@Test
	public void shouldRetrieveGroupMemberAttributesForSingleGroup() throws EngineException
	{
		groupsMan.addGroup(new Group("/A"));

		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"),
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());

		groupsMan.addMemberFromParent("/A", entity);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/", Lists.newArrayList("Anonymous User"));
		Attribute saInA = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/A", Lists.newArrayList("Inspector"));
		attrsMan.createAttribute(entity, saRoot);
		attrsMan.createAttribute(entity, saInA);


		List<GroupMemberWithAttributes> groupMembers = groupMembersService.getGroupMembersWithSelectedAttributes("/A", List.of(AUTHORIZATION_ROLE));

		assertThat(groupMembers.size(), is(1));
		assertThat(groupMembers.get(0).getEntityInformation().getId(), is(added.getEntityId()));
		assertThat(groupMembers.get(0).getIdentities(), hasItem(added));
		assertThat(groupMembers.get(0).getAttributes().stream().map(Attribute::getName).collect(Collectors.toList()), hasItem(AUTHORIZATION_ROLE));
	}

	@Test
	public void shouldNotRetrieveNotSelectedGroupMemberAttributesForSingleGroup() throws EngineException
	{
		aTypeMan.addAttributeType(new AttributeType("name", "string"));
		groupsMan.addGroup(new Group("/A"));

		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"),
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());

		groupsMan.addMemberFromParent("/A", entity);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/", Lists.newArrayList("Anonymous User"));
		Attribute saRootName = StringAttribute.of("name", "/", "ala");
		Attribute saInA = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/A", Lists.newArrayList("Inspector"));
		Attribute saInAName = StringAttribute.of("name", "/A", "ala");

		attrsMan.createAttribute(entity, saRoot);
		attrsMan.createAttribute(entity, saRootName);
		attrsMan.createAttribute(entity, saInA);
		attrsMan.createAttribute(entity, saInAName);


		List<GroupMemberWithAttributes> groupMembers = groupMembersService.getGroupMembersWithSelectedAttributes("/A", List.of(AUTHORIZATION_ROLE));

		assertThat(groupMembers.size(), is(1));
		assertThat(groupMembers.get(0).getEntityInformation().getId(), is(added.getEntityId()));
		assertThat(groupMembers.get(0).getIdentities(), hasItem(added));
		assertThat(groupMembers.get(0).getAttributes().size(), is(1));
		assertThat(groupMembers.get(0).getAttributes().stream().map(Attribute::getName).collect(Collectors.toList()), hasItem(AUTHORIZATION_ROLE));
	}

	@Test
	public void shouldRetrieveAllGroupMemberAttributesIfNotSelectedAny() throws EngineException
	{
		aTypeMan.addAttributeType(new AttributeType("name", "string"));
		groupsMan.addGroup(new Group("/A"));

		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"),
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());

		groupsMan.addMemberFromParent("/A", entity);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/", Lists.newArrayList("Anonymous User"));
		Attribute saRootName = StringAttribute.of("name", "/", "ala");
		Attribute saInA = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/A", Lists.newArrayList("Inspector"));
		Attribute saInAName = StringAttribute.of("name", "/A", "ala");

		attrsMan.createAttribute(entity, saRoot);
		attrsMan.createAttribute(entity, saRootName);
		attrsMan.createAttribute(entity, saInA);
		attrsMan.createAttribute(entity, saInAName);


		List<GroupMemberWithAttributes> groupMembers = groupMembersService.getGroupMembersWithSelectedAttributes("/A", List.of());

		assertThat(groupMembers.size(), is(1));
		assertThat(groupMembers.get(0).getEntityInformation().getId(), is(added.getEntityId()));
		assertThat(groupMembers.get(0).getIdentities(), hasItem(added));
		assertThat(groupMembers.get(0).getAttributes().size(), is(2));
		assertThat(groupMembers.get(0).getAttributes().stream().map(Attribute::getName).collect(Collectors.toList()), hasItems(AUTHORIZATION_ROLE, "name"));
	}

	@Test
	public void shouldRetrieveDirectGroupMemberAttributesIfDuplicatedWithGlobalAttribute() throws EngineException
	{
		AttributeType at = new AttributeType("name", "string");
		at.setGlobal(true);
		aTypeMan.addAttributeType(at);
		groupsMan.addGroup(new Group("/A"));

		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"),
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());

		groupsMan.addMemberFromParent("/A", entity);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/", Lists.newArrayList("Anonymous User"));
		Attribute saRootName = StringAttribute.of("name", "/", "ala");
		Attribute saInA = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/A", Lists.newArrayList("Inspector"));
		Attribute saInAName = StringAttribute.of("name", "/A", "ola");

		attrsMan.createAttribute(entity, saRoot);
		attrsMan.createAttribute(entity, saRootName);
		attrsMan.createAttribute(entity, saInA);
		attrsMan.createAttribute(entity, saInAName);


		List<GroupMemberWithAttributes> groupMembers = groupMembersService.getGroupMembersWithSelectedAttributes("/A", List.of());

		assertThat(groupMembers.size(), is(1));
		assertThat(groupMembers.get(0).getEntityInformation().getId(), is(added.getEntityId()));
		assertThat(groupMembers.get(0).getIdentities(), hasItem(added));
		assertThat(groupMembers.get(0).getAttributes().size(), is(2));
		assertThat(groupMembers.get(0).getAttributes().stream().map(Attribute::getName).collect(Collectors.toList()), hasItems(AUTHORIZATION_ROLE, "name"));
		assertThat(groupMembers.get(0).getAttributes().stream().flatMap(attributeExt -> attributeExt.getValues().stream()).collect(Collectors.toList()), hasItems("ola"));
	}

	@Test
	public void shouldRetrieveGlobalGroupMemberAttributes() throws EngineException
	{
		AttributeType at = new AttributeType("name", "string");
		at.setGlobal(true);
		aTypeMan.addAttributeType(at);
		groupsMan.addGroup(new Group("/A"));

		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"),
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());

		groupsMan.addMemberFromParent("/A", entity);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/", Lists.newArrayList("Anonymous User"));
		Attribute saRootName = StringAttribute.of("name", "/", "ala");
		Attribute saInA = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/A", Lists.newArrayList("Inspector"));

		attrsMan.createAttribute(entity, saRoot);
		attrsMan.createAttribute(entity, saRootName);
		attrsMan.createAttribute(entity, saInA);

		List<GroupMemberWithAttributes> groupMembers = groupMembersService.getGroupMembersWithSelectedAttributes("/A", List.of());

		assertThat(groupMembers.size(), is(1));
		assertThat(groupMembers.get(0).getEntityInformation().getId(), is(added.getEntityId()));
		assertThat(groupMembers.get(0).getIdentities(), hasItem(added));
		assertThat(groupMembers.get(0).getAttributes().size(), is(2));
		assertThat(groupMembers.get(0).getAttributes().stream().map(Attribute::getName).collect(Collectors.toList()), hasItems(AUTHORIZATION_ROLE, "name"));
		assertThat(groupMembers.get(0).getAttributes().stream().flatMap(attributeExt -> attributeExt.getValues().stream()).collect(Collectors.toList()), hasItems("ala"));
	}

	@Test
	public void shouldRetrieveGroupMemberAttributesForTwoGroups() throws EngineException
	{
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));

		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"),
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());

		groupsMan.addMemberFromParent("/A", entity);
		groupsMan.addMemberFromParent("/B", entity);

		Attribute saInB = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/B", Lists.newArrayList("Anonymous User"));
		Attribute saInA = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/A", Lists.newArrayList("Inspector"));
		attrsMan.createAttribute(entity, saInB);
		attrsMan.createAttribute(entity, saInA);


		Map<String, List<GroupMemberWithAttributes>> groupMembers = groupMembersService.getGroupsMembersInGroupsWithSelectedAttributes(List.of("/A", "/B"), List.of(AUTHORIZATION_ROLE));

		assertThat(groupMembers.size(), is(2));
		assertThat(groupMembers.get("/A").get(0).getEntityInformation().getId(), is(added.getEntityId()));
		assertThat(groupMembers.get("/A").get(0).getIdentities(), hasItem(added));
		assertThat(
				groupMembers.get("/A").get(0).getAttributes().stream().map(Attribute::getName).collect(Collectors.toList()),
				hasItem(AUTHORIZATION_ROLE)
		);
		assertThat(
				groupMembers.get("/A").get(0).getAttributes().stream().map(Attribute::getValues).flatMap(Collection::stream).collect(Collectors.toList()),
				hasItem("Inspector")
		);

		assertThat(groupMembers.get("/B").get(0).getEntityInformation().getId(), is(added.getEntityId()));
		assertThat(groupMembers.get("/B").get(0).getIdentities(), hasItem(added));
		assertThat(
				groupMembers.get("/B").get(0).getAttributes().stream().map(Attribute::getName).collect(Collectors.toList()),
				hasItem(AUTHORIZATION_ROLE)
		);
		assertThat(
				groupMembers.get("/B").get(0).getAttributes().stream().map(Attribute::getValues).flatMap(Collection::stream).collect(Collectors.toList()),
				hasItem("Anonymous User")
		);

	}
}
