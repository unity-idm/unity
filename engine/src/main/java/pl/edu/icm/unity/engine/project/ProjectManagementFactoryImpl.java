package pl.edu.icm.unity.engine.project;

import java.util.Objects;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.ProjectAuthorization;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.engine.api.project.ProjectManagementFactory;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;

@Component
class ProjectManagementFactoryImpl implements ProjectManagementFactory
{
	private final BeanFactory beans;

	ProjectManagementFactoryImpl(BeanFactory beans)
	{
		this.beans = beans;
	}

	@Override
	public ProjectManagement create(ProjectAuthorization authorization)
	{
		Objects.requireNonNull(authorization);
		return new ProjectManagement(
				(DelegatedGroupManagement) beans.getBean("projectGroupsWithAuthorization", authorization),
				(ProjectInvitationsManagement) beans.getBean("projectInvitationsWithAuthorization", authorization),
				(ProjectRequestManagement) beans.getBean("projectRequestsWithAuthorization", authorization));
	}
}
