/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
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
	private EnquiryForm enquiryForm;
	
	public EnquiryResponseEditor(UnityMessageSource msg, EnquiryForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributeTypeManagement atMan, CredentialManagement credMan,
			GroupsManagement groupsMan) throws Exception
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, atMan, credMan, groupsMan);
		this.enquiryForm = form;
		initUI();
	}
	
	@Override
	public EnquiryResponse getRequest(boolean withCredentials) throws FormValidationException
	{
		EnquiryResponse ret = new EnquiryResponse();
		FormErrorStatus status = new FormErrorStatus();

		super.fillRequest(ret, status, withCredentials);
		
		if (status.hasFormException)
			throw new FormValidationException();
		
		return ret;
	}
	
	private void initUI() throws EngineException
	{
		FormLayout mainFormLayout = createMainFormLayout();
		
		createControls(mainFormLayout, enquiryForm.getEffectiveFormLayout(msg), null);
	}
}


