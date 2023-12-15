/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directoryBrowser.groupbrowser;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.data.Binder;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

import io.imunity.webconsole.signupAndEnquiry.forms.EnquiryFormEditDialog;
import io.imunity.webconsole.signupAndEnquiry.forms.EnquiryFormEditor;
import io.imunity.webconsole.signupAndEnquiry.forms.RegistrationFormEditDialog;
import io.imunity.webconsole.signupAndEnquiry.forms.RegistrationFormEditor;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryFormChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormChangedEvent;

/**
 * Edit dialog for {@link GroupDelegationConfiguration}.
 * 
 * @author P.Piernik
 *
 */
class GroupDelegationEditConfigDialog extends AbstractDialog
{
	private Consumer<GroupDelegationConfiguration> callback;
	private GroupDelegationConfiguration toEdit;
	private Group group;

	private TextField logoUrl;
	private CheckBox enableDelegation;
	private CheckBox enableSubprojects;
	private FormComboWithButtons registrationFormComboWithButtons;
	private FormComboWithButtons signupEnquiryFormComboWithButtons;
	private FormComboWithButtons membershipUpdateEnquiryFormComboWithButtons;
	private ChipsWithDropdown<String> attributes;
	private ChipsWithDropdown<PolicyDocumentWithRevision> policyDocuments;
	private Binder<DelegationConfiguration> binder;

	private RegistrationsManagement registrationMan;
	private EnquiryManagement enquiryMan;
	private AttributeTypeManagement attrTypeMan;
	private final PolicyDocumentManagement policyDocumentManagement;
	private ObjectFactory<RegistrationFormEditor> regFormEditorFactory;
	private ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory;
	private EventsBus bus;
	private GroupDelegationConfigGenerator configGenerator;

