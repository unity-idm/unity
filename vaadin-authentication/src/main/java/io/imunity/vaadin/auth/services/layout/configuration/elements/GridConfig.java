/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout.configuration.elements;

import com.google.common.base.Objects;
import io.imunity.vaadin.auth.AuthnOptionsColumns;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import static io.imunity.vaadin.endpoint.common.VaadinEndpointProperties.*;

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
	public int hashCode()
	{
		return Objects.hashCode(content, rows);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final GridConfig other = (GridConfig) obj;

		return Objects.equal(this.content, other.content) && Objects.equal(this.rows, other.rows);			
	}
	
	public static class Parser implements AuthnElementParser<GridConfig>
	{
		private final Supplier<String> gridIdGenerator;
		
		public Parser(Supplier<String> gridIdGenerator)
		{
			this.gridIdGenerator = gridIdGenerator;
		}

		@Override
		public Optional<GridConfig> getConfigurationElement(VaadinEndpointProperties properties, String specEntry)
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
		
		@Override
		public PropertiesRepresentation toProperties(GridConfig config)
		{
			String id = gridIdGenerator.get();
			Properties raw = new Properties();
			raw.put(VaadinEndpointProperties.PREFIX + AUTHN_GRIDS_PFX + id + "."
					+ AUTHN_GRID_CONTENTS, config.content);

			raw.put(VaadinEndpointProperties.PREFIX + AUTHN_GRIDS_PFX + id + "."
					+ AUTHN_GRID_ROWS, String.valueOf(config.rows));
			return new PropertiesRepresentation(AuthnOptionsColumns.SPECIAL_ENTRY_GRID + id, raw);	
		}
	}
}