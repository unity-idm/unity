/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.bulk.GroupsWithMembers;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.*;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider.AUTHORIZATION_ROLE;

public class BulkGroupQueryServiceImplTest extends DBIntegrationTestBase
{
	@Autowired
	private BulkGroupQueryService bulkService;

	final int currentUsersLimitAllowingEffectivelySearch = CompositeEntitiesInfoProvider.USERS_THRESHOLD_ALLOWING_EFFECTIVE_GET_OF_SELECTED_USERS;

	void changeUsersLimitAllowingToSearchToDefault()
	{
		CompositeEntitiesInfoProvider.USERS_THRESHOLD_ALLOWING_EFFECTIVE_GET_OF_SELECTED_USERS = currentUsersLimitAllowingEffectivelySearch;
	}

	void changeUsersLimitAllowingToSearch(int value)
	{
		CompositeEntitiesInfoProvider.USERS_THRESHOLD_ALLOWING_EFFECTIVE_GET_OF_SELECTED_USERS = value;
	}

	@Test
	public void shouldRetrieveEntitiesFromSubgroup() throws EngineException
	{
		groupsMan.addGroup(new Group("/A"));
		
		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), 
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());
		
		groupsMan.addMemberFromParent("/A", entity);
		
		
		GroupMembershipData bulkData = bulkService.getBulkMembershipData("/A");
		Map<Long, Entity> result = bulkService.getGroupEntitiesNoContextWithTargeted(bulkData);
		
		assertThat(result.size(), is(1));
		assertThat(result.get(added.getEntityId()), is(notNullValue()));
		assertThat(result.get(added.getEntityId()).getIdentities(), hasItem(added));
	}

	@Test
	public void shouldRetrieveAttributesOfEntitiesInGroupInEffectiveWay() throws EngineException
	{
		groupsMan.addGroup(new Group("/A"));

		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"),
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());

		Identity added2 = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "2"),
				EntityState.valid);
		EntityParam entity2 = new EntityParam(added2.getEntityId());

		groupsMan.addMemberFromParent("/A", entity);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/", Lists.newArrayList("Anonymous User"));
		Attribute saInA = EnumAttribute.of(AUTHORIZATION_ROLE,
				"/A", Lists.newArrayList("Inspector"));
		attrsMan.createAttribute(entity, saRoot);
		attrsMan.createAttribute(entity2, saRoot);
		attrsMan.createAttribute(entity, saInA);

		changeUsersLimitAllowingToSearch(0);
		GroupMembershipData bulkData = bulkService.getBulkMembershipData("/A");
		Map<Long, Map<String, AttributeExt>> resultInA = bulkService.getGroupUsersAttributes("/A", bulkData);
		Map<Long, Map<String, AttributeExt>> resultInRoot = bulkService.getGroupUsersAttributes("/", bulkData);

		assertThat(resultInA.size(), is(1));
		assertThat(resultInA.get(added.getEntityId()), is(notNullValue()));
		assertThat(resultInA.get(added.getEntityId()).size(), is(1));
		assertThat(resultInA.get(added.getEntityId()).get(AUTHORIZATION_ROLE).getValues().get(0), is("Inspector"));

		assertThat(resultInRoot.size(), is(1));
		assertThat(resultInRoot.get(added.getEntityId()), is(notNullValue()));
		assertThat(resultInRoot.get(added.getEntityId()).size(), is(2)); //+ cred req mandatory attr
		assertThat(resultInRoot.get(added.getEntityId()).get(AUTHORIZATION_ROLE).getValues().get(0), is("Anonymous User"));
		changeUsersLimitAllowingToSearchToDefault();
	}

	@Test
	public void shouldRetrieveAttributesOfEntitiesInGroup() throws EngineException
	{
		groupsMan.addGroup(new Group("/A"));
		
		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), 
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());
		
		Identity added2 = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "2"), 
				EntityState.valid);
		EntityParam entity2 = new EntityParam(added2.getEntityId());
		
		groupsMan.addMemberFromParent("/A", entity);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE, 
				"/", Lists.newArrayList("Anonymous User"));
		Attribute saInA = EnumAttribute.of(AUTHORIZATION_ROLE, 
				"/A", Lists.newArrayList("Inspector"));
		attrsMan.createAttribute(entity, saRoot);
		attrsMan.createAttribute(entity2, saRoot);
		attrsMan.createAttribute(entity, saInA);
		
		changeUsersLimitAllowingToSearch(0);
		GroupMembershipData bulkData = bulkService.getBulkMembershipData("/A");
		Map<Long, Map<String, AttributeExt>> resultInA = bulkService.getGroupUsersAttributes("/A", bulkData);
		Map<Long, Map<String, AttributeExt>> resultInRoot = bulkService.getGroupUsersAttributes("/", bulkData);
		
		assertThat(resultInA.size(), is(1));
		assertThat(resultInA.get(added.getEntityId()), is(notNullValue()));
		assertThat(resultInA.get(added.getEntityId()).size(), is(1));
		assertThat(resultInA.get(added.getEntityId()).get(AUTHORIZATION_ROLE).getValues().get(0), is("Inspector"));

		assertThat(resultInRoot.size(), is(1));
		assertThat(resultInRoot.get(added.getEntityId()), is(notNullValue()));
		assertThat(resultInRoot.get(added.getEntityId()).size(), is(2)); //+ cred req mandatory attr
		assertThat(resultInRoot.get(added.getEntityId()).get(AUTHORIZATION_ROLE).getValues().get(0), is("Anonymous User"));
		changeUsersLimitAllowingToSearchToDefault();
	}
	
	@Test
	public void shouldRetrieveDynamicAttributesEntitiesInGroup() throws EngineException
	{
		Group example = new Group("/example");
		AttributeStatement addAttr = new AttributeStatement("eattr contains 'sys:AuthorizationRole'", "/", 
				ConflictResolution.skip, 
				"sys:AuthorizationRole", "eattr['sys:AuthorizationRole']");
		example.setAttributeStatements(new AttributeStatement[] {addAttr});
		groupsMan.addGroup(example);
		
		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), 
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());
		
		groupsMan.addMemberFromParent("/example", entity);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE, "/", Lists.newArrayList("Inspector"));
		attrsMan.createAttribute(entity, saRoot);
		
		
		GroupMembershipData bulkData = bulkService.getBulkMembershipData("/example");
		Map<Long, Map<String, AttributeExt>> resultInA = bulkService.getGroupUsersAttributes("/example", bulkData);
		Map<Long, Map<String, AttributeExt>> resultInRoot = bulkService.getGroupUsersAttributes("/", bulkData);
		
		assertThat(resultInA.size(), is(1));
		assertThat(resultInA.get(added.getEntityId()), is(notNullValue()));
		assertThat(resultInA.get(added.getEntityId()).size(), is(1));
		assertThat(resultInA.get(added.getEntityId()).get(AUTHORIZATION_ROLE).getValues().get(0), is("Inspector"));

		assertThat(resultInRoot.size(), is(1));
		assertThat(resultInRoot.get(added.getEntityId()), is(notNullValue()));
		assertThat(resultInRoot.get(added.getEntityId()).size(), is(2)); //+ cred req mandatory attr
		assertThat(resultInRoot.get(added.getEntityId()).get(AUTHORIZATION_ROLE).getValues().get(0), is("Inspector"));
	}
	
	@Test
	public void shouldRetrieveSubgroups() throws EngineException
	{
		Group a = new Group("/A");
		a.setDescription(new I18nString("desc"));
		Group ab = new Group("/A/B");
		groupsMan.addGroup(a);
		groupsMan.addGroup(ab);
		groupsMan.addGroup(new Group("/C"));
		
		
		GroupStructuralData bulkData = bulkService.getBulkStructuralData("/A");
		Map<String, GroupContents> result = bulkService.getGroupAndSubgroups(bulkData);
		
		assertThat(result.size(), is(2));
		assertThat(result.get("/A"), is(notNullValue()));
		assertThat(result.get("/A").getGroup(), is(a));
		assertThat(result.get("/A").getSubGroups(), is(Lists.newArrayList("/A/B")));
		assertThat(result.get("/A/B"), is(notNullValue()));
		assertThat(result.get("/A/B").getGroup(), is(ab));
		assertThat(result.get("/A/B").getSubGroups(), is(Lists.newArrayList()));
	}
	
	@Test
	public void shouldRetrieveSubgroupsOfSelectedSubgroup() throws EngineException
	{
		Group a = new Group("/A");
		a.setDescription(new I18nString("desc"));
		Group ab = new Group("/A/B");
		Group abd = new Group("/A/B/D");
		groupsMan.addGroup(a);
		groupsMan.addGroup(ab);
		groupsMan.addGroup(abd);
		groupsMan.addGroup(new Group("/C"));
		
		GroupStructuralData bulkData = bulkService.getBulkStructuralData("/A");
		Map<String, GroupContents> result = bulkService.getGroupAndSubgroups(bulkData, "/A/B");
		assertThat(result.size(), is(2));
		assertThat(result.get("/A/B"), is(notNullValue()));
		assertThat(result.get("/A/B").getGroup(), is(ab));
		assertThat(result.get("/A/B").getSubGroups(), is(Lists.newArrayList("/A/B/D")));
		assertThat(result.get("/A/B/D"), is(notNullValue()));
		assertThat(result.get("/A/B/D").getGroup(), is(abd));
		assertThat(result.get("/A/B/D").getSubGroups(), is(Collections.emptyList()));
	}

	
	@Test
	public void shouldRetrieveContentsFromMultipleGroups() throws EngineException
	{
		Group example1 = new Group("/example1");
		AttributeStatement addAttr1 = new AttributeStatement("eattr contains 'sys:AuthorizationRole'", "/", 
				ConflictResolution.skip, 
				"sys:AuthorizationRole", "eattr['sys:AuthorizationRole']");
		example1.setAttributeStatements(new AttributeStatement[] {addAttr1});
		groupsMan.addGroup(example1);

		Group example2 = new Group("/example2");
		AttributeStatement addAttr2 = new AttributeStatement("true", "/", 
				ConflictResolution.skip, 
				"sys:AuthorizationRole", "'System Manager'");
		example2.setAttributeStatements(new AttributeStatement[] {addAttr2});
		groupsMan.addGroup(example2);

		
		Identity added1 = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), 
				EntityState.valid);
		EntityParam entity1 = new EntityParam(added1.getEntityId());
		
		groupsMan.addMemberFromParent("/example1", entity1);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE, "/", Lists.newArrayList("Inspector"));
		attrsMan.createAttribute(entity1, saRoot);

		Identity added2 = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "2"), 
				EntityState.valid);
		EntityParam entity2 = new EntityParam(added2.getEntityId());
		groupsMan.addMemberFromParent("/example2", entity2);
		
		
		GroupsWithMembers result = bulkService.getMembersWithAttributeForAllGroups("/", Collections.emptySet());

		
		assertThat(result.entities.size(), is(3));
		assertThat(result.entities.get(added1.getEntityId()), is(notNullValue()));
		assertThat(result.entities.get(added1.getEntityId()).getIdentities(), hasItem(added1));
		assertThat(result.entities.get(added2.getEntityId()), is(notNullValue()));
		assertThat(result.entities.get(added2.getEntityId()).getIdentities(), hasItem(added2));

		assertThat(result.membersByGroup.size(), is(3));
		assertThat(result.membersByGroup.get("/example1"), is(notNullValue()));
		assertThat(result.membersByGroup.get("/example1").size(), is(1));
		assertThat(result.membersByGroup.get("/example1").get(0).entityId, is(added1.getEntityId()));
		assertThat(result.membersByGroup.get("/example1").get(0).attribtues.size(), is(1));
		assertThat(result.membersByGroup.get("/example1").get(0).attribtues.get(AUTHORIZATION_ROLE).getValues().get(0), 
				is("Inspector"));

		assertThat(result.membersByGroup.get("/example2"), is(notNullValue()));
		assertThat(result.membersByGroup.get("/example2").size(), is(1));
		assertThat(result.membersByGroup.get("/example2").get(0).entityId, is(added2.getEntityId()));
		assertThat(result.membersByGroup.get("/example2").get(0).attribtues.size(), is(1));
		assertThat(result.membersByGroup.get("/example2").get(0).attribtues.get(AUTHORIZATION_ROLE).getValues().get(0), 
				is("System Manager"));
	}

	
	@Test
	public void shouldRetrieveContentsFromMultipleGroupsWithAttrStmtOnNotIncludedGroup() throws EngineException
	{
		Group root = new Group("/root");
		groupsMan.addGroup(root);

		Group example1 = new Group("/root/example1");
		AttributeStatement addAttr1 = new AttributeStatement("true", "/root", 
				ConflictResolution.skip, 
				"sys:AuthorizationRole", "eattr['sys:AuthorizationRole']");
		example1.setAttributeStatements(new AttributeStatement[] {addAttr1});
		groupsMan.addGroup(example1);

		Identity added1 = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), 
				EntityState.valid);
		EntityParam entity1 = new EntityParam(added1.getEntityId());
		
		groupsMan.addMemberFromParent("/root", entity1);
		Attribute saEx2 = EnumAttribute.of(AUTHORIZATION_ROLE, "/root", Lists.newArrayList("Inspector"));
		attrsMan.createAttribute(entity1, saEx2);

		groupsMan.addMemberFromParent("/root/example1", entity1);


		GroupsWithMembers result = bulkService.getMembersWithAttributeForAllGroups("/root/example1", Collections.emptySet());

		
		assertThat(result.entities.size(), is(1));
		assertThat(result.entities.get(added1.getEntityId()), is(notNullValue()));
		assertThat(result.entities.get(added1.getEntityId()).getIdentities(), hasItem(added1));

		assertThat(result.membersByGroup.size(), is(1));
		assertThat(result.membersByGroup.get("/root/example1"), is(notNullValue()));
		assertThat(result.membersByGroup.get("/root/example1").size(), is(1));
		assertThat(result.membersByGroup.get("/root/example1").get(0).entityId, is(added1.getEntityId()));
		assertThat(result.membersByGroup.get("/root/example1").get(0).attribtues.size(), is(1));
		assertThat(result.membersByGroup.get("/root/example1").get(0).attribtues.get(AUTHORIZATION_ROLE).getValues().get(0), 
				is("Inspector"));
	}
	
	@Test
	public void shouldFilterGroupsInMultiQuery() throws EngineException
	{
		Group example1 = new Group("/example1");
		groupsMan.addGroup(example1);

		Group example2 = new Group("/example2");
		groupsMan.addGroup(example2);

		Identity added1 = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), 
				EntityState.valid);
		EntityParam entity1 = new EntityParam(added1.getEntityId());
		groupsMan.addMemberFromParent("/example1", entity1);

		GroupsWithMembers result = bulkService.getMembersWithAttributeForAllGroups("/", Sets.newHashSet("/example1"));

		assertThat(result.entities.size(), is(1));

		assertThat(result.membersByGroup.size(), is(1));
		assertThat(result.membersByGroup.get("/example1"), is(notNullValue()));
		assertThat(result.membersByGroup.get("/example1").size(), is(1));
		assertThat(result.membersByGroup.get("/example1").get(0).attribtues.size(), is(0));
	}
	
	
	
	@Test
	public void shouldNotAcceptFilterOutsideOfRootGroup() throws EngineException
	{
		Throwable error = catchThrowable(() -> bulkService.getMembersWithAttributeForAllGroups("/example1/foo", 
				Sets.newHashSet("/example1/bar/test")));

		assertThat(error).isNotNull().isInstanceOf(IllegalArgumentException.class);
	}
}
