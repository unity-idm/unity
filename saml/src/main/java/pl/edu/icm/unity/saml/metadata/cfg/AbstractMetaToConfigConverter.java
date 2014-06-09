/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.util.Properties;

import pl.edu.icm.unity.saml.SamlProperties;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;

/**
 * Base for converters of SAML metadata into a series of property statements.
 *  
 * @author K. Benedyczak
 */
public abstract class AbstractMetaToConfigConverter
{
	/**
	 * Inserts metadata to the configuration in properties. All entries which are present in 
	 * realConfig are preserved. 
	 * @param meta
	 * @param properties
	 * @param realConfig
	 */
	protected void convertToProperties(EntitiesDescriptorDocument metaDoc, Properties properties, 
			SamlProperties realConfig, String configKey)
	{
		EntitiesDescriptorType meta = metaDoc.getEntitiesDescriptor();
		convertToProperties(meta, properties, realConfig, configKey);
	}
	
	protected void convertToProperties(EntitiesDescriptorType meta, Properties properties, 
			SamlProperties realConfig, String configKey)
	{
		EntitiesDescriptorType[] nested = meta.getEntitiesDescriptorArray();
		if (nested != null)
		{
			for (EntitiesDescriptorType nestedD: nested)
				convertToProperties(nestedD, properties, realConfig, configKey);
		}
		EntityDescriptorType[] entities = meta.getEntityDescriptorArray();
		
		if (entities != null)
		{
			for (EntityDescriptorType entity: entities)
			{
				convertToProperties(entity, properties, realConfig, configKey);
			}
		}
	}
	
	protected abstract void convertToProperties(EntityDescriptorType meta, Properties properties, 
			SamlProperties realConfig, String configKey);
}
