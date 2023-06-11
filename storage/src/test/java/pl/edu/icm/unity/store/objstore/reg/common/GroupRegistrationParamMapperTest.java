/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.function.Function;

import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam.IncludeGroupsMode;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class GroupRegistrationParamMapperTest extends MapperTestBase<GroupRegistrationParam, DBGroupRegistrationParam>
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
	protected DBGroupRegistrationParam getFullDBObject()
	{
		return DBGroupRegistrationParam.builder()
				.withGroupPath("/group")
				.withDescription("desc")
				.withIncludeGroupsMode("all")
				.withMultiSelect(true)
				.withLabel("label")
				.withRetrievalSettings("automatic")
				.build();
	}

	@Override
	protected Pair<Function<GroupRegistrationParam, DBGroupRegistrationParam>, Function<DBGroupRegistrationParam, GroupRegistrationParam>> getMapper()
	{
		return Pair.of(GroupRegistrationParamMapper::map, GroupRegistrationParamMapper::map);
	}

}
