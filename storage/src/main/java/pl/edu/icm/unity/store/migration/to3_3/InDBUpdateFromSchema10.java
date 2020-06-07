/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_3;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

/**
 * Empty update from schema version 9
 */
@Component
public class InDBUpdateFromSchema10 implements InDBContentsUpdater
{
	@Override
	public int getUpdatedVersion()
	{
		return 10;
	}
	
	@Override
	public void update() throws IOException
	{
	}
}
