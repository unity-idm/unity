/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Serialization to from Json and to from RDBMS beans. 
 * @author K. Benedyczak
 */
@Component
public class GroupJsonSerializer implements RDBMSObjectSerializer<Group, GroupBean>, 
			JsonSerializerForKryo<Group>
{
	@Override
	public Group fromJson(ObjectNode main)
	{
		return new Group(main);
	}

	@Override
	public ObjectNode toJson(Group src)
	{
		return src.toJson();
	}

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
		main.set("i18nDescription", I18nStringJsonUtil.toJson(null));
		main.set("displayedName", I18nStringJsonUtil.toJson(null));
		main.putArray("attributeStatements");
		main.putArray("attributesClasses");
		return main;
	}

	@Override
	public Class<? extends Group> getClazz()
	{
		return Group.class;
	}
}
