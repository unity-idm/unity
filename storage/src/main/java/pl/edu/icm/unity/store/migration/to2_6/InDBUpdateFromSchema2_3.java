/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to2_6;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;
import pl.edu.icm.unity.store.objstore.realm.RealmHandler;

/**
 * Update db from to 2.6.0 release version (DB schema version 2.4) from previous versions (schema 2.3)
 * @author P.Piernik
 */
@Component
public class InDBUpdateFromSchema2_3 implements InDBContentsUpdater
{
	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
	@Override
	public int getUpdatedVersion()
	{
		return 3;
	}
	
	@Override
	public void update() throws IOException
	{
		updateEndpointConfiguration();
		updateRealm();
	}
	
	private void updateRealm()
	{
		List<GenericObjectBean> realms = genericObjectsDAO
				.getObjectsOfType(RealmHandler.REALM_OBJECT_TYPE);
		for (GenericObjectBean realm : realms)
		{
			ObjectNode objContent = JsonUtil.parse(realm.getContents());
			UpdateHelperTo2_5.updateRealm(objContent);
			realm.setContents(JsonUtil.serialize2Bytes(objContent));
			genericObjectsDAO.updateByKey(realm.getId(), realm);
		}

	}

	private void updateEndpointConfiguration()
	{
		List<GenericObjectBean> endpoints = genericObjectsDAO.getObjectsOfType(EndpointHandler.ENDPOINT_OBJECT_TYPE);
		for (GenericObjectBean endpoint : endpoints)
		{
			ObjectNode objContent = JsonUtil.parse(endpoint.getContents());
			if (UpdateHelperTo2_5.updateEndpointConfiguration(objContent).isPresent())
			{		
				endpoint.setContents(JsonUtil.serialize2Bytes(objContent));
				genericObjectsDAO.updateByKey(endpoint.getId(), endpoint);
			}
		}
		
	}
}
