/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

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
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.HtmlSimplifiedLabel;
import pl.edu.icm.unity.webui.common.HtmlTag;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.google.common.html.HtmlEscapers;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

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

	private void checkRemotelyObtainedData() throws AuthenticationException
	{
		List<IdentityRegistrationParam> idParams = form.getIdentityParams();
		remoteIdentitiesByType = new HashMap<>();	
		if (idParams != null)
		{
			for (IdentityRegistrationParam idParam: idParams)
			{	
				if (idParam.getRetrievalSettings() == ParameterRetrievalSettings.interactive)
					continue;
				
				Collection<IdentityTaV> identities = remotelyAuthenticated.getIdentities();
				boolean found = false;
				for (IdentityTaV id: identities)
					if (id.getTypeId().equals(idParam.getIdentityType()))
					{
						remoteIdentitiesByType.put(id.getTypeId(), id);
						found = true;
						break;
					}
				if (!found && !idParam.isOptional() 
						&& (idParam.getRetrievalSettings() == ParameterRetrievalSettings.automatic
						|| idParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden))
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
				if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.interactive)
					continue;
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
				if (!found && !aParam.isOptional() 
						&& (aParam.getRetrievalSettings() == ParameterRetrievalSettings.automatic
						|| aParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden))
					throw new AuthenticationException("This registration form may be used only by " +
							"users which were remotely authenticated first and who have " +
							aParam.getAttributeType() + " in group " + aParam.getGroup() 
							+ " provided by the remote authentication source.");
			}
		}
	}
	
	public RegistrationRequest getRequest() throws FormValidationException
	{
		RegistrationRequest ret = new RegistrationRequest();
		
		ret.setFormId(form.getName());

		boolean hasFormException = false;
		
		List<IdentityParam> identities = new ArrayList<>();
		IdentityParam ip;
		int j=0;
		for (int i=0; i<form.getIdentityParams().size(); i++)
		{
			IdentityRegistrationParam regParam = form.getIdentityParams().get(i);
			String id;
			IdentityTaV rid = remoteIdentitiesByType.get(regParam.getIdentityType());
			if (regParam.getRetrievalSettings() == ParameterRetrievalSettings.interactive
					|| regParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive
					|| (regParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticOrInteractive && rid == null))
			{
				IdentityEditor editor = identityParamEditors.get(j++);
				try
				{
					id = editor.getValue();
				} catch (IllegalIdentityValueException e)
				{
					hasFormException = true;
					continue;
				}
				ip = id == null ? null : new IdentityParam(regParam.getIdentityType(), id);
			} else
			{
				id = rid == null ? null : rid.getValue();
				ip = id == null ? null : new IdentityParam(regParam.getIdentityType(), id, 
						remotelyAuthenticated.getRemoteIdPName(), 
						remotelyAuthenticated.getInputTranslationProfile());
			}
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
					credentials.add(cp);
				} catch (IllegalCredentialException e)
				{
					hasFormException = true;
					continue;
				}
			}
			ret.setCredentials(credentials);
		}
		if (form.getAttributeParams() != null)
		{
			List<Attribute<?>> a = new ArrayList<>();
			int interactiveIndex=0;
			for (int i=0; i<form.getAttributeParams().size(); i++)
			{
				AttributeRegistrationParam aparam = form.getAttributeParams().get(i);
				
				Attribute<?> attr;
				Attribute<?> rattr = remoteAttributes.get(aparam.getGroup()+ "//" + aparam.getAttributeType());
				if (aparam.getRetrievalSettings() == ParameterRetrievalSettings.interactive
						|| aparam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive
						|| (aparam.getRetrievalSettings() == ParameterRetrievalSettings.automaticOrInteractive && rattr == null))
				{
					FixedAttributeEditor ae = attributeEditor.get(interactiveIndex++);
					try
					{
						attr = ae.getAttribute();
					} catch (FormValidationException e)
					{
						hasFormException = true;
						continue;
					}
				} else
				{
					attr = rattr;
					if (attr != null)
					{
						attr.setTranslationProfile(remotelyAuthenticated
								.getInputTranslationProfile());
						attr.setRemoteIdp(remotelyAuthenticated
								.getRemoteIdPName());
					}
				}

				if (attr != null)
				{
					a.add(attr);
				} else
					a.add(null);
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
				if (gp.getRetrievalSettings() == ParameterRetrievalSettings.interactive
						|| gp.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive
						|| (gp.getRetrievalSettings() == ParameterRetrievalSettings.automaticOrInteractive && !remotelyAuthenticated
								.getGroups().contains(gp.getGroupPath())))
				{
					g.add(new Selection(groupSelectors.get(interactiveIndex++).getValue()));
				} else
				{
					g.add(new Selection(remotelyAuthenticated.getGroups().contains(gp.getGroupPath()),
							remotelyAuthenticated.getRemoteIdPName()));
				}
			}
			ret.setGroupSelections(g);
		}
		if (form.getAgreements() != null)
		{
			List<Selection> a = new ArrayList<>();
			for (int i=0; i<form.getAgreements().size(); i++)
			{
				CheckBox cb = agreementSelectors.get(i);
				a.add(new Selection(cb.getValue()));
				if (form.getAgreements().get(i).isManatory() && !cb.getValue())
					cb.setComponentError(new UserError(msg.getMessage("selectionRequired")));
				else
					cb.setComponentError(null);
			}
			ret.setAgreements(a);
		}
		if (form.isCollectComments())
			ret.setComments(comment.getValue());
		if (form.getRegistrationCode() != null)
		{
			ret.setRegistrationCode(registrationCode.getValue());
			if (registrationCode.getValue().isEmpty())
				registrationCode.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			else
				registrationCode.setComponentError(null);
		}
		
		if (hasFormException)
			throw new FormValidationException();
		
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
		HtmlSimplifiedLabel formInformation = new HtmlSimplifiedLabel(info);
		main.addComponent(formInformation);

		FormLayout mainFormLayout = new FormLayout();
		main.addComponent(mainFormLayout);
		
		if (form.getRegistrationCode() != null)
		{
			registrationCode = new TextField(msg.getMessage("RegistrationRequest.registrationCode"));
			registrationCode.setRequired(true);
			mainFormLayout.addComponent(registrationCode);
		}
		
		if (form.getIdentityParams() != null && form.getIdentityParams().size() > 0)
			createIdentityUI(mainFormLayout);

		if (form.getCredentialParams() != null && form.getCredentialParams().size() > 0)
			createCredentialsUI(mainFormLayout);
		
		if (form.getAttributeParams() != null && form.getAttributeParams().size() > 0)
			createAttributesUI(mainFormLayout);
		
		if (form.getGroupParams() != null && form.getGroupParams().size() > 0)
		{
			createGroupsUI(mainFormLayout);
			mainFormLayout.addComponent(HtmlTag.br());
		}
		
		if (form.isCollectComments())
		{
			Label identityL = new Label(msg.getMessage("RegistrationRequest.comment"));
			identityL.addStyleName(Styles.formSection.toString());
			mainFormLayout.addComponent(identityL);
			comment = new TextArea();
			comment.setWidth(80, Unit.PERCENTAGE);
			mainFormLayout.addComponent(comment);
			mainFormLayout.addComponent(HtmlTag.br());
		}

		if (form.getAgreements() != null && form.getAgreements().size() > 0)
		{
			createAgreementsUI(mainFormLayout);
			mainFormLayout.addComponent(HtmlTag.br());
		}
		
		setCompositionRoot(main);
	}
	
	private void createIdentityUI(Layout layout)
	{
		boolean headerAdded = false;
		List<IdentityRegistrationParam> idParams = form.getIdentityParams();
		identityParamEditors = new ArrayList<>();
		for (int i=0; i<idParams.size(); i++)
		{
			IdentityRegistrationParam idParam = idParams.get(i);
			if (idParam.getRetrievalSettings() == ParameterRetrievalSettings.automatic)
				continue;
				
			IdentityTaV rid = remoteIdentitiesByType.get(idParam.getIdentityType());
			if (idParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticOrInteractive && rid != null)
				continue;
				
			if (!headerAdded)
			{
				Label identityL = new Label(msg.getMessage("RegistrationRequest.identities"));
				identityL.addStyleName(Styles.formSection.toString());
				layout.addComponent(identityL);
				headerAdded = true;
			}
			
			IdentityEditor editor = identityEditorRegistry.getEditor(idParam.getIdentityType());
			identityParamEditors.add(editor);
			ComponentsContainer editorUI = editor.getEditor(!idParam.isOptional());
			layout.addComponents(editorUI.getComponents());
			
			if (idParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive && rid != null)
			{
				 if (rid.getValue() != null)
					 editor.setDefaultValue(rid.getValue());
			}
			if (idParam.getLabel() != null)
				editorUI.setCaption(idParam.getLabel());
			if (idParam.getDescription() != null)
				editorUI.setDescription(HtmlEscapers.htmlEscaper().escape(idParam.getDescription()));
		}
		createExternalIdentitiesUI(layout);
	}
	
	private void createCredentialsUI(Layout layout) throws EngineException
	{
		Label identityL = new Label(msg.getMessage("RegistrationRequest.credentials"));
		identityL.addStyleName(Styles.formSection.toString());
		layout.addComponent(identityL);
		
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
			ComponentsContainer editorUI = editor.getEditor(false, credDefinition.getJsonConfiguration(), true);
			if (param.getLabel() != null)
				editorUI.setCaption(param.getLabel());
			else
				editorUI.setCaption(param.getCredentialName()+":");
			if (param.getDescription() != null)
				editorUI.setDescription(HtmlEscapers.htmlEscaper().escape(param.getDescription()));
			credentialParamEditors.add(editor);
			layout.addComponents(editorUI.getComponents());
				
			if (i < credParams.size() - 1)
				layout.addComponent(HtmlTag.hr());
		}
	}
	
	private void createAttributesUI(AbstractOrderedLayout layout) throws EngineException
	{
		boolean headerAdded = false;
		Map<String, AttributeType> atTypes = attrsMan.getAttributeTypesAsMap();
		List<AttributeRegistrationParam> attributeParams = form.getAttributeParams();
		attributeEditor = new ArrayList<>();
		for (int i=0; i<attributeParams.size(); i++)
		{
			AttributeRegistrationParam aParam = attributeParams.get(i);
			if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.automatic)
				continue;
				
			Attribute<?> rattr = remoteAttributes.get(aParam.getGroup() + "//" + aParam.getAttributeType());
			if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticOrInteractive && rattr != null)
				continue;
			
			if (!headerAdded)
			{
				Label identityL = new Label(msg.getMessage("RegistrationRequest.attributes"));
				identityL.addStyleName(Styles.formSection.toString());
				layout.addComponent(identityL);
				headerAdded = true;
			}
			AttributeType at = atTypes.get(aParam.getAttributeType());
			String description = aParam.isUseDescription() ? at.getDescription() : aParam.getDescription();
			String aName = isEmpty(aParam.getLabel()) ? (aParam.getAttributeType()+":") : aParam.getLabel();
			FixedAttributeEditor editor = new FixedAttributeEditor(msg, attributeHandlerRegistry, 
					at, aParam.isShowGroups(), aParam.getGroup(), AttributeVisibility.full, 
					aName, description, !aParam.isOptional(), layout);
			if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive && rattr != null)
			{	
				editor.setAttributeValues(rattr.getValues());
			}
			attributeEditor.add(editor);
		}
		createExternalAttributesUI(layout);
	}
	
	private void createGroupsUI(Layout layout)
	{
		boolean headerAdded = false;
		
		List<GroupRegistrationParam> groupParams = form.getGroupParams();
		groupSelectors = new ArrayList<>();
		for (int i=0; i<groupParams.size(); i++)
		{
			GroupRegistrationParam gParam = groupParams.get(i);
			if (gParam.getRetrievalSettings() == ParameterRetrievalSettings.automatic)
				continue;
			boolean conGroup = remotelyAuthenticated.getGroups().contains(gParam.getGroupPath());
			if (gParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticOrInteractive && conGroup)
				continue;
			
			if (!headerAdded)
			{
				Label titleL = new Label(msg.getMessage("RegistrationRequest.groups"));
				titleL.addStyleName(Styles.formSection.toString());
				layout.addComponent(titleL);
				headerAdded = true;
			}
			CheckBox cb = new CheckBox();
			cb.setCaption(isEmpty(gParam.getLabel()) ? gParam.getGroupPath() : gParam.getLabel());
			if (gParam.getDescription() != null)
				cb.setDescription(HtmlEscapers.htmlEscaper().escape(gParam.getDescription()));
			
			if (gParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive && conGroup)
			{
				cb.setValue(conGroup);
			}			
			groupSelectors.add(cb);
			layout.addComponent(cb);
		}
		createExternalGroupsUI(layout);
	}

	private void createAgreementsUI(Layout layout)
	{
		Label titleL = new Label(msg.getMessage("RegistrationRequest.agreements"));
		titleL.addStyleName(Styles.formSection.toString());
		layout.addComponent(titleL);
		
		List<AgreementRegistrationParam> aParams = form.getAgreements();
		agreementSelectors = new ArrayList<>();
		for (int i=0; i<aParams.size(); i++)
		{
			AgreementRegistrationParam aParam = aParams.get(i);
			HtmlSimplifiedLabel aText = new HtmlSimplifiedLabel(aParam.getText());
			CheckBox cb = new CheckBox(msg.getMessage("RegistrationRequest.agree"));
			agreementSelectors.add(cb);
			layout.addComponent(aText);
			layout.addComponent(cb);
			if (aParam.isManatory())
			{
				Label mandatory = new Label(msg.getMessage("RegistrationRequest.mandatoryAgreement"));
				mandatory.addStyleName(Styles.italic.toString());
				layout.addComponent(mandatory);
			}
			if (i < aParams.size() - 1)
				layout.addComponent(HtmlTag.hr());
		}		
	}
	
	private void createExternalIdentitiesUI(Layout layout)
	{
		ListOfElements<String> identitiesList = new ListOfElements<>(msg, new ListOfElements.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new Label(value);
			}
		});
		List<IdentityRegistrationParam> idParams = form.getIdentityParams();
		for (int i=0; i<idParams.size(); i++)
		{
			IdentityRegistrationParam idParam = idParams.get(i);
			if (idParam.getRetrievalSettings() != ParameterRetrievalSettings.automatic &&
					idParam.getRetrievalSettings() != ParameterRetrievalSettings.automaticOrInteractive)
				continue;
			IdentityTaV id = remoteIdentitiesByType.get(idParam.getIdentityType());
			if (id == null)
				continue;
			identitiesList.addEntry(id.toString());
		}
		if (identitiesList.size() > 0)
		{
			Label titleL = new Label(msg.getMessage("RegistrationRequest.externalIdentities"));
			titleL.addStyleName(Styles.italic.toString());
			layout.addComponent(titleL);
			layout.addComponent(identitiesList);
		}
	}
	
	private void createExternalAttributesUI(Layout layout)
	{
		List<AttributeRegistrationParam> attributeParams = form.getAttributeParams();
		ListOfElements<String> attributesList = new ListOfElements<>(msg, new ListOfElements.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new Label(value);
			}
		});
		for (int i=0; i<attributeParams.size(); i++)
		{
			AttributeRegistrationParam aParam = attributeParams.get(i);
			if (aParam.getRetrievalSettings() != ParameterRetrievalSettings.automatic &&
					aParam.getRetrievalSettings() != ParameterRetrievalSettings.automaticOrInteractive)
				continue;
			Attribute<?> a = remoteAttributes.get(aParam.getGroup() + "//" + aParam.getAttributeType());
			if (a == null)
				continue;
			String aString = attributeHandlerRegistry.getSimplifiedAttributeRepresentation(a, 120);
			attributesList.addEntry(aString);
		}
		if (attributesList.size() > 0)
		{
			Label titleL = new Label(msg.getMessage("RegistrationRequest.externalAttributes"));
			titleL.addStyleName(Styles.italic.toString());
			layout.addComponent(titleL);
			layout.addComponent(attributesList);
		}
	}
	
	private void createExternalGroupsUI(Layout layout)
	{
		ListOfElements<String> groupsList = new ListOfElements<>(msg, new ListOfElements.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new Label(value);
			}
		});
		List<GroupRegistrationParam> groupParams = form.getGroupParams();
		for (int i=0; i<groupParams.size(); i++)
		{
			GroupRegistrationParam gParam = groupParams.get(i);
			if (gParam.getRetrievalSettings() != ParameterRetrievalSettings.automatic  &&
					gParam.getRetrievalSettings() != ParameterRetrievalSettings.automaticOrInteractive)
				continue;
			if (remotelyAuthenticated.getGroups().contains(gParam.getGroupPath()))
				groupsList.addEntry(gParam.getGroupPath());
		}
		if (groupsList.size() > 0)
		{
			Label titleL = new Label(msg.getMessage("RegistrationRequest.externalGroups"));
			titleL.addStyleName(Styles.italic.toString());
			layout.addComponent(titleL);
			layout.addComponent(groupsList);
		}
	}
	
	private boolean isEmpty(String str)
	{
		return str == null || str.equals("");
	}
}














