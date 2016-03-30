/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;

/**
 * Generates a UI based on a given {@link EnquiryForm}. 
 * @author K. Benedyczak
 */
public class EnquiryResponseEditor extends BaseRequestEditor<EnquiryResponse>
{
	public EnquiryResponseEditor(UnityMessageSource msg, EnquiryForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, AuthenticationManagement authnMan,
			GroupsManagement groupsMan) throws Exception
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, attrsMan, authnMan, groupsMan);
		initUI();
	}
	
	@Override
	public EnquiryResponse getRequest() throws FormValidationException
	{
		EnquiryResponse ret = new EnquiryResponse();
		FormErrorStatus status = new FormErrorStatus();

		super.fillRequest(ret, status);
		
		if (status.hasFormException)
			throw new FormValidationException();
		
		return ret;
	}
	
	private void initUI() throws EngineException
	{
		FormLayout mainFormLayout = createMainFormLayout();
		
		createControls(mainFormLayout, null);
	}
}


