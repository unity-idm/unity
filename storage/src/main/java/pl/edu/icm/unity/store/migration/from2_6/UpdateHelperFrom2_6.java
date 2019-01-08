/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_6;


import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Shared code updating group selections in registration requests. Group selections are now holding the selected
 * group instead of boolean selection flag, and can hold a list of selections (although in effect of migration
 * at max one element can be in the list). Also invitations needs to updated accordingly.
 */
class UpdateHelperFrom2_6
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateHelperFrom2_6.class);
	
	static Optional<ObjectNode> updateRegistrationRequest(ObjectNode request, Map<String, ObjectNode> formsMap)
	{
		log.info("Updating registration request:\n{}", JsonUtil.toJsonString(request)); 
		ArrayNode groupSelections = (ArrayNode) request.get("GroupSelections");
		String formId = request.get("FormId").asText();
		ObjectNode form = formsMap.get(formId);
		if (form == null)
		{
			log.warn("Can not find form with id {}, for registration request {}, skipping it", formId, 
					JsonUtil.toJsonString(request));
			return Optional.empty();
		}

		int i=0;
		for (JsonNode groupSelection: groupSelections)
			updateSelection((ObjectNode) groupSelection, i++, form);
		return Optional.of(request);
	}

	private static void updateSelection(ObjectNode groupSelection, int index, ObjectNode form)
	{
		String source = JsonUtil.toJsonString(groupSelection); 
		boolean selected = groupSelection.remove("selected").asBoolean();
		ArrayNode updated = groupSelection.withArray("selectedGroups");
		if (selected)
		{
			ArrayNode groupParams = (ArrayNode) form.get("GroupParams");
			if (groupParams == null || groupParams.size() <= index)
			{
				log.warn("No group parameter found in the form for the request parameter, skipping it");
				return;
			}
			String correspondingFormGroup = groupParams.get(index).get("groupPath").asText();
			updated.add(correspondingFormGroup);
		}
		log.info("Updated request group selection:\n{}\nto\n{}", source, JsonUtil.toJsonString(groupSelection)); 

	}
	
	static Optional<ObjectNode> updateInvitation(ObjectNode invitation, Map<String, ObjectNode> formsMap)
	{
		log.info("Updating invitation:\n{}", JsonUtil.toJsonString(invitation)); 
		ObjectNode groupSelections = (ObjectNode) invitation.get("groupSelections");
		String formId = invitation.get("formId").asText();
		ObjectNode form = formsMap.get(formId);
		if (form == null)
		{
			log.warn("Can not find form with id {}, for invitation {}, skipping it", formId, 
					JsonUtil.toJsonString(invitation));
			return Optional.empty();
		}

		Iterator<Entry<String, JsonNode>> fields = groupSelections.fields();
		while(fields.hasNext())
		{
			Entry<String, JsonNode> next = fields.next();
			updateSelection((ObjectNode)next.getValue().get("entry"), Integer.parseInt(next.getKey()), form);
		}
		return Optional.of(invitation);
	}
	
	static void updateRegistrationFormLayout(ObjectNode registrationForm)
	{
		log.info("Updating registration form from: \n{}", JsonUtil.toJsonString(registrationForm));
		
		ObjectNode formLayout = (ObjectNode) registrationForm.remove("FormLayout");
		if (formLayout == null)
		{
			log.info("No migration required for registration form {}", registrationForm.get("Name"));
			return;
		}
		
		ObjectNode registrationFormLayouts = Constants.MAPPER.createObjectNode();
		registrationForm.set("RegistrationFormLayouts", registrationFormLayouts);
		registrationFormLayouts.set("primaryLayout", formLayout);
		
		log.info("Updated registration form to: \n{}", JsonUtil.toJsonString(registrationForm));
	}
}
