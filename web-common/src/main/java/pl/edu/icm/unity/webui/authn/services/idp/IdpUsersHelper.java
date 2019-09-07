/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services.idp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipInfo;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * A collection of methods helpful in getting Idp users
 * 
 * @author P.Piernik
 *
 */
@Component
public class IdpUsersHelper
{
	private BulkGroupQueryService bulkService;
	private AttributeSupport atttributeSupport;

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

		Map<Long, GroupMembershipInfo> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData("/"));
		String nameAttr = getClientNameAttr();

		for (GroupMembershipInfo info : membershipInfo.values())
		{
			EntityState state = info.entityInfo.getEntityState();
			Long entity = info.entityInfo.getId();
			String name = "";
			if (nameAttr != null && info.attributes.get("/").keySet().contains(nameAttr))
			{
				name = info.attributes.get("/").get(nameAttr).getValues().get(0);
			}

			for (String group : info.groups)
			{

				for (Identity id : info.identities)
				{
					IdpUser user = new IdpUser(entity, name, group, id.getValue(), id.getTypeId(),
							state);
					users.add(user);
				}
			}
		}

		return users;

	}
}
