/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.eresp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

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
		try
		{
			return new GenericObjectBean(value.getName(),
					jsonMapper.writeValueAsBytes(EnquiryResponseStateMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize registration request to JSON", e);
		}
	}

	@Override
	public EnquiryResponseState fromBlob(GenericObjectBean blob)
	{
		try
		{
			return EnquiryResponseStateMapper.map(jsonMapper.readValue(blob.getContents(), DBEnquiryResponseState.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration request from JSON", e);
		}
	}
}
