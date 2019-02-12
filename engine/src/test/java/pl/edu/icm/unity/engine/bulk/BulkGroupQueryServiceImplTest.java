/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider.AUTHORIZATION_ROLE;

import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class BulkGroupQueryServiceImplTest extends DBIntegrationTestBase
{
	@Autowired
	private BulkGroupQueryService bulkService;
	
	@Test
	public void shouldRetrieveEntitiesFromSubgroup() throws EngineException
	{
		groupsMan.addGroup(new Group("/A"));
		
		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), 
				EntityState.valid, false);
		EntityParam entity = new EntityParam(added.getEntityId());
		
		groupsMan.addMemberFromParent("/A", entity);
		
		
		GroupMembershipData bulkData = bulkService.getBulkMembershipData("/A");
		Map<Long, Entity> result = bulkService.getGroupEntitiesNoContextWithTargeted(bulkData);
		
		assertThat(result.size(), is(1));
		assertThat(result.get(added.getEntityId()), is(notNullValue()));
		assertThat(result.get(added.getEntityId()).getIdentities(), hasItem(added));
	}
	
	@Test
	public void shouldRetrieveAttributesOfEntitiesInGroup() throws EngineException
	{
		groupsMan.addGroup(new Group("/A"));
		
		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), 
				EntityState.valid, false);
		EntityParam entity = new EntityParam(added.getEntityId());
		
		Identity added2 = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "2"), 
				EntityState.valid, false);
		EntityParam entity2 = new EntityParam(added2.getEntityId());
		
		groupsMan.addMemberFromParent("/A", entity);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE, 
				"/", Lists.newArrayList("Anonymous User"));
		Attribute saInA = EnumAttribute.of(AUTHORIZATION_ROLE, 
				"/A", Lists.newArrayList("Inspector"));
		attrsMan.createAttribute(entity, saRoot);
		attrsMan.createAttribute(entity2, saRoot);
		attrsMan.createAttribute(entity, saInA);
		
		
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
				EntityState.valid, false);
		EntityParam entity = new EntityParam(added.getEntityId());
		
		groupsMan.addMemberFromParent("/example", entity);
		Attribute saRoot = EnumAttribute.of(AUTHORIZATION_ROLE, 
				"/", Lists.newArrayList("Inspector"));
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

}
