/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_2;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

/**
 * Dummy update - just to keep updaters chain consistent. This is needed as we aligned DB schema 
 * versions with JSON dump version, and DB number skipped 9.
 */
@Component
public class InDBUpdateFromSchema9 implements InDBContentsUpdater
{
	@Override
	public int getUpdatedVersion()
	{
		return 9;
	}
	
	@Override
	public void update() throws IOException
	{
	}
}
