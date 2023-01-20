/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.credentials;


import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;

public interface CredentialEditorFactory
{
	String getSupportedCredentialType();
	
	CredentialEditor createCredentialEditor();
	
	CredentialDefinitionEditor creteCredentialDefinitionEditor();
	
	CredentialDefinitionViewer creteCredentialDefinitionViewer();
}
