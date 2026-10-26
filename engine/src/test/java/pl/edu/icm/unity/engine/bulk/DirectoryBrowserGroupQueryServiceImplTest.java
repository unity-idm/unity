/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import static org.assertj.core.api.Assertions.assertThat;

import static pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider.AUTHORIZATION_ROLE;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.bulk.DirectoryBrowserGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;

public class DirectoryBrowserGroupQueryServiceImplTest extends DBIntegrationTestBase
{
	@Autowired
	private DirectoryBrowserGroupQueryService listingService;
	@Autowired
	private BulkGroupQueryService bulkService;

	@Test
	public void shouldRetrieveEntitiesFromSubgroupWithCredentialStatusButWithoutCustomAttributes() throws EngineException
	{
		groupsMan.addGroup(new Group("/A"));

		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"),
				EntityState.valid);
		EntityParam entity = new EntityParam(added.getEntityId());
		groupsMan.addMemberFromParent("/A", entity);

		Attribute customAttr = EnumAttribute.of(AUTHORIZATION_ROLE, "/", Lists.newArrayList("Anonymous User"));
		attrsMan.createAttribute(entity, customAttr);

		GroupMembershipData bulkData = listingService.getGroupListingData("/A");
		Map<Long, Entity> result = listingService.getGroupEntitiesNoContextWithTargeted(bulkData);

