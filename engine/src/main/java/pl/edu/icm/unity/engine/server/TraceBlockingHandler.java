/**********************************************************************
 *                     Copyright (c) 2023, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.engine.server;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

class TraceBlockingHandler extends Handler.Wrapper
{
	TraceBlockingHandler(Handler wrapped)
	{
		super(wrapped);
	}
	
	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception
	{
		if ("TRACE".equals(request.getMethod()))
		{
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			callback.succeeded();
			return true;
		} else
		{
			return super.handle(request, response, callback);
		}
	}
}