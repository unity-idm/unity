/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formfill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.authn.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeParamValue;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.security.AuthenticationException;

/**
 * Generates a UI based on a given registration form. User can fill the form and a request is returned.
 * The class verifies if the data obtained from an upstream IdP is complete wrt requirements of the form.
 * 
 * @author K. Benedyczak
 */
public class RegistrationRequestEditor extends CustomComponent
{
	private UnityMessageSource msg;
	private RegistrationForm form;
	private RemotelyAuthenticatedContext remotelyAuthenticated;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attrsMan;
	private AuthenticationManagement authnMan;
	
	private List<IdentityEditor> identityParamEditors;
	private List<CredentialEditor> credentialParamEditors;
	private List<FixedAttributeEditor> attributeEditor;
	private List<CheckBox> groupSelectors;
	private List<CheckBox> agreementSelectors;
	private TextArea comment;
	private TextField registrationCode;

	/**
	 * Note - the two managers must be insecure, if the form is used in not-authenticated context, 
	 * what is possible for registration form.
	 *  
	 * @param msg
	 * @param form
	 * @param remotelyAuthenticated
	 * @param identityEditorRegistry
	 * @param credentialEditorRegistry
	 * @param attributeHandlerRegistry
	 * @param attrsMan
	 * @param authnMan
	 * @throws EngineException
	 */
	public RegistrationRequestEditor(UnityMessageSource msg, RegistrationForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, AuthenticationManagement authnMan) throws EngineException
	{
		this.msg = msg;
		this.form = form;
		this.remotelyAuthenticated = remotelyAuthenticated;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attrsMan = attrsMan;
		this.authnMan = authnMan;
		checkRemotelyObtainedData();
		initUI();
	}

	private void checkRemotelyObtainedData()
	{
		List<IdentityRegistrationParam> idParams = form.getIdentityParams();
		for (IdentityRegistrationParam idParam: idParams)
		{
			if (!idParam.isOptional() && 
					(idParam.getRetrievalSettings() == ParameterRetrievalSettings.automatic
					|| idParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden))
			{
				Collection<IdentityTaV> identities = remotelyAuthenticated.getIdentities();
				boolean found = false;
				for (IdentityTaV id: identities)
					if (id.getTypeId().equals(idParam.getIdentityType()))
					{
						found = true;
						break;
					}
				if (!found)
					throw new AuthenticationException("This registration form may be used only by " +
							"users which were remotely authenticated first and who have " +
							idParam.getIdentityType() + 
							" identity provided by the remote authentication source.");
			}
		}
		
		List<AttributeRegistrationParam> aParams = form.getAttributeParams();
		if (aParams != null)
		{
			for (AttributeRegistrationParam aParam: aParams)
			{
				if (!aParam.isOptional() && 
						(aParam.getRetrievalSettings() == ParameterRetrievalSettings.automatic
						|| aParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden))
				{
					Collection<Attribute<?>> attrs = remotelyAuthenticated.getAttributes();
					boolean found = false;
					for (Attribute<?> a: attrs)
						if (a.getName().equals(aParam.getAttributeType()) && 
								a.getGroupPath().equals(aParam.getGroup()))
						{
							found = true;
							break;
						}
					if (!found)
						throw new AuthenticationException("This registration form may be used only by " +
								"users which were remotely authenticated first and who have " +
								aParam.getAttributeType() + " in group " + aParam.getGroup() 
								+ " provided by the remote authentication source.");
				}
			}
		}
		
		List<GroupRegistrationParam> gParams = form.getGroupParams();
		if (gParams != null)
		{
			for (GroupRegistrationParam gParam: gParams)
			{
				if (!gParam.isOptional() && 
						(gParam.getRetrievalSettings() == ParameterRetrievalSettings.automatic
						|| gParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden))
				{
					Collection<String> groups = remotelyAuthenticated.getGroups();
					boolean found = false;
					for (String g: groups)
						if (g.equals(gParam.getGroupPath()))
						{
							found = true;
							break;
						}
					if (!found)
						throw new AuthenticationException("This registration form may be used only by " +
								"users which were remotely authenticated first and who are members of " +
								 gParam.getGroupPath() + " group according to "
								+ " the remote authentication source.");
				}				
			}
		}
	}
	
