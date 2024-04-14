/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_16;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

/**
 * Empty update
 */
@Component
public class InDBUpdateFromSchema18 implements InDBContentsUpdater
{
	@Override
	public int getUpdatedVersion()
	{
		return 18;
	}
	
	@Override
	public void update() throws IOException
	{
	}
}
