/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 21, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.saml.idp.ctx;

import org.apache.xmlbeans.XmlObject;

import pl.edu.icm.unity.saml.idp.SamlIdpProperties;

import xmlbeans.org.oasis.saml2.protocol.RequestAbstractType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * SAML Context for all protocols returning {@link ResponseDocument}
 * 
 * @author K. Benedyczak
 */
public class SAMLAssertionResponseContext<T extends XmlObject, C extends RequestAbstractType> 
	extends SAMLContext<T, C>
{
	public SAMLAssertionResponseContext(T reqDoc, C req, SamlIdpProperties samlConfiguration)
	{
		super(reqDoc, req, samlConfiguration);
	}
}
