/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_5;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;

/**
 * Update db from to 2.6.0 release version (DB schema version 2.4) from previous versions (schema 2.3)
 * @author P.Piernik
 */
@Component
public class InDBUpdateFromSchema2_3
{
	
	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
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
			if (UpdateHelperFrom2_5.updateEndpointConfiguration(objContent).isPresent())
			{		
				endpoint.setContents(JsonUtil.serialize2Bytes(objContent));
				genericObjectsDAO.updateByKey(endpoint.getId(), endpoint);
			}
		}
		
	}
}
