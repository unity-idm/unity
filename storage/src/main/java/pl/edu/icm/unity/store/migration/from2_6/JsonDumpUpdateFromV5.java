/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_6;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.export.Update;
import pl.edu.icm.unity.store.objstore.reg.eform.EnquiryFormHandler;
import pl.edu.icm.unity.store.objstore.reg.eresp.EnquiryResponseHandler;
import pl.edu.icm.unity.store.objstore.reg.form.RegistrationFormHandler;
import pl.edu.icm.unity.store.objstore.reg.invite.InvitationHandler;
import pl.edu.icm.unity.store.objstore.reg.req.RegistrationRequestHandler;

/**
 * Update JSon dump from V6 version, see {@link UpdateHelperFrom2_6}
 */
@Component
public class JsonDumpUpdateFromV5 implements Update
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV5.class);
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		updateRequests(contents);
		updateInvitations(contents);
		removeOrphanedAttributes(contents);
		updateRegistrationForms(contents);
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void removeOrphanedAttributes(ObjectNode contents)
	{
		ArrayNode attributes = (ArrayNode) contents.get("attributes");
		ArrayNode members = (ArrayNode) contents.get("groupMembers");
		Map<Long, Set<String>> entityGroups = new HashMap<>();
		for (JsonNode member: members)
		{
			String group = member.get("group").asText();
			long entityId = member.get("entityId").asLong();
			Set<String> set = entityGroups.get(entityId);
			if (set == null)
			{
				set = new HashSet<>();
				entityGroups.put(entityId, set);
			}
			set.add(group);
		}
		
		Iterator<JsonNode> iterator = attributes.iterator();
		while(iterator.hasNext())
		{
			JsonNode a = iterator.next();
			String group = a.get("groupPath").asText();
			long entityId = a.get("entityId").asLong();
			Set<String> set = entityGroups.get(entityId);
			if (set == null || !set.contains(group))
			{
				log.info("Removing orphaned attribute {} from group {}", JsonUtil.serialize(a), group);
				iterator.remove();
			}
		}
	}

	
	private void updateRequests(ObjectNode contents)
	{
		updateRequestsGeneric(contents, RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE, 
				RegistrationRequestHandler.REGISTRATION_REQUEST_OBJECT_TYPE);
		updateRequestsGeneric(contents, EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE, 
				EnquiryResponseHandler.ENQUIRY_RESPONSE_OBJECT_TYPE);
	}
	
	private void updateRequestsGeneric(ObjectNode contents, String formType, String requestType)
	{
		Map<String, ObjectNode> formsMap = new HashMap<>();
		for (ObjectNode form: getGenericContent(contents, formType))
			formsMap.put(form.get("Name").asText(), form);
	
		ArrayNode generics = (ArrayNode) contents.get(requestType);
		if (generics == null)
			return;
		
		Iterator<JsonNode> elements = generics.elements();
		
		while (elements.hasNext())
		{
			JsonNode next = elements.next();
			ObjectNode request = (ObjectNode)next.get("obj");
			Optional<ObjectNode> updated = UpdateHelperFrom2_6.updateRegistrationRequest(request, formsMap);
			if (!updated.isPresent())
				elements.remove();
		}
	}

	private void updateInvitations(ObjectNode contents)
	{
		Map<String, ObjectNode> formsMap = new HashMap<>();
		for (ObjectNode form: getGenericContent(contents, RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE))
			formsMap.put(form.get("Name").asText(), form);
	
		ArrayNode generics = (ArrayNode) contents.get(InvitationHandler.INVITATION_OBJECT_TYPE);
		if (generics == null)
			return;
		
		Iterator<JsonNode> elements = generics.elements();
		
		while (elements.hasNext())
		{
			JsonNode next = elements.next();
			ObjectNode invitation = (ObjectNode)next.get("obj");
			Optional<ObjectNode> updated = UpdateHelperFrom2_6.updateInvitation(invitation, formsMap);
			if (!updated.isPresent())
				elements.remove();
		}
	}
	
	private Set<ObjectNode> getGenericContent(ObjectNode contents, String type)
	{
		Set<ObjectNode> ret = new HashSet<>();
		ArrayNode generics = (ArrayNode) contents.get(type);
		if (generics != null)
		{
			for (JsonNode obj : generics)
			{
				ret.add((ObjectNode) obj.get("obj"));
			}
		}
		return ret;
	}
	
	private void updateRegistrationForms(ObjectNode contents)
	{
		for (ObjectNode registrationForm: getGenericContent(contents, RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE))
		{
			UpdateHelperFrom2_6.updateRegistrationFormLayout(registrationForm);
		}
	}
}
