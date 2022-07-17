/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.home;

import java.util.List;

import pl.edu.icm.unity.engine.api.home.TrustedApplicationData.AccessProtocol;
import pl.edu.icm.unity.exceptions.EngineException;

public interface TrustedApplicationManagement
{
	List<TrustedApplicationData> getExternalApplicationData() throws EngineException;

	void unblockAccess(String appId) throws EngineException;

	void revokeAccess(String appId) throws EngineException;

	AccessProtocol getSupportedProtocol();
}
