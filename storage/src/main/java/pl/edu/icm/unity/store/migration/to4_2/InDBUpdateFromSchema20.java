/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_2;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorConfigurationHandler;

@Component
class InDBUpdateFromSchema20 implements InDBContentsUpdater
{
	private final ObjectStoreDAO genericObjectsDAO;

	@Autowired
	InDBUpdateFromSchema20(ObjectStoreDAO genericObjectsDAO)
	{
		this.genericObjectsDAO = genericObjectsDAO;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 20;
	}

	@Override
	public void update() throws IOException
	{
		removeCredentialSettingFromLocalAuthenticator();
	}

	void removeCredentialSettingFromLocalAuthenticator()
	{
		List<GenericObjectBean> genericObjects = genericObjectsDAO
				.getObjectsOfType(AuthenticatorConfigurationHandler.AUTHENTICATOR_OBJECT_TYPE);
		for (GenericObjectBean genericObject : genericObjects)
		{
			ObjectNode objContent = JsonUtil.parse(genericObject.getContents());
			UpdateHelperTo4_2.removeCredentialSettingFromLocalAuthenticator(objContent)
					.ifPresent(o ->
					{
						genericObject.setContents(JsonUtil.serialize2Bytes(o));
						genericObjectsDAO.updateByKey(genericObject.getId(), genericObject);
					});
		}
	}
}
