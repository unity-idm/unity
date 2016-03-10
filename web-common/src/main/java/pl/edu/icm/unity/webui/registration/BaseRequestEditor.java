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
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

import com.google.common.html.HtmlEscapers;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextArea;

/**
 * Base for enquiry and registration request editors
 * @author K. Benedyczak
 */
public class BaseRequestEditor extends CustomComponent
{
	protected UnityMessageSource msg;
	private BaseForm form;
	private RemotelyAuthenticatedContext remotelyAuthenticated;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attrsMan;
	private GroupsManagement groupsMan;
	private AuthenticationManagement authnMan;
	
	private Map<String, IdentityTaV> remoteIdentitiesByType;
	private Map<String, Attribute<?>> remoteAttributes;
	private Map<Integer, IdentityEditor> identityParamEditors;
	private List<CredentialEditor> credentialParamEditors;
	private Map<Integer, FixedAttributeEditor> attributeEditor;
	private Map<Integer, CheckBox> groupSelectors;
	private List<CheckBox> agreementSelectors;
	private TextArea comment;

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
	public BaseRequestEditor(UnityMessageSource msg, BaseForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, AuthenticationManagement authnMan,
			GroupsManagement groupsMan) throws Exception
	{
		this.msg = msg;
		this.form = form;
		this.remotelyAuthenticated = remotelyAuthenticated;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attrsMan = attrsMan;
		this.authnMan = authnMan;
		this.groupsMan = groupsMan;
		
		checkRemotelyObtainedData();
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
				if (!found && !idParam.isOptional() && (idParam.getRetrievalSettings().isAutomaticOnly()))
					throw new AuthenticationException("This registration form may be used only by " +
							"users who were remotely authenticated first and who have " +
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
				if (!found && !aParam.isOptional() && (aParam.getRetrievalSettings().isAutomaticOnly()))
					throw new AuthenticationException("This registration form may be used only by " +
							"users who were remotely authenticated first and who have attribute '" +
							aParam.getAttributeType() + "' in group '" + aParam.getGroup() 
							+ "' provided by the remote authentication source.");
			}
		}
	}
	
	protected void fillRequest(BaseRegistrationInput ret, FormErrorStatus status) throws FormValidationException
	{
		ret.setFormId(form.getName());

		setRequestIdentities(ret, status);
		setRequestCredentials(ret, status);
		setRequestAttributes(ret, status);
		setRequestGroups(ret, status);
		setRequestAgreements(ret, status);
		
		if (form.isCollectComments())
			ret.setComments(comment.getValue());
		
		ret.setUserLocale(msg.getLocaleCode());
	}
	
	private void setRequestIdentities(BaseRegistrationInput ret, FormErrorStatus status)
	{
		List<IdentityParam> identities = new ArrayList<>();
		IdentityParam ip;
		for (int i=0; i<form.getIdentityParams().size(); i++)
		{
			IdentityRegistrationParam regParam = form.getIdentityParams().get(i);
			IdentityTaV rid = remoteIdentitiesByType.get(regParam.getIdentityType());
			if (regParam.getRetrievalSettings().isInteractivelyEntered(rid != null))
			{
				IdentityEditor editor = identityParamEditors.get(i);
				if (editor == null) //OK - invitation parameter
				{
					ip = null;
				} else
				{
					try
					{
						ip = editor.getValue();
					} catch (IllegalIdentityValueException e)
					{
						status.hasFormException = true;
						continue;
					}
				}
			} else
			{
				if (rid instanceof IdentityParam) //important - we may have metadata set by profile
				{
					ip = (IdentityParam)rid;
				} else
				{
					String id = rid == null ? null : rid.getValue();
					ip = id == null ? null : new IdentityParam(regParam.getIdentityType(), id, 
							remotelyAuthenticated.getRemoteIdPName(), 
							remotelyAuthenticated.getInputTranslationProfile());
				}
			}
			identities.add(ip);
		}
		ret.setIdentities(identities);
	}

	private void setRequestCredentials(BaseRegistrationInput ret, FormErrorStatus status)
	{
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
					status.hasFormException = true;
					continue;
				}
			}
			ret.setCredentials(credentials);
		}
	}
	
	private void setRequestAttributes(BaseRegistrationInput ret, FormErrorStatus status)
	{
		if (form.getAttributeParams() != null)
		{
			List<Attribute<?>> a = new ArrayList<>();
			for (int i=0; i<form.getAttributeParams().size(); i++)
			{
				AttributeRegistrationParam aparam = form.getAttributeParams().get(i);
				
				Attribute<?> attr;
				Attribute<?> rattr = remoteAttributes.get(aparam.getGroup()+ "//" + aparam.getAttributeType());
				if (aparam.getRetrievalSettings().isInteractivelyEntered(rattr != null))
				{
					FixedAttributeEditor ae = attributeEditor.get(i);
					if (ae == null)	//ok, attribute specified by invitation
					{
						attr = null;
					} else
					{
						try
						{
							attr = ae.getAttribute();
						} catch (FormValidationException e)
						{
							status.hasFormException = true;
							continue;
						}
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

				a.add(attr);
			}
			ret.setAttributes(a);
		}
	}
	
	private void setRequestGroups(BaseRegistrationInput ret, FormErrorStatus status)
	{
		if (form.getGroupParams() != null)
		{
			List<Selection> g = new ArrayList<>();
			for (int i=0; i<form.getGroupParams().size(); i++)
			{
				GroupRegistrationParam gp = form.getGroupParams().get(i);
				boolean hasRemoteGroup = remotelyAuthenticated.getGroups().contains(gp.getGroupPath());
				if (gp.getRetrievalSettings().isInteractivelyEntered(hasRemoteGroup))
				{
					CheckBox selector = groupSelectors.get(i);
					if (selector == null)	//ok, group specified by invitation
						g.add(null);
					else
						g.add(new Selection(selector.getValue()));
				} else
				{
					g.add(new Selection(remotelyAuthenticated.getGroups().contains(gp.getGroupPath()),
							remotelyAuthenticated.getRemoteIdPName(),
							remotelyAuthenticated.getInputTranslationProfile()));
				}
			}
			ret.setGroupSelections(g);
		}
	}

	private void setRequestAgreements(BaseRegistrationInput ret, FormErrorStatus status)
	{
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
	}

	protected void createIdentityUI(Layout layout, Map<Integer, PrefilledEntry<IdentityParam>> fromInvitation)
	{
		boolean headerAdded = false;
		List<IdentityRegistrationParam> idParams = form.getIdentityParams();
		identityParamEditors = new HashMap<>();
		for (int i=0; i<idParams.size(); i++)
		{
			IdentityRegistrationParam idParam = idParams.get(i);
			IdentityTaV rid = remoteIdentitiesByType.get(idParam.getIdentityType());

			if (!idParam.getRetrievalSettings().isInteractivelyEntered(rid != null))
				continue;
			
			PrefilledEntry<IdentityParam> prefilledEntry = fromInvitation.get(i);
			if (prefilledEntry != null && !prefilledEntry.getMode().isInteractivelyEntered())
				continue;
			
			if (!headerAdded)
			{
				Label identityL = new Label(msg.getMessage("RegistrationRequest.identities"));
				identityL.addStyleName(Styles.formSection.toString());
				layout.addComponent(identityL);
				headerAdded = true;
			}
			
			IdentityEditor editor = identityEditorRegistry.getEditor(idParam.getIdentityType());
			identityParamEditors.put(i, editor);
			ComponentsContainer editorUI = editor.getEditor(!idParam.isOptional(), false);
			layout.addComponents(editorUI.getComponents());
			
			if (idParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive && rid != null)
			{
				 if (rid.getValue() != null)
					editor.setDefaultValue(new IdentityParam(idParam.getIdentityType(),
							 rid.getValue()));
			}
			if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.DEFAULT)
				editor.setDefaultValue(prefilledEntry.getEntry());
			
			if (idParam.getLabel() != null)
				editorUI.setCaption(idParam.getLabel());
			if (idParam.getDescription() != null)
				editorUI.setDescription(HtmlEscapers.htmlEscaper().escape(idParam.getDescription()));
		}
		createExternalIdentitiesUI(layout, fromInvitation);
	}
	
	protected void createCredentialsUI(Layout layout) throws EngineException
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
				editorUI.setCaption(credDefinition.getDisplayedName().getValue(msg) + ":");
			if (param.getDescription() != null)
				editorUI.setDescription(HtmlConfigurableLabel.conditionallyEscape(param.getDescription()));
			else if (!credDefinition.getDescription().isEmpty())
				editorUI.setDescription(HtmlConfigurableLabel.conditionallyEscape(
						credDefinition.getDescription().getValue(msg)));
			credentialParamEditors.add(editor);
			layout.addComponents(editorUI.getComponents());
				
			if (i < credParams.size() - 1)
				layout.addComponent(HtmlTag.hr());
		}
	}
	
	protected void createAttributesUI(AbstractOrderedLayout layout, 
			Map<Integer, PrefilledEntry<Attribute<?>>> fromInvitation) throws EngineException
	{
		boolean headerAdded = false;
		Map<String, AttributeType> atTypes = attrsMan.getAttributeTypesAsMap();
		List<AttributeRegistrationParam> attributeParams = form.getAttributeParams();
		attributeEditor = new HashMap<>();
		for (int i=0; i<attributeParams.size(); i++)
		{
			AttributeRegistrationParam aParam = attributeParams.get(i);
			Attribute<?> rattr = remoteAttributes.get(aParam.getGroup() + "//" + aParam.getAttributeType());
			if (!aParam.getRetrievalSettings().isInteractivelyEntered(rattr != null))
				continue;
				
			PrefilledEntry<Attribute<?>> prefilledEntry = fromInvitation.get(i);
			if (prefilledEntry != null && !prefilledEntry.getMode().isInteractivelyEntered())
				continue;
			
			if (!headerAdded)
			{
				Label identityL = new Label(msg.getMessage("RegistrationRequest.attributes"));
				identityL.addStyleName(Styles.formSection.toString());
				layout.addComponent(identityL);
				headerAdded = true;
			}
			AttributeType at = atTypes.get(aParam.getAttributeType());
			String description = (aParam.getDescription() != null && !aParam.getDescription().isEmpty()) ? 
					aParam.getDescription() : null;
			String aName = isEmpty(aParam.getLabel()) ? null : aParam.getLabel();
			FixedAttributeEditor editor = new FixedAttributeEditor(msg, attributeHandlerRegistry, 
					at, aParam.isShowGroups(), aParam.getGroup(), at.getVisibility(), 
					aName, description, !aParam.isOptional(), false, layout);
			
			if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive 
					&& rattr != null)
				editor.setAttributeValues(rattr.getValues());
			if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.DEFAULT)
				editor.setAttributeValues(prefilledEntry.getEntry().getValues());
			
			attributeEditor.put(i, editor);
		}
		createExternalAttributesUI(layout, atTypes, fromInvitation);
	}
	
	protected void createGroupsUI(Layout layout, Map<Integer, PrefilledEntry<Selection>> fromInvitation) 
			throws EngineException
	{
		boolean headerAdded = false;
		List<GroupRegistrationParam> groupParams = form.getGroupParams();
		groupSelectors = new HashMap<>();
		for (int i=0; i<groupParams.size(); i++)
		{
			GroupRegistrationParam gParam = groupParams.get(i);
			boolean conGroup = remotelyAuthenticated.getGroups().contains(gParam.getGroupPath());
			if (!gParam.getRetrievalSettings().isInteractivelyEntered(conGroup))
				continue;
			PrefilledEntry<Selection> prefilledEntry = fromInvitation.get(i);
			if (prefilledEntry != null && !prefilledEntry.getMode().isInteractivelyEntered())
				continue;
			
			if (!headerAdded)
			{
				Label titleL = new Label(msg.getMessage("RegistrationRequest.groups"));
				titleL.addStyleName(Styles.formSection.toString());
				layout.addComponent(titleL);
				headerAdded = true;
			}

			GroupContents contents = groupsMan.getContents(gParam.getGroupPath(), GroupContents.METADATA);
			Group grp = contents.getGroup();

			CheckBox cb = new CheckBox();
			cb.setCaption(isEmpty(gParam.getLabel()) ? grp.getDisplayedName().getValue(msg) 
					: gParam.getLabel());
			if (gParam.getDescription() != null)
				cb.setDescription(HtmlConfigurableLabel.conditionallyEscape(gParam.getDescription()));
			else if (!grp.getDescription().isEmpty())
				cb.setDescription(HtmlConfigurableLabel.conditionallyEscape(grp.getDescription().getValue(msg)));
			
			if (gParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive && conGroup)
				cb.setValue(conGroup);
			if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.DEFAULT)
				cb.setValue(prefilledEntry.getEntry().isSelected());
			groupSelectors.put(i, cb);
			layout.addComponent(cb);
		}
		createExternalGroupsUI(layout, fromInvitation);
	}

	protected void createAgreementsUI(Layout layout)
	{
		Label titleL = new Label(msg.getMessage("RegistrationRequest.agreements"));
		titleL.addStyleName(Styles.formSection.toString());
		layout.addComponent(titleL);
		
		List<AgreementRegistrationParam> aParams = form.getAgreements();
		agreementSelectors = new ArrayList<>();
		for (int i=0; i<aParams.size(); i++)
		{
			AgreementRegistrationParam aParam = aParams.get(i);
			HtmlConfigurableLabel aText = new HtmlConfigurableLabel(aParam.getText().getValue(msg));
			CheckBox cb = new CheckBox(msg.getMessage("RegistrationRequest.agree"));
			agreementSelectors.add(cb);
			layout.addComponent(aText);
			layout.addComponent(cb);
			if (aParam.isManatory())
			{
				Label mandatory = new Label(msg.getMessage("RegistrationRequest.mandatoryAgreement"));
				mandatory.addStyleName(Styles.emphasized.toString());
				layout.addComponent(mandatory);
			}
			if (i < aParams.size() - 1)
				layout.addComponent(HtmlTag.hr());
		}		
	}
	
	private void createExternalIdentitiesUI(Layout layout, Map<Integer, PrefilledEntry<IdentityParam>> fromInvitation)
	{
		ListOfElements<String> identitiesList = new StringListOfElements(msg);
		List<IdentityRegistrationParam> idParams = form.getIdentityParams();
		for (int i=0; i<idParams.size(); i++)
		{
			IdentityRegistrationParam idParam = idParams.get(i);
			PrefilledEntry<IdentityParam> prefilledEntry = fromInvitation.get(i);
			if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.READ_ONLY)
			{
				identitiesList.addEntry(prefilledEntry.getEntry().toString());
				continue;
			}
			if (!idParam.getRetrievalSettings().isPotentiallyAutomaticAndVisible())
				continue;
			IdentityTaV id = remoteIdentitiesByType.get(idParam.getIdentityType());
			if (id == null)
				continue;
			identitiesList.addEntry(id.toString());
		}
		
		addReadOnlySection(msg.getMessage("RegistrationRequest.externalIdentities"), identitiesList, layout);
	}
	
	private void createExternalAttributesUI(Layout layout, Map<String, AttributeType> atTypes,
			Map<Integer, PrefilledEntry<Attribute<?>>> fromInvitation)
	{
		List<AttributeRegistrationParam> attributeParams = form.getAttributeParams();
		ListOfElements<String> attributesList = new StringListOfElements(msg);
		for (int i=0; i<attributeParams.size(); i++)
		{
			Attribute<?> a = getReadOnlyAttribute(i, attributeParams, fromInvitation);
			if (a == null)
				continue;
			AttributeRegistrationParam aParam = attributeParams.get(i);
			String displayedName = atTypes.get(aParam.getAttributeType()).getDisplayedName().getValue(msg);
			String aString = attributeHandlerRegistry.getSimplifiedAttributeRepresentation(a, 120,
					displayedName);
			attributesList.addEntry(aString);
		}
		addReadOnlySection(msg.getMessage("RegistrationRequest.externalAttributes"), attributesList, layout);
	}
	
	protected Attribute<?> getReadOnlyAttribute(int i, List<AttributeRegistrationParam> attributeParams,
			Map<Integer, PrefilledEntry<Attribute<?>>> fromInvitation)
	{
		AttributeRegistrationParam aParam = attributeParams.get(i);
		PrefilledEntry<Attribute<?>> prefilledEntry = fromInvitation.get(i);
		if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.READ_ONLY)
			return prefilledEntry.getEntry();
		if (!aParam.getRetrievalSettings().isPotentiallyAutomaticAndVisible())
			return null;
		return remoteAttributes.get(aParam.getGroup() + "//" + aParam.getAttributeType());
	}
	
	private void createExternalGroupsUI(Layout layout, Map<Integer, PrefilledEntry<Selection>> fromInvitation)
	{
		ListOfElements<String> groupsList = new StringListOfElements(msg);
		List<GroupRegistrationParam> groupParams = form.getGroupParams();
		for (int i=0; i<groupParams.size(); i++)
		{
			GroupRegistrationParam gParam = groupParams.get(i);
			PrefilledEntry<Selection> prefilledEntry = fromInvitation.get(i);
			if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.READ_ONLY 
					&& prefilledEntry.getEntry().isSelected())
			{
				groupsList.addEntry(gParam.getGroupPath());
				continue;
			}	
			if (!gParam.getRetrievalSettings().isPotentiallyAutomaticAndVisible())
				continue;
			if (remotelyAuthenticated.getGroups().contains(gParam.getGroupPath()))
				groupsList.addEntry(gParam.getGroupPath());
		}
		addReadOnlySection(msg.getMessage("RegistrationRequest.externalGroups"), groupsList, layout);
	}

	protected void addReadOnlySection(String caption, ListOfElements<String> list, Layout layout)
	{
		if (list.size() > 0)
		{
			Label titleL = new Label(caption);
			titleL.addStyleName(Styles.emphasized.toString());
			layout.addComponent(titleL);
			layout.addComponent(list);
		}
	}
	
	protected void createCommentsUI(Layout layout)
	{
		Label identityL = new Label(msg.getMessage("RegistrationRequest.comment"));
		identityL.addStyleName(Styles.formSection.toString());
		layout.addComponent(identityL);
		comment = new TextArea();
		comment.setWidth(80, Unit.PERCENTAGE);
		layout.addComponent(comment);
		layout.addComponent(HtmlTag.br());
	}
	
	protected boolean isEmpty(String str)
	{
		return str == null || str.equals("");
	}
	
	static class FormErrorStatus
	{
		boolean hasFormException = false;
	}
	
	static class StringListOfElements extends ListOfElements<String>
	{
		public StringListOfElements(UnityMessageSource msg)
		{
			super(msg, value -> new Label(value));
		}
	}
}


