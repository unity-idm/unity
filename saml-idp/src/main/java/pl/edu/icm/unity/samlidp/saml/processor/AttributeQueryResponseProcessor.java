/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.saml.processor;

import java.util.Calendar;
import java.util.Collection;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.proto.AssertionResponse;
import pl.edu.icm.unity.samlidp.saml.SAMLProcessingException;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAttributeQueryContext;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.assertion.SubjectType;
import xmlbeans.org.oasis.saml2.protocol.AttributeQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.AttributeQueryType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Extends {@link StatusResponseProcessor} to produce SAML Response documents, 
 * which are returned in the Attribute Query exchange of the Assertion Query protocol.
 * @author K. Benedyczak
 */
public class AttributeQueryResponseProcessor extends BaseResponseProcessor<AttributeQueryDocument, AttributeQueryType>
{
	public AttributeQueryResponseProcessor(SAMLAttributeQueryContext context)
	{
		this(context, Calendar.getInstance());
	}
	
	public AttributeQueryResponseProcessor(SAMLAttributeQueryContext context, Calendar authnTime)
	{
		super(context, authnTime);
	}

	public IdentityTaV getSubjectsIdentity()
	{
		NameIDType subject = context.getRequest().getSubject().getNameID();
		String nFormat = subject.getFormat();
		String nContents = subject.getStringValue();
		if (nFormat == null || nFormat.equals(SAMLConstants.NFORMAT_ENTITY))
		{
			return new IdentityTaV(PersistentIdentity.ID, nContents);
		}
		if (nFormat.equals(SAMLConstants.NFORMAT_DN))
		{
			return new IdentityTaV(X500Identity.ID, nContents);
		}
		throw new IllegalStateException("Unsupported subject format, which passed request validation - " +
				"it's a bug. " + nFormat + " :: " + nContents);
	}
	
	public ResponseDocument processAtributeRequest(Collection<Attribute<?>> attributes) 
			throws SAMLRequesterException, SAMLProcessingException
	{
		AssertionResponse resp = getOKResponseDocument();
		if (attributes != null)
		{
			SubjectType subjectWithConf = setSenderVouchesSubjectConfirmation(
					context.getRequest().getSubject());
			Assertion assertion = createAttributeAssertion(subjectWithConf, attributes);
			if (assertion != null)
				resp.addAssertion(assertion);
		}
		return resp.getXMLBeanDoc();
	}
}
