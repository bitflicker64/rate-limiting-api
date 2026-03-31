package com.behl.overseer.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@Schema(title = "Plan", accessMode = Schema.AccessMode.READ_ONLY)
public class PlanResponseDto implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private UUID id;
	private String name;
	private Integer limitPerHour;

}