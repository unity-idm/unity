/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.console.services.authnlayout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.META;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.PREFIX;

import java.util.Optional;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutPropertiesParser;

public class TestServiceWebConfiguration
{
	private MessageSource msg = mock(MessageSource.class);
	private ImageAccessService imageAccessSrv = mock(ImageAccessService.class);
	private FileStorageService fileStorageSrv = mock(FileStorageService.class);

	@Before
	public void init()
	{
		when(imageAccessSrv.getEditableImageResourceFromUri(eq("file:../common/img/other/logo.pngfoo"), any())).thenReturn(Optional.of(new LocalOrRemoteResource("file:../common/img/other/logo.pngfoo")));
		when(imageAccessSrv.getEditableImageResourceFromUri(eq("file:../common/img/other/logo.png"), any())).thenReturn(Optional.of(new LocalOrRemoteResource("file:../common/img/other/logo.png")));
	}
	
	
	@Test
	public void serializationIsIdempotentForMinimalWithoutDefaultConfig() throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithoutDefaults(PREFIX, META)
				.update("authnScreenColumn.1.columnWidth", "15") //by default has '15.0'
				.remove("authnGrid.1.gridRows").remove("authnGrid.1.gridContents") // as tested separately below
				.get();

		Properties result = parseAndBackAll(sourceCfg, null);

		createComparator(PREFIX, META)
				.checkMatching(result, sourceCfg);
	}

	@Test
	public void serializationIsIdempotentForMinimalWithDefaultsSetConfig() throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithDefaults(PREFIX, META)
				.update("authnScreenColumn.1.columnWidth", "15") //by default has '15.0'
				.remove("authnGrid.1.gridRows").remove("authnGrid.1.gridContents") // as tested separately below
				.get();

		Properties result = parseAndBackAll(sourceCfg, "theme-foo");

		createComparator(PREFIX, META)
				.expectExtra("mainTheme", "theme-foo")
				.checkMatching(result, sourceCfg);
	}
	
	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfig() throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, META)
				.update("authnScreenColumn.1.columnWidth", "16") //by default has '16.0' is generated
				// as tested separately below
				.remove("authnScreenColumn.1.columnSeparator").remove("authnScreenColumn.1.columnTitle")
				.remove("authnGrid.1.gridContents")
				.remove("authnGrid.1.gridRows").remove("authnScreenOptionsLabel.1.text")
				// for internal use, auto set
				.remove("defaultTheme").get();

		Properties result = parseAndBackAll(sourceCfg, "theme-foo");

		createComparator(PREFIX, META)
				.checkMatching(result, sourceCfg);
	}

	@Test
	public void serializationOfEmptyColumnIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + "authnScreenColumn.1.columnContents", "");
		sourceCfg.put(PREFIX + "authnScreenColumn.1.columnWidth", "20");

		Properties layoutContentProperties = parseAndBackLayout(sourceCfg);

		createComparator(PREFIX, META).checkMatching(layoutContentProperties, sourceCfg);
	}
	
	@Test
	public void serializationOfColumnIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + "authnScreenColumn.1.columnContents", "_SEPARATOR");
		sourceCfg.put(PREFIX + "authnScreenColumn.1.columnTitle", "title");
		sourceCfg.put(PREFIX + "authnScreenColumn.1.columnWidth", "20");

		Properties layoutContentProperties = parseAndBackLayout(sourceCfg);

		createComparator(PREFIX, META).checkMatching(layoutContentProperties, sourceCfg);
	}

	@Test
	public void serializationOfColumnWithSingleAuthnElementIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + "authnScreenColumn.1.columnContents", "pwdSys _SEPARATOR");

		Properties layoutContentProperties = parseAndBackLayout(sourceCfg);

		createComparator(PREFIX, META)
				.expectExtra("authnScreenColumn.1.columnWidth", "15")
				.checkMatching(layoutContentProperties, sourceCfg);
	}

	@Test
	public void serializationOfColumnWithGridAuthnElementIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + "authnScreenColumn.1.columnContents", "_SEPARATOR _GRID_G1");
		sourceCfg.put(PREFIX + "authnGrid.G1.gridContents", "saml");
		sourceCfg.put(PREFIX + "authnGrid.G1.gridRows", "6");

		Properties layoutContentProperties = parseAndBackLayout(sourceCfg);
		
		assertThat(layoutContentProperties.get(PREFIX + "authnGrid.1.gridContents")).isEqualTo("saml");
		assertThat(layoutContentProperties.get(PREFIX + "authnGrid.1.gridRows")).isEqualTo("6");
		assertThat(layoutContentProperties.get(PREFIX + "authnScreenColumn.1.columnContents"))
				.isEqualTo("_SEPARATOR _GRID_1");
	}

	@Test
	public void serializationOfColumnWithSeparatorElementIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + "authnScreenColumn.1.columnContents", "_SEPARATOR_S1");
		sourceCfg.put(PREFIX + "authnScreenOptionsLabel.S1.text", "sep1");

		Properties layoutContentProperties = parseAndBackLayout(sourceCfg);
		
		assertThat(layoutContentProperties.get(PREFIX + "authnScreenOptionsLabel.1.text")).isEqualTo("sep1");
		assertThat(layoutContentProperties.get(PREFIX + "authnScreenColumn.1.columnContents")).isEqualTo("_SEPARATOR_1");
	}

	@Test
	public void serializationOfColumnWithHeaderElementIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + "authnScreenColumn.1.columnContents", "_HEADER_S1");
		sourceCfg.put(PREFIX + "authnScreenOptionsLabel.S1.text", "sep1");

		Properties layoutContentProperties = parseAndBackLayout(sourceCfg);
		
		assertThat(layoutContentProperties.get(PREFIX + "authnScreenOptionsLabel.1.text")).isEqualTo("sep1");
		assertThat(layoutContentProperties.get(PREFIX + "authnScreenColumn.1.columnContents")).isEqualTo("_HEADER_1");
	}

	@Test
	public void serializationOfColumnWithElementsWithoutValuesIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + "authnScreenColumn.1.columnContents", "_SEPARATOR _REGISTRATION _LAST_USED _EXPAND");

		Properties layoutContentProperties = parseAndBackLayout(sourceCfg);

		createComparator(PREFIX, META)
				.expectExtra("authnScreenColumn.1.columnWidth", "15")
				.checkMatching(layoutContentProperties, sourceCfg);
	}
	
	private Properties parseAndBackAll(Properties sourceCfg, String defTheme)
	{
		ServiceWebConfiguration processor = new ServiceWebConfiguration(defTheme);

		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, imageAccessSrv, "systemTheme");
		return processor.toProperties(msg, fileStorageSrv, "authName");
	}
	
	private Properties parseAndBackLayout(Properties sourceCfg)
	{
		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		AuthnLayoutConfiguration layoutContentParsed = parser
				.fromProperties(new VaadinEndpointProperties(sourceCfg));
		return parser.toProperties(layoutContentParsed);
	}

}
