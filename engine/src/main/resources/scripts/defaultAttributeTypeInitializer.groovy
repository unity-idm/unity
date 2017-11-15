/*
 * Script with default initialization of attribute types
 *
 */
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;
import groovy.transform.Field

@Field final String COMMON_ATTR_FILE = "common"


//run only if it is the first start of the server on clean DB.
if (!isColdStart)
{
	log.info("Database already initialized with content, skipping...");
	return;
}


log.info("Adding the common attribute types...");

try
{
	initializeCommonAttrTypes();
		
} catch (Exception e)
{
	log.warn("Error loading common attribute types", e);
}


void initializeCommonAttrTypes() throws EngineException
{
		List<File> files = attributeTypeSupport.getAttibuteTypeFilesFromClasspathResource();
		for (File file : files)
			if (FilenameUtils.getBaseName(file.getName()).equals(COMMON_ATTR_FILE))
			{
				List<AttributeType> attrTypes = attributeTypeSupport
						.loadAttributeTypesFromFile(file);
				for (AttributeType type : attrTypes)
					attributeTypeManagement.addAttributeType(type);
				log.info("Common attributes added from file: " + file.getName());
			}
}




