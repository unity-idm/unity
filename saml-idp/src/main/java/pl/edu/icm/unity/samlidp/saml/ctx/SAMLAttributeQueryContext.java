/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 21, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.samlidp.saml.ctx;

import pl.edu.icm.unity.samlidp.SamlProperties;

import xmlbeans.org.oasis.saml2.protocol.AttributeQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.AttributeQueryType;

/**
 * SAML Context for attribute query of the assertion query protocol.
 * 
 * @author K. Benedyczak
 */
public class SAMLAttributeQueryContext extends SAMLAssertionResponseContext<AttributeQueryDocument, AttributeQueryType>
{
	public SAMLAttributeQueryContext(AttributeQueryDocument reqDoc, SamlProperties samlConfiguration)
	{
		super(reqDoc, reqDoc.getAttributeQuery(), samlConfiguration);
	}
}
