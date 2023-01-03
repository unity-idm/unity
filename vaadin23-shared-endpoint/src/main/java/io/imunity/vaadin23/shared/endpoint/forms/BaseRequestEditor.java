/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.forms;

import com.google.common.html.HtmlEscapers;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.components.CheckboxWithError;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import io.imunity.vaadin23.shared.endpoint.components.ReadOnlyField;
import io.imunity.vaadin23.shared.endpoint.components.RegistrationLayoutsContainer;
import io.imunity.vaadin23.shared.endpoint.forms.groups.GroupMultiComboBox;
import io.imunity.vaadin23.shared.endpoint.forms.groups.GroupTreeNode;
import io.imunity.vaadin23.shared.endpoint.forms.policy_agreements.PolicyAgreementRepresentation;
import io.imunity.vaadin23.shared.endpoint.forms.policy_agreements.PolicyAgreementRepresentationBuilderV23;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.*;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorContext;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorRegistryV23;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditorContext;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditorRegistryV23;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.api.utils.FreemarkerUtils;
import pl.edu.icm.unity.exceptions.*;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException.Category;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.*;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.types.registration.*;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.layout.*;
import pl.edu.icm.unity.webui.common.ComponentWithLabel;
import pl.edu.icm.unity.webui.common.ConfirmationEditMode;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;
import pl.edu.icm.unity.webui.forms.PrefilledSet;

import java.util.*;
import java.util.stream.Collectors;

import static io.imunity.vaadin23.shared.endpoint.forms.FormParser.isGroupParamUsedAsMandatoryAttributeGroup;


