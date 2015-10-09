/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.sql.Date;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.GroupMembership;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles serialization of {@link GroupMembership}.
 * @author K. Benedyczak
 */
@Component
public class GroupMembershipSerializer
{
	private ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * @param src
	 * @return Json as byte[] with the src contents.
	 */
	public byte[] toJson(GroupMembership src)
	{
		ObjectNode main = mapper.createObjectNode();
		if (src.getRemoteIdp() != null)
			main.put("remoteIdp", src.getRemoteIdp());
		if (src.getTranslationProfile() != null)
			main.put("translationProfile", src.getTranslationProfile());
		if (src.getCreationTs() != null)
			main.put("creationTs", src.getCreationTs().getTime());
		
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
	public GroupMembership fromJson(byte[] json, long entity, String group)
	{
		GroupMembership ret = new GroupMembership(group, entity, null);
		if (json == null)
			return ret;
		ObjectNode main;
		try
		{
			main = mapper.readValue(json, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

		if (main.has("remoteIdp"))
			ret.setRemoteIdp(main.get("remoteIdp").asText());
		if (main.has("translationProfile"))
			ret.setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("creationTs"))
			ret.setCreationTs(new Date(main.get("creationTs").asLong()));		
		return ret;
	}
}
