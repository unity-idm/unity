/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_7;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

@Component
public class InDBUpdateFromSchema13 implements InDBContentsUpdater
{

	@Override
	public int getUpdatedVersion()
	{
		return 13;
	}

	@Override
	public void update() throws IOException
	{

	}

}
