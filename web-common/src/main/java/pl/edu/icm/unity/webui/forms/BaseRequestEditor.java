/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.html.HtmlEscapers;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException.Category;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.Log;
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
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.types.registration.layout.FormSeparatorElement;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Base for enquiry and registration request editors
 * @author K. Benedyczak
 */
public abstract class BaseRequestEditor<T extends BaseRegistrationInput> extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, BaseRequestEditor.class);
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
	private Map<String, AttributeType> atTypes;
	private Map<String, CredentialDefinition> credentials;

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
	
	@Override
	public void setWidthUndefined()
	{
		super.setWidthUndefined();
		getCompositionRoot().setWidthUndefined();
	}
	
	public abstract T getRequest() throws FormValidationException;
	
	/**
	 * Called if a form being edited was not accepted by the engine. 
	 * @param e
	 */
	public void markErrorsFromException(IllegalFormContentsException e)
	{
		Category category = e.getCategory();
		int position = e.getPosition();
		if (category == null)
			return;
		
		if (category == Category.CREDENTIAL)
		{
			EngineException error = e;
			if (e.getCause() != null && e.getCause() instanceof IllegalCredentialException)
				error = (IllegalCredentialException) e.getCause();
			credentialParamEditors.get(position).setCredentialError(error);
		}
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

	/**
	 * Creates main layout, inserts title and form information
	 */
	protected com.vaadin.ui.FormLayout createMainFormLayout()
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

		com.vaadin.ui.FormLayout mainFormLayout = new com.vaadin.ui.FormLayout();
		main.addComponent(mainFormLayout);
		return mainFormLayout;
	}
	
	protected void createControls(AbstractOrderedLayout layout, InvitationWithCode invitation) 
			throws EngineException
	{
		identityParamEditors = new HashMap<>();
		attributeEditor = new HashMap<>();
		atTypes = attrsMan.getAttributeTypesAsMap();
		agreementSelectors = new ArrayList<>();
		groupSelectors = new HashMap<>();
		credentialParamEditors = new ArrayList<>();
		Collection<CredentialDefinition> allCreds = authnMan.getCredentialDefinitions();
		credentials = new HashMap<String, CredentialDefinition>();
		for (CredentialDefinition credential: allCreds)
			credentials.put(credential.getName(), credential);
		
		FormElement previousInserted = null;
		for (FormElement element : form.getEffectiveFormLayout(msg).getElements())
		{
			if (createControlFor(layout, element, previousInserted, invitation))
				previousInserted = element;
		}
	}
	
	protected boolean createControlFor(AbstractOrderedLayout layout, FormElement element, 
			FormElement previousInserted, InvitationWithCode invitation) throws EngineException
	{
		switch (element.getType())
		{
		case FormLayout.IDENTITY:
			return createIdentityControl(layout, (FormParameterElement) element, 
					invitation != null ? invitation.getIdentities() : new HashMap<>());
			
		case FormLayout.ATTRIBUTE:
			return createAttributeControl(layout, (FormParameterElement) element, 
					invitation != null ? invitation.getAttributes() : new HashMap<>());
			
		case FormLayout.GROUP:
			return createGroupControl(layout, (FormParameterElement) element, 
					invitation != null ? invitation.getGroupSelections() : new HashMap<>());
			
		case FormLayout.CAPTION:
			return createLabelControl(layout, previousInserted, (FormCaptionElement) element);
			
		case FormLayout.SEPARATOR:
			return createSeparatorControl(layout, (FormSeparatorElement) element);
			
		case FormLayout.AGREEMENT:
			return createAgreementControl(layout, (FormParameterElement) element);
			
		case FormLayout.COMMENTS:
			return createCommentsControl(layout, (BasicFormElement) element);
			
		case FormLayout.CREDENTIAL:
			return createCredentialControl(layout, (FormParameterElement) element);
		}
		log.error("Unsupported form element, skipping: " + element);
		return false;
	}
	
	protected boolean createLabelControl(AbstractOrderedLayout layout, FormElement previousInserted, FormCaptionElement element)
	{
		//we don't allow for empty sections - the previously added caption is removed.
		if (previousInserted != null && previousInserted instanceof FormCaptionElement)
		{
			Component lastComponent = layout.getComponent(layout.getComponentCount()-1);
			layout.removeComponent(lastComponent);
		}
		
		Label label = new Label(element.getValue().getValue(msg));
		label.addStyleName(Styles.formSection.toString());
		layout.addComponent(label);
		return true;
	}	

	protected boolean createSeparatorControl(Layout layout, FormSeparatorElement element)
	{
		layout.addComponent(HtmlTag.hr());
		return true;
	}	
	
	protected boolean createAgreementControl(Layout layout, FormParameterElement element)
	{
		AgreementRegistrationParam aParam = form.getAgreements().get(element.getIndex());

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
		return true;
	}
	
	protected boolean createCommentsControl(Layout layout, BasicFormElement element)
	{
		comment = new TextArea();
		comment.setWidth(80, Unit.PERCENTAGE);
		comment.setCaption(msg.getMessage("RegistrationRequest.comment"));
		layout.addComponent(comment);
		return true;
	}
	
	protected boolean createIdentityControl(Layout layout, FormParameterElement element, 
			Map<Integer, PrefilledEntry<IdentityParam>> fromInvitation)
	{
		int index = element.getIndex();
		IdentityRegistrationParam idParam = form.getIdentityParams().get(index);
		IdentityTaV rid = remoteIdentitiesByType.get(idParam.getIdentityType());
		PrefilledEntry<IdentityParam> prefilledEntry = fromInvitation.get(index);

		if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.HIDDEN)
			return false;
		if (idParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden)
			return false;
		
		if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.READ_ONLY)
		{
			Label label = new Label(prefilledEntry.getEntry().toString());
			layout.addComponent(label);
		} else if (!idParam.getRetrievalSettings().isInteractivelyEntered(rid != null))
		{
			if (!idParam.getRetrievalSettings().isPotentiallyAutomaticAndVisible())
				return false;
			IdentityTaV id = remoteIdentitiesByType.get(idParam.getIdentityType());
			if (id == null)
				return false;
			
			Label label = new Label(id.toString());
			layout.addComponent(label);
		} else
		{
			IdentityEditor editor = identityEditorRegistry.getEditor(idParam.getIdentityType());
			identityParamEditors.put(index, editor);
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
		return true;
	}
	
	protected boolean createAttributeControl(AbstractOrderedLayout layout, FormParameterElement element, 
			Map<Integer, PrefilledEntry<Attribute<?>>> fromInvitation)
	{
		int index = element.getIndex();
		AttributeRegistrationParam aParam = form.getAttributeParams().get(index);
		Attribute<?> rattr = remoteAttributes.get(aParam.getGroup() + "//" + aParam.getAttributeType());
		PrefilledEntry<Attribute<?>> prefilledEntry = fromInvitation.get(index);
		Attribute<?> readOnlyAttribute = getReadOnlyAttribute(index, form.getAttributeParams(), fromInvitation);
		AttributeType aType = atTypes.get(aParam.getAttributeType());
		
		if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.HIDDEN)
			return false;
		if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden)
			return false;
		if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticOrInteractive &&
				rattr != null)
			return false;
		
		
		if (readOnlyAttribute != null)
		{
			String displayedName = aType.getDisplayedName().getValue(msg);
			String aString = attributeHandlerRegistry.getSimplifiedAttributeRepresentation(
					readOnlyAttribute, 120,	displayedName);
			Label label = new Label(aString);
			layout.addComponent(label);
		} else
		{
			String description = (aParam.getDescription() != null && !aParam.getDescription().isEmpty()) ? 
					aParam.getDescription() : null;
			String aName = isEmpty(aParam.getLabel()) ? null : aParam.getLabel();
			FixedAttributeEditor editor = new FixedAttributeEditor(msg, attributeHandlerRegistry, 
					aType, aParam.isShowGroups(), aParam.getGroup(), aType.getVisibility(), 
					aName, description, !aParam.isOptional(), false, layout);
			
			if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive 
					&& rattr != null)
				editor.setAttributeValues(rattr.getValues());
			if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.DEFAULT)
				editor.setAttributeValues(prefilledEntry.getEntry().getValues());
			
			attributeEditor.put(index, editor);
		}
		return true;
	}	
	
	protected boolean createGroupControl(AbstractOrderedLayout layout, FormParameterElement element, 
			Map<Integer, PrefilledEntry<Selection>> fromInvitation) throws EngineException
	{
		int index = element.getIndex();
		GroupRegistrationParam groupParam = form.getGroupParams().get(index);
		boolean hasRemoteGroup = remotelyAuthenticated.getGroups().contains(groupParam.getGroupPath());
		PrefilledEntry<Selection> prefilledEntry = fromInvitation.get(index);
		
		if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.HIDDEN)
			return false;
		if (groupParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden)
			return false;
		
		boolean hasPrefilledROSelected = prefilledEntry != null && 
				prefilledEntry.getMode() == PrefilledEntryMode.READ_ONLY 
				&& prefilledEntry.getEntry().isSelected();
		boolean hasAutomaticRO = groupParam.getRetrievalSettings().isPotentiallyAutomaticAndVisible() 
				&& hasRemoteGroup; 
		
		if (hasPrefilledROSelected || hasAutomaticRO)
		{
			Label label = new Label(groupParam.getGroupPath());
			layout.addComponent(label);
		} else
		{
			GroupContents contents = groupsMan.getContents(groupParam.getGroupPath(), GroupContents.METADATA);
			Group grp = contents.getGroup();

			CheckBox cb = new CheckBox();
			cb.setCaption(isEmpty(groupParam.getLabel()) ? grp.getDisplayedName().getValue(msg) 
					: groupParam.getLabel());
			if (groupParam.getDescription() != null)
				cb.setDescription(HtmlConfigurableLabel.conditionallyEscape(
						groupParam.getDescription()));
			else if (!grp.getDescription().isEmpty())
				cb.setDescription(HtmlConfigurableLabel.conditionallyEscape(
						grp.getDescription().getValue(msg)));
			
			if (groupParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive 
					&& hasRemoteGroup)
				cb.setValue(hasRemoteGroup);
			if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.DEFAULT)
				cb.setValue(prefilledEntry.getEntry().isSelected());
			groupSelectors.put(index, cb);
			layout.addComponent(cb);
		}
		return true;
	}
	
	protected boolean createCredentialControl(AbstractOrderedLayout layout, FormParameterElement element) throws EngineException
	{
		int index = element.getIndex();
		CredentialRegistrationParam param = form.getCredentialParams().get(index);
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
		return true;
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
	

	protected boolean isEmpty(String str)
	{
		return str == null || str.equals("");
	}
	
	protected static class FormErrorStatus
	{
		public FormErrorStatus()
		{
		}

		public boolean hasFormException = false;
	}
}


