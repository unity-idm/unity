/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.samlidp.saml;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.AttributeQueryValidator;
import eu.unicore.samly2.validators.ReplayAttackChecker;

import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.assertion.SubjectType;
import xmlbeans.org.oasis.saml2.protocol.AttributeQueryDocument;

/**
 * Adds Unity specific limitations to the Attribute Queries:
 *  Subject must have the nameID set.
 *  subject's name format must be not set, be entity or DN. 
 * 
 * @author K. Benedyczak
 */
public class UnityAttributeQueryValidator extends AttributeQueryValidator
{
	public UnityAttributeQueryValidator(String responderEndpointUri, SamlTrustChecker trustChecker,
			long requestValidity, ReplayAttackChecker replayChecker)
	{
		super(responderEndpointUri, trustChecker, requestValidity, replayChecker);
	}

	@Override
	public void validate(AttributeQueryDocument wrappingDocument) throws SAMLServerException
	{
		super.validate(wrappingDocument, wrappingDocument.getAttributeQuery());
		SubjectType subject = wrappingDocument.getAttributeQuery().getSubject();
		NameIDType subjectName = subject.getNameID();
		if (subjectName == null || subjectName.isNil())
			throw new SAMLRequesterException("Subject name must be set");
		if (subjectName.getStringValue() == null)
			throw new SAMLRequesterException("Subject name contents must be set");
		String format = subjectName.getFormat();
		if (format != null && !format.equals(SAMLConstants.NFORMAT_DN) && 
				!format.equals(SAMLConstants.NFORMAT_ENTITY))
			throw new SAMLRequesterException("Subject's name format '" + format + "' is unsupported");
	}
}
