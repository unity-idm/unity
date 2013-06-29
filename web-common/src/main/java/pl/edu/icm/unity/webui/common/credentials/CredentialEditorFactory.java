/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;


/**
 * Implementation are intended to be app singletons, and should provide instances of {@link CredentialEditor}.
 * @author K. Benedyczak
 */
public interface CredentialEditorFactory
{
	public String getSupportedCredentialType();
	
	public CredentialEditor createCredentialEditor();
	
	public CredentialDefinitionEditor creteCredentialDefinitionEditor();
	
	public CredentialDefinitionViewer creteCredentialDefinitionViewer();	
}