	public RegistrationRequest getRequest() throws FormValidationException
	{
		RegistrationRequest ret = new RegistrationRequest();
		
		if (form.getAttributeParams() != null)
		{
			List<AttributeParamValue> a = new ArrayList<>();
			for (int i=0; i<form.getAttributeParams().size(); i++)
			{
				//TODO remote
				FixedAttributeEditor ae = attributeEditor.get(i);
				Attribute<?> attr = ae.getAttribute();
				AttributeParamValue ap = new AttributeParamValue();
				ap.setAttribute(attr);
				ap.setExternal(false);
				a.add(ap);
			}
			ret.setAttributes(a);
		}
		if (form.getGroupParams() != null)
		{
			List<Selection> g = new ArrayList<>();
			for (int i=0; i<form.getGroupParams().size(); i++)
			{
				//TODO remote
				g.add(new Selection(groupSelectors.get(i).getValue()));
			}
			ret.setGroupSelections(g);
		}
		if (form.getAgreements() != null)
		{
			List<Selection> a = new ArrayList<>();
			for (int i=0; i<form.getAgreements().size(); i++)
				a.add(new Selection(agreementSelectors.get(i).getValue()));
			ret.setAgreements(a);
		}
		if (form.isCollectComments())
			ret.setComments(comment.getValue());
		if (form.getRegistrationCode() != null)
			ret.setRegistrationCode(registrationCode.getValue());
		//TODO 
		return ret;
	}
	
	private void initUI() throws EngineException
	{
		GridLayout gl = new GridLayout(2, 5);
		gl.setSpacing(true);
		
		Label formName = new Label(msg.getMessage("RegistrationRequest.formName", form.getName()));
		gl.addComponent(formName, 0, 0);
		
		String info = form.getFormInformation() == null ? "" : form.getFormInformation();
		Label formInformation = new Label(info, ContentMode.HTML);
		gl.addComponent(formInformation, 1, 0);
		
		Panel identityP = new Panel(msg.getMessage("RegistrationRequest.identities"));
		gl.addComponent(identityP, 0, 1);
		identityP.setContent(createIdentityUI());

		if (form.getCredentialParams() != null && form.getCredentialParams().size() > 0)
		{
			Panel credentialP = new Panel(msg.getMessage("RegistrationRequest.credentials"));
			gl.addComponent(credentialP, 1, 1);
			credentialP.setContent(createCredentialsUI());
		}
		
		int col = 0;
		if (form.getAttributeParams() != null && form.getAttributeParams().size() > 0)
		{
			Panel attributeP = new Panel(msg.getMessage("RegistrationRequest.attributes"));
			gl.addComponent(attributeP, col, 2);
			attributeP.setContent(createAttributesUI());
			col++;
		}
		
		if (form.getGroupParams() != null && form.getGroupParams().size() > 0)
		{
			Panel groupP = new Panel(msg.getMessage("RegistrationRequest.groups"));
			gl.addComponent(groupP, col, 2);
			groupP.setContent(createGroupsUI());
		}
		
		col = 0;
		if (form.getAgreements() != null && form.getAgreements().size() > 0)
		{
			Panel agreementsP = new Panel(msg.getMessage("RegistrationRequest.agreements"));
			gl.addComponent(agreementsP, col, 3);
			agreementsP.setContent(createAgreementsUI());
			col++;
		}
		
		if (form.isCollectComments())
		{
			comment = new TextArea(msg.getMessage("RegistrationRequest.comment"));
			gl.addComponent(comment, col, 3);
		}
		
		if (form.getRegistrationCode() != null)
		{
			registrationCode = new TextField(msg.getMessage("RegistrationRequest.comment"));
			gl.addComponent(registrationCode, 0, 4);
		}
	}
	
