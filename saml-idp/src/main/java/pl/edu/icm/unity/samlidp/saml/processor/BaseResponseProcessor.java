/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.saml.processor;

import org.apache.xmlbeans.XmlObject;

import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.proto.AssertionResponse;

import pl.edu.icm.unity.samlidp.saml.SAMLProcessingException;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAssertionResponseContext;
import xmlbeans.org.oasis.saml2.protocol.RequestAbstractType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Base class for all processors which return SAML Response. I.e. processors for Authentication and
 * Assertion query protocols.
 * @author K. Benedyczak
 * @param <T>
 * @param <C>
 */
public abstract class BaseResponseProcessor<T extends XmlObject, C extends RequestAbstractType> 
	extends StatusResponseProcessor<T, C>
{
	public BaseResponseProcessor(SAMLAssertionResponseContext<T, C> context)
	{
		super(context);
	}

	public AssertionResponse getOKResponseDocument()
	{
		return new AssertionResponse(getResponseIssuer(), getContext().getRequest().getID());
	}

	public ResponseDocument getErrorResponse(Exception e) 
			throws SAMLProcessingException
	{
		return getErrorResponse(convert2SAMLError(e, null, true));
	}	

	public ResponseDocument getErrorResponse(Exception e, String message) 
			throws SAMLProcessingException
	{
		return getErrorResponse(convert2SAMLError(e, message, false));
	}	

	public ResponseDocument getErrorResponse(SAMLServerException e)
	{
		String id = null;
		C request = getContext().getRequest();
		if (request != null)
			id = request.getID();
		return new AssertionResponse(getResponseIssuer(), id, e).getXMLBeanDoc();
	}
}
