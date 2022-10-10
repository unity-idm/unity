/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static com.vaadin.shared.ApplicationConstants.HEARTBEAT_PATH;
import static java.util.Optional.ofNullable;

public class VaadinRequestTypeMatcher
{
	private static final List<String> PUSH_AND_HEARTBEAT_PARAMETERS = List.of("push", "heartbeat");

	public static boolean isVaadinBackgroundRequest(HttpServletRequest request)
	{
		return isVaadin8HeartbeatRequest(request) || isVaadin23PushOrHeartbeatRequest(request);
	}

	public static boolean isVaadin8HeartbeatRequest(HttpServletRequest request)
	{
		String pathInfo = request.getPathInfo();
		if (pathInfo == null || pathInfo.equals(""))
			return false;
		return pathInfo.startsWith(HEARTBEAT_PATH + '/');
	}

	public static boolean isVaadin23PushOrHeartbeatRequest(HttpServletRequest request)
	{
		if(request.getMethod().equalsIgnoreCase("post") || request.getMethod().equalsIgnoreCase("get"))
			return PUSH_AND_HEARTBEAT_PARAMETERS.contains(ofNullable(request.getParameter("v-r")).orElse(""));
		return false;
	}
}
