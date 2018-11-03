/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
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
}
