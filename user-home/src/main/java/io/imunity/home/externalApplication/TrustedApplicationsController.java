/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.externalApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.home.TrustedApplicationManagement;
import pl.edu.icm.unity.engine.api.home.TrustedApplicationData;
import pl.edu.icm.unity.engine.api.home.TrustedApplicationData.AccessProtocol;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component
public class TrustedApplicationsController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, TrustedApplicationsController.class);

	private final Map<AccessProtocol, TrustedApplicationManagement> providers;
	private final MessageSource msg;

	@Autowired
	public TrustedApplicationsController(List<TrustedApplicationManagement> providers, MessageSource msg)
	{
		this.providers = providers.stream().collect(Collectors.toMap(p -> p.getSupportedProtocol(), p -> p));
		this.msg = msg;
	}

	List<TrustedApplicationData> getApplications() throws ControllerException
	{
		List<TrustedApplicationData> applications = new ArrayList<>();
		for (TrustedApplicationManagement p : providers.values().stream()
				.sorted((p1, p2) -> p1.getSupportedProtocol().compareTo(p2.getSupportedProtocol()))
				.collect(Collectors.toList()))
		{
			try
			{
				applications.addAll(p.getExternalApplicationData());
			} catch (EngineException e)
			{
				log.error("Can not get trusted applications", e);
				throw new ControllerException(msg.getMessage("TrustedApplicationsController.cannotGetApplications"), e);
			}
		}
		return applications;
	}

	void ublockAccess(String appId, AccessProtocol accessProtocol) throws ControllerException
	{
		try
		{
			providers.get(accessProtocol).unblockAccess(appId);
		} catch (EngineException e)
		{
			log.error("Can not unblock access for application " + appId, e);
			throw new ControllerException(msg.getMessage("TrustedApplicationsController.cannotUnblockAccess", appId),
					e);
		}
	}

	void revokeAccess(String appId, AccessProtocol accessProtocol) throws ControllerException
	{
		try
		{
			providers.get(accessProtocol).revokeAccess(appId);
		} catch (EngineException e)
		{
			log.error("Can not revoke access for application " + appId, e);
			throw new ControllerException(msg.getMessage("TrustedApplicationsController.cannotRevokeAccess", appId), e);
		}
	}

}
