/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_3;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;

/**
 * Update from schema version 10
 */
@Component
public class InDBUpdateFromSchema10 implements InDBContentsUpdater
{
	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
	
	
	@Override
	public int getUpdatedVersion()
	{
		return 10;
	}
	
	@Override
	public void update() throws IOException
	{
		updateEndpointConfiguration();
	}
	
	private void updateEndpointConfiguration()
	{
		List<GenericObjectBean> endpoints = genericObjectsDAO.getObjectsOfType(EndpointHandler.ENDPOINT_OBJECT_TYPE);
		for (GenericObjectBean endpoint : endpoints)
		{
			ObjectNode objContent = JsonUtil.parse(endpoint.getContents());
			if (UpdateHelperTo11.replaceSidebarThemeWithUnityTheme(objContent).isPresent())
			{		
				endpoint.setContents(JsonUtil.serialize2Bytes(objContent));
				genericObjectsDAO.updateByKey(endpoint.getId(), endpoint);
			}
		}
		
	}
}