	GroupDelegationEditConfigDialog(MessageSource msg, RegistrationsManagement registrationMan,
			EnquiryManagement enquiryMan, AttributeTypeManagement attrTypeMan, 
			PolicyDocumentManagement policyDocumentManagement,
			ObjectFactory<RegistrationFormEditor> regFormEditorFactory,
			ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory, EventsBus bus,
			GroupDelegationConfigGenerator configGenerator, Group group,
			Consumer<GroupDelegationConfiguration> callback)
	{
		super(msg, msg.getMessage("GroupDelegationEditConfigDialog.caption"), msg.getMessage("ok"),
				msg.getMessage("cancel"));
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

	@Override
	protected Component getContents() throws Exception
	{

		enableDelegation = new CheckBox(
				msg.getMessage("GroupDelegationEditConfigDialog.enableDelegationCaption"));
		enableDelegation.addValueChangeListener(e -> {
			enableEdit(e.getValue());
		});
		
		enableSubprojects = new CheckBox(
				msg.getMessage("GroupDelegationEditConfigDialog.enableSubprojectsCaption"));
		
		logoUrl = new TextField(msg.getMessage("GroupDelegationEditConfigDialog.logoUrlCaption"));
		logoUrl.setWidth(100, Unit.PERCENTAGE);

		registrationFormComboWithButtons = new FormComboWithButtons(msg,
				msg.getMessage("GroupDelegationEditConfigDialog.registrationForm") + ":",
				msg.getMessage("GroupDelegationEditConfigDialog.registrationFormDesc"),	
				e -> generateJoinRegistrationForm(),
				e -> showJoinRegistrationValidation(registrationFormComboWithButtons.getValue()),
				e -> showRegFormEditDialog(registrationFormComboWithButtons.getValue()));
		reloadRegistrationForm();

		signupEnquiryFormComboWithButtons = new FormComboWithButtons(msg,
				msg.getMessage("GroupDelegationEditConfigDialog.signupEnquiry") + ":",
				msg.getMessage("GroupDelegationEditConfigDialog.signupEnquiryDesc"),
				e -> generateJoinEnquiryForm(),
				e -> showJoinEnquiryValidation(signupEnquiryFormComboWithButtons.getValue()),
				e -> showEnquiryFormEditDialog(signupEnquiryFormComboWithButtons.getValue()));

		membershipUpdateEnquiryFormComboWithButtons = new FormComboWithButtons(msg,
				msg.getMessage("GroupDelegationEditConfigDialog.membershipUpdateEnquiry") + ":",
				msg.getMessage("GroupDelegationEditConfigDialog.membershipUpdateEnquiryDesc"),
				e -> generateUpdateEnquiryForm(),
				e -> showUpdateEnquiryValidation(
						membershipUpdateEnquiryFormComboWithButtons.getValue()),
				e -> showEnquiryFormEditDialog(membershipUpdateEnquiryFormComboWithButtons.getValue()));
		reloadEnquiryForms();

		attributes = new ChipsWithDropdown<>();
		attributes.setCaption(msg.getMessage("GroupDelegationEditConfigDialog.attributes"));
		attributes.setMaxSelection(4);

		Collection<AttributeType> attributeTypes = attrTypeMan.getAttributeTypes();
		attributes.setItems(attributeTypes.stream().map(a -> a.getName()).collect(Collectors.toList()));
		if (toEdit.attributes != null)
		{
			attributes.setSelectedItems(toEdit.attributes.stream().collect(Collectors.toList()));
		}

		policyDocuments = new ChipsWithDropdown<>(p -> p.name, true);
		policyDocuments.setCaption(msg.getMessage("GroupDelegationEditConfigDialog.policyDocuments"));
	
		Collection<PolicyDocumentWithRevision> policyDocs = policyDocumentManagement.getPolicyDocuments();
		policyDocuments.setItems(policyDocs);
		policyDocuments.setEmptyComboLabel(msg.getMessage("GroupDelegationEditConfigDialog.noPolicyDocuments"));
		policyDocuments.setSelectedItems(List.of());
		
		if (toEdit.policyDocumentsIds != null)
		{
			policyDocuments.setSelectedItems(policyDocs.stream()
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
		main.addComponents(enableDelegation, logoUrl, enableSubprojects, attributes, policyDocuments, registrationFormComboWithButtons,
				signupEnquiryFormComboWithButtons, membershipUpdateEnquiryFormComboWithButtons);
		return main;
	}

	private void reloadEnquiryForms()
	{
		List<EnquiryForm> forms;
		try
		{
			forms = enquiryMan.getEnquires();
			signupEnquiryFormComboWithButtons
					.setItems(forms.stream().map(f -> f.getName()).collect(Collectors.toList()));
			membershipUpdateEnquiryFormComboWithButtons
					.setItems(forms.stream().filter(f -> f.getType().equals(EnquiryType.STICKY))
							.map(f -> f.getName()).collect(Collectors.toList()));
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupDelegationEditConfigDialog.cannotLoadForms"), e);
		}
	}

	private void generateJoinEnquiryForm()
	{
		EnquiryForm form;
		try
		{
			form = configGenerator.generateProjectJoinEnquiryForm(group.toString(), logoUrl.getValue(), policyDocuments.getSelectedItems().stream().map(p -> p.id).collect(Collectors.toList()));
			enquiryMan.addEnquiry(form);

		} catch (EngineException e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupDelegationEditConfigDialog.cannotGenerateForm"), e);
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
			NotificationPopup.showError(msg,
					msg.getMessage("GroupDelegationEditConfigDialog.cannotGenerateForm"), e);
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
					.setItems(forms.stream().map(f -> f.getName()).collect(Collectors.toList()));
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupDelegationEditConfigDialog.cannotLoadForms"), e);
		}

	}

	private void generateJoinRegistrationForm()
	{
		RegistrationForm form;
		try
		{
			form = configGenerator.generateProjectRegistrationForm(group.toString(), logoUrl.getValue(),
					attributes.getSelectedItems(),policyDocuments.getSelectedItems().stream().map(p -> p.id).collect(Collectors.toList()));
			registrationMan.addForm(form);

		} catch (EngineException e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupDelegationEditConfigDialog.cannotGenerateForm"), e);
			return;
		}
		reloadRegistrationForm();
		registrationFormComboWithButtons.setValue(form.getName());
	}

	@Override
	protected void onConfirm() {
		try {
			DelegationConfiguration groupDelConfig = binder.getBean();
			GroupDelegationConfiguration config = new GroupDelegationConfiguration(groupDelConfig.isEnabled(),
					groupDelConfig.isEnableSubprojects(), groupDelConfig.getLogoUrl(),
					groupDelConfig.getRegistrationForm(), groupDelConfig.getSignupEnquiryForm(),
					groupDelConfig.getmembershipUpdateEnquiryForm(), attributes.getSelectedItems(),
					policyDocuments.getSelectedItems().stream().map(p -> p.id).collect(Collectors.toList()));

			Map<String, FormWithType> formsToSynch = getFormsToSynch(groupDelConfig.getRegistrationForm(), groupDelConfig.getSignupEnquiryForm(),
					policyDocuments.getSelectedItems().stream().map(p -> p.id).collect(Collectors.toSet()));	
			if (formsToSynch.size() > 0)
				getSynchronizationDialog(formsToSynch, () -> {
					callback.accept(config);
					close();	
				}).show();
			else {
				callback.accept(config);
				close();
			}
			
		} catch (Exception e) {
			NotificationPopup.showError(msg, msg.getMessage("GroupDelegationEditConfigDialog.cannotUpdate"), e);
		}
	}
	
	private ConfirmDialog getSynchronizationDialog(Map<String, FormWithType> formsToSynch, Runnable close)
	{
		ConfirmDialog confirmDialog = new ConfirmDialog(msg, msg.getMessage("GroupDelegationEditConfigDialog.synchronizePolicyDialogTitle"),
				msg.getMessage("GroupDelegationEditConfigDialog.synchronizePolicy",
						String.join(" " + msg.getMessage("and") + " ", formsToSynch.values()
								.stream()
								.map(f -> "<b>" + f.label + "</b>")
								.collect(Collectors.toList()))), msg.getMessage("yes"), msg.getMessage("no"),
				() ->
				{
					try {
						for (Entry<String, FormWithType> form : formsToSynch.entrySet()) {
							configGenerator.resetFormsPolicies(form.getKey(), form.getValue().type, policyDocuments
									.getSelectedItems().stream().map(p -> p.id).collect(Collectors.toList()));
						}

					} catch (EngineException e) {
						NotificationPopup.showError(msg,
								msg.getMessage("GroupDelegationEditConfigDialog.errorUpdateForm"), e);
					}
					close.run();
				}, () -> {
					close.run();
				});
		confirmDialog.setHTMLContent(true);
		return confirmDialog;
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
				ret.put(registrationFormName, new FormWithType(FormType.REGISTRATION, registrationForm,
						msg.getMessage("GroupDelegationEditConfigDialog.registrationForm")));
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
				ret.put(enquiryFormName, new FormWithType(FormType.ENQUIRY, enquiryForm,
						msg.getMessage("GroupDelegationEditConfigDialog.signupEnquiry")));
			}
		}

		return ret;
	}

	private void showJoinRegistrationValidation(String formName)
	{
		List<String> messages = configGenerator.validateRegistrationForm(group.toString(), formName,
				policyDocuments.getSelectedItems().stream().map(p -> p.id).collect(Collectors.toSet()));
		new ValidationResultDialog(msg, messages, formName).show();
	}

	private void showJoinEnquiryValidation(String formName) {
		List<String> messages = configGenerator.validateJoinEnquiryForm(group.toString(), formName,
				policyDocuments.getSelectedItems().stream().map(p -> p.id).collect(Collectors.toSet()));
		new ValidationResultDialog(msg, messages, formName).show();
	}

	private void showUpdateEnquiryValidation(String formName)
	{
		List<String> messages = configGenerator.validateUpdateEnquiryForm(group.toString(), formName);
		new ValidationResultDialog(msg, messages, formName).show();
	}

	private void showRegFormEditDialog(String target)
	{

		RegistrationForm form;
		try
		{
			form = registrationMan.getForm(target);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("GroupDelegationEditConfigDialog.errorGetForm"),
					e);
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
			NotificationPopup.showError(msg,
					msg.getMessage("GroupDelegationEditConfigDialog.errorInFormEdit"), e);
			return;
		}
		RegistrationFormEditDialog dialog = new RegistrationFormEditDialog(msg, caption,
				(form, ignoreRequestsAndInvitations) -> 
					updateRegistrationForm(form, ignoreRequestsAndInvitations)
				, editor);
		dialog.show();
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
			NotificationPopup.showError(msg,
					msg.getMessage("GroupDelegationEditConfigDialog.errorUpdateForm"), e);
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
			NotificationPopup.showError(msg, msg.getMessage("GroupDelegationEditConfigDialog.errorGetForm"),
					e);
			return;
		}

