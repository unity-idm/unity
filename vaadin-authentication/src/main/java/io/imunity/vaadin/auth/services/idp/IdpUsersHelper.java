/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.idp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A collection of methods helpful in getting Idp users
 * 
 * @author P.Piernik
 *
 */
@Component
public class IdpUsersHelper
{
	private final BulkGroupQueryService bulkService;
	private final AttributeSupport atttributeSupport;

	@Autowired
	IdpUsersHelper(BulkGroupQueryService bulkService, AttributeSupport atttributeSupport)
	{

		this.bulkService = bulkService;
		this.atttributeSupport = atttributeSupport;
	}

	public String getClientNameAttr() throws EngineException
	{
		List<AttributeType> nameAttrs = atttributeSupport
				.getAttributeTypeWithMetadata(EntityNameMetadataProvider.NAME);
		if (!nameAttrs.isEmpty())
		{
			return nameAttrs.get(0).getName();
		}
		return null;
	}

	public List<IdpUser> getAllUsers() throws EngineException
	{
		List<IdpUser> users = new ArrayList<>();

		Map<Long, EntityInGroupData> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData("/"));
		String nameAttr = getClientNameAttr();

		for (EntityInGroupData info : membershipInfo.values())
		{
			EntityState state = info.entity.getState();
			Long entity = info.entity.getId();
			String name = "";
			if (nameAttr != null && info.groupAttributesByName.containsKey(nameAttr))
			{
				name = info.groupAttributesByName.get(nameAttr).getValues().get(0);
			}

			for (String group : info.groups)
			{
				IdpUser user = new IdpUser(entity, name, group, state);
				users.add(user);
			}
		}

		return users;

	}
}
