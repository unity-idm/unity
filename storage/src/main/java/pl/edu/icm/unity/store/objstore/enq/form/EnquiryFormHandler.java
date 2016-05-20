/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.enq.form;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.registration.EnquiryForm;

/**
 * Handler for {@link EnquiryForm}s storage.
 * @author K. Benedyczak
 */
@Component
public class EnquiryFormHandler extends DefaultEntityHandler<EnquiryForm>
{
	public static final String ENQUIRY_FORM_OBJECT_TYPE = "enquiryForm";
	
	@Autowired
	public EnquiryFormHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, ENQUIRY_FORM_OBJECT_TYPE, EnquiryForm.class);
	}

	@Override
	public GenericObjectBean toBlob(EnquiryForm value)
	{
		return new GenericObjectBean(value.getName(), 
				JsonUtil.serialize2Bytes(value.toJson()), supportedType);
	}

	@Override
	public EnquiryForm fromBlob(GenericObjectBean blob)
	{
		return new EnquiryForm(JsonUtil.parse(blob.getContents()));
	}
}