		showEditEnquriyDialog(form, msg.getMessage("GroupDelegationEditConfigDialog.editEnquiryFormAction"));
	}

	private void showEditEnquriyDialog(EnquiryForm target, String caption)
	{
		EnquiryFormEditor editor;
		try
		{
			editor = enquiryFormEditorFactory.getObject().init(false);
			editor.setForm(target);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupDelegationEditConfigDialog.errorInFormEdit"), e);
			return;
		}
		EnquiryFormEditDialog dialog = new EnquiryFormEditDialog(msg, caption,
				(form, ignoreRequestsAndInvitations) -> 
					 updateEnquiryForm(form, ignoreRequestsAndInvitations)
				, editor);
		dialog.show();
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
			NotificationPopup.showError(msg,
					msg.getMessage("GroupDelegationEditConfigDialog.errorUpdateForm"), e);
			return false;
		}
	}

	// for binder only
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
			setmembershipUpdateEnquiryForm(org.membershipUpdateEnquiryForm);
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

		public String getmembershipUpdateEnquiryForm()
		{
			return membershipUpdateEnquiryForm;
		}

		public void setmembershipUpdateEnquiryForm(String stickyEnquiryForm)
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
		private ComboBox<String> combo;
		private Button validate;
		private Button edit;
		private HorizontalLayout main;

		public FormComboWithButtons(MessageSource msg, String caption, String description, ClickListener generateListener,
				ClickListener validateListener, ClickListener editListener)
		{
			setCaption(caption);
			combo = new ComboBox<String>();
			combo.setWidth(20, Unit.EM);
			combo.setDescription(description);
			main = new HorizontalLayout();
			main.addComponent(combo);	

			Button generate = new Button();
			generate.setDescription(msg.getMessage("GroupDelegationEditConfigDialog.generateForm"));
			generate.addStyleName(Styles.toolbarButton.toString());
			generate.addStyleName(Styles.vButtonLink.toString());
			generate.setIcon(Images.wizard.getResource());
			if (generateListener != null)
			{
				generate.addClickListener(generateListener);
				main.addComponent(generate);
			}

			validate = new Button();
			validate.setDescription(msg.getMessage("GroupDelegationEditConfigDialog.validateForm"));
			validate.addStyleName(Styles.toolbarButton.toString());
			validate.addStyleName(Styles.vButtonLink.toString());
			validate.setIcon(Images.handshake.getResource());
			if (validateListener != null)
			{
				validate.addClickListener(validateListener);
				main.addComponent(validate);
			}

			edit = new Button();
			edit.setDescription(msg.getMessage("GroupDelegationEditConfigDialog.editForm"));
			edit.addStyleName(Styles.toolbarButton.toString());
			edit.addStyleName(Styles.vButtonLink.toString());
			edit.setIcon(Images.edit.getResource());
			if (editListener != null)
			{
				edit.addClickListener(editListener);
				main.addComponent(edit);
			}

			combo.addValueChangeListener(e -> {
				refreshButtons();
			});
			combo.addValueChangeListener(e -> fireEvent(e));

			refreshButtons();
		}

		@Override
		public String getValue()
		{
			return combo.getValue();
		}

		@Override
		protected Component initContent()
		{
			return main;
		}

		private void refreshButtons()
		{
			boolean en = combo.getValue() != null;
			edit.setEnabled(en);
			validate.setEnabled(en);
		}

		@Override
		protected void doSetValue(String value)
		{
			combo.setValue(value);
		}

		public void setItems(Collection<String> items)
		{
			combo.setItems(items);
		}
	}

	private static class ValidationResultDialog extends AbstractDialog
	{
		private List<String> messages;
		private String formName;

		public ValidationResultDialog(MessageSource msg, List<String> messages, String formName)
		{
			super(msg, msg.getMessage("GroupDelegationEditConfigDialog.validationDialogCaption"),
					msg.getMessage("ok"));
			this.messages = messages;
			this.formName = formName;
		}

		@Override
		protected Component getContents() throws Exception
		{
			FormLayout main = new FormLayout();

			if (messages.isEmpty())
			{
				Label l = new Label(msg.getMessage(
						"GroupDelegationEditConfigDialog.noneValidationWarns", formName));
				l.setStyleName(Styles.success.toString());
				main.addComponent(l);
			} else
			{

				for (String m : messages)
				{
					Label l = new Label(m);
					l.setIcon(Images.bullet.getResource());
					l.setStyleName(Styles.error.toString());
					main.addComponent(l);
				}
			}
			return main;
		}

		@Override
		protected void onConfirm()
		{
			close();

		}
	}
	
	private static class FormWithType
	{
		public final FormType type;
		public final BaseForm form;
		public final String label;
		
		public FormWithType(FormType type, BaseForm form, String label)
		{
			this.type = type;
			this.form = form;
			this.label = label;
		}
		
		
		
		
	}
	
}
