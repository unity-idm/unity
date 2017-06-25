/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.eresp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;

/**
 * Handler for {@link EnquiryResponseState}s storage.
 * @author K. Benedyczak
 */
@Component
public class EnquiryResponseHandler extends DefaultEntityHandler<EnquiryResponseState>
{
	public static final String ENQUIRY_RESPONSE_OBJECT_TYPE = "enquiryResponse";
	
	@Autowired
	public EnquiryResponseHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, ENQUIRY_RESPONSE_OBJECT_TYPE, EnquiryResponseState.class);
	}

	@Override
	public GenericObjectBean toBlob(EnquiryResponseState value)
	{
		return new GenericObjectBean(value.getName(), 
				JsonUtil.serialize2Bytes(value.toJson()), supportedType);
	}

	@Override
	public EnquiryResponseState fromBlob(GenericObjectBean blob)
	{
		return new EnquiryResponseState(JsonUtil.parse(blob.getContents()));
	}
}
