/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_3;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.export.JsonDumpUpdate;

@Component
public class JsonDumpUpdateFromV22 implements JsonDumpUpdate
{
	JsonDumpUpdateFromV22(ObjectMapper objectMapper)
	{
	}

	@Override
	public int getUpdatedVersion()
	{
		return 22;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		return is;
	}
}
