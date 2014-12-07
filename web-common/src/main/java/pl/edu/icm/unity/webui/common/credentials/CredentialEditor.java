/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
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
	public ComponentsContainer getEditor(boolean askAboutCurrent, 
			String credentialConfiguration, boolean required);
	
	/**
	 * @param credentialInfo extra information about the credential as returned by the credential's verificator
	 * {@link LocalCredentialVerificator#checkCredentialState(String)}.
	 * @return the viewer component, or null if there is nothing to show
	 */
	public Component getViewer(String credentialInfo);
	
	
	/**
	 * @return the credential value
	 * @throws IllegalCredentialException if the entered data is incomplete or invalid.
	 */
	public String getValue() throws IllegalCredentialException;
	
	/**
	 * It is guaranteed that this method will be called only if the editor was 
	 * created with askAboutCurrent==true.
	 * @return the current credential entered by the user.
	 * @throws IllegalCredentialException 
	 */
	public String getCurrentValue() throws IllegalCredentialException;
	
	/**
	 * Signals that the UI should render an error on the dialog with credentials.
	 * Additionally the previousy entered values should be reset.
	 * @param message either an error message or null to clear the previous error.
	 */
	public void setCredentialError(String message);
	
	/**
	 * Signals that the UI should render an error on the dialog with the previous credential.
	 * Only called when previous credential UI is displayed.
	 * Additionally the previously entered value should be reset.
	 * @param message either an error message or null to clear the previous error.
	 */
	public void setPreviousCredentialError(String message);
	
	
}
