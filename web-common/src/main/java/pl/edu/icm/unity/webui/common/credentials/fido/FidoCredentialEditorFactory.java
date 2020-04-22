/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.fido;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.FidoManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.fido.FidoCredentialVerificator;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;

/**
 * Factory for {@link FidoCredentialEditor}
 *
 * @author R. Ledznski
 */
@Component
public class FidoCredentialEditorFactory implements CredentialEditorFactory
{
    private UnityMessageSource msg;
    private FidoManagement fidoService;

    @Autowired
    public FidoCredentialEditorFactory(final UnityMessageSource msg, final FidoManagement fidoService)
    {
        this.msg = msg;
        this.fidoService = fidoService;
    }

    @Override
    public String getSupportedCredentialType()
    {
        return FidoCredentialVerificator.NAME;
    }

    @Override
    public CredentialEditor createCredentialEditor()
    {
        return new FidoCredentialEditor(msg, fidoService);
    }

    @Override
    public CredentialDefinitionEditor creteCredentialDefinitionEditor()
    {
        return new FidoCredentialDefinitionEditor(msg);
    }

    @Override
    public CredentialDefinitionViewer creteCredentialDefinitionViewer()
    {
        return new FidoCredentialDefinitionEditor(msg);
    }
}
