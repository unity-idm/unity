/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_7;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBSchemaUpdater;

/**
 * Update db from 2.7 release version (DB schema version 2.5). See {@link UpdateHelperFrom2_7}
 */
@Component
public class InDBUpdateFromSchema2_5 implements InDBSchemaUpdater
{
	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
	@Override
	public void update() throws IOException
	{
		updateAuthenticators();
	}
	
	private void updateAuthenticators()
	{
		List<GenericObjectBean> authenticators = genericObjectsDAO.getObjectsOfType("authenticator");
		for (GenericObjectBean authenticator : authenticators)
		{
			ObjectNode objContent = JsonUtil.parse(authenticator.getContents());
			ObjectNode updated = UpdateHelperFrom2_7.updateAuthenticator(objContent);
			authenticator.setContents(JsonUtil.serialize2Bytes(updated));
			genericObjectsDAO.updateByKey(authenticator.getId(), authenticator);
		}
	}
}
