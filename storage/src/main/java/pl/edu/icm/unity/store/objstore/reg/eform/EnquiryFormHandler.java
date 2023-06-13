/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.eform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

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
		try
		{
			return new GenericObjectBean(value.getName(),
					jsonMapper.writeValueAsBytes(EnquiryFormMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize enquiry form to JSON", e);
		}
	}

	@Override
	public EnquiryForm fromBlob(GenericObjectBean blob)
	{
		try
		{
			return EnquiryFormMapper
					.map(jsonMapper.readValue(blob.getContents(), DBEnquiryForm.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize enquiry form from JSON", e);
		}
	}
}
