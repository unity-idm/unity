/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.FullAttributeSerializer;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handler for {@link EnquiryResponseState}
 * @author K. Benedyczak
 */
@Component
public class EnquiryResponseHandler extends BaseRequestHandler<EnquiryResponse, EnquiryResponseState>
{
	public static final String ENQUIRY_RESPONSE_OBJECT_TYPE = "enquiryResponse";
	
	@Autowired
	public EnquiryResponseHandler(ObjectMapper jsonMapper, FullAttributeSerializer attributeSerializer)
	{
		super(jsonMapper, ENQUIRY_RESPONSE_OBJECT_TYPE, EnquiryResponseState.class, 
				attributeSerializer);
	}

	@Override
	protected EnquiryResponseState fromObjectNode(ObjectNode root, SqlSession sql)
	{
		EnquiryResponseState ret = new EnquiryResponseState();
		parsePreamble(ret, root, sql);
		EnquiryResponse retReq = new EnquiryResponse();
		ret.setRequest(retReq);
		parseRequest(retReq, root, sql);
		return ret;
	}
}
