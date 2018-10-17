/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_6;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBSchemaUpdater;
import pl.edu.icm.unity.store.objstore.reg.eform.EnquiryFormHandler;
import pl.edu.icm.unity.store.objstore.reg.eresp.EnquiryResponseHandler;
import pl.edu.icm.unity.store.objstore.reg.form.RegistrationFormHandler;
import pl.edu.icm.unity.store.objstore.reg.invite.InvitationHandler;
import pl.edu.icm.unity.store.objstore.reg.req.RegistrationRequestHandler;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Update db from 2.6.0 release version (DB schema version 2.4). See {@link UpdateHelperFrom2_6}
 */
@Component
public class InDBUpdateFromSchema2_4 implements InDBSchemaUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema2_4.class);
	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
	@Autowired
	private MembershipDAO membershipDAO;
	@Autowired
	private AttributeDAO attributeDAO;
	
	@Override
	public void update() throws IOException
	{
		updateRegistrationRequests();
		updateInvitations();
		removeOrphanedAttributes();
	}
	
	private void removeOrphanedAttributes()
	{
		List<StoredAttribute> attributes = attributeDAO.getAll();
		List<GroupMembership> memberships = membershipDAO.getAll();
		Map<Long, Set<String>> entityGroups = new HashMap<>();
		for (GroupMembership member: memberships)
		{
			Set<String> set = entityGroups.get(member.getEntityId());
			if (set == null)
			{
				set = new HashSet<>();
				entityGroups.put(member.getEntityId(), set);
			}
			set.add(member.getGroup());
		}
		
		
		for (StoredAttribute a: attributes)
		{
			Set<String> set = entityGroups.get(a.getEntityId());
			if (set == null || !set.contains(a.getAttribute().getGroupPath()))
			{
				log.info("Removing orphaned attribute {} from group {}", a, 
						a.getAttribute().getGroupPath());
				attributeDAO.deleteAttribute(a.getAttribute().getName(), 
						a.getEntityId(), a.getAttribute().getGroupPath());
			}
		}
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
				RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE);
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
			log.info("Loaded form {}", objContent);
			formsMap.put(objContent.get("Name").asText(), objContent);
		}
		return formsMap;
	}
}
