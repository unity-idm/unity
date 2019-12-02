/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.console.services.authnlayout;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.META;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.PREFIX;

import java.util.Properties;

import org.junit.Test;

import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;

public class TestServiceWebConfiguration
{
	private UnityMessageSource msg = mock(UnityMessageSource.class);
	private URIAccessService uriAccessSrv = mock(URIAccessService.class);
	private FileStorageService fileStorageSrv = mock(FileStorageService.class);
	
	//TODO add tests for default & minimal cases 
	
	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfig() throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, META)
				.update("authnScreenLogo", "http://foo")
				//as tested separately below
				.remove("authnScreenColumn.1.columnContents")
				.remove("authnScreenColumn.1.columnSeparator")
				.remove("authnScreenColumn.1.columnTitle")
				.remove("authnScreenColumn.1.columnWidth")
				.remove("authnGrid.1.gridContents")
				.remove("authnGrid.1.gridRows")
				//for internal use, auto set
				.remove("defaultTheme")
				.get();

		ServiceWebConfiguration processor = new ServiceWebConfiguration();
		
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, uriAccessSrv);
		Properties result = processor.toProperties(msg, fileStorageSrv, "authName");
		
		createComparator(PREFIX, META)
			.checkMatching(result, sourceCfg);
	}


	@Test
	public void serializationOfColumnContentsIsIdempotentForCompleteNonDefaultConfig() throws EngineException
	{
		//TODO prepare proper configuration for testing, created manually
		Properties sourceCfg = new Properties();

		//TODO this can not be unit-tested as mixes parsing, data and UI state objects in one bucket.
		//needs to be refactored first.
		//TODO the trailing 7 arguments AuthnLaoutPropertiesHelper should be removed, have nothing to do with properties parsing/serialization
		//TODO AuthnLayoutPropertiesHelper should be renamed. Helper -> Parser? but depends on shape of classes after refactoring.
//		AuthenticationLayoutContent layoutContentParsed = AuthnLayoutPropertiesHelper.loadFromProperties(
//				new VaadinEndpointProperties(sourceCfg), msg, 
//				null, null, null, null, null, null, null);
//		Properties layoutContentProperties = AuthnLayoutPropertiesHelper.toProperties(msg, layoutContentParsed);
		Properties result = new Properties();
		
		createComparator(PREFIX, META)
			.checkMatching(result, sourceCfg);
		
		fail("Implement me");
	}

}
