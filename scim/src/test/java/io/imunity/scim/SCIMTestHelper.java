/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;

public class SCIMTestHelper
{
	private static final PersistentIdentity persistentIdType = new PersistentIdentity();

	
	public static GroupContents getGroupContent(String path, List<String> subgroups)
	{
		GroupContents con = new GroupContents();
		Group group = new Group(path);
		group.setDisplayedName(new I18nString(path));
		con.setGroup(group);
		con.setSubGroups(subgroups);
		return con;
	}

	public static GroupContents getGroupContent(String path)
	{
		return getGroupContent(path, List.of());
	}

	public static Entity createPersitentEntity(String id, long entityId) throws IllegalIdentityValueException
	{
		IdentityParam idParam = persistentIdType.convertFromString(id, "ridp", null);
		Identity identity = new Identity(idParam, entityId,
				persistentIdType.getComparableValue(idParam.getValue(), "realm", null));
		identity.setCreationTs(new Date());
		identity.setUpdateTs(new Date());

		return new Entity(Lists.newArrayList(identity), new EntityInformation(entityId), null);
	}
}
