/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.datefield.DateTimeResolution;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.UpManNavigationInfoProviderBase;
import io.imunity.upman.UpManRootNavigationInfoProvider;
import io.imunity.upman.UpManUI;
import io.imunity.upman.common.UpManView;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.ProjectInvitation;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.groups.OptionalGroupsSelection;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Invitations view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class ProjectInvitationsView extends CustomComponent implements UpManView
{

	public static final String VIEW_NAME = "Invitations";

	private MessageSource msg;
	private ProjectInvitationsController controller;
	private DelegatedGroup project;
	private ProjectInvitationsComponent invitationsComponent;

	@Autowired
	public ProjectInvitationsView(MessageSource msg, ProjectInvitationsController controller)
	{
		this.msg = msg;
		this.controller = controller;
		setSizeFull();
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		try
		{
			project = UpManUI.getProjectGroup();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setMargin(false);
		setCompositionRoot(main);
		invitationsComponent = new ProjectInvitationsComponent(msg, controller, project.path);
		main.addComponent(invitationsComponent);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("UpManMenu.invitations");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public com.vaadin.ui.Component getViewHeader()
	{
		HorizontalLayout header = new HorizontalLayout();
		header.setMargin(false);
		Label name = new Label(getDisplayedName());
		name.addStyleName(Styles.viewHeader.toString());
		Button addInvitationButton = new Button(msg.getMessage("Invitations.newInvite"),
				Images.add.getResource());
		addInvitationButton.addStyleName(Styles.buttonAction.toString());
		addInvitationButton.addClickListener(e -> {

			new NewInvitationDialog(msg, invitations -> {

				try
				{
					controller.addInvitations(invitations);
				} catch (ControllerException er)
				{
					NotificationPopup.showError(er);
				}
				invitationsComponent.reload();
			}).show();

		});

		try
		{
			Optional<DelegatedGroup> projectGroup = controller.getProjectGroups(project.path).stream()
					.filter(dg -> dg.path.equals(project.path)).findFirst();

			if (projectGroup.isPresent())
			{
				GroupDelegationConfiguration config = projectGroup.get().delegationConfiguration;
				addInvitationButton.setVisible((config.registrationForm != null
						&& !config.registrationForm.isEmpty())
						|| (config.signupEnquiryForm != null
								&& !config.signupEnquiryForm.isEmpty()));

			}

		} catch (ControllerException er)
		{
			NotificationPopup.showError(er);
		}

		header.addComponents(name, addInvitationButton);
		header.setComponentAlignment(name, Alignment.MIDDLE_CENTER);
		header.setComponentAlignment(addInvitationButton, Alignment.MIDDLE_CENTER);
		return header;
	}

	@Component
	public class InvitationsNavigationInfoProvider extends UpManNavigationInfoProviderBase
	{
		@Autowired
		public InvitationsNavigationInfoProvider(MessageSource msg,
				ObjectFactory<ProjectInvitationsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(UpManRootNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("UpManMenu.invitations"))
					.withIcon(Images.envelope_open.getResource()).withPosition(2).build());

		}
	}

	private class NewInvitationDialog extends AbstractDialog
	{
		private Consumer<List<ProjectInvitationParam>> selectionConsumer;
		private TextField email;
		private OptionalGroupsSelection groups;
		private DateTimeField lifeTime;
		private Binder<ProjectInvitationParams> binder;

		public NewInvitationDialog(MessageSource msg, Consumer<List<ProjectInvitationParam>> selectionConsumer)
		{
			super(msg, msg.getMessage("NewInvitationDialog.caption"));
			this.selectionConsumer = selectionConsumer;
			setSizeEm(45, 24);
		}

		@Override
		protected Button createConfirmButton()
		{
			Button ok = super.createConfirmButton();
			ok.addStyleName(Styles.buttonAction.toString());
			return ok;
		}

		@Override
		protected FormLayout getContents()
		{
			email = new TextField(msg.getMessage("NewInvitationDialog.emails"));
			List<DelegatedGroup> allowedGroups = new ArrayList<>();
			try
			{
				allowedGroups.addAll(controller.getProjectGroups(project.path).stream()
						.filter(dg -> !dg.path.equals(project.path))
						.collect(Collectors.toList()));
			} catch (ControllerException e)
			{
				NotificationPopup.showError(e);
			}
			email.setWidth(25, Unit.EM);
			email.setDescription(msg.getMessage("NewInvitationDialog.emailsDesc"));
			groups = new OptionalGroupsSelection(msg, true);
			groups.setCaption(msg.getMessage("NewInvitationDialog.allowedGroups"));
			groups.setItems(allowedGroups.stream().map(dg -> {
				Group g = new Group(dg.path);
				g.setDisplayedName(new I18nString(dg.displayedName));
				return g;
			}).collect(Collectors.toList()));
			groups.setDescription(msg.getMessage("NewInvitationDialog.allowedGroupsDesc"));

			lifeTime = new DateTimeField(msg.getMessage("NewInvitationDialog.invitationLivetime"));
			lifeTime.setResolution(DateTimeResolution.MINUTE);

			binder = new Binder<>(ProjectInvitationParams.class);
			binder.forField(email).asRequired(msg.getMessage("fieldRequired")).withValidator(v -> {
				for (String email : v.split(","))
				{
					if (EmailUtils.validate(email) != null)
						return false;
				}
				return true;
			}, msg.getMessage("NewInvitationDialog.incorrectEmail")).bind("contactAddress");
			binder.forField(lifeTime).asRequired(msg.getMessage("fieldRequired"))
					.withConverter(d -> d.atZone(ZoneId.systemDefault()).toInstant(),
							d -> LocalDateTime.ofInstant(d, ZoneId.systemDefault()))

					.withValidator((v, c) -> {
						return v.isAfter(Instant.now()) ? ValidationResult.ok()
								: ValidationResult.error(msg.getMessage(
										"NewInvitationDialog.invalidLifeTime"));
					}).bind("expiration");

			ProjectInvitationParams bean = new ProjectInvitationParams();
			bean.setExpiration(LocalDateTime.now(ZoneId.systemDefault())
					.plusDays(ProjectInvitation.DEFAULT_TTL_DAYS).atZone(ZoneId.systemDefault())
					.toInstant());
			binder.setBean(bean);

			FormLayout main = new CompactFormLayout();
			main.addComponents(email, groups, lifeTime);
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			if (!binder.validate().isOk())
				return;
			ProjectInvitationParams inv = binder.getBean();

			List<ProjectInvitationParam> params = new ArrayList<>();

			Stream.of(binder.getBean().getContactAddress().split(",")).map(String::trim)
					.forEach(email -> params.add(new ProjectInvitationParam(project.path, email,
							groups.getSelectedItems().stream().map(g -> g.toString())
									.collect(Collectors.toList()),
							inv.getExpiration())));

			selectionConsumer.accept(params);
			close();
		}
	}

	// for binder only
	public static class ProjectInvitationParams
	{
		private String contactAddresses;
		private Instant expiration;

		public ProjectInvitationParams()
		{

		}

		public Instant getExpiration()
		{
			return expiration;
		}

		public void setExpiration(Instant expiration)
		{
			this.expiration = expiration;
		}

		public String getContactAddress()
		{
			return contactAddresses;
		}

		public void setContactAddress(String contactAddresses)
		{
			this.contactAddresses = contactAddresses;
		}
	}
}
