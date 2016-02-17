/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.registration.EnquiryForm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handler for {@link EnquiryForm}
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
	public GenericObjectBean toBlob(EnquiryForm value, SqlSession sql)
	{
		try
		{
			ObjectNode root = value.toJson();
			byte[] contents = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize enquiry form to JSON", e);
		}
	}

	@Override
	public EnquiryForm fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(blob.getContents());
			return new EnquiryForm(root);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize enquiry form from JSON", e);
		}
	}
}



