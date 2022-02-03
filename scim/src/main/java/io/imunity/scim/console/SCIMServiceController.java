/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import org.springframework.stereotype.Component;

import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.console.SCIMServiceEditor.SCIMServiceEditorFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServicesControllerBase;
import pl.edu.icm.unity.webui.console.services.ServiceController;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;

@Component
class SCIMServiceController extends DefaultServicesControllerBase implements ServiceController
{
	private final SCIMServiceEditorFactory editorFactory;

	SCIMServiceController(MessageSource msg, EndpointManagement endpointMan,
			EndpointFileConfigurationManagement serviceFileConfigController, SCIMServiceEditorFactory editorFactory)
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.editorFactory = editorFactory;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return SCIMEndpoint.NAME;
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{
		return editorFactory.getEditor(subViewSwitcher);
	}
}
