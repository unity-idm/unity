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
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.authn.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeParamValue;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

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
	
	private Map<String, IdentityTaV> remoteIdentitiesByType;
	private Map<String, Attribute<?>> remoteAttributes;
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
		remoteIdentitiesByType = new HashMap<>();
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
						remoteIdentitiesByType.put(id.getTypeId(), id);
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
		remoteAttributes = new HashMap<>();
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
							remoteAttributes.put(a.getGroupPath()+"//"+
									a.getName(), a);
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
				if (gParam.getRetrievalSettings() == ParameterRetrievalSettings.automatic
						|| gParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden)
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
		
		ret.setFormId(form.getName());

		List<IdentityParam> identities = new ArrayList<>();
		int j=0;
		for (int i=0; i<form.getIdentityParams().size(); i++)
		{
			IdentityRegistrationParam regParam = form.getIdentityParams().get(i);
			String id;
			if (regParam.getRetrievalSettings() == ParameterRetrievalSettings.interactive)
			{
				IdentityEditor editor = identityParamEditors.get(j++);
				try
				{
					id = editor.getValue();
				} catch (IllegalIdentityValueException e)
				{
					throw new FormValidationException(e);
				}
			} else
			{
				id = remoteIdentitiesByType.get(regParam.getIdentityType()).getValue();
			}
			IdentityParam ip = new IdentityParam(regParam.getIdentityType(), id, true);
			identities.add(ip);
		}
		ret.setIdentities(identities);
		
		if (form.getCredentialParams() != null)
		{
			List<CredentialParamValue> credentials = new ArrayList<>();
			for (int i=0; i<form.getCredentialParams().size(); i++)
			{
				CredentialEditor credE = credentialParamEditors.get(i);
				try
				{
					String credValue = credE.getValue();
					CredentialParamValue cp = new CredentialParamValue();
					cp.setCredentialId(form.getCredentialParams().get(i).getCredentialName());
					cp.setSecrets(credValue);
				} catch (IllegalCredentialException e)
				{
					throw new FormValidationException(e);
				}
			}
			ret.setCredentials(credentials);
		}
		if (form.getAttributeParams() != null)
		{
			List<AttributeParamValue> a = new ArrayList<>();
			int interactiveIndex=0;
			for (int i=0; i<form.getAttributeParams().size(); i++)
			{
				AttributeRegistrationParam aparam = form.getAttributeParams().get(i);
				
				AttributeParamValue ap = new AttributeParamValue();
				if (aparam.getRetrievalSettings() == ParameterRetrievalSettings.interactive)
				{
					FixedAttributeEditor ae = attributeEditor.get(interactiveIndex++);
					Attribute<?> attr = ae.getAttribute();
					ap.setAttribute(attr);
					ap.setExternal(false);
				} else
				{
					Attribute<?> attr = remoteAttributes.get(
							aparam.getGroup() + "//" + aparam.getAttributeType());
					ap.setAttribute(attr);
					ap.setExternal(true);
				}
				a.add(ap);
			}
			ret.setAttributes(a);
		}
		if (form.getGroupParams() != null)
		{
			List<Selection> g = new ArrayList<>();
			int interactiveIndex=0;
			for (int i=0; i<form.getGroupParams().size(); i++)
			{
				GroupRegistrationParam gp = form.getGroupParams().get(i);
				if (gp.getRetrievalSettings() == ParameterRetrievalSettings.interactive)
				{
					g.add(new Selection(groupSelectors.get(interactiveIndex++).getValue()));
				} else
				{
					g.add(new Selection(remotelyAuthenticated.getGroups().contains(gp.getGroupPath())));
				}
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
		return ret;
	}
	
	private void initUI() throws EngineException
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setWidth(80, Unit.PERCENTAGE);
		
		Label formName = new Label(form.getName());
		formName.addStyleName(Reindeer.LABEL_H1);
		main.addComponent(formName);
		
		String info = form.getFormInformation() == null ? "" : form.getFormInformation();
		Label formInformation = new Label(info, ContentMode.HTML);
		main.addComponent(formInformation);

		FormLayout mainFormLayout = new FormLayout();
		main.addComponent(mainFormLayout);
		
		if (form.getRegistrationCode() != null)
		{
			registrationCode = new TextField(msg.getMessage("RegistrationRequest.registrationCode"));
			registrationCode.setRequired(true);
			mainFormLayout.addComponent(registrationCode);
		}
		
		createIdentityUI(mainFormLayout);

		if (form.getCredentialParams() != null && form.getCredentialParams().size() > 0)
		{
			Panel credentialP = new Panel(msg.getMessage("RegistrationRequest.credentials"));
			credentialP.setStyleName(Reindeer.PANEL_LIGHT);
			credentialP.setContent(createCredentialsUI());
			main.addComponent(credentialP);
		}
		
		if (form.getAttributeParams() != null && form.getAttributeParams().size() > 0)
		{
			Panel attributeP = new Panel(msg.getMessage("RegistrationRequest.attributes"));
			attributeP.setStyleName(Reindeer.PANEL_LIGHT);
			main.addComponent(attributeP);
			attributeP.setContent(createAttributesUI());
		}
		
		if (form.getGroupParams() != null && form.getGroupParams().size() > 0)
		{
			Panel groupP = new Panel(msg.getMessage("RegistrationRequest.groups"));
			groupP.setStyleName(Reindeer.PANEL_LIGHT);
			main.addComponent(groupP);
			groupP.setContent(createGroupsUI());
			main.addComponent(new Label("<br>", ContentMode.HTML));
		}
		
		if (form.isCollectComments())
		{
			Panel commentsP = new Panel(msg.getMessage("RegistrationRequest.comment"));
			commentsP.setStyleName(Reindeer.PANEL_LIGHT);
			comment = new TextArea();
			comment.setWidth(80, Unit.PERCENTAGE);
			commentsP.setContent(comment);
			main.addComponent(commentsP);
			main.addComponent(new Label("<br>", ContentMode.HTML));
		}

		if (form.getAgreements() != null && form.getAgreements().size() > 0)
		{
			Panel agreementsP = new Panel(msg.getMessage("RegistrationRequest.agreements"));
			agreementsP.setStyleName(Reindeer.PANEL_LIGHT);
			main.addComponent(agreementsP);
			agreementsP.setContent(createAgreementsUI());
			main.addComponent(new Label("<br>", ContentMode.HTML));
		}
		
		setCompositionRoot(main);
	}
	
	private void createIdentityUI(Layout layout)
	{
		Label identityL = new Label(msg.getMessage("RegistrationRequest.identities"));
		identityL.addStyleName(Styles.formSection.toString());
		layout.addComponent(identityL);
		List<IdentityRegistrationParam> idParams = form.getIdentityParams();
		identityParamEditors = new ArrayList<>();
		for (int i=0; i<idParams.size(); i++)
		{
			IdentityRegistrationParam idParam = idParams.get(i);
			if (idParam.getRetrievalSettings() != ParameterRetrievalSettings.interactive)
				continue;
			IdentityEditor editor = identityEditorRegistry.getEditor(idParam.getIdentityType());
			identityParamEditors.add(editor);
			AbstractField<String> editorUI = editor.getEditor(!idParam.isOptional());
			layout.addComponent(editorUI);
			if (idParam.getLabel() != null)
				editorUI.setCaption(idParam.getLabel());
			if (idParam.getDescription() != null)
				editorUI.setDescription(idParam.getDescription());
			
			if (i < idParams.size() - 1)
				layout.addComponent(new Label("<hr>", ContentMode.HTML));
		}
	}
	
	private Component createCredentialsUI() throws EngineException
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		Collection<CredentialDefinition> allCreds = authnMan.getCredentialDefinitions();
		Map<String, CredentialDefinition> credentials = new HashMap<String, CredentialDefinition>();
		for (CredentialDefinition credential: allCreds)
			credentials.put(credential.getName(), credential);
		
		credentialParamEditors = new ArrayList<>();
		List<CredentialRegistrationParam> credParams = form.getCredentialParams();
		for (int i=0; i<credParams.size(); i++)
		{
			CredentialRegistrationParam param = credParams.get(i);
			CredentialDefinition credDefinition = credentials.get(param.getCredentialName());
			CredentialEditor editor = credentialEditorRegistry.getEditor(credDefinition.getTypeId());
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
		attributeEditor = new ArrayList<>();
		for (int i=0; i<attributeParams.size(); i++)
		{
			AttributeRegistrationParam aParam = attributeParams.get(i);
			if (aParam.getRetrievalSettings() != ParameterRetrievalSettings.interactive)
				continue;
			AttributeType at = atTypes.get(aParam.getAttributeType());
			String description = aParam.isUseDescription() ? at.getDescription() : aParam.getDescription();
			String aName = isEmpty(aParam.getLabel()) ? aParam.getAttributeType() : aParam.getLabel();
			FixedAttributeEditor editor = new FixedAttributeEditor(msg, attributeHandlerRegistry, 
					at, aParam.isShowGroups(), aParam.getGroup(), AttributeVisibility.full, 
					aName, description);
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
		groupSelectors = new ArrayList<>();
		for (int i=0; i<groupParams.size(); i++)
		{
			GroupRegistrationParam gParam = groupParams.get(i);
			if (gParam.getRetrievalSettings() != ParameterRetrievalSettings.interactive)
				continue;
			CheckBox cb = new CheckBox();
			cb.setCaption(isEmpty(gParam.getLabel()) ? gParam.getGroupPath() : gParam.getLabel());
			if (gParam.getDescription() != null)
				cb.setDescription(gParam.getDescription());
			groupSelectors.add(cb);
			vl.addComponent(cb);
		}		
		return vl;
	}

	private Component createAgreementsUI()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		List<AgreementRegistrationParam> aParams = form.getAgreements();
		agreementSelectors = new ArrayList<>();
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
	
	private boolean isEmpty(String str)
	{
		return str == null || str.equals("");
	}
}














