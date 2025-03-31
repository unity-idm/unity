/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;

import pl.edu.icm.unity.base.attribute.Attribute;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;

/**
 * Defines mappings of Unity attributes to and from SAML attributes.
 * TODO - only Unity->SAML is supported for now. In future the whole attribute mapping must be refactored and enhanced.
 * @author K. Benedyczak
 */
public interface SamlAttributeMapper
{
	boolean isHandled(Attribute unityAttribute);
	AttributeType convertToSaml(Attribute unityAttribute);
	
	<T extends XmlObject> T convertFromSaml(AttributeType attribute, Class<T> clazz, SchemaType type);
	/*
	public boolean isHandled(AttributeType samlAttribute);
	public Attribute convertToUnity(AttributeType samlAttribute);
	*/
}
