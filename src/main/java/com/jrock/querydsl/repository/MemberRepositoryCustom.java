package com.jrock.querydsl.repository;

import com.jrock.querydsl.dto.MemberSearchCondition;
import com.jrock.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition condition);

}
