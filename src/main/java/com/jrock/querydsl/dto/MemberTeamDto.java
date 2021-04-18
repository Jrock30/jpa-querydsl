package com.jrock.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberTeamDto {

    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    /**
     * 참고:
     * @QueryProjection 을 사용하면 해당 DTO가 Querydsl을 의존하게 된다.
     * 이런 의존이 싫으면, 해당 에노테이션을 제거하고,
     * Projection.bean(), fields(), constructor() 을 사용하면 된다.
     */
    @QueryProjection // 이것을 사용하면 gradle 에서 compileQuerydsl 로 qFile 생성해 주어야한다.
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
