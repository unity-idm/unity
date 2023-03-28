/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.eform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase2;
import pl.edu.icm.unity.types.registration.EnquiryForm;

/**
 * Handles import/export of {@link EnquiryForm}.
 * @author K. Benedyczak
 */
@Component
public class EnquiryFormIE extends GenericObjectIEBase2<EnquiryForm>
{
	@Autowired
	public EnquiryFormIE(EnquiryFormDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 113, 
				EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE);
	}
	
	@Override
	protected EnquiryForm convert(ObjectNode src)
	{
		return EnquiryFormMapper.map(jsonMapper.convertValue(src, DBEnquiryForm.class));
	}

	@Override
	protected ObjectNode convert(EnquiryForm src)
	{
		return jsonMapper.convertValue(EnquiryFormMapper.map(src), ObjectNode.class);
	}
}



