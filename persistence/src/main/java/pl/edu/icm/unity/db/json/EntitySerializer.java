/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.sql.Date;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;

/**
 * Handles serialization of {@link EntityInformation}.
 * @author K. Benedyczak
 */
@Component
public class EntitySerializer
{
	private ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * @param src
	 * @return Json as byte[] with the src contents.
	 */
	public byte[] toJson(EntityInformation src)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("state", src.getState().name());
		if (src.getScheduledOperationTime() != null)
			main.put("ScheduledOperationTime", src.getScheduledOperationTime().getTime());
		if (src.getScheduledOperation() != null)
			main.put("ScheduledOperation", src.getScheduledOperation().name());
		if (src.getRemovalByUserTime() != null)
			main.put("RemovalByUserTime", src.getRemovalByUserTime().getTime());
		
		try
		{
			return mapper.writeValueAsBytes(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
	
	/**
	 * Fills target with JSON contents, checking it for correctness
	 * @param json
	 * @param target
	 */
	public EntityInformation fromJson(byte[] json)
	{
		if (json == null)
			return new EntityInformation(EntityState.valid);
		ObjectNode main;
		try
		{
			main = mapper.readValue(json, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

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
