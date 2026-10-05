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
	}
}
