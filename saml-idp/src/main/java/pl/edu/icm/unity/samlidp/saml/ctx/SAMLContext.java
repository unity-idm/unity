/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 21, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.samlidp.saml.ctx;

import org.apache.xmlbeans.XmlObject;

import pl.edu.icm.unity.samlidp.SamlProperties;

import xmlbeans.org.oasis.saml2.protocol.RequestAbstractType;

/**
 * SAML Context is responsible for storing of request data and accompanying information
 * which is required to create response.
 * 
 * This class is extended by more concrete contexts.
 * 
 * @author K. Benedyczak
 */
public class SAMLContext<T extends XmlObject, C extends RequestAbstractType>
{
	protected SamlProperties samlConfiguration;
	protected C request;
	protected T requestDoc;

	public SAMLContext(T reqDoc, C req, SamlProperties samlConfiguration)
	{
		this.samlConfiguration=samlConfiguration;
		request = req;
		requestDoc = reqDoc;
	}
	
	public SamlProperties getSamlConfiguration()
	{
		return samlConfiguration;
	}
	
	public T getRequestDocument()
	{
		return requestDoc;
	}
	
	public C getRequest()
	{
		return request;
	}	
}