		assertThat(result.size()).isEqualTo(1);
		Entity resultEntity = result.get(added.getEntityId());
		assertThat(resultEntity).isNotNull();
		assertThat(resultEntity.getIdentities()).contains(added);
		assertThat(resultEntity.getCredentialInfo()).isNotNull();
		assertThat(resultEntity.getCredentialInfo().getCredentialRequirementId()).isNotBlank();
	}

	@Test
	public void shouldNotRetrieveEntitiesOutsideOfGroup() throws EngineException
	{
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));

		Identity inA = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "inA"),
				EntityState.valid);
		groupsMan.addMemberFromParent("/A", new EntityParam(inA.getEntityId()));

		Identity inB = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "inB"),
				EntityState.valid);
		groupsMan.addMemberFromParent("/B", new EntityParam(inB.getEntityId()));

		GroupMembershipData bulkData = listingService.getGroupListingData("/A");
		Map<Long, Entity> result = listingService.getGroupEntitiesNoContextWithoutTargeted(bulkData);

		assertThat(result.size()).isEqualTo(1);
		assertThat(result).containsKey(inA.getEntityId());
		assertThat(((GroupMembershipDataImpl) bulkData).entitiesData.getDirectAttributes())
				.containsOnlyKeys(inA.getEntityId());
	}

	@Test
	public void shouldIncludeTargetedIdentitiesOnlyWhenRequested() throws Exception
	{
		groupsMan.addGroup(new Group("/A"));
		Identity plain = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "plain"), EntityState.valid);
		EntityParam entity = new EntityParam(plain.getEntityId());
		groupsMan.addMemberFromParent("/A", entity);
		Identity targeted = idsMan.getEntity(entity, "some-sp", true, "/").getIdentities().stream()
				.filter(id -> id.getTarget() != null).findFirst().orElseThrow();

		GroupMembershipData data = listingService.getGroupListingData("/A");

		assertThat(listingService.getGroupEntitiesNoContextWithTargeted(data).get(plain.getEntityId())
				.getIdentities()).contains(plain, targeted);
		assertThat(listingService.getGroupEntitiesNoContextWithoutTargeted(data).get(plain.getEntityId())
				.getIdentities()).contains(plain).doesNotContain(targeted);
	}

	@Test
	public void shouldResolveMeaningfulCredentialStates() throws Exception
	{
		setupPasswordAuthn();
		Identity withPassword = createUsernameUser("withPassword", null, DEF_PASSWORD, CRED_REQ_PASS);
		Identity withoutPassword = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "withoutPassword"),
				CRED_REQ_PASS, EntityState.valid);

		Map<Long, Entity> listed = listingService.getGroupEntitiesNoContextWithoutTargeted(
				listingService.getGroupListingData("/"));

		CredentialInfo set = listed.get(withPassword.getEntityId()).getCredentialInfo();
		assertThat(set.getCredentialRequirementId()).isEqualTo(CRED_REQ_PASS);
		assertThat(set.getCredentialsState().get("credential1").getState()).isEqualTo(LocalCredentialState.correct);
		CredentialInfo notSet = listed.get(withoutPassword.getEntityId()).getCredentialInfo();
		assertThat(notSet.getCredentialsState().get("credential1").getState())
				.isEqualTo(LocalCredentialState.notSet);
	}

	@Test
	public void shouldAssembleSameEntitiesAsFullBulkQuery() throws Exception
	{
		setupPasswordAuthn();
		groupsMan.addGroup(new Group("/A"));
		Identity user = createUsernameUser("user", null, DEF_PASSWORD, CRED_REQ_PASS);
		groupsMan.addMemberFromParent("/A", new EntityParam(user.getEntityId()));
		idsMan.getEntity(new EntityParam(user.getEntityId()), "some-sp", true, "/");
		idsMan.setEntityStatus(new EntityParam(user.getEntityId()), EntityState.disabled);

		GroupMembershipData listing = listingService.getGroupListingData("/A");
		GroupMembershipData full = bulkService.getBulkMembershipData("/A");

		assertThat(listingService.getGroupEntitiesNoContextWithTargeted(listing))
				.isEqualTo(bulkService.getGroupEntitiesNoContextWithTargeted(full));
		assertThat(listingService.getGroupEntitiesNoContextWithoutTargeted(listing))
				.isEqualTo(bulkService.getGroupEntitiesNoContextWithoutTargeted(full));
	}

	@Test
	public void shouldNotLoadDataNeededForAttributesEvaluation() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("custom", StringAttributeSyntax.ID));
		Group groupA = new Group("/A");
		groupA.setAttributeStatements(new AttributeStatement[] {AttributeStatement.getFixedEverybodyStatement(
				StringAttribute.of("custom", "/A", "dynamic"))});
		groupsMan.addGroup(groupA);
		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "e1"), EntityState.valid);
		groupsMan.addMemberFromParent("/A", new EntityParam(added.getEntityId()));
		attrsMan.createAttribute(new EntityParam(added.getEntityId()), StringAttribute.of("custom", "/A", "direct"));

		GroupMembershipDataImpl listing = (GroupMembershipDataImpl) listingService.getGroupListingData("/A");

		// without attribute types, statements, classes and other groups the effective attributes can't be
		// evaluated - only the credential related attributes of the root group are loaded
		assertThat(listing.globalSystemData.getAttributeTypes()).isEmpty();
		assertThat(listing.globalSystemData.getGroups()).isEmpty();
		assertThat(listing.globalSystemData.getAttributeClasses()).isEmpty();
		assertThat(listing.entitiesData.getMemberships()).isEmpty();
		Map<String, Map<String, AttributeExt>> loaded = listing.entitiesData.getDirectAttributes()
				.get(added.getEntityId());
		assertThat(loaded).containsOnlyKeys("/");
		assertThat(loaded.get("/").keySet()).allMatch(name -> name.startsWith("sys:Credential"));
	}

	@Test
	public void subgroupListingScopeDoesNotDependOnNumberOfUnrelatedUsers() throws Exception
	{
		groupsMan.addGroup(new Group("/small"));
		for (int i = 0; i < 3; i++)
		{
			Identity member = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "member" + i),
					EntityState.valid);
			groupsMan.addMemberFromParent("/small", new EntityParam(member.getEntityId()));
		}
		addUnrelatedUsers(0, 10);
		int[] scopeWithFewUnrelated = listingScope("/small");

		addUnrelatedUsers(10, 100);
		int[] scopeWithManyUnrelated = listingScope("/small");

		assertThat(scopeWithFewUnrelated).containsExactly(3, 3, 3);
		assertThat(scopeWithManyUnrelated).containsExactly(scopeWithFewUnrelated);
	}

	private void addUnrelatedUsers(int from, int to) throws Exception
	{
		for (int i = from; i < to; i++)
			idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "unrelated" + i), EntityState.valid);
	}

	/**
	 * @return number of entities for which entity information, identities and attributes were loaded
	 */
	private int[] listingScope(String group) throws Exception
	{
		GroupMembershipDataImpl data = (GroupMembershipDataImpl) listingService.getGroupListingData(group);
		return new int[] { data.entitiesData.getEntityInfo().size(), data.entitiesData.getIdentities().size(),
				data.entitiesData.getDirectAttributes().size() };
	}
}