public abstract class BaseRequestEditor<T extends BaseRegistrationInput> extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, BaseRequestEditor.class);
	protected MessageSource msg;
	protected NotificationPresenter notificationPresenter;
	private BaseForm form;
	protected RemotelyAuthenticatedPrincipal remotelyAuthenticated;
	private IdentityEditorRegistryV23 identityEditorRegistry;
	private CredentialEditorRegistryV23 credentialEditorRegistry;
	private AttributeHandlerRegistryV23 attributeHandlerRegistry;
	private AttributeTypeManagement aTypeMan;
	private GroupsManagement groupsMan;
	private CredentialManagement credMan;
	private PolicyAgreementRepresentationBuilderV23 policyAgreementsRepresentationBuilder;
	
	private Map<String, IdentityTaV> remoteIdentitiesByType;
	private Map<String, Attribute> remoteAttributes;
	private Map<Integer, IdentityEditor> identityParamEditors;
	private List<CredentialEditor> credentialParamEditors;
	private Map<Integer, FixedAttributeEditor> attributeEditor;
	private Map<Integer, GroupMultiComboBox> groupSelectors;
	private List<CheckboxWithError> agreementSelectors;
	private List<PolicyAgreementRepresentation> policyAgreementSelectors;
	private TextArea comment;
	private Map<String, AttributeType> atTypes;
	private Map<String, CredentialDefinition> credentials;
	private PrefilledSet prefilled;

	/**
	 * Note - the two managers must be insecure, if the form is used in not-authenticated context, 
	 * what is possible for registration form.
	 */
	public BaseRequestEditor(MessageSource msg, BaseForm form,
	                         RemotelyAuthenticatedPrincipal remotelyAuthenticated,
	                         IdentityEditorRegistryV23 identityEditorRegistry,
	                         CredentialEditorRegistryV23 credentialEditorRegistry,
	                         AttributeHandlerRegistryV23 attributeHandlerRegistry,
	                         AttributeTypeManagement atMan, CredentialManagement credMan,
	                         GroupsManagement groupsMan, NotificationPresenter notificationPresenter,
	                         PolicyAgreementRepresentationBuilderV23 policyAgreementsRepresentationBuilder)
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
		this.notificationPresenter = notificationPresenter;
		this.policyAgreementsRepresentationBuilder = policyAgreementsRepresentationBuilder;
		
		this.remoteAttributes = RemoteDataRegistrationParser.parseRemoteAttributes(form, remotelyAuthenticated);
		this.remoteIdentitiesByType = RemoteDataRegistrationParser.parseRemoteIdentities(
				form, remotelyAuthenticated);
	}
	
	protected void validateMandatoryRemoteInput() throws AuthenticationException
	{
		RemoteDataRegistrationParser.assertMandatoryRemoteAttributesArePresent(form, remoteAttributes);
		RemoteDataRegistrationParser.assertMandatoryRemoteIdentitiesArePresent(form, remoteIdentitiesByType);
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
				notificationPresenter.showError(e.getMessage(), "");
			return Optional.empty();
		} catch (Exception e) 
		{
			notificationPresenter.showError(msg.getMessage("Generic.formError"), e.getMessage());
			return Optional.empty();
		}
	}

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
		setRequestPolicyAgreements(ret, status);
		
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
				if (editor == null) //was pre-filled in a way we don't have editor
				{
					ip = prefilled.identities.get(i).getEntry();
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
					if (ae == null)	//was pre-filled in a way we don't have editor
					{
						attr = prefilled.attributes.get(i).getEntry();
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
					GroupMultiComboBox selector = groupSelectors.get(i);
					if (selector == null)	//ok, group specified by invitation
						g.add(null);
					else
						g.add(new GroupSelection(selector.getSelectedGroupsWithoutParents()));
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
				CheckboxWithError cb = agreementSelectors.get(i);
				a.add(new Selection(cb.getValue()));
				if (form.getAgreements().get(i).isManatory() && !cb.getValue())
					cb.setErrorMessage(msg.getMessage("selectionRequired"));
				else
					cb.setErrorMessage(null);
			}
			ret.setAgreements(a);
		}
	}

	private void setRequestPolicyAgreements(BaseRegistrationInput ret, FormErrorStatus status)
	{
		if (policyAgreementSelectors != null)
		{
			List<PolicyAgreementDecision> a = new ArrayList<>();
			for (PolicyAgreementRepresentation ar : policyAgreementSelectors)
			{
				if (ar == null)
				{
					a.add(null);
					continue;
				}


				if (!ar.isValid())
				{
					ar.setErrorMessage(msg.getMessage("selectionRequired"));
				}else
				{
					ar.setErrorMessage(null);
				}
				a.add(ar.getDecision());
			}
			ret.setPolicyAgreements(a);
		}
	}
	
	/**
	 * Creates main layout, inserts title and form information
	 */
	protected RegistrationLayoutsContainer createLayouts(Map<String, Object> params)
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		main.setWidthFull();
		add(main);
		
		addLogo(main);
		
		Label formName = new Label(processFreeemarkerTemplate(params, form.getDisplayedName().getValue(msg)));
		formName.addClassName("u-reg-title");
		main.add(formName);
		main.setAlignItems(Alignment.CENTER);
		
		String info = form.getFormInformation() == null ? null : processFreeemarkerTemplate(params, form.getFormInformation().getValue(msg));
		if (info != null)
		{
			Html formInformation = new Html(info);
			main.add(formInformation);
		}
		
		RegistrationLayoutsContainer container = new RegistrationLayoutsContainer(formWidth(), formWidthUnit());
		container.addFormLayoutToRootLayout(main);
		return container;
	}

	protected String processFreeemarkerTemplate(Map<String, Object> params, String template) 
	{
		return FreemarkerUtils.processStringTemplate(
				params != null ? params : Collections.emptyMap(), template);
	}
	
	private void addLogo(VerticalLayout main)
	{
		String logoURL = form.getLayoutSettings().getLogoURL();

		Image image = new Image(logoURL, "");
		main.add(image);
	}
	
	protected void createControls(RegistrationLayoutsContainer layoutContainer, FormLayout formLayout, PrefilledSet prefilled)
	{
		this.prefilled = prefilled;
		identityParamEditors = new HashMap<>();
		attributeEditor = new HashMap<>();
		atTypes = getAttributeTypesMap();
		agreementSelectors = new ArrayList<>();
		policyAgreementSelectors  = new ArrayList<>();
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
		Iterator<Component> iterator = container.getChildren().iterator();
		while(iterator.hasNext())
		{
			Component next = iterator.next();
			if (next.isVisible() && next instanceof Focusable)
			{
				((Focusable<?>)next).focus();
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
		case POLICY_AGREEMENT:
			return createPolicyAgreementControl(layoutContainer.registrationFormLayout, (FormParameterElement) element);	
		case COMMENTS:
			return createCommentsControl(layoutContainer.registrationFormLayout, (BasicFormElement) element);
			
		case CREDENTIAL:
			return createCredentialControl(layoutContainer.registrationFormLayout, (FormParameterElement) element);
		default:
			log.error("Unsupported form element, skipping: " + element);
		}
		return false;
	}

	protected boolean createLabelControl(VerticalLayout layout, FormElement previousInserted,
			FormElement next, FormCaptionElement element)
	{
		if (previousInserted == null || next == null)
			return false;
		if (previousInserted instanceof FormCaptionElement)
			return false;
		Label label = new Label(element.getValue().getValue(msg));
		label.addClassName("u-reg-sectionHeader");
		layout.add(label);
		layout.setAlignItems(FlexComponent.Alignment.CENTER);
		return true;
	}	

	private void removePreviousIfSection(VerticalLayout layout, FormElement previousInserted)
	{
		if (previousInserted instanceof FormCaptionElement)
		{
			Component lastComponent = layout.getComponentAt(layout.getComponentCount() - 1);
			layout.remove(lastComponent);
		}
	}

	protected boolean createSeparatorControl(VerticalLayout layout, FormSeparatorElement element)
	{
		Hr horizontalLine = new Hr();
		horizontalLine.setWidth(formWidth(), formWidthUnit());
		layout.add(horizontalLine);
		return true;
	}	
	
	protected boolean createAgreementControl(VerticalLayout layout, FormParameterElement element)
	{
		VerticalLayout container = new VerticalLayout();
		container.setPadding(false);
		container.setMargin(false);
		container.getStyle().set("gap", "0");

		AgreementRegistrationParam aParam = form.getAgreements().get(element.getIndex());
		Html aText = new Html("<div>" + aParam.getText().getValue(msg) + "</div>");
		aText.getElement().getStyle().set("width", formWidth() + formWidthUnit().getSymbol());
		CheckboxWithError cb = new CheckboxWithError(msg.getMessage("RegistrationRequest.agree"));
		agreementSelectors.add(cb);
		container.add(aText);
		container.add(cb);
		if (aParam.isManatory())
		{
			Label mandatory = new Label(msg.getMessage("RegistrationRequest.mandatoryAgreement"));
			container.add(mandatory);
		}
		layout.add(container);
		return true;
	}
	
	private boolean createPolicyAgreementControl(VerticalLayout layout, FormParameterElement element)
	{
		PolicyAgreementConfiguration aParam = form.getPolicyAgreements().get(element.getIndex());
		if (isPolicyAgreementsIsFiltered(aParam))
		{
			policyAgreementSelectors.add(null);
			return true;
		}
		PolicyAgreementRepresentation ar = policyAgreementsRepresentationBuilder.getAgreementRepresentation(aParam);
		policyAgreementSelectors.add(ar);
		layout.add(ar);
		return true;	
	}
	
	protected abstract boolean isPolicyAgreementsIsFiltered(PolicyAgreementConfiguration toCheck);
	
	
	protected boolean createCommentsControl(VerticalLayout layout, BasicFormElement element)
	{
		comment = new TextArea();
		comment.setWidth(formWidth(), formWidthUnit());
		String label = ComponentWithLabel.normalizeLabel(msg.getMessage("RegistrationRequest.comment"));
		if (form.getLayoutSettings().isCompactInputs())
			comment.setPlaceholder(label);
		else
			comment.setLabel(label);
		layout.add(comment);
		return true;
	}
	
	protected boolean createIdentityControl(VerticalLayout layout, FormParameterElement element,
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
			layout.add(readOnlyField);
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
			layout.add(readOnlyField);
		} else
		{
			IdentityEditor editor = identityEditorRegistry.getEditor(idParam.getIdentityType());
			identityParamEditors.put(index, editor);
			ComponentsContainer editorUI = editor.getEditor(IdentityEditorContext.builder()
					.withRequired(!idParam.isOptional())
					.withLabelInLine(form.getLayoutSettings().isCompactInputs())
					.withCustomWidth(formWidth())
					.withCustomWidthUnit(formWidthUnit())
					.withConfirmationEditMode(registrationConfirmModeToConfirmationEditMode(idParam.getConfirmationMode()))
					.build());
			layout.add(editorUI.getComponents());
			
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
	
	
	protected boolean createAttributeControl(VerticalLayout layout, FormParameterElement element,
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
			
			
			ConfirmationEditMode confirmationMode = 
					registrationConfirmModeToConfirmationEditMode(aParam.getConfirmationMode());
			
			AttributeEditContext editContext = AttributeEditContext.builder()
					.withConfirmationMode(confirmationMode)
					.withRequired(!aParam.isOptional())
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
	
	private ConfirmationEditMode registrationConfirmModeToConfirmationEditMode(ConfirmationMode registrationMode)
	{
		if (registrationMode == ConfirmationMode.ON_SUBMIT)
			return ConfirmationEditMode.FORCE_CONFIRMED_IF_SYNC;
		return ConfirmationEditMode.OFF;
	}
	
	public BaseForm getForm()
	{
		return form;
	}

	protected boolean createGroupControl(VerticalLayout layout, FormParameterElement element,
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

		GroupMultiComboBox groupMultiComboBox = new GroupMultiComboBox(msg);
		groupMultiComboBox.setWidthFull();
		groupMultiComboBox.setLabel(groupParam.getLabel());
		groupMultiComboBox.setRequired(isGroupParamUsedAsMandatoryAttributeGroup(form, groupParam));

		if (hasPrefilledROSelected)
		{
			groupMultiComboBox.setReadOnly(true);
			List<Group> prefilled = GroupPatternMatcher.filterByIncludeGroupsMode(
					GroupPatternMatcher.filterMatching(allMatchingGroups,
							prefilledEntry.getEntry().getSelectedGroups()),
					groupParam.getIncludeGroupsMode());

			if (!prefilled.isEmpty())
			{
				List<GroupTreeNode> groupTreeNodes = getGroupTreeNode(prefilled).getAllOffspring();
				groupMultiComboBox.setItems(groupTreeNodes);
				groupMultiComboBox.select(groupTreeNodes);
				layout.add(groupMultiComboBox);
			}
		} else if (hasAutomaticRO)
		{
			List<Group> remotelySelectedFiltered = GroupPatternMatcher
					.filterByIncludeGroupsMode(remotelySelected, groupParam.getIncludeGroupsMode());
			if (!remotelySelectedFiltered.isEmpty())
			{
				groupMultiComboBox.setReadOnly(true);
				List<GroupTreeNode> allOffspring = getGroupTreeNode(remotelySelectedFiltered).getAllOffspring();
				groupMultiComboBox.setItems(allOffspring);
				groupMultiComboBox.select(allOffspring);
				layout.add(groupMultiComboBox);
			}

		} else
		{
			if (groupParam.getDescription() != null)
			{
				groupMultiComboBox.getElement().setProperty("title", HtmlConfigurableLabel.conditionallyEscape(groupParam.getDescription()));
			}

			GroupSelection allowedGroupSel = allowedFromInvitation.get(index);
			List<Group> allowedGroup = allMatchingGroups;
			if (allowedGroupSel != null && !allowedGroupSel.getSelectedGroups().isEmpty())
			{
				allowedGroup = GroupPatternMatcher.filterMatching(allMatchingGroups,
						allowedFromInvitation.get(index).getSelectedGroups());
			}
			
			List<Group> allowedFilteredByMode = GroupPatternMatcher.filterByIncludeGroupsMode(allowedGroup, groupParam.getIncludeGroupsMode());
			GroupTreeNode groupTreeNode = getGroupTreeNode(allowedFilteredByMode);
			groupMultiComboBox.setItems(groupTreeNode.getAllOffspring());

			if (groupParam.getRetrievalSettings() == ParameterRetrievalSettings.automaticAndInteractive
					&& hasRemoteGroup)
			{
				List<Group> remotelySelectedLimited = GroupPatternMatcher.filterMatching(allowedFilteredByMode,
						remotelySelected.stream().map(Group::getName).collect(Collectors.toList()));
				List<GroupTreeNode> allOffspring = getGroupTreeNode(remotelySelectedLimited).getAllOffspring();
				groupMultiComboBox.select(allOffspring);
			}
			
			if (prefilledEntry != null && prefilledEntry.getMode() == PrefilledEntryMode.DEFAULT)
			{
				List<GroupTreeNode> allOffspring = getGroupTreeNode(GroupPatternMatcher.filterMatching(allowedFilteredByMode,
						prefilledEntry.getEntry().getSelectedGroups())).getAllOffspring();
				groupMultiComboBox.select(allOffspring);
			}
			
			groupSelectors.put(index, groupMultiComboBox);
			if (!allowedFilteredByMode.isEmpty())
			{
				layout.add(groupMultiComboBox);
				return false;
			}
		}
		
		return true;
	}

	private GroupTreeNode getGroupTreeNode(List<Group> allowedFilteredByMode)
	{
		GroupTreeNode groupTreeNode = new GroupTreeNode(new Group("/"), 0, msg);
		allowedFilteredByMode
				.stream()
				.sorted(Comparator.comparing(Group::getPathEncoded))
				.forEach(groupTreeNode::addChild);
		return groupTreeNode;
	}

	protected boolean createCredentialControl(VerticalLayout layout, FormParameterElement element)
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
				.withCredentialName(param.getCredentialName())
				.build());
		if (param.getLabel() != null)
			editorUI.setLabel(param.getLabel());
		else 
		{
			I18nString displayedName = credDefinition.getDisplayedName();
			if (displayedName.hasNonDefaultValue())
				editorUI.setLabel(displayedName.getValue(msg));
		}
		if (param.getDescription() != null)
			editorUI.setDescription(HtmlConfigurableLabel.conditionallyEscape(param.getDescription()));
		credentialParamEditors.add(editor);
		layout.add(editorUI.getComponents());
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
				|| !(groupSelectors.values().stream().filter(a -> a != null && !a.getValue().isEmpty())
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


