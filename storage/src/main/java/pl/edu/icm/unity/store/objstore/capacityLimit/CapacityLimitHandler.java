/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.capacityLimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.capacityLimit.CapacityLimit;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

/**
 * Handler for {@link CapacityLimit}s storage.
 * 
 * @author P.Piernik
 *
 */
@Component
public class CapacityLimitHandler extends DefaultEntityHandler<CapacityLimit>
{
	public static final String CAPACITY_LIMIT_OBJECT_TYPE = "capacityLimit";

	@Autowired
	public CapacityLimitHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, CAPACITY_LIMIT_OBJECT_TYPE, CapacityLimit.class);
	}

	@Override
	public GenericObjectBean toBlob(CapacityLimit value)
	{
		return new GenericObjectBean(value.getName(), JsonUtil.serialize2Bytes(value.toJson()), supportedType);
	}

	@Override
	public CapacityLimit fromBlob(GenericObjectBean blob)
	{
		return new CapacityLimit(JsonUtil.parse(blob.getContents()));
	}
}
