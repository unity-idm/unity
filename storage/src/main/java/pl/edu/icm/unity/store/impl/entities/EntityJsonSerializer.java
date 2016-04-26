/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.JsonSerializer;
import pl.edu.icm.unity.store.api.StoredEntity;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.store.rdbms.model.BaseBean;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Serializes {@link StoredEntity} to/from DB form.
 * @author K. Benedyczak
 */
@Component
public class EntityJsonSerializer implements RDBMSObjectSerializer<StoredEntity, BaseBean>, JsonSerializer<StoredEntity>
{
	@Autowired
	private ObjectMapper mapper;

	@Override
	public BaseBean toDB(StoredEntity object)
	{
		byte[] contents = JsonUtil.serialize2Bytes(toJsonBase(object.getEntityInformation()));
		BaseBean ret = new BaseBean(null, contents);
		ret.setId(object.getId());
		return ret;
	}

	@Override
	public StoredEntity fromDB(BaseBean bean)
	{
		byte[] contents = bean.getContents();
		if (contents == null)
			return new StoredEntity(bean.getId(), new EntityInformation(EntityState.valid));
		ObjectNode main = JsonUtil.parse(contents);
		return new StoredEntity(bean.getId(), fromJsonBase(main));
	}
	
	@Override
	public StoredEntity fromJson(ObjectNode src)
	{
		EntityInformation ei = fromJsonBase(src);
		long id = src.get("entityId").asLong();
		return new StoredEntity(id, ei);
	}

	@Override
	public ObjectNode toJson(StoredEntity src)
	{
		ObjectNode main = toJsonBase(src.getEntityInformation());
		main.put("entityId", src.getId());
		return main;
	}
	
	
	private ObjectNode toJsonBase(EntityInformation src)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("state", src.getState().name());
		if (src.getScheduledOperationTime() != null)
			main.put("ScheduledOperationTime", src.getScheduledOperationTime().getTime());
		if (src.getScheduledOperation() != null)
			main.put("ScheduledOperation", src.getScheduledOperation().name());
		if (src.getRemovalByUserTime() != null)
			main.put("RemovalByUserTime", src.getRemovalByUserTime().getTime());
		return main;
	}
	
	private EntityInformation fromJsonBase(ObjectNode main)
	{
		String stateStr = main.get("state").asText();
		EntityInformation ret = new EntityInformation(EntityState.valueOf(stateStr));
		if (main.has("ScheduledOperationTime"))
			ret.setScheduledOperationTime(new Date(main.get("ScheduledOperationTime").asLong()));
		if (main.has("ScheduledOperation"))
			ret.setScheduledOperation(EntityScheduledOperation.valueOf(
					main.get("ScheduledOperation").asText()));
		if (main.has("RemovalByUserTime"))
			ret.setRemovalByUserTime(new Date(main.get("RemovalByUserTime").asLong()));		
		return ret;
	}


}
