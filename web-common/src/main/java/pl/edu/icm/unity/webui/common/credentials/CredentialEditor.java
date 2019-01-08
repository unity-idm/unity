/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.ComponentsContainer;

/**
 * Implementations allow to edit a value of a credential of a fixed type.
 * @author K. Benedyczak
 */
public interface CredentialEditor
{
	/**
	 * @return the editor component
	 */
	ComponentsContainer getEditor(CredentialEditorContext context);
	
	/**
	 * @param credentialInfo extra information about the credential as returned by the credential's verificator
	 * {@link LocalCredentialVerificator#checkCredentialState(String)}.
	 * @return the viewer component, or null if there is nothing to show
	 */
	ComponentsContainer getViewer(String credentialInfo);
	
	
	/**
	 * @return the credential value
	 * @throws IllegalCredentialException if the entered data is incomplete or invalid.
	 */
	String getValue() throws IllegalCredentialException;
	
	/**
	 * Signals that the UI should render an error on the dialog with credentials.
	 * Additionally the previously entered values should be reset.
	 * @param error either an error or null to clear the previous error.
	 */
	void setCredentialError(EngineException error);
}
