/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.console;

import org.springframework.stereotype.Component;

import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.console.SCIMServiceEditor.SCIMServiceEditorFactory;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.DefaultServicesControllerBase;
import io.imunity.vaadin.auth.services.ServiceController;
import io.imunity.vaadin.auth.services.ServiceEditor;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;

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
