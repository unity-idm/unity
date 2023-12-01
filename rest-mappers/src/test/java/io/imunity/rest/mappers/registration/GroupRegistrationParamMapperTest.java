/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.function.Function;

import io.imunity.rest.api.types.registration.RestGroupRegistrationParam;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam.IncludeGroupsMode;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;

public class GroupRegistrationParamMapperTest extends MapperTestBase<GroupRegistrationParam, RestGroupRegistrationParam>
{
	@Override
	protected GroupRegistrationParam getFullAPIObject()
	{
		GroupRegistrationParam groupRegistrationParam = new GroupRegistrationParam();
		groupRegistrationParam.setDescription("desc");
		groupRegistrationParam.setGroupPath("/group");
		groupRegistrationParam.setIncludeGroupsMode(IncludeGroupsMode.all);
		groupRegistrationParam.setMultiSelect(true);
		groupRegistrationParam.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		groupRegistrationParam.setLabel("label");
		return groupRegistrationParam;
	}

	@Override
	protected RestGroupRegistrationParam getFullRestObject()
	{
		return RestGroupRegistrationParam.builder()
				.withGroupPath("/group")
				.withDescription("desc")
				.withIncludeGroupsMode("all")
				.withMultiSelect(true)
				.withLabel("label")
				.withRetrievalSettings("automatic")
				.build();
	}

	@Override
	protected Pair<Function<GroupRegistrationParam, RestGroupRegistrationParam>, Function<RestGroupRegistrationParam, GroupRegistrationParam>> getMapper()
	{
		return Pair.of(GroupRegistrationParamMapper::map, GroupRegistrationParamMapper::map);
	}

}
