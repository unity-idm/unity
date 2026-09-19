/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_3;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

@Component
class InDBUpdateFromSchema22 implements InDBContentsUpdater
{
	@Override
	public int getUpdatedVersion()
	{
		return 22;
	}

	@Override
	public void update() throws IOException
	{
	}
}
