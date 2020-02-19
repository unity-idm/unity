/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_0;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

/**
 * Empty update from schema version 6
 */
@Component
public class InDBUpdateFromSchema6 implements InDBContentsUpdater
{
	@Override
	public int getUpdatedVersion()
	{
		return 6;
	}
	
	@Override
	public void update() throws IOException
	{
	}
}
