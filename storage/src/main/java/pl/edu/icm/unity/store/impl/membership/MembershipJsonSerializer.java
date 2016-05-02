/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.sql.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.store.rdbms.model.GroupElementBean;
import pl.edu.icm.unity.types.basic.GroupMembership;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Serialization to from Json and to from RDBMS beans. 
 * @author K. Benedyczak
 */
@Component
public class MembershipJsonSerializer implements RDBMSObjectSerializer<GroupMembership, GroupElementBean>, 
			JsonSerializerForKryo<GroupMembership>
{
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private GroupDAO groupDAO;
	
	@Override
	public Class<? extends GroupMembership> getClazz()
	{
		return GroupMembership.class;
	}

	@Override
	public GroupMembership fromJson(ObjectNode src)
	{
		String group = src.get("group").asText();
		long entityId = src.get("entityId").asLong();
		GroupMembership ret = new GroupMembership(group, entityId, null);
		fromJsonBase(src, ret);
		return ret;
	}

	@Override
	public ObjectNode toJson(GroupMembership src)
	{
		ObjectNode main = toJsonBase(src);
		main.put("group", src.getGroup());
		main.put("entityId", src.getEntityId());
		return main;
	}

	@Override
	public GroupElementBean toDB(GroupMembership object)
	{
		long groupId = groupDAO.getKeyForName(object.getGroup());
		GroupElementBean geb = new GroupElementBean(groupId, object.getEntityId());
		geb.setContents(JsonUtil.serialize2Bytes(toJsonBase(object)));
		return geb;
	}

	@Override
	public GroupMembership fromDB(GroupElementBean bean)
	{
		GroupMembership gm = new GroupMembership(bean.getGroup(), 
				bean.getElementId(), null);
		fromJsonBase(JsonUtil.parse(bean.getContents()), gm);
		return gm;
	}
	
	
	private ObjectNode toJsonBase(GroupMembership src)
	{
		ObjectNode main = mapper.createObjectNode();
		if (src.getRemoteIdp() != null)
			main.put("remoteIdp", src.getRemoteIdp());
		if (src.getTranslationProfile() != null)
			main.put("translationProfile", src.getTranslationProfile());
		if (src.getCreationTs() != null)
			main.put("creationTs", src.getCreationTs().getTime());
		return main;
	}
	
	private void fromJsonBase(ObjectNode main, GroupMembership ret)
	{
		if (main.has("remoteIdp"))
			ret.setRemoteIdp(main.get("remoteIdp").asText());
		if (main.has("translationProfile"))
			ret.setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("creationTs"))
			ret.setCreationTs(new Date(main.get("creationTs").asLong()));		
	}
}
