/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_6;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBSchemaUpdater;
import pl.edu.icm.unity.store.objstore.reg.eform.EnquiryFormHandler;
import pl.edu.icm.unity.store.objstore.reg.eresp.EnquiryResponseHandler;
import pl.edu.icm.unity.store.objstore.reg.form.RegistrationFormHandler;
import pl.edu.icm.unity.store.objstore.reg.invite.InvitationHandler;
import pl.edu.icm.unity.store.objstore.reg.req.RegistrationRequestHandler;

/**
 * Update db from 2.6.0 release version (DB schema version 2.4). See {@link UpdateHelperFrom2_6}
 */
@Component
public class InDBUpdateFromSchema2_4 implements InDBSchemaUpdater
{
	
	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
	@Override
	public void update() throws IOException
	{
		updateRegistrationRequests();
		updateInvitations();
	}
	
	private void updateRegistrationRequests()
	{
		updateRequestsGeneric(
				genericObjectsDAO.getObjectsOfType(RegistrationRequestHandler.REGISTRATION_REQUEST_OBJECT_TYPE), 
				genericObjectsDAO.getObjectsOfType(RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE));

		updateRequestsGeneric(
				genericObjectsDAO.getObjectsOfType(EnquiryResponseHandler.ENQUIRY_RESPONSE_OBJECT_TYPE), 
				genericObjectsDAO.getObjectsOfType(EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE));
	}
	
	private void updateRequestsGeneric(List<GenericObjectBean> requests, List<GenericObjectBean> forms)
	{
		Map<String, ObjectNode> formsMap = getFormsMap(forms);
		for (GenericObjectBean request : requests)
		{
			ObjectNode objContent = JsonUtil.parse(request.getContents());
			Optional<ObjectNode> updated = UpdateHelperFrom2_6.updateRegistrationRequest(objContent, formsMap);
			if (updated.isPresent())
			{
				request.setContents(JsonUtil.serialize2Bytes(updated.get()));
				genericObjectsDAO.updateByKey(request.getId(), request);
			} else
			{
				genericObjectsDAO.deleteByKey(request.getId());
			}
		}
	}
	
	private void updateInvitations()
	{
		List<GenericObjectBean> forms = genericObjectsDAO.getObjectsOfType(
				RegistrationRequestHandler.REGISTRATION_REQUEST_OBJECT_TYPE);
		Map<String, ObjectNode> formsMap = getFormsMap(forms);
		List<GenericObjectBean> invitations = genericObjectsDAO.getObjectsOfType(
				InvitationHandler.INVITATION_OBJECT_TYPE);
		for (GenericObjectBean invitation : invitations)
		{
			ObjectNode objContent = JsonUtil.parse(invitation.getContents());
			Optional<ObjectNode> updated = UpdateHelperFrom2_6.updateInvitation(objContent, formsMap);
			if (updated.isPresent())
			{
				invitation.setContents(JsonUtil.serialize2Bytes(updated.get()));
				genericObjectsDAO.updateByKey(invitation.getId(), invitation);
			} else
			{
				genericObjectsDAO.deleteByKey(invitation.getId());
			}
		}
	}
	
	Map<String, ObjectNode> getFormsMap(List<GenericObjectBean> forms)
	{
		Map<String, ObjectNode> formsMap = new HashMap<>();
		for (GenericObjectBean form: forms)
		{
			ObjectNode objContent = JsonUtil.parse(form.getContents());
			formsMap.put(objContent.get("Name").asText(), objContent);
		}
		return formsMap;
	}
}
