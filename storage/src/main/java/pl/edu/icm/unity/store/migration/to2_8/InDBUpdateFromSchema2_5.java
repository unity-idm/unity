/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to2_8;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

/**
 * Update db from 2.7 release version (DB schema version 2.5). See {@link UpdateHelperTo2_8}
 */
@Component
public class InDBUpdateFromSchema2_5 implements InDBContentsUpdater
{
	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
	@Override
	public int getUpdatedVersion()
	{
		return 5;
	}
	
	@Override
	public void update() throws IOException
	{
		updateAuthenticators();
		updateRegistrationRequest();
		updateEnquiryResponse();
		updateInvitationWithCode();
	}

	private void updateInvitationWithCode()
	{
		updateGenericObjects("invitationWithCode", UpdateHelperTo2_8::updateInvitationWithCode);
	}
	
	
	private void updateAuthenticators()
	{
		updateGenericObjects("authenticator", UpdateHelperTo2_8::updateAuthenticator);
	}
	
	private void updateRegistrationRequest()
	{
		updateGenericObjects("registrationRequest", UpdateHelperTo2_8::updateRegistrationRequest);
	}

	private void updateEnquiryResponse()
	{
		updateGenericObjects("enquiryResponse", UpdateHelperTo2_8::updateEnquiryResponse);
	}
	
	
	private void updateGenericObjects(String type, GenericObjectDataUpdater updater)
	{
		List<GenericObjectBean> genereicObjects = genericObjectsDAO.getObjectsOfType(type);
		for (GenericObjectBean genericObject : genereicObjects)
		{
			ObjectNode objContent = JsonUtil.parse(genericObject.getContents());
			ObjectNode updated = updater.update(objContent);
			genericObject.setContents(JsonUtil.serialize2Bytes(updated));
			genericObjectsDAO.updateByKey(genericObject.getId(), genericObject);
		}
	}
}
