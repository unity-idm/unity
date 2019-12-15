/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_1;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

/**
 * Empty update
 */
@Component
public class InDBUpdateFromSchema7 implements InDBContentsUpdater
{
	@Override
	public int getUpdatedVersion()
	{
		return 7;
	}
	
	@Override
	public void update() throws IOException
	{
	}
}
