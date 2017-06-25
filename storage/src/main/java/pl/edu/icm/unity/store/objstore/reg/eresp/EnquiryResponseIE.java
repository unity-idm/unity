/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.eresp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;

/**
 * Handles import/export of {@link EnquiryResponseState}.
 * @author K. Benedyczak
 */
@Component
public class EnquiryResponseIE extends GenericObjectIEBase<EnquiryResponseState>
{
	@Autowired
	public EnquiryResponseIE(EnquiryResponseDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, EnquiryResponseState.class, 115, 
				EnquiryResponseHandler.ENQUIRY_RESPONSE_OBJECT_TYPE);
	}
}



