/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import static pl.edu.icm.unity.webui.forms.FormParser.isGroupParamUsedAsMandatoryAttributeGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import com.google.common.html.HtmlEscapers;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException.Category;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.types.registration.layout.FormSeparatorElement;
import pl.edu.icm.unity.webui.common.ComponentWithLabel;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.ReadOnlyField;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewer;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext.ConfirmationMode;
import pl.edu.icm.unity.webui.common.attributes.edit.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.composite.ComponentsGroup;
import pl.edu.icm.unity.webui.common.composite.CompositeLayoutAdapter;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;
import pl.edu.icm.unity.webui.common.groups.GroupsSelection;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorContext;
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
	protected RemotelyAuthenticatedContext remotelyAuthenticated;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributeTypeManagement aTypeMan;
	private GroupsManagement groupsMan;
	private CredentialManagement credMan;
	
	private Map<String, IdentityTaV> remoteIdentitiesByType;
	private Map<String, Attribute> remoteAttributes;
	private Map<Integer, IdentityEditor> identityParamEditors;
	private List<CredentialEditor> credentialParamEditors;
	private Map<Integer, FixedAttributeEditor> attributeEditor;
	private Map<Integer, GroupsSelection> groupSelectors;
	private List<CheckBox> agreementSelectors;
	private TextArea comment;
	private Map<String, AttributeType> atTypes;
	private Map<String, CredentialDefinition> credentials;

	/**
	 * Note - the two managers must be insecure, if the form is used in not-authenticated context, 
	 * what is possible for registration form.
	 */
	public BaseRequestEditor(UnityMessageSource msg, BaseForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributeTypeManagement atMan, CredentialManagement credMan,
			GroupsManagement groupsMan)
	{
		this.msg = msg;
		this.form = form;
		this.remotelyAuthenticated = remotelyAuthenticated;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.aTypeMan = atMan;
		this.credMan = credMan;
		this.groupsMan = groupsMan;
		
		this.remoteAttributes = RemoteDataRegistrationParser.parseRemoteAttributes(form, remotelyAuthenticated);
		this.remoteIdentitiesByType = RemoteDataRegistrationParser.parseRemoteIdentities(
				form, remotelyAuthenticated);
	}
	
	protected void validateMandatoryRemoteInput() throws AuthenticationException
	{
		RemoteDataRegistrationParser.assertMandatoryRemoteAttributesArePresent(form, remoteAttributes);
		RemoteDataRegistrationParser.assertMandatoryRemoteIdentitiesArePresent(form, remoteIdentitiesByType);
	}
	
	@Override
	public void setWidthUndefined()
	{
		super.setWidthUndefined();
		getCompositionRoot().setWidthUndefined();
	}
	
	public abstract T getRequest(boolean withCredentials) throws FormValidationException;
	
	public Optional<T> getRequestWithStandardErrorHandling(boolean withCredentials)
	{
		try
		{
			return Optional.of(getRequest(withCredentials));
		} catch (FormValidationException e)
		{
			if (e.hasMessage())
				NotificationPopup.showError(e.getMessage(), "");
			return Optional.empty();
		} catch (Exception e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return Optional.empty();
		}
	}
	
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
	
	protected void fillRequest(BaseRegistrationInput ret, FormErrorStatus status, boolean withCredentials) throws FormValidationException
	{
		ret.setFormId(form.getName());

		setRequestIdentities(ret, status);
		if (withCredentials)
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
				} catch (MissingCredentialException e)
				{
					status.hasFormException = true;
					continue;
				} catch (IllegalCredentialException e)
				{
					status.hasFormException = true;
					status.errorMsg = e.getMessage();
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
			List<Attribute> a = new ArrayList<>();
			for (int i=0; i<form.getAttributeParams().size(); i++)
			{
				AttributeRegistrationParam aparam = form.getAttributeParams().get(i);
				
				Attribute attr;
				Attribute rattr = remoteAttributes.get(RemoteDataRegistrationParser.getAttributeKey(aparam));
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
							attr = ae.getAttribute().orElse(null);
						} catch (FormValidationException e)
						{
							status.hasFormException = true;
							if (e.getCause() instanceof IllegalAttributeValueException)
							{
								if (!Strings.isEmpty(e.getCause().getMessage()))
									status.errorMsg = e.getCause().getMessage();
							}
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
			List<GroupSelection> g = new ArrayList<>();
			for (int i=0; i<form.getGroupParams().size(); i++)
			{
				GroupRegistrationParam gp = form.getGroupParams().get(i);
				List<Group> allMatchingGroups = groupsMan.getGroupsByWildcard(gp.getGroupPath());
				List<Group> remotelySelected = GroupPatternMatcher.filterMatching(allMatchingGroups, 
						remotelyAuthenticated.getGroups());
				boolean hasRemoteGroup = !remotelySelected.isEmpty();
				if (gp.getRetrievalSettings().isInteractivelyEntered(hasRemoteGroup))
				{
					GroupsSelection selector = groupSelectors.get(i);
					if (selector == null)	//ok, group specified by invitation
						g.add(null);
					else
						g.add(new GroupSelection(selector.getSelectedGroups()));
				} else
				{
					List<String> remotelySelectedPaths = remotelySelected.stream()
							.map(grp -> grp.toString()).collect(Collectors.toList());
					g.add(new GroupSelection(remotelySelectedPaths,
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
	protected RegistrationLayoutsContainer createLayouts()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		main.setWidth(100, Unit.PERCENTAGE);
		setCompositionRoot(main);
		
		addLogo(main);
		
		Label formName = new Label(form.getDisplayedName().getValue(msg));
		formName.addStyleName(Styles.vLabelH1.toString());
		formName.addStyleName("u-reg-title");
		main.addComponent(formName);
		main.setComponentAlignment(formName, Alignment.MIDDLE_CENTER);
		
		String info = form.getFormInformation() == null ? null : form.getFormInformation().getValue(msg);
		if (info != null)
		{
			HtmlConfigurableLabel formInformation = new HtmlConfigurableLabel(info);
			formInformation.addStyleName("u-reg-info");
			main.addComponent(formInformation);
			main.setComponentAlignment(formInformation, Alignment.MIDDLE_CENTER);
		}
		
		RegistrationLayoutsContainer container = new RegistrationLayoutsContainer(formWidth(), formWidthUnit());
		container.addFormLayoutToRootLayout(main);
		return container;
	}
	
	private void addLogo(VerticalLayout main)
	{
		String logoURL = form.getLayoutSettings().getLogoURL();
		if (logoURL != null && !logoURL.isEmpty())
		{
			Resource logoResource;
			try
			{
				logoResource = ImageUtils.getConfiguredImageResource(logoURL);
			} catch (Exception e)
			{
				log.warn("Can't add logo", e);
				return;
			}
			Image image = new Image(null, logoResource);
			image.addStyleName("u-signup-logo");
			main.addComponent(image);
			main.setComponentAlignment(image, Alignment.TOP_CENTER);
		}
	}
	
	protected void createControls(RegistrationLayoutsContainer layoutContainer, FormLayout formLayout, PrefilledSet prefilled) 
	{
		identityParamEditors = new HashMap<>();
		attributeEditor = new HashMap<>();
		atTypes = getAttributeTypesMap();
		agreementSelectors = new ArrayList<>();
		groupSelectors = new HashMap<>();
		credentialParamEditors = new ArrayList<>();
		Collection<CredentialDefinition> allCreds = getCredentialDefinitions();
		credentials = new HashMap<>();
		for (CredentialDefinition credential: allCreds)
			credentials.put(credential.getName(), credential);
		
		FormElement previousInserted = null;
		List<FormElement> elements = formLayout.getElements();
		for (int i=0; i<elements.size(); i++)
		{
			FormElement element = elements.get(i);
			FormElement nextElement = i + 1 < elements.size() ? elements.get(i+1) : null;
			if (createControlFor(layoutContainer, element, previousInserted, nextElement, prefilled))
				previousInserted = element;
		}
		// we don't allow for empty sections
		removePreviousIfSection(layoutContainer.registrationFormLayout, previousInserted);
		
		focusFirst(layoutContainer.registrationFormLayout);
	}
	
	private Map<String, AttributeType> getAttributeTypesMap()
	{
		try
		{
			return aTypeMan.getAttributeTypesAsMap();
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not read attribute types", e);
		}
	}
	
	private Collection<CredentialDefinition> getCredentialDefinitions()
	{
		try
		{
			return credMan.getCredentialDefinitions();
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not read credential definitions", e);
		}
	}
	
	protected void focusFirst(VerticalLayout container)
	{
		Iterator<Component> iterator = container.iterator();
		while(iterator.hasNext())
		{
			Component next = iterator.next();
			if (next.isVisible() && next instanceof Focusable)
			{
				((Focusable)next).focus();
				break;
			}
		}
	}
	
	protected boolean createControlFor(RegistrationLayoutsContainer layoutContainer, FormElement element, 
			FormElement previousInserted, FormElement next, PrefilledSet prefilled)
	{
		switch (element.getType())
		{
		case IDENTITY:
			return createIdentityControl(layoutContainer.registrationFormLayout, (FormParameterElement) element, 
					prefilled.identities);
			
		case ATTRIBUTE:
			return createAttributeControl(layoutContainer.registrationFormLayout, (FormParameterElement) element, 
					prefilled.attributes);
			
		case GROUP:
			return createGroupControl(layoutContainer.registrationFormLayout,
					(FormParameterElement) element,
					prefilled.groupSelections,
					prefilled.allowedGroups);
			
		case CAPTION:
			return createLabelControl(layoutContainer.registrationFormLayout, previousInserted, 
					next, (FormCaptionElement) element);
			
		case SEPARATOR:
			return createSeparatorControl(layoutContainer.registrationFormLayout, (FormSeparatorElement) element);
			
		case AGREEMENT:
			return createAgreementControl(layoutContainer.registrationFormLayout, (FormParameterElement) element);
			
		case COMMENTS:
			return createCommentsControl(layoutContainer.registrationFormLayout, (BasicFormElement) element);
			
		case CREDENTIAL:
			return createCredentialControl(layoutContainer.registrationFormLayout, (FormParameterElement) element);
		default:
			log.error("Unsupported form element, skipping: " + element);
		}
		return false;
	}
	
	protected boolean createLabelControl(AbstractOrderedLayout layout, FormElement previousInserted, 
			FormElement next, FormCaptionElement element)
	{
		if (previousInserted == null || next == null)
			return false;
		if (previousInserted instanceof FormCaptionElement)
			return false;
		Label label = new Label(element.getValue().getValue(msg));
		label.addStyleName(Styles.formSection.toString());
		label.addStyleName("u-reg-sectionHeader");
		layout.addComponent(label);
		layout.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
		return true;
	}	

	private void removePreviousIfSection(AbstractOrderedLayout layout, FormElement previousInserted)
	{
		if (previousInserted != null && previousInserted instanceof FormCaptionElement)
		{
			Component lastComponent = layout.getComponent(layout.getComponentCount() - 1);
			layout.removeComponent(lastComponent);
		}
	}

	protected boolean createSeparatorControl(Layout layout, FormSeparatorElement element)
	{
		Label horizontalLine = HtmlTag.horizontalLine();
		horizontalLine.addStyleName("u-reg-separatorLine");
		horizontalLine.setWidth(formWidth(), formWidthUnit());
		layout.addComponent(horizontalLine);
		return true;
	}	
	
	protected boolean createAgreementControl(Layout layout, FormParameterElement element)
	{
		AgreementRegistrationParam aParam = form.getAgreements().get(element.getIndex());

		HtmlConfigurableLabel aText = new HtmlConfigurableLabel(aParam.getText().getValue(msg));
		aText.setWidth(formWidth(), formWidthUnit());
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
		comment.setWidth(formWidth(), formWidthUnit());
		String label = ComponentWithLabel.normalizeLabel(msg.getMessage("RegistrationRequest.comment"));
		if (form.getLayoutSettings().isCompactInputs())
			comment.setPlaceholder(label);
		else
			comment.setCaption(label);
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
			ReadOnlyField readOnlyField = new ReadOnlyField(prefilledEntry.getEntry().getValue(), 
					formWidth(), formWidthUnit());
			layout.addComponent(readOnlyField);
		} else if (!idParam.getRetrievalSettings().isInteractivelyEntered(rid != null))
		{
			if (!idParam.getRetrievalSettings().isPotentiallyAutomaticAndVisible())
			{
				return false;
			}
			IdentityTaV id = remoteIdentitiesByType.get(idParam.getIdentityType());
			if (id == null)
				return false;
			
			ReadOnlyField readOnlyField = new ReadOnlyField(id.getValue(), formWidth(), formWidthUnit());
			layout.addComponent(readOnlyField);
		} else
		{
			IdentityEditor editor = identityEditorRegistry.getEditor(idParam.getIdentityType());
			identityParamEditors.put(index, editor);
			ComponentsContainer editorUI = editor.getEditor(IdentityEditorContext.builder()
					.withRequired(!idParam.isOptional())
					.withLabelInLine(form.getLayoutSettings().isCompactInputs())
					.withCustomWidth(formWidth())
					.withCustomWidthUnit(formWidthUnit())
					.build());
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
				editorUI.setLabel(idParam.getLabel());
			if (idParam.getDescription() != null)
				editorUI.setDescription(HtmlEscapers.htmlEscaper().escape(idParam.getDescription()));
		}
		return true;
	}
	
	
	protected boolean createAttributeControl(AbstractOrderedLayout layout, FormParameterElement element, 
			Map<Integer, PrefilledEntry<Attribute>> fromInvitation)
	{
		int index = element.getIndex();
		AttributeRegistrationParam aParam = form.getAttributeParams().get(index);
		Attribute rattr = remoteAttributes.get(RemoteDataRegistrationParser.getAttributeKey(aParam));
		PrefilledEntry<Attribute> prefilledEntry = fromInvitation.get(index);
		Attribute readOnlyAttribute = getReadOnlyAttribute(index, form.getAttributeParams(), fromInvitation);
		AttributeType aType = atTypes.get(aParam.getAttributeType());
		
		if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.HIDDEN)
			return false;
			
		if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden)
			return false;
		if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticOrInteractive && rattr != null)
			return false;
		
		CompositeLayoutAdapter layoutAdapter = new CompositeLayoutAdapter(layout);
		layoutAdapter.setOffset(layout.getComponentCount());
		if (readOnlyAttribute != null)
		{
			AttributeViewerContext context = AttributeViewerContext.builder()
					.withCustomWidth(formWidth())
					.withCustomWidthUnit(formWidthUnit())
					.withShowCaption(!(form.getLayoutSettings().isCompactInputs()))
					.build();
			AttributeViewer viewer = new AttributeViewer(msg, attributeHandlerRegistry, 
					aType, readOnlyAttribute, false, context);
			ComponentsGroup componentsGroup = viewer.getComponentsGroup();
			layoutAdapter.addContainer(componentsGroup);
		} else
		{
			String description = (aParam.getDescription() != null && !aParam.getDescription().isEmpty()) ? 
					aParam.getDescription() : null;
			String aName = isEmpty(aParam.getLabel()) ? null : aParam.getLabel();
			
			
			ConfirmationMode confirmationMode = ConfirmationMode.OFF;
			if (aParam.getConfirmationMode().equals(
					pl.edu.icm.unity.types.registration.ConfirmationMode.ON_SUBMIT))
				confirmationMode = ConfirmationMode.FORCE_CONFIRMED;
			else if (aParam.getConfirmationMode().equals(
					pl.edu.icm.unity.types.registration.ConfirmationMode.ON_ACCEPT))
				confirmationMode = ConfirmationMode.USER;
			
			AttributeEditContext editContext = AttributeEditContext.builder()
					.withConfirmationMode(confirmationMode).withRequired(!aParam.isOptional())
					.withAttributeType(aType)
					.withAttributeGroup(aParam.isUsingDynamicGroup() ? "/" : aParam.getGroup())
					.withLabelInline(form.getLayoutSettings().isCompactInputs())
					.withCustomWidth(formWidth())
					.withCustomWidthUnit(formWidthUnit())
					.build();

			FixedAttributeEditor editor = new FixedAttributeEditor(msg,
					attributeHandlerRegistry, editContext,
					aParam.isShowGroups(), aName, description);
			layoutAdapter.addContainer(editor.getComponentsGroup());
			
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
			Map<Integer, PrefilledEntry<GroupSelection>> prefillFromInvitation, Map<Integer, GroupSelection> allowedFromInvitation)
	{
		int index = element.getIndex();
		GroupRegistrationParam groupParam = form.getGroupParams().get(index);
		List<Group> allMatchingGroups = groupsMan.getGroupsByWildcard(groupParam.getGroupPath());
		List<Group> remotelySelected = GroupPatternMatcher.filterMatching(allMatchingGroups, 
				remotelyAuthenticated.getGroups());
		boolean hasRemoteGroup = !remotelySelected.isEmpty();
		PrefilledEntry<GroupSelection> prefilledEntry = prefillFromInvitation.get(index);
		
		if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.HIDDEN)
			return false;
		if (groupParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden)
			return false;
		
		boolean hasPrefilledROSelected = prefilledEntry != null && 
				prefilledEntry.getMode() == PrefilledEntryMode.READ_ONLY;
		boolean hasAutomaticRO = groupParam.getRetrievalSettings().isPotentiallyAutomaticAndVisible() 
				&& hasRemoteGroup; 

		GroupsSelection selection = GroupsSelection.getGroupsSelection(msg, groupParam.isMultiSelect(), 
			isGroupParamUsedAsMandatoryAttributeGroup(form, groupParam));
		selection.setCaption(isEmpty(groupParam.getLabel()) ? "" : groupParam.getLabel());
		selection.setWidth(formWidth(), formWidthUnit());
	
		if (hasPrefilledROSelected)
		{
			selection.setReadOnly(true);
			List<Group> prefilled = GroupPatternMatcher.filterByIncludeGroupsMode(
					GroupPatternMatcher.filterMatching(allMatchingGroups,
							prefilledEntry.getEntry().getSelectedGroups()),
					groupParam.getIncludeGroupsMode());
			
			if (!prefilled.isEmpty())
			{
				selection.setItems(prefilled);
				selection.setSelectedItems(prefilled);
				layout.addComponent(selection);
			}
		} else if (hasAutomaticRO)
		{
			List<Group> remotelySelectedFiltered = GroupPatternMatcher
					.filterByIncludeGroupsMode(remotelySelected, groupParam.getIncludeGroupsMode());
			if (!remotelySelectedFiltered.isEmpty())
			{
				selection.setReadOnly(true);
				selection.setItems(remotelySelectedFiltered);
				selection.setSelectedItems(remotelySelectedFiltered);
				layout.addComponent(selection);
			}

		} else
		{
			if (groupParam.getDescription() != null)
			{
				selection.setDescription(
						HtmlConfigurableLabel.conditionallyEscape(groupParam.getDescription()));
			}

			GroupSelection allowedGroupSel = allowedFromInvitation.get(index);
			List<Group> allowedGroup = allMatchingGroups;
			if (allowedGroupSel != null && !allowedGroupSel.getSelectedGroups().isEmpty())
			{
				allowedGroup = GroupPatternMatcher.filterMatching(allMatchingGroups,
						allowedFromInvitation.get(index).getSelectedGroups());
			}
			
			List<Group> allowedFilteredByMode = GroupPatternMatcher.filterByIncludeGroupsMode(allowedGroup, groupParam.getIncludeGroupsMode());
			selection.setItems(allowedFilteredByMode);
			
			if (groupParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive
					&& hasRemoteGroup)
			{
				List<Group> remotelySelectedLimited = GroupPatternMatcher.filterMatching(allowedFilteredByMode,
						remotelySelected.stream().map(g -> g.getName()).collect(Collectors.toList()));
				selection.setSelectedItems(remotelySelectedLimited);
			}
			
			if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.DEFAULT)
			{

				selection.setSelectedItems(GroupPatternMatcher.filterMatching(allowedFilteredByMode,
						prefilledEntry.getEntry().getSelectedGroups()));

			}
			
			groupSelectors.put(index, selection);
			if (!allowedFilteredByMode.isEmpty())
			{
				layout.addComponent(selection);
				return false;
			}
		}
		
		return true;
	}
	
	protected boolean createCredentialControl(AbstractOrderedLayout layout, FormParameterElement element)
	{
		int index = element.getIndex();
		CredentialRegistrationParam param = form.getCredentialParams().get(index);
		CredentialDefinition credDefinition = credentials.get(param.getCredentialName());
		CredentialEditor editor = credentialEditorRegistry.getEditor(credDefinition.getTypeId());
		ComponentsContainer editorUI = editor.getEditor(CredentialEditorContext.builder()
				.withConfiguration(credDefinition.getConfiguration())
				.withRequired(true)
				.withShowLabelInline(form.getLayoutSettings().isCompactInputs())
				.withCustomWidth(formWidth())
				.withCustomWidthUnit(formWidthUnit())
				.build());
		if (param.getLabel() != null)
			editorUI.setLabel(param.getLabel());
		else
			editorUI.setLabel(credDefinition.getDisplayedName().getValue(msg));
		if (param.getDescription() != null)
			editorUI.setDescription(HtmlConfigurableLabel.conditionallyEscape(param.getDescription()));
		else if (!credDefinition.getDescription().isEmpty())
			editorUI.setDescription(HtmlConfigurableLabel.conditionallyEscape(
					credDefinition.getDescription().getValue(msg)));
		credentialParamEditors.add(editor);
		layout.addComponents(editorUI.getComponents());
		return true;
	}
	
	
	protected Attribute getReadOnlyAttribute(int i, List<AttributeRegistrationParam> attributeParams,
			Map<Integer, PrefilledEntry<Attribute>> fromInvitation)
	{
		AttributeRegistrationParam aParam = attributeParams.get(i);
		PrefilledEntry<Attribute> prefilledEntry = fromInvitation.get(i);
		if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.READ_ONLY)
			return prefilledEntry.getEntry();
		if (!aParam.getRetrievalSettings().isPotentiallyAutomaticAndVisible())
			return null;
		return remoteAttributes.get(RemoteDataRegistrationParser.getAttributeKey(aParam));
	}
	
	public boolean isUserInteractionRequired()
	{
		return !identityParamEditors.isEmpty()
				|| !attributeEditor.isEmpty()
				|| !(groupSelectors.values().stream().filter(a -> a != null && !a.getItems().isEmpty())
				.count() == 0)
				|| !agreementSelectors.isEmpty()
				|| !credentialParamEditors.isEmpty()
				|| form.isCollectComments();
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
		public String errorMsg = null;
	}
	
	public Float formWidth()
	{
		return form.getLayoutSettings().getColumnWidth();
	}
	
	public Unit formWidthUnit()
	{
		return Unit.getUnitFromSymbol(form.getLayoutSettings().getColumnWidthUnit());
	}
}


