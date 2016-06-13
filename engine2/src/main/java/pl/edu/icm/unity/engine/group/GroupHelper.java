/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.group;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Shared group-related utility methods
 * @author K. Benedyczak
 */
@Component
public class GroupHelper
{
	private MembershipDAO membershipDAO;
	private EntityResolver entityResolver;

	
	@Autowired
	public GroupHelper(MembershipDAO membershipDAO, EntityResolver entityResolver)
	{
		this.membershipDAO = membershipDAO;
		this.entityResolver = entityResolver;
	}


	/**
	 * Adds entity to the given group. The entity must be a member of the parent group
	 * (unless adding to the root group).
	 * @param path
	 * @param entity
	 * @param idp
	 * @param translationProfile
	 * @param creationTs
	 * @throws IllegalGroupValueException
	 * @throws IllegalIdentityValueException
	 */
	public void addMemberFromParent(String path, EntityParam entity, String idp, String translationProfile,
			Date creationTs) 
				throws IllegalGroupValueException, IllegalIdentityValueException
	{
		long entityId = entityResolver.getEntityId(entity);
		Group group = new Group(path);
		if (!group.isTopLevel())
		{
			if (!membershipDAO.isMember(entityId, group.getParentPath()))
				throw new IllegalGroupValueException("Can't add to the group " + path + 
						", as the entity is not a member of its parent group");
		}
		if (membershipDAO.isMember(entityId, path))
			throw new IllegalGroupValueException("The entity is already a member of this group");

		GroupMembership param = new GroupMembership(path, entityId, creationTs, translationProfile, idp);
		membershipDAO.create(param);
	}

}
