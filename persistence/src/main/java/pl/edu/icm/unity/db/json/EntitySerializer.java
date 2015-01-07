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
		if (src.getTimeToDisableAdmin() != null)
			main.put("TimeToDisableAdmin", src.getTimeToDisableAdmin().getTime());
		if (src.getTimeToRemoveAdmin() != null)
			main.put("TimeToRemoveAdmin", src.getTimeToRemoveAdmin().getTime());
		if (src.getTimeToRemoveUser() != null)
			main.put("TimeToRemoveUser", src.getTimeToRemoveUser().getTime());
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
		if (main.has("TimeToDisableAdmin"))
			ret.setTimeToDisableAdmin(new Date(main.get("TimeToDisableAdmin").asLong()));
		if (main.has("TimeToRemoveAdmin"))
			ret.setTimeToRemoveAdmin(new Date(main.get("TimeToRemoveAdmin").asLong()));
		if (main.has("TimeToRemoveUser"))
			ret.setTimeToRemoveUser(new Date(main.get("TimeToRemoveUser").asLong()));
		return ret;
	}
}
