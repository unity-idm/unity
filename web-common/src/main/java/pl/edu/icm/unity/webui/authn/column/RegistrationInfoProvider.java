/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Provides information about registration forms that is used to build sign in 
 * links at login page.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public interface RegistrationInfoProvider
{
	public List<RegistrationFormInfo> getRegistrationFormLinksInfo(Collection<String> configuredForms)
			throws EngineException;

	static class RegistrationFormInfo
	{
		public final String displayedName;
		public final String link;

		public RegistrationFormInfo(String displayedName, String link)
		{
			this.displayedName = displayedName;
			this.link = link;
		}
	}
}
