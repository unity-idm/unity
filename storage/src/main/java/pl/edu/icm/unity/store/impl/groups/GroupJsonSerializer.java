/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Serialization to from Json and to from RDBMS beans. 
 * @author K. Benedyczak
 */
@Component
public class GroupJsonSerializer implements RDBMSObjectSerializer<Group, GroupBean>
{
	private final Cache<GroupBean, Group> resolvedGroupsCache = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofDays(1)).build();
	
	@Override
	public GroupBean toDB(Group object)
	{
		GroupBean gb = new GroupBean(object.toString(), object.getParentPath());
		gb.setContents(JsonUtil.serialize2Bytes(object.toJsonBase()));
		return gb;
	}

	@Override
	public Group fromDB(GroupBean bean)
	{
		try
		{
			return resolvedGroupsCache.get(bean, () -> parse(bean)).clone();
		} catch (ExecutionException e)
		{
			throw new IllegalStateException("Error parsing group from DB", e);
		}
	}
	
	private Group parse(GroupBean bean)
	{
		Group ret = new Group(bean.getName());
		ret.fromJsonBase(JsonUtil.parse(bean.getContents()));
		return ret;
	}
	
	/**
	 * @return minimal contents for the initialization of the root group '/'.
	 * Needs to be static as it is created early on startup when real DAO infrastructure is not ready.
	 */
	public static ObjectNode createRootGroupContents()
	{
		ObjectNode main = new ObjectMapper().createObjectNode();
		main.set("i18nDescription", null);
		main.set("displayedName", null);
		main.putArray("attributeStatements");
		main.putArray("attributesClasses");
		return main;
	}
}
