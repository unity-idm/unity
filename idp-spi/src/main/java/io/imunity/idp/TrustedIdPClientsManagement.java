/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.idp;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;

public interface TrustedIdPClientsManagement
{
	List<IdPClientData> getIdpClientsData() throws EngineException;

	void unblockAccess(ApplicationId appId) throws EngineException;

	void revokeAccess(ApplicationId appId) throws EngineException;

	AccessProtocol getSupportedProtocol();
}
