/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRIDS_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRID_CONTENTS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRID_ROWS;

import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.AuthnOptionsColumns;

public class GridConfig implements AuthnElementConfiguration
{
	public final String content;
	public final int rows;

	public GridConfig(String content, int rows)
	{
		this.content = content;
		this.rows = rows;
	}

	@Override
	public PropertiesRepresentation toProperties(UnityMessageSource msg)
	{
		String id = RandomStringUtils.randomAlphabetic(6).toUpperCase();
		Properties raw = new Properties();
		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_GRIDS_PFX + id + "."
				+ VaadinEndpointProperties.AUTHN_GRID_CONTENTS, content);

		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_GRIDS_PFX + id + "."
				+ VaadinEndpointProperties.AUTHN_GRID_ROWS, String.valueOf(rows));
		return new PropertiesRepresentation(AuthnOptionsColumns.SPECIAL_ENTRY_GRID + id, raw);	
	}
	
	
	public static class GridConfigFactory implements AuthnElementConfigurationFactory
	{

		@Override
		public Optional<AuthnElementConfiguration> getConfigurationElement(UnityMessageSource msg, VaadinEndpointProperties properties, String specEntry)
		{
			if (!specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_GRID))
			{
				return Optional.empty();
			}
			
			String key = specEntry.substring(AuthnOptionsColumns.SPECIAL_ENTRY_GRID.length());
			if (key.length() == 0)
				Optional.empty();
			String contents = properties.getValue(AUTHN_GRIDS_PFX + key + "." + AUTHN_GRID_CONTENTS);
			if (contents == null)
				Optional.empty();
			int height = properties.getIntValue(AUTHN_GRIDS_PFX + key + "." + AUTHN_GRID_ROWS);

			
			return Optional.of(new GridConfig(contents, height));
		}	
	}
}