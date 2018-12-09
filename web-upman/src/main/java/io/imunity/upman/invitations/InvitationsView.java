/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.data.Binder;
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
import io.imunity.upman.utils.GroupIndentHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Invitations view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class InvitationsView extends CustomComponent implements UpManView
{

	public static final String VIEW_NAME = "Invitations";

	private UnityMessageSource msg;
	private InvitationsController controller;
	private String project;
	private InvitationsComponent invitationsComponent;

	@Autowired
	public InvitationsView(UnityMessageSource msg, InvitationsController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		project = UpManUI.getProjectGroup();
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		setCompositionRoot(main);
		invitationsComponent = new InvitationsComponent(msg, controller, project);
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
		name.addStyleName(SidebarStyles.viewHeader.toString());
		Button addInvitationButton = new Button(msg.getMessage("Invitations.newInvite"),
				Images.add.getResource());
		addInvitationButton.addClickListener(e -> {

			new NewInvitationDialog(msg, invitation -> {

				try
				{
					controller.addInvitation(invitation);

				} catch (ControllerException er)
				{
					NotificationPopup.showError(er);
				}
				invitationsComponent.reload();
			}).show();

		});

		header.addComponents(name, addInvitationButton);
		header.setComponentAlignment(name, Alignment.MIDDLE_CENTER);
		header.setComponentAlignment(addInvitationButton, Alignment.MIDDLE_CENTER);
		return header;
	}

	@Component
	public class InvitationsNavigationInfoProvider extends UpManNavigationInfoProviderBase
	{
		@Autowired
		public InvitationsNavigationInfoProvider(UnityMessageSource msg, UpManRootNavigationInfoProvider parent,
				ObjectFactory<InvitationsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("UpManMenu.invitations"))
					.withIcon(Images.envelope_open.getResource()).withPosition(2).build());

		}
	}

	private class NewInvitationDialog extends AbstractDialog
	{
		private static final long DEFAULT_TTL_DAYS = 3;

		private Consumer<ProjectInvitationParam> selectionConsumer;
		private TextField email;
		private ChipsWithDropdown<NamedGroup> groups;
		private DateTimeField lifeTime;
		private Binder<ProjectInvitationParams> binder;

		public NewInvitationDialog(UnityMessageSource msg, Consumer<ProjectInvitationParam> selectionConsumer)
		{
			super(msg, msg.getMessage("NewInvitationDialog.caption"));
			this.selectionConsumer = selectionConsumer;
			setSizeEm(30, 18);
		}

		@Override
		protected FormLayout getContents()
		{
			email = new TextField(msg.getMessage("NewInvitationDialog.email"));
			Map<String, String> groupsMap = new HashMap<>();
			try
			{
				groupsMap.putAll(controller.getAllowedIndentGroupsMap(project));
			} catch (ControllerException e)
			{
				NotificationPopup.showError(e);
			}

			groups = new ChipsWithDropdown<>(g -> g.name, g -> {
				return new String(g.name).replace(GroupIndentHelper.GROUPS_TREE_INDENT_CHAR, "");
			}, true);
			groups.setCaption(msg.getMessage("NewInvitationDialog.allowedGroups"));
			groups.setItems(groupsMap.entrySet().stream().map(e -> new NamedGroup(e.getKey(), e.getValue()))
					.collect(Collectors.toList()));

			lifeTime = new DateTimeField(msg.getMessage("NewInvitationDialog.invitationLivetime"));
			lifeTime.setResolution(DateTimeResolution.MINUTE);

			binder = new Binder<>(ProjectInvitationParams.class);
			binder.forField(email).asRequired(msg.getMessage("fieldRequired"))
					.withValidator(v -> EmailUtils.validate(v) == null,
							msg.getMessage("NewInvitationDialog.incorrectEmail"))
					.bind("contactAddress");
			binder.forField(lifeTime).asRequired(msg.getMessage("fieldRequired"))
					.withConverter(d -> d.atZone(ZoneId.systemDefault()).toInstant(),
							d -> LocalDateTime.ofInstant(d, ZoneId.systemDefault()))
					.bind("expiration");

			ProjectInvitationParams bean = new ProjectInvitationParams();
			bean.setExpiration(LocalDateTime.now(ZoneId.systemDefault()).plusDays(DEFAULT_TTL_DAYS)
					.atZone(ZoneId.systemDefault()).toInstant());
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

			ProjectInvitationParam param = new ProjectInvitationParam(
					project, inv.getContactAddress(), groups.getSelectedItems().stream()
							.map(g -> g.path).collect(Collectors.toList()),
					inv.getExpiration());

			selectionConsumer.accept(param);
			close();
		}

		private class NamedGroup
		{
			public final String path;
			public final String name;

			public NamedGroup(String path, String name)
			{
				this.path = path;
				this.name = name;
			}
		}
	}

	// for binder only
	public static class ProjectInvitationParams
	{
		private String contactAddress;
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
			return contactAddress;
		}

		public void setContactAddress(String contactAddress)
		{
			this.contactAddress = contactAddress;
		}
	}
}
