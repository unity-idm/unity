/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import java.io.File;
import java.io.IOException;

import org.apache.xmlbeans.XmlException;

import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

class EntitiesDescriptorDocumentParser
{
	static EntitiesDescriptorDocument loadMetadata(String path)
	{
		try
		{
			return EntitiesDescriptorDocument.Factory.parse(new File(path));
		} catch (XmlException | IOException e)
		{
			throw new RuntimeException("Can't load test XML", e);
		}
	}
}
