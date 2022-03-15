/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.version;

import pl.edu.icm.unity.exceptions.EngineException;

public interface VersionInformationProvider
{
	VersionInformation getVersionInformation() throws EngineException;
}
