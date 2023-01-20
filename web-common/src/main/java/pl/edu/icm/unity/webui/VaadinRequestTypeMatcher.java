/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui;

import javax.servlet.http.HttpServletRequest;

import static com.vaadin.shared.ApplicationConstants.HEARTBEAT_PATH;
import static java.util.Optional.ofNullable;

public class VaadinRequestTypeMatcher
{
	private static final String PUSH_PARAMETER = "push";
	private static final String HEARTBEAT_PARAMETER = "heartbeat";
	private static final String GET_METHOD = "get";
	private static final String POST_METHOD = "post";

	public static boolean isVaadinBackgroundRequest(HttpServletRequest request)
	{
		String pathInfo = request.getPathInfo();
		if (pathInfo == null || pathInfo.isBlank())
			return false;
		return isVaadin8HeartbeatRequest(request) || isVaadin23PushRequest(request) || isVaadin23HeartbeatRequest(request);
	}

	private static boolean isVaadin8HeartbeatRequest(HttpServletRequest request)
	{
		String pathInfo = request.getPathInfo();
		return pathInfo.startsWith('/' + HEARTBEAT_PATH + '/');
	}


	private static boolean isVaadin23PushRequest(HttpServletRequest request)
	{
		return request.getMethod().equalsIgnoreCase(GET_METHOD) &&
				ofNullable(request.getParameter("v-r")).orElse("").equalsIgnoreCase(PUSH_PARAMETER) &&
				request.getPathInfo().equals("/");
	}

	private static boolean isVaadin23HeartbeatRequest(HttpServletRequest request)
	{
		return request.getMethod().equalsIgnoreCase(POST_METHOD) &&
				HEARTBEAT_PARAMETER.equalsIgnoreCase(request.getParameter("v-r")) &&
				request.getPathInfo().equals("/");
	}
}
