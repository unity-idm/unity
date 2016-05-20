/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.enq.form;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.registration.EnquiryForm;

/**
 * Handles import/export of {@link EnquiryForm}.
 * @author K. Benedyczak
 */
@Component
public class EnquiryFormIE extends GenericObjectIEBase<EnquiryForm>
{
	@Autowired
	public EnquiryFormIE(EnquiryFormDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, EnquiryForm.class, 113, 
				EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE);
	}
}



