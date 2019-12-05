/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.console.services.authnlayout;

import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.META;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.Test;

import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutPropertiesParser;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.*;

public class TestServiceWebConfiguration
{
	private UnityMessageSource msg = mock(UnityMessageSource.class);
	private URIAccessService uriAccessSrv = mock(URIAccessService.class);
	private FileStorageService fileStorageSrv = mock(FileStorageService.class);

	// TODO add tests for default & minimal cases

	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfig() throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, META)
				.update("authnScreenLogo", "http://foo")
				// as tested separately below
				.remove("authnScreenColumn.1.columnContents")
				.remove("authnScreenColumn.1.columnSeparator").remove("authnScreenColumn.1.columnTitle")
				.remove("authnScreenColumn.1.columnWidth").remove("authnGrid.1.gridContents")
				.remove("authnGrid.1.gridRows").remove("authnScreenOptionsLabel.1.text")
				// for internal use, auto set
				.remove("defaultTheme").get();

		ServiceWebConfiguration processor = new ServiceWebConfiguration();

		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, uriAccessSrv);
		Properties result = processor.toProperties(msg, fileStorageSrv, "authName");

		createComparator(PREFIX, META)
				.ignoringSuperflous(AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_TITLE,
						AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_WIDTH)
				.checkMatching(result, sourceCfg);
	}

	@Test
	public void serializationOfColumnIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS, "_SEPARATOR");
		sourceCfg.put(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_TITLE, "title");
		sourceCfg.put(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_WIDTH, "20");

		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		AuthnLayoutConfiguration layoutContentParsed = parser
				.fromProperties(new VaadinEndpointProperties(sourceCfg));
		Properties layoutContentProperties = parser.toProperties(layoutContentParsed);

		createComparator(PREFIX, META).checkMatching(layoutContentProperties, sourceCfg);
	}

	@Test
	public void serializationOfColumnWithSingleAuthnElementIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS, "pwdSys _SEPARATOR");

		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		AuthnLayoutConfiguration layoutContentParsed = parser
				.fromProperties(new VaadinEndpointProperties(sourceCfg));
		Properties layoutContentProperties = parser.toProperties(layoutContentParsed);

		createComparator(PREFIX, META)
				.ignoringSuperflous(AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_TITLE,
						AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_WIDTH)
				.checkMatching(layoutContentProperties, sourceCfg);
	}

	@Test
	public void serializationOfColumnWithGridAuthnElementIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS, "_SEPARATOR _GRID_G1");
		sourceCfg.put(PREFIX + AUTHN_GRIDS_PFX + "G1." + AUTHN_GRID_CONTENTS, "saml");
		sourceCfg.put(PREFIX + AUTHN_GRIDS_PFX + "G1." + AUTHN_GRID_ROWS, "6");

		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		AuthnLayoutConfiguration layoutContentParsed = parser
				.fromProperties(new VaadinEndpointProperties(sourceCfg));
		Properties layoutContentProperties = parser.toProperties(layoutContentParsed);

		for (Object k : layoutContentProperties.keySet())
		{
			String key = (String) k;
			if (key.startsWith(PREFIX + AUTHN_GRIDS_PFX) && key.endsWith(AUTHN_GRID_CONTENTS))
			{
				String newKey = key.substring((PREFIX + AUTHN_GRIDS_PFX).length(),
						key.length() - ((AUTHN_GRID_CONTENTS).length() + 1));
				assertThat(layoutContentProperties
						.get(PREFIX + AUTHN_GRIDS_PFX + newKey + "." + AUTHN_GRID_CONTENTS))
								.isEqualTo("saml");

				assertThat(layoutContentProperties
						.get(PREFIX + AUTHN_GRIDS_PFX + newKey + "." + AUTHN_GRID_ROWS))
								.isEqualTo("6");

				assertThat(layoutContentProperties
						.get(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS))
								.isEqualTo("_SEPARATOR _GRID_" + newKey);
				return;
			}
		}

		fail();
	}

	@Test
	public void serializationOfColumnWithSeparatorElementIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS, "_SEPARATOR_S1");
		sourceCfg.put(PREFIX + AUTHN_OPTION_LABEL_PFX + "S1." + AUTHN_OPTION_LABEL_TEXT, "sep1");

		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		AuthnLayoutConfiguration layoutContentParsed = parser
				.fromProperties(new VaadinEndpointProperties(sourceCfg));
		Properties layoutContentProperties = parser.toProperties(layoutContentParsed);

		for (Object k : layoutContentProperties.keySet())
		{
			String key = (String) k;
			if (key.startsWith(PREFIX + AUTHN_OPTION_LABEL_PFX) && key.endsWith(AUTHN_OPTION_LABEL_TEXT))
			{
				String newKey = key.substring((PREFIX + AUTHN_OPTION_LABEL_PFX).length(),
						key.length() - ((AUTHN_OPTION_LABEL_TEXT).length() + 1));
				assertThat(layoutContentProperties.get(PREFIX + AUTHN_OPTION_LABEL_PFX + newKey + "."
						+ AUTHN_OPTION_LABEL_TEXT)).isEqualTo("sep1");

				assertThat(layoutContentProperties
						.get(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS))
								.isEqualTo("_SEPARATOR_" + newKey);

				return;
			}
		}

		fail();
	}

	@Test
	public void serializationOfColumnWithHeaderElementIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS, "_HEADER_S1");
		sourceCfg.put(PREFIX + AUTHN_OPTION_LABEL_PFX + "S1." + AUTHN_OPTION_LABEL_TEXT, "sep1");

		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		AuthnLayoutConfiguration layoutContentParsed = parser
				.fromProperties(new VaadinEndpointProperties(sourceCfg));
		Properties layoutContentProperties = parser.toProperties(layoutContentParsed);

		for (Object k : layoutContentProperties.keySet())
		{
			String key = (String) k;
			if (key.startsWith(PREFIX + AUTHN_OPTION_LABEL_PFX) && key.endsWith(AUTHN_OPTION_LABEL_TEXT))
			{
				String newKey = key.substring((PREFIX + AUTHN_OPTION_LABEL_PFX).length(),
						key.length() - ((AUTHN_OPTION_LABEL_TEXT).length() + 1));
				assertThat(layoutContentProperties.get(PREFIX + AUTHN_OPTION_LABEL_PFX + newKey + "."
						+ AUTHN_OPTION_LABEL_TEXT)).isEqualTo("sep1");
				assertThat(layoutContentProperties
						.get(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS))
								.isEqualTo("_HEADER_" + newKey);
				return;
			}
		}

		fail();
	}

	@Test
	public void serializationOfColumnWithElementsWithoutValuesIsIdempotent() throws EngineException
	{
		Properties sourceCfg = new Properties();
		sourceCfg.put(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS,
				"_SEPARATOR _REGISTRATION _LAST_USED _EXPAND");

		AuthnLayoutPropertiesParser parser = new AuthnLayoutPropertiesParser(msg);
		AuthnLayoutConfiguration layoutContentParsed = parser
				.fromProperties(new VaadinEndpointProperties(sourceCfg));
		Properties layoutContentProperties = parser.toProperties(layoutContentParsed);

		createComparator(PREFIX, META)
				.ignoringSuperflous(AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_TITLE,
						AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_WIDTH)
				.checkMatching(layoutContentProperties, sourceCfg);
	}

}
