/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.io.IOException;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.Constants;
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
		try
		{
			gb.setContents(Constants.MAPPER.writeValueAsBytes(GroupMapper.mapBaseGroup(object)));
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Error saving group to DB", e);
		}
		return gb;
	}

	@Override
	public Group fromDB(GroupBean bean)
	{
		try
		{
			return resolvedGroupsCache.get(bean, () -> parse(bean)).clone();
		} catch (Exception e)
		{
			throw new IllegalStateException("Error parsing group from DB", e);
		}
	}
	
	private Group parse(GroupBean bean)
	{
		DBGroupBase dbGroup;

		try
		{
			dbGroup = Constants.MAPPER.readValue(bean.getContents(), DBGroupBase.class);
		} catch (IOException e)
		{
			throw new IllegalStateException("Error parsing group from DB", e);
		}

		return GroupMapper.mapFromBaseGroup(dbGroup, bean.getName());
	}
	
	/**
	 * @return minimal contents for the initialization of the root group '/'.
	 * Needs to be static as it is created early on startup when real DAO infrastructure is not ready.
	 */
	public static byte[] createRootGroupContents()
	{
		try
		{
			return Constants.MAPPER.writeValueAsBytes(DBGroupBase.builder().build());
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Error parsing group", e);
		}
		
	}
}
