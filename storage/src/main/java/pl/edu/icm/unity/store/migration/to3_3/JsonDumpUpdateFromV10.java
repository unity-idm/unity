/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_3;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.export.JsonDumpUpdate;

/**
 * Empty update from schema version 9
 */
@Component
public class JsonDumpUpdateFromV10 implements JsonDumpUpdate
{
	@Override
	public int getUpdatedVersion()
	{
		return 10;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		return is;
	}
}