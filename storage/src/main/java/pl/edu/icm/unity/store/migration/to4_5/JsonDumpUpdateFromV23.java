/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to4_5;

import java.io.InputStream;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.export.JsonDumpUpdate;

/**
 * No format change: the attributes cache is not (yet) part of the JSON dump, as it is fully derivable
 * from the other persisted data.
 */
@Component
public class JsonDumpUpdateFromV23 implements JsonDumpUpdate
{
	@Override
	public int getUpdatedVersion()
	{
		return 23;
	}

	@Override
	public InputStream update(InputStream is)
	{
		return is;
	}
}
