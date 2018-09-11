/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.cert;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;

/**
 * Allows to setup certificate credential. Currently no input needed.
 * 
 * @author K. Benedyczak
 */
public class CertificateCredentialEditor implements CredentialEditor
{
	public CertificateCredentialEditor()
	{	
	}

	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context)
	{
		return new ComponentsContainer();
	}

	@Override
	public String getValue() throws IllegalCredentialException
	{
		return "";
	}

	@Override
	public ComponentsContainer getViewer(String credentialConfiguration)
	{
		return new ComponentsContainer();
	}

	@Override
	public void setCredentialError(EngineException message)
	{
	}
}
