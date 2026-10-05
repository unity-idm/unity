/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.engine.api.bulk.DirectoryBrowserGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;

@Component
class DirectoryBrowserGroupQueryServiceImpl implements DirectoryBrowserGroupQueryService
{
	private final EntityAssembler entityAssembler;
	private final CompositeEntitiesInfoProvider dataProvider;
	private final InternalAuthorizationManager authz;

	@Autowired
	DirectoryBrowserGroupQueryServiceImpl(EntityAssembler entityAssembler,
			CompositeEntitiesInfoProvider dataProvider,
			InternalAuthorizationManager authz)
	{
		this.entityAssembler = entityAssembler;
		this.dataProvider = dataProvider;
		this.authz = authz;
	}

	@Transactional
	@Override
	public GroupMembershipData getGroupListingData(String group) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		return dataProvider.getCompositeGroupContentsForListing(group);
	}

	@Override
	public Map<Long, Entity> getGroupEntitiesNoContextWithTargeted(GroupMembershipData dataO)
	{
		GroupMembershipDataImpl data = (GroupMembershipDataImpl) dataO;
		return entityAssembler.getGroupEntitiesNoContext(true, data.entitiesData, data.globalSystemData);
	}

	@Override
	public Map<Long, Entity> getGroupEntitiesNoContextWithoutTargeted(GroupMembershipData dataO)
	{
		GroupMembershipDataImpl data = (GroupMembershipDataImpl) dataO;
		return entityAssembler.getGroupEntitiesNoContext(false, data.entitiesData, data.globalSystemData);
	}
}
