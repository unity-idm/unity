/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.pam;

import java.util.Properties;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.userimport.UserImportSPI;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPIFactory;


/**
 * Factory for {@link OSImporter}
 * 
 * @author K. Benedyczak
 */
@Component
public class OSImporterFactory implements UserImportSPIFactory
{
	public static final String NAME = "hostOS";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public UserImportSPI getInstance(Properties configuration, String idpName)
	{
		return new OSImporter(idpName);
	}
}
