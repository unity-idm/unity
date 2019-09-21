/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.audit;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.WebConsoleRootNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Provides @{link {@link NavigationInfo} about audit events setup submenu
 * 
 * @author R.Ledzinski
 *
 */
@Component
public class AuditEventsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
{
	public static final String ID = "AuditEvents";

	@Autowired
	public AuditEventsNavigationInfoProvider(UnityMessageSource msg,
											 WebConsoleRootNavigationInfoProvider parent)
	{
		super(new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup)
				.withParent(parent.getNavigationInfo())
				.withCaption(msg.getMessage("WebConsoleMenu.auditEvents"))
				.withIcon(Images.records.getResource()).withPosition(100).build());

	}

}
