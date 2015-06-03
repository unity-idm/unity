/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Handles serialization of {@link IdentityParam} metadata.
 * @author K. Benedyczak
 */
@Component
public class IdentitySerializer
{
	private ObjectMapper mapper = Constants.MAPPER;
	
	/**
	 * @param src
	 * @return Json as byte[] with the src contents.
	 */
	public byte[] toJson(IdentityParam src, Date created, Date updated)
	{
		ObjectNode main = mapper.createObjectNode();
		if (created != null)
			main.put("creationTs", created.getTime());
		if (updated != null)
			main.put("updateTs", updated.getTime());
		
		if (src.getRemoteIdp() != null)
			main.put("remoteIdp", src.getRemoteIdp());
		if (src.getTranslationProfile() != null)
			main.put("translationProfile", src.getTranslationProfile());

		if (src.getValue() != null)
			main.put("value", src.getValue());
		if (src.getRealm() != null)
			main.put("realm", src.getRealm());
		if (src.getTarget() != null)
			main.put("target", src.getTarget());
		if (src.getConfirmationInfo() != null)
			main.put("confirmationInfo", src.getConfirmationInfo().getSerializedConfiguration());
		if (src.getMetadata() != null)
			main.set("metadata", src.getMetadata());
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
	public void fromJson(byte[] json, Identity target)
	{
		if (json == null)
			return;
		ObjectNode main;
		try
		{
			main = mapper.readValue(json, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		if (main.has("creationTs"))
			target.setCreationTs(new Date(main.get("creationTs").asLong()));
		if (main.has("updateTs"))
			target.setUpdateTs(new Date(main.get("updateTs").asLong()));
		if (main.has("translationProfile"))
			target.setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("remoteIdp"))
			target.setRemoteIdp(main.get("remoteIdp").asText());
		if (main.has("translationProfile"))
			target.setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("value"))
			target.setValue(main.get("value").asText());
		if (main.has("realm"))
			target.setRealm(main.get("realm").asText());
		if (main.has("target"))
			target.setTarget(main.get("target").asText());
		if (main.has("confirmationInfo"))
		{
			ConfirmationInfo conData = new ConfirmationInfo();
			conData.setSerializedConfiguration(main.get("confirmationInfo").asText());
			target.setConfirmationInfo(conData);
		}
		if (main.has("metadata"))
			target.setMetadata(main.get("metadata"));
	}
}
