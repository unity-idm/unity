/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Lists;
import com.vaadin.shared.ApplicationConstants;

import pl.edu.icm.unity.engine.api.utils.HiddenResourcesFilter;

/**
 * Allows to match internal Vaadin requests, which quite often needs to be handled in a special way.
 * @author K. Benedyczak
 */
public class VaadinRequestMatcher
{
	private static final List<String> VAADIN_PATHS = Lists.newArrayList(
			ApplicationConstants.HEARTBEAT_PATH + '/',
			ApplicationConstants.UIDL_PATH + '/'); 
	
	public static boolean isVaadinRequest(HttpServletRequest request)
	{
		return HiddenResourcesFilter.hasPathPrefix(request.getPathInfo(), VAADIN_PATHS);
	}
}
