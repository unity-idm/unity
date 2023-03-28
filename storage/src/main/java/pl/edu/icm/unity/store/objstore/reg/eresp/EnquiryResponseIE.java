/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.eresp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase2;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;

/**
 * Handles import/export of {@link EnquiryResponseState}.
 * @author K. Benedyczak
 */
@Component
public class EnquiryResponseIE extends GenericObjectIEBase2<EnquiryResponseState>
{
	@Autowired
	public EnquiryResponseIE(EnquiryResponseDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 115, 
				EnquiryResponseHandler.ENQUIRY_RESPONSE_OBJECT_TYPE);
	}
	
	@Override
	protected EnquiryResponseState convert(ObjectNode src)
	{
		return EnquiryResponseStateMapper.map(jsonMapper.convertValue(src, DBEnquiryResponseState.class));
	}

	@Override
	protected ObjectNode convert(EnquiryResponseState src)
	{
		return jsonMapper.convertValue(EnquiryResponseStateMapper.map(src), ObjectNode.class);
	}
}



