/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.List;

import pl.edu.icm.unity.exceptions.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.webui.WebSession;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.UI;

/**
 * Handles results of authentication and if it is all right, redirects to the source application.
 * 
 * TODO - this is far from being complete: needs to support remote unresolved entities and
 * support no original URI and fragments.
 * 
 * @author K. Benedyczak
 */
public class AuthenticationProcessor
{
	public static void processResults(List<AuthenticationResult> results) throws AuthenticationException
	{
		Long entityId = null;
		for (AuthenticationResult result: results)
		{
			if (result.getStatus() != Status.success)
				throw new AuthenticationException("AuthenticationProcessor.authnFailed");
			long curId = result.getAuthenticatedEntity().getEntityId();
			if (entityId == null)
				entityId = curId;
			else
				if (entityId != curId)
				{
					throw new AuthenticationException("AuthenticationProcessor.authnWrongUsers");
				}
		}
		logged(results.get(0).getAuthenticatedEntity());
	}
	
	
	private static void logged(AuthenticatedEntity authenticatedEntity)
	{
		VaadinSession vss = VaadinSession.getCurrent();
		if (vss == null)
			throw new RuntimeException("BUG Can't get VaadinSession to store authenticated user's data.");
		WrappedSession session = vss.getSession();
		session.setAttribute(WebSession.USER_SESSION_KEY, authenticatedEntity);
		UI ui = UI.getCurrent();
		if (ui == null)
			throw new RuntimeException("BUG Can't get UI to redirect the authenticated user.");
		String origURL = getOriginalURL(session);
		
		ui.getPage().open(origURL, "");
	}
	
	public static String getOriginalURL(WrappedSession session)
	{
		String origURL = (String) session.getAttribute(AuthenticationFilter.ORIGINAL_ADDRESS);
		//String origFragment = (String) session.getAttribute(AuthenticationApp.ORIGINAL_FRAGMENT);
		if (origURL == null)
			throw new RuntimeException("No original address - this is not implemented yet.");
			//origURL = DEFAULT_PORTAL_PATH;
		//if (origFragment == null)
		//	origFragment = "";
		//else
		//	origFragment = "#" + origFragment;
		
		//origURL = origURL+origFragment;
		return origURL;
	}
}
