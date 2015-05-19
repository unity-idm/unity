/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.processor;

import org.apache.xmlbeans.XmlObject;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLContext;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.translation.ExecutionFailException;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.RequestAbstractType;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLServerException;

/**
 * Base class for producing all SAML responses. This class handles everything what is included in 
 * the SAML base type: StatusResponse. This class is unusable alone, as all SAML protocols define
 * subtypes as their responses.
 * 
 * @author K. Benedyczak
 */
public abstract class StatusResponseProcessor<T extends XmlObject, C extends RequestAbstractType>
{
	protected SAMLContext<T, C> context;
	protected SamlIdpProperties samlConfiguration;
	
	public StatusResponseProcessor(SAMLContext<T, C> context)
	{
		this.context = context;
		this.samlConfiguration = context.getSamlConfiguration();
	}

	protected SAMLContext<T, C> getContext()
	{
		return context;
	}

	protected SamlIdpProperties getSamlConfiguration()
	{
		return samlConfiguration;
	}

	protected NameIDType getResponseIssuer()
	{
		NameIDType ret = NameIDType.Factory.newInstance();
		ret.setFormat(SAMLConstants.NFORMAT_ENTITY);
		ret.setStringValue(samlConfiguration.getValue(SamlIdpProperties.ISSUER_URI));
		return ret;
	}
	
	protected boolean doSignResponse()
	{
		if (samlConfiguration.isSignRespAlways())
			return true;
		if (samlConfiguration.isSignRespNever())
			return false;
		C request = context.getRequest();
		return request.getSignature() != null && !request.getSignature().isNil();
	}
	
	/**
	 * Converts a engine generated exception into a SAML error (returned as an Exception object).
	 * In case no mapping can be performed then {@link SAMLProcessingException} is thrown.
	 * @param e
	 * @param msg
	 * @param useExDetail
	 * @return
	 * @throws SAMLProcessingException
	 */
	public SAMLServerException convert2SAMLError(Exception e, String msg, boolean useExDetail)
	{
		SAMLConstants.Status code = null;
		SAMLConstants.SubStatus subcode = null;
		String message = null;
		
		if (msg != null)
			message = msg;
		if (msg == null && useExDetail)
		{
			if (e.getMessage() != null)
				message = e.getMessage();
			else
				message = e.getClass().getSimpleName();
		}
			
		if (e instanceof IllegalIdentityValueException)
		{
			code = SAMLConstants.Status.STATUS_REQUESTER;
			subcode = SAMLConstants.SubStatus.STATUS2_UNKNOWN_PRINCIPIAL;
		} else if (e instanceof AuthenticationException)
		{
			code = SAMLConstants.Status.STATUS_REQUESTER;
			subcode = SAMLConstants.SubStatus.STATUS2_AUTHN_FAILED;
		} else if ((e instanceof IllegalAttributeTypeException) || 
				(e instanceof IllegalAttributeValueException))
		{
			code = SAMLConstants.Status.STATUS_REQUESTER;
			subcode = SAMLConstants.SubStatus.STATUS2_INVALID_ATTR;
		} else if (e instanceof SecurityException)
		{
			code = SAMLConstants.Status.STATUS_REQUESTER;
			subcode = SAMLConstants.SubStatus.STATUS2_REQUEST_DENIED;
		} else if (e instanceof ExecutionFailException)
		{
			code = SAMLConstants.Status.STATUS_REQUESTER;
			subcode = SAMLConstants.SubStatus.STATUS2_REQUEST_DENIED;
		} else
		{
			code = SAMLConstants.Status.STATUS_RESPONDER;
		}
		
		return (code == SAMLConstants.Status.STATUS_REQUESTER) ?
				new SAMLRequesterException(subcode, message) :
				new SAMLResponderException(subcode, message);
	}
}