	private Component createIdentityUI()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		List<IdentityRegistrationParam> idParams = form.getIdentityParams();
		for (int i=0; i<idParams.size(); i++)
		{
			IdentityRegistrationParam idParam = idParams.get(i);
			if (idParam.getRetrievalSettings() != ParameterRetrievalSettings.interactive)
				continue;
			IdentityEditor editor = identityEditorRegistry.getEditor(idParam.getIdentityType());
			identityParamEditors.add(editor);
			Component editorUI = editor.getEditor();
			vl.addComponent(editorUI);
			if (idParam.getLabel() != null)
				editorUI.setCaption(idParam.getLabel());
			else
				editorUI.setCaption(idParam.getIdentityType());
			//TODO
			//if (idParam.getDescription() != null)
			
			//TODO idPram.isOptional()
			
			if (i < idParams.size() - 1)
				vl.addComponent(new Label("<hr>", ContentMode.HTML));
		}
		return vl;
	}
	
	private Component createCredentialsUI() throws EngineException
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		Collection<CredentialDefinition> allCreds = authnMan.getCredentialDefinitions();
		Map<String, CredentialDefinition> credentials = new HashMap<String, CredentialDefinition>();
		for (CredentialDefinition credential: allCreds)
			credentials.put(credential.getName(), credential);

		List<CredentialRegistrationParam> credParams = form.getCredentialParams();
		for (int i=0; i<credParams.size(); i++)
		{
			CredentialRegistrationParam param = credParams.get(i);
			CredentialEditor editor = credentialEditorRegistry.getEditor(param.getCredentialName());
			CredentialDefinition credDefinition = credentials.get(param.getCredentialName());
			Component editorUI = editor.getEditor(credDefinition.getJsonConfiguration());
			if (param.getLabel() != null)
				editorUI.setCaption(param.getLabel());
			else
				editorUI.setCaption(param.getCredentialName());
			credentialParamEditors.add(editor);
			vl.addComponent(editorUI);
			//TODO
			//if (credParam.getDescription() != null)
			if (i < credParams.size() - 1)
				vl.addComponent(new Label("<hr>", ContentMode.HTML));
		}
		return vl;
	}
	
	private Component createAttributesUI() throws EngineException
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);

		Map<String, AttributeType> atTypes = attrsMan.getAttributeTypesAsMap();
		List<AttributeRegistrationParam> attributeParams = form.getAttributeParams();
		for (int i=0; i<attributeParams.size(); i++)
		{
			AttributeRegistrationParam aParam = attributeParams.get(i);
			if (aParam.getRetrievalSettings() != ParameterRetrievalSettings.interactive)
				continue;
			AttributeType at = atTypes.get(aParam.getAttributeType());
			String description = aParam.isUseDescription() ? at.getDescription() : aParam.getDescription();
			FixedAttributeEditor editor = new FixedAttributeEditor(msg, attributeHandlerRegistry, 
					at, aParam.isShowGroups(), aParam.getGroup(), AttributeVisibility.full, 
					aParam.getLabel(), description);
			attributeEditor.add(editor);
			//TODO aPram.isOptional()
			vl.addComponent(editor);
			if (i < attributeParams.size() - 1)
				vl.addComponent(new Label("<hr>", ContentMode.HTML));
		}		
		
		return vl;
	}
	
	private Component createGroupsUI()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		List<GroupRegistrationParam> groupParams = form.getGroupParams();
		for (int i=0; i<groupParams.size(); i++)
		{
			GroupRegistrationParam gParam = groupParams.get(i);
			if (gParam.getRetrievalSettings() != ParameterRetrievalSettings.interactive)
				continue;
			CheckBox cb = new CheckBox();
			cb.setCaption(gParam.getLabel() == null ? gParam.getGroupPath() : gParam.getLabel());
			if (gParam.getDescription() != null)
				cb.setDescription(gParam.getDescription());
			groupSelectors.add(cb);
			vl.addComponent(cb);
			if (i < groupParams.size() - 1)
				vl.addComponent(new Label("<hr>", ContentMode.HTML));
		}		
		return vl;
	}

	private Component createAgreementsUI()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		List<AgreementRegistrationParam> aParams = form.getAgreements();
		for (int i=0; i<aParams.size(); i++)
		{
			AgreementRegistrationParam aParam = aParams.get(i);
			Label aText = new Label(aParam.getText(), ContentMode.HTML);
			CheckBox cb = new CheckBox(msg.getMessage("RegistrationRequest.agree"));
			cb.setRequired(aParam.isManatory());
			agreementSelectors.add(cb);
			vl.addComponent(aText);
			vl.addComponent(cb);
			if (i < aParams.size() - 1)
				vl.addComponent(new Label("<hr>", ContentMode.HTML));
		}		
		return vl;
	}
}














