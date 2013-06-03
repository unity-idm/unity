/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.samlidp;

import pl.edu.icm.unity.types.basic.Attribute;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;

/**
 * Defines mappings of Unity attributes to and from SAML attributes.
 * TODO - only Unity->SAML is supported for now. In future the whole attribute mapping must be refactored and enhanced.
 * @author K. Benedyczak
 */
public interface SamlAttributeMapper
{
	public boolean isHandled(Attribute<?> unityAttribute);
	public AttributeType convertToSaml(Attribute<?> unityAttribute);
	
	/*
	public boolean isHandled(AttributeType samlAttribute);
	public Attribute<?> convertToUnity(AttributeType samlAttribute);
	*/
}
