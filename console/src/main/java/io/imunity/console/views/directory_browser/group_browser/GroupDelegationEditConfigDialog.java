/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser.group_browser;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.console.views.signup_and_enquiry.*;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import org.springframework.beans.factory.ObjectFactory;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;
import static io.imunity.vaadin.elements.CssClassNames.DISABLED_ICON;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;


class GroupDelegationEditConfigDialog extends DialogWithActionFooter
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final Consumer<GroupDelegationConfiguration> callback;
	private final GroupDelegationConfiguration toEdit;
	private final Group group;

	private TextField logoUrl;
	private Checkbox enableSubprojects;
	private FormComboWithButtons registrationFormComboWithButtons;
	private FormComboWithButtons signupEnquiryFormComboWithButtons;
	private FormComboWithButtons membershipUpdateEnquiryFormComboWithButtons;
	private MultiSelectComboBox<String> attributes;
	private MultiSelectComboBox<PolicyDocumentWithRevision> policyDocuments;

	private Binder<DelegationConfiguration> binder;

	private final RegistrationsManagement registrationMan;
	private final EnquiryManagement enquiryMan;
	private final AttributeTypeManagement attrTypeMan;
	private final ObjectFactory<RegistrationFormEditor> regFormEditorFactory;
	private final ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory;
	private final EventsBus bus;
	private final GroupDelegationConfigGenerator configGenerator;
	private final PolicyDocumentManagement policyDocumentManagement;


	GroupDelegationEditConfigDialog(MessageSource msg, RegistrationsManagement registrationMan,
			EnquiryManagement enquiryMan, AttributeTypeManagement attrTypeMan, PolicyDocumentManagement policyDocumentManagement,
			ObjectFactory<RegistrationFormEditor> regFormEditorFactory,
			ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory, EventsBus bus,
			GroupDelegationConfigGenerator configGenerator, Group group,
			Consumer<GroupDelegationConfiguration> callback, NotificationPresenter notificationPresenter)
	{
		super(msg::getMessage);
		this.msg = msg;
		this.toEdit = group.getDelegationConfiguration();
		this.callback = callback;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
		this.attrTypeMan = attrTypeMan;
		this.policyDocumentManagement = policyDocumentManagement;
		this.regFormEditorFactory = regFormEditorFactory;
		this.enquiryFormEditorFactory = enquiryFormEditorFactory;
		this.bus = bus;
		this.configGenerator = configGenerator;
		this.group = group;
		this.notificationPresenter = notificationPresenter;

		setHeaderTitle(msg.getMessage("GroupDelegationEditConfigDialog.caption"));
		setWidth("50em");
		setActionButton(msg.getMessage("ok"), this::onConfirm);
		add(getContents());
	}

	private void enableEdit(boolean enabled)
	{
		logoUrl.setEnabled(enabled);
		registrationFormComboWithButtons.setEnabled(enabled);
		signupEnquiryFormComboWithButtons.setEnabled(enabled);
		membershipUpdateEnquiryFormComboWithButtons.setEnabled(enabled);
		attributes.setEnabled(enabled);
		enableSubprojects.setEnabled(enabled);
		policyDocuments.setEnabled(enabled);
	}

	private Component getContents()
	{

		Checkbox enableDelegation = new Checkbox(
				msg.getMessage("GroupDelegationEditConfigDialog.enableDelegationCaption"));
		enableDelegation.addValueChangeListener(e -> enableEdit(e.getValue()));
		
		enableSubprojects = new Checkbox(
				msg.getMessage("GroupDelegationEditConfigDialog.enableSubprojectsCaption"));
		
		logoUrl = new TextField();
		logoUrl.setWidth(100, Unit.PERCENTAGE);

		registrationFormComboWithButtons = new FormComboWithButtons(msg,
				"",
				msg.getMessage("GroupDelegationEditConfigDialog.registrationFormDesc"),	
				e -> generateJoinRegistrationForm(),
				e -> showJoinRegistrationValidation(registrationFormComboWithButtons.getValue()),
				e -> showRegFormEditDialog(registrationFormComboWithButtons.getValue()));
		reloadRegistrationForm();

		signupEnquiryFormComboWithButtons = new FormComboWithButtons(msg,
				"",
				msg.getMessage("GroupDelegationEditConfigDialog.signupEnquiryDesc"),
				e -> generateJoinEnquiryForm(),
				e -> showJoinEnquiryValidation(signupEnquiryFormComboWithButtons.getValue()),
				e -> showEnquiryFormEditDialog(signupEnquiryFormComboWithButtons.getValue()));

		membershipUpdateEnquiryFormComboWithButtons = new FormComboWithButtons(msg,
				"",
				msg.getMessage("GroupDelegationEditConfigDialog.membershipUpdateEnquiryDesc"),
				e -> generateUpdateEnquiryForm(),
				e -> showUpdateEnquiryValidation(
						membershipUpdateEnquiryFormComboWithButtons.getValue()),
				e -> showEnquiryFormEditDialog(membershipUpdateEnquiryFormComboWithButtons.getValue()));
		reloadEnquiryForms();

		attributes = new MultiSelectComboBox<>();
		attributes.setWidthFull();

		Collection<AttributeType> attributeTypes;
		try
		{
			attributeTypes = attrTypeMan.getAttributeTypes();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			throw new RuntimeException(e);
		}
		attributes.setItems(attributeTypes.stream().map(AttributeType::getName).sorted().collect(Collectors.toList()));
		if (toEdit.attributes != null)
		{
			attributes.setValue(new ArrayList<>(toEdit.attributes));
		}
		
		policyDocuments = new MultiSelectComboBox<>();
		policyDocuments.setWidthFull();
		policyDocuments.setItemLabelGenerator(p -> p.name);
	
		Collection<PolicyDocumentWithRevision> policyDocs;
		try
		{
			policyDocs = policyDocumentManagement.getPolicyDocuments();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			throw new RuntimeException(e);
		}

		policyDocuments.setItems(policyDocs);

		if (toEdit.policyDocumentsIds != null)
		{
			policyDocuments.setValue(policyDocs.stream()
					.filter(d -> toEdit.policyDocumentsIds.contains(d.id)).collect(Collectors.toList()));
		}

		binder = new Binder<>(DelegationConfiguration.class);
		binder.forField(enableDelegation).bind("enabled");
		binder.forField(enableSubprojects).bind("enableSubprojects");
		binder.forField(logoUrl).bind("logoUrl");
		binder.forField(registrationFormComboWithButtons).bind("registrationForm");
		binder.forField(membershipUpdateEnquiryFormComboWithButtons).bind("membershipUpdateEnquiryForm");
		binder.forField(signupEnquiryFormComboWithButtons).bind("signupEnquiryForm");
		binder.setBean(new DelegationConfiguration(toEdit));
		enableEdit(toEdit.enabled);

		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		main.addFormItem(enableDelegation, "");
		main.addFormItem(logoUrl, msg.getMessage("GroupDelegationEditConfigDialog.logoUrlCaption"));
		main.addFormItem(enableSubprojects, "");
		main.addFormItem(attributes, msg.getMessage("GroupDelegationEditConfigDialog.attributes"));
		main.addFormItem(policyDocuments, msg.getMessage("GroupDelegationEditConfigDialog.policyDocuments"));

		main.addFormItem(registrationFormComboWithButtons, msg.getMessage("GroupDelegationEditConfigDialog.registrationForm"));
		main.addFormItem(signupEnquiryFormComboWithButtons, msg.getMessage("GroupDelegationEditConfigDialog.signupEnquiry"));
		main.addFormItem(membershipUpdateEnquiryFormComboWithButtons, msg.getMessage("GroupDelegationEditConfigDialog.membershipUpdateEnquiry"));
		return main;
	}

	private void reloadEnquiryForms()
	{
		List<EnquiryForm> forms;
		try
		{
			forms = enquiryMan.getEnquires();
			signupEnquiryFormComboWithButtons
					.setItems(forms.stream().map(DescribedObjectROImpl::getName).collect(Collectors.toList()));
			membershipUpdateEnquiryFormComboWithButtons
					.setItems(forms.stream().filter(f -> f.getType().equals(EnquiryType.STICKY))
							.map(DescribedObjectROImpl::getName).collect(Collectors.toList()));
		} catch (EngineException e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupDelegationEditConfigDialog.cannotLoadForms"), e.getMessage());
		}
	}

	private void generateJoinEnquiryForm()
	{
		EnquiryForm form;
		try
		{
			form = configGenerator.generateProjectJoinEnquiryForm(group.toString(), logoUrl.getValue(),
					policyDocuments.getSelectedItems()
							.stream()
							.map(p -> p.id)
							.collect(Collectors.toList()));
			enquiryMan.addEnquiry(form);

		} catch (EngineException e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupDelegationEditConfigDialog.cannotGenerateForm"), e.getMessage());
			return;
		}
		reloadEnquiryForms();
		signupEnquiryFormComboWithButtons.setValue(form.getName());
	}

	private void generateUpdateEnquiryForm()
	{
		EnquiryForm form;
		try
		{
			form = configGenerator.generateProjectUpdateEnquiryForm(group.toString(), logoUrl.getValue());
			enquiryMan.addEnquiry(form);

		} catch (EngineException e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupDelegationEditConfigDialog.cannotGenerateForm"), e.getMessage());
			return;
		}
		reloadEnquiryForms();
		membershipUpdateEnquiryFormComboWithButtons.setValue(form.getName());
	}

	private void reloadRegistrationForm()
	{
		List<RegistrationForm> forms;
		try
		{
			forms = registrationMan.getForms();
			registrationFormComboWithButtons
					.setItems(forms.stream().map(DescribedObjectROImpl::getName).collect(Collectors.toList()));
		} catch (EngineException e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupDelegationEditConfigDialog.cannotLoadForms"), e.getMessage());
		}

	}

	private void generateJoinRegistrationForm()
	{
		RegistrationForm form;
		try
		{
			form = configGenerator.generateProjectRegistrationForm(group.toString(), logoUrl.getValue(),
					new ArrayList<>(attributes.getSelectedItems()), policyDocuments.getSelectedItems()
							.stream()
							.map(p -> p.id)
							.collect(Collectors.toList()));
			registrationMan.addForm(form);

		} catch (EngineException e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupDelegationEditConfigDialog.cannotGenerateForm"), e.getMessage());
			return;
		}
		reloadRegistrationForm();
		registrationFormComboWithButtons.setValue(form.getName());
	}

	private void onConfirm()
	{
		try
		{
			DelegationConfiguration groupDelConfig = binder.getBean();
			GroupDelegationConfiguration config = new GroupDelegationConfiguration(
					groupDelConfig.isEnabled(),  groupDelConfig.isEnableSubprojects(), groupDelConfig.getLogoUrl(),
					groupDelConfig.getRegistrationForm(), groupDelConfig.getSignupEnquiryForm(),
					groupDelConfig.getMembershipUpdateEnquiryForm(), new ArrayList<>(attributes.getSelectedItems()), 
					policyDocuments.getSelectedItems()
					.stream()
					.map(p -> p.id)
					.collect(Collectors.toList()));

			Map<String, FormWithType> formsToSynch = getFormsToSynch(groupDelConfig.getRegistrationForm(),
					groupDelConfig.getSignupEnquiryForm(), policyDocuments.getSelectedItems()
							.stream()
							.map(p -> p.id)
							.collect(Collectors.toSet()));
			if (formsToSynch.size() > 0)
				getSynchronizationDialog(formsToSynch, () ->
				{
					callback.accept(config);
					close();
				}).open();
			else {
				callback.accept(config);
				close();
			}
		} catch (Exception e)
		{
			notificationPresenter.showError( msg.getMessage("GroupDelegationEditConfigDialog.cannotUpdate"),
					e.getMessage());
		}
	}
	
	private ConfirmDialog getSynchronizationDialog(Map<String, FormWithType> formsToSynch, Runnable close)
	{
		 ConfirmDialog dialog = new ConfirmDialog(msg.getMessage("GroupDelegationEditConfigDialog.synchronizePolicyDialogTitle"),
				"", msg.getMessage("yes"), e ->
				{
					try
					{
						for (Entry<String, FormWithType> form : formsToSynch.entrySet())
						{
							configGenerator.resetFormsPolicies(form.getKey(), form.getValue().type,
									policyDocuments.getSelectedItems()
											.stream()
											.map(p -> p.id)
											.collect(Collectors.toList()));
						}

					} catch (EngineException ex)
					{
						notificationPresenter.showError(
								msg.getMessage("GroupDelegationEditConfigDialog.errorUpdateForm"), ex.getMessage());
					}
					close.run();
				}, msg.getMessage("no"), e -> close.run());
			Html label = new Html("<div>"
					+ msg.getMessage("GroupDelegationEditConfigDialog.synchronizePolicy",
							String.join(" " + msg.getMessage("and") + " ", formsToSynch.values()
									.stream()
									.map(f -> "<b>" + f.label + "</b>")
									.collect(Collectors.toList())))
					+ "</div>");
			dialog.setText(label);
		 return dialog;
		
		
	}

	private Map<String, FormWithType> getFormsToSynch(String registrationFormName, String enquiryFormName,
			Set<Long> policyDocumentsIds) throws EngineException {

		Map<String, FormWithType> ret = new HashMap<>();
		if (registrationFormName != null)
		{
			RegistrationForm registrationForm = registrationMan.getForm(registrationFormName);
			if (!policyDocumentsIds.equals(registrationForm.getPolicyAgreements()
					.stream()
					.map(p -> p.documentsIdsToAccept)
					.flatMap(Collection::stream)
					.collect(Collectors.toSet())))
			{
				ret.put(registrationFormName, new FormWithType(FormType.REGISTRATION,
						msg.getMessage("GroupDelegationEditConfigDialog.registrationFormLabel")));
			}
		}
		if (enquiryFormName != null)
		{
			EnquiryForm enquiryForm = enquiryMan.getEnquiry(enquiryFormName);
			if (!policyDocumentsIds.equals(enquiryForm.getPolicyAgreements()
					.stream()
					.map(p -> p.documentsIdsToAccept)
					.flatMap(Collection::stream)
					.collect(Collectors.toSet())))
			{
				ret.put(enquiryFormName, new FormWithType(FormType.ENQUIRY,
						msg.getMessage("GroupDelegationEditConfigDialog.signupEnquiryLabel")));
			}
		}

		return ret;
	}

	private void showJoinRegistrationValidation(String formName)
	{
		List<String> messages = configGenerator.validateRegistrationForm(group.toString(), formName,
				policyDocuments.getSelectedItems()
						.stream()
						.map(p -> p.id)
						.collect(Collectors.toSet()));
		new ValidationResultDialog(msg, messages, formName).open();
	}

	private void showJoinEnquiryValidation(String formName)
	{
		List<String> messages = configGenerator.validateJoinEnquiryForm(group.toString(), formName,
				policyDocuments.getSelectedItems()
						.stream()
						.map(p -> p.id)
						.collect(Collectors.toSet()));
		new ValidationResultDialog(msg, messages, formName).open();
	}

	private void showUpdateEnquiryValidation(String formName)
	{
		List<String> messages = configGenerator.validateUpdateEnquiryForm(group.toString(), formName);
		new ValidationResultDialog(msg, messages, formName).open();
	}

	private void showRegFormEditDialog(String target)
	{

		RegistrationForm form;
		try
		{
			form = registrationMan.getForm(target);
		} catch (EngineException e)
		{
			notificationPresenter.showError( msg.getMessage("GroupDelegationEditConfigDialog.errorGetForm"),
					e.getMessage());
			return;
		}

		showEditRegFormEditDialog(form,
				msg.getMessage("GroupDelegationEditConfigDialog.editRegistraionFormAction"));
	}

	private void showEditRegFormEditDialog(RegistrationForm target, String caption)
	{
		RegistrationForm deepCopy = new RegistrationForm(target.toJson());
		RegistrationFormEditor editor;
		try
		{
			editor = regFormEditorFactory.getObject().init(false);
			editor.setForm(deepCopy);
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupDelegationEditConfigDialog.errorInFormEdit"), e.getMessage());
			return;
		}
		RegistrationFormEditDialog dialog = new RegistrationFormEditDialog(msg, caption, this::updateRegistrationForm, editor, notificationPresenter);
		dialog.open();
	}

	private boolean updateRegistrationForm(RegistrationForm updatedForm, boolean ignoreRequestsAndInvitations)
	{
		try
		{
			registrationMan.updateForm(updatedForm, ignoreRequestsAndInvitations);
			bus.fireEvent(new RegistrationFormChangedEvent(updatedForm));
			return true;
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupDelegationEditConfigDialog.errorUpdateForm"), e.getMessage());
			return false;
		}
	}

	private void showEnquiryFormEditDialog(String target)
	{
		EnquiryForm form;
		try
		{
			form = enquiryMan.getEnquiry(target);
		} catch (EngineException e)
		{
			notificationPresenter.showError( msg.getMessage("GroupDelegationEditConfigDialog.errorGetForm"),
					e.getMessage());
			return;
		}

		showEditEnquiryDialog(form, msg.getMessage("GroupDelegationEditConfigDialog.editEnquiryFormAction"));
	}

	private void showEditEnquiryDialog(EnquiryForm target, String caption)
	{
		EnquiryFormEditor editor;
		try
		{
			editor = enquiryFormEditorFactory.getObject().init(false);
			editor.setForm(target);
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupDelegationEditConfigDialog.errorInFormEdit"), e.getMessage());
			return;
		}
		EnquiryFormEditDialog dialog = new EnquiryFormEditDialog(msg, caption, this::updateEnquiryForm, editor, notificationPresenter);
		dialog.open();
	}

	private boolean updateEnquiryForm(EnquiryForm updatedForm, boolean ignoreRequestsAndInvitations)
	{
		try
		{
			enquiryMan.updateEnquiry(updatedForm, ignoreRequestsAndInvitations);
			bus.fireEvent(new EnquiryFormChangedEvent(updatedForm));
			return true;
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("GroupDelegationEditConfigDialog.errorUpdateForm"), e.getMessage());
			return false;
		}
	}

	public static class DelegationConfiguration
	{
		private boolean enabled;
		private boolean enableSubprojects;
		private String logoUrl;
		private String registrationForm;
		private String signupEnquiryForm;
		private String membershipUpdateEnquiryForm;

		public DelegationConfiguration(GroupDelegationConfiguration org)
		{
			setEnabled(org.enabled);
			setEnableSubprojects(org.enableSubprojects);
			setLogoUrl(org.logoUrl);
			setRegistrationForm(org.registrationForm);
			setSignupEnquiryForm(org.signupEnquiryForm);
			setMembershipUpdateEnquiryForm(org.membershipUpdateEnquiryForm);
		}

		public boolean isEnabled()
		{
			return enabled;
		}

		public void setEnabled(boolean enabled)
		{
			this.enabled = enabled;
		}

		public String getLogoUrl()
		{
			return logoUrl;
		}

		public void setLogoUrl(String logoUrl)
		{
			this.logoUrl = logoUrl;
		}

		public String getRegistrationForm()
		{
			return registrationForm;
		}

		public void setRegistrationForm(String registrationForm)
		{
			this.registrationForm = registrationForm;
		}

		public String getSignupEnquiryForm()
		{
			return signupEnquiryForm;
		}

		public void setSignupEnquiryForm(String signupEnquiryForm)
		{
			this.signupEnquiryForm = signupEnquiryForm;
		}

		public String getMembershipUpdateEnquiryForm()
		{
			return membershipUpdateEnquiryForm;
		}

		public void setMembershipUpdateEnquiryForm(String stickyEnquiryForm)
		{
			this.membershipUpdateEnquiryForm = stickyEnquiryForm;
		}

		public boolean isEnableSubprojects()
		{
			return enableSubprojects;
		}

		public void setEnableSubprojects(boolean enableSubprojects)
		{
			this.enableSubprojects = enableSubprojects;
		}
	}

	private static class FormComboWithButtons extends CustomField<String>
	{
		private final Select<String> combo;
		private final Icon generate;
		private final Icon validate;
		private final Icon edit;

		public FormComboWithButtons(MessageSource msg, String caption, String description, ComponentEventListener<ClickEvent<Icon>> generateListener,
				ComponentEventListener<ClickEvent<Icon>> validateListener, ComponentEventListener<ClickEvent<Icon>> editListener)
		{
			setLabel(caption);
			combo = new Select<>();
			//combo.setClearButtonVisible(true);
			combo.setEmptySelectionAllowed(true);
			combo.setWidth(20, Unit.EM);
			combo.setTooltipText(description);
			HorizontalLayout main = new HorizontalLayout();
			main.setAlignItems(FlexComponent.Alignment.CENTER);
			main.add(combo);

			generate = VaadinIcon.MAGIC.create();
			generate.setTooltipText(msg.getMessage("GroupDelegationEditConfigDialog.generateForm"));
			if (generateListener != null)
			{
				generate.addClickListener(generateListener);
				main.add(generate);
			}

			validate = VaadinIcon.HANDSHAKE.create();
			validate.setTooltipText(msg.getMessage("GroupDelegationEditConfigDialog.validateForm"));
			if (validateListener != null)
			{
				validate.addClickListener(validateListener);
				main.add(validate);
			}

			edit = VaadinIcon.EDIT.create();
			edit.setTooltipText(msg.getMessage("GroupDelegationEditConfigDialog.editForm"));
			if (editListener != null)
			{
				edit.addClickListener(editListener);
				main.add(edit);
			}

			combo.addValueChangeListener(e -> refreshButtons());
			combo.addValueChangeListener(this::fireEvent);

			add(main);
			refreshButtons();
		}

		@Override
		public String getValue()
		{
			return combo.getValue();
		}

		private void refreshButtons()
		{
			boolean en = combo.getValue() != null;
			if(en)
			{
				edit.setClassName(POINTER.getName());
				validate.setClassName(POINTER.getName());
			}
			else
			{
				edit.setClassName(DISABLED_ICON.getName());
				validate.setClassName(DISABLED_ICON.getName());
			}
		}

		public void setItems(Collection<String> items)
		{
			String value = combo.getValue();
			combo.setItems(items);
			combo.setValue(value);
		}

		@Override
		public void setValue(String value)
		{
			combo.setValue(value);
		}

		@Override
		public void setEnabled(boolean enabled)
		{
			super.setEnabled(enabled);
			if(enabled)
				generate.setClassName(POINTER.getName());
			else
				generate.setClassName(DISABLED_ICON.getName());
		}

		@Override
		protected String generateModelValue()
		{
			return getValue();
		}

		@Override
		protected void setPresentationValue(String s)
		{
			setValue(s);
		}
	}

	private static class ValidationResultDialog extends DialogWithActionFooter
	{
		private final MessageSource msg;
		private final List<String> messages;
		private final String formName;

		public ValidationResultDialog(MessageSource msg, List<String> messages, String formName)
		{
			super(msg::getMessage);
			this.msg = msg;
			this.messages = messages;
			this.formName = formName;
			setHeaderTitle(msg.getMessage("GroupDelegationEditConfigDialog.validationDialogCaption"));
			setActionButton(msg.getMessage("ok"), this::close);
			setCancelButtonVisible(false);
			add(getContents());
		}

		protected Component getContents()
		{
			FormLayout main = new FormLayout();
			main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

			if (messages.isEmpty())
			{
				Span l = new Span(msg.getMessage("GroupDelegationEditConfigDialog.noneValidationWarns", formName));
				main.add(l);
			} else
			{
				for (String m : messages)
				{
					Span l = new Span(VaadinIcon.CIRCLE.create());
					l.add(" " + m);
					main.add(l);
				}
			}
			return main;
		}
	}
	
	private static class FormWithType
	{
		public final FormType type;
		public final String label;
		
		public FormWithType(FormType type, String label)
		{
			this.type = type;
			this.label = label;
		}
	}
}
