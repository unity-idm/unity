/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.HashMap;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Generates a UI based on a given {@link EnquiryForm}. 
 * @author K. Benedyczak
 */
public class EnquiryResponseEditor extends BaseRequestEditor<EnquiryResponse>
{
	private EnquiryForm form;
	
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
		this.form = form;
		
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
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setWidth(80, Unit.PERCENTAGE);
		setCompositionRoot(main);
		
		Label formName = new Label(form.getDisplayedName().getValue(msg));
		formName.addStyleName(Styles.vLabelH1.toString());
		main.addComponent(formName);
		
		String info = form.getFormInformation() == null ? null : form.getFormInformation().getValue(msg);
		if (info != null)
		{
			HtmlConfigurableLabel formInformation = new HtmlConfigurableLabel(info);
			main.addComponent(formInformation);
		}

		FormLayout mainFormLayout = new FormLayout();
		main.addComponent(mainFormLayout);
		
		if (form.getIdentityParams() != null && form.getIdentityParams().size() > 0)
		{
			createIdentityUI(mainFormLayout, new HashMap<>());
		}

		if (form.getCredentialParams() != null && form.getCredentialParams().size() > 0)
			createCredentialsUI(mainFormLayout);
		
		if (form.getAttributeParams() != null && form.getAttributeParams().size() > 0)
		{
			createAttributesUI(mainFormLayout, new HashMap<>());
		}
		
		if (form.getGroupParams() != null && form.getGroupParams().size() > 0)
		{
			createGroupsUI(mainFormLayout, new HashMap<>());
			mainFormLayout.addComponent(HtmlTag.br());
		}
		
		if (form.isCollectComments())
			createCommentsUI(mainFormLayout);

		if (form.getAgreements() != null && form.getAgreements().size() > 0)
		{
			createAgreementsUI(mainFormLayout);
			mainFormLayout.addComponent(HtmlTag.br());
		}
	}
}


