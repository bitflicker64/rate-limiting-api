package com.behl.overseer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.behl.overseer.dto.PlanResponseDto;
import com.behl.overseer.dto.PlanUpdationRequestDto;
import com.behl.overseer.entity.UserPlanMapping;
import com.behl.overseer.exception.InvalidPlanException;
import com.behl.overseer.repository.PlanRepository;
import com.behl.overseer.repository.UserPlanMappingRepository;
import com.behl.overseer.utility.AuthenticatedUserIdProvider;

class PlanServiceTest {

	private final PlanRepository planRepository = mock(PlanRepository.class);
	private final RateLimitingService rateLimitingService = mock(RateLimitingService.class);
	private final CachedLookupService cachedLookupService = mock(CachedLookupService.class);
	private final UserPlanMappingRepository userPlanMappingRepository = mock(UserPlanMappingRepository.class);
	private final AuthenticatedUserIdProvider authenticatedUserIdProvider = mock(AuthenticatedUserIdProvider.class);
	private final PlanService planService = new PlanService(planRepository, rateLimitingService, cachedLookupService,
			userPlanMappingRepository, authenticatedUserIdProvider);

	@Test
	void planUpdationshouldThrowExceptionForInvalidPlanId() {
		// prepare plan updation request
		final var planId = UUID.randomUUID();
		final var planUpdationRequest = mock(PlanUpdationRequestDto.class);
		when(planUpdationRequest.getPlanId()).thenReturn(planId);

		// prepare datasource to evaluate invalid plan-id
		when(planRepository.existsById(planId)).thenReturn(Boolean.FALSE);

		// invoke method under test and assert exception
		final var exception = assertThrows(InvalidPlanException.class, () -> planService.update(planUpdationRequest));
		assertThat(exception.getReason()).isEqualTo("No plan exists in the system with provided-id");

		// verify mock interactions
		verify(planUpdationRequest, times(1)).getPlanId();
		verify(planRepository, times(1)).existsById(planId);
		verify(userPlanMappingRepository, times(0)).save(any(UserPlanMapping.class));
	}

	@Test
	void datasourceShouldNotBeUpdatedForExistingPlanId() {
		// prepare plan updation request
		final var planId = UUID.randomUUID();
		final var planUpdationRequest = mock(PlanUpdationRequestDto.class);
		when(planUpdationRequest.getPlanId()).thenReturn(planId);

		// prepare datasource to evaluate plan-id
		when(planRepository.existsById(planId)).thenReturn(Boolean.TRUE);

		// configure authenticated user
		final var userId = UUID.randomUUID();
		when(authenticatedUserIdProvider.getUserId()).thenReturn(userId);

		// configure datasource to evaluate active plan
		when(userPlanMappingRepository.isActivePlan(userId, planId)).thenReturn(Boolean.TRUE);

		// invoke method under test
		planService.update(planUpdationRequest);

		// verify mock interactions
		verify(planUpdationRequest, times(1)).getPlanId();
		verify(planRepository, times(1)).existsById(planId);
		verify(authenticatedUserIdProvider, times(1)).getUserId();
		verify(userPlanMappingRepository, times(1)).isActivePlan(userId, planId);
		verify(userPlanMappingRepository, times(0)).deactivateCurrentPlan(userId);
		verify(userPlanMappingRepository, times(0)).save(any(UserPlanMapping.class));
		verify(cachedLookupService, times(0)).evictActivePlanLimitPerHour(userId);
	}

	@Test
	void shouldUpdateUserPlanForValidRequestAndDeactivatePreviousPlan() {
		// prepare plan updation request
		final var planId = UUID.randomUUID();
		final var planUpdationRequest = mock(PlanUpdationRequestDto.class);
		when(planUpdationRequest.getPlanId()).thenReturn(planId);

		// prepare datasource to evaluate plan-id
		when(planRepository.existsById(planId)).thenReturn(Boolean.TRUE);

		// configure authenticated user
		final var userId = UUID.randomUUID();
		when(authenticatedUserIdProvider.getUserId()).thenReturn(userId);

		// configure datasource to evaluate active plan
		when(userPlanMappingRepository.isActivePlan(userId, planId)).thenReturn(Boolean.FALSE);

		// invoke method under test
		planService.update(planUpdationRequest);

		// verify mock interactions
		verify(planUpdationRequest, times(1)).getPlanId();
		verify(planRepository, times(1)).existsById(planId);
		verify(authenticatedUserIdProvider, times(1)).getUserId();
		verify(userPlanMappingRepository, times(1)).isActivePlan(userId, planId);
		verify(userPlanMappingRepository, times(1)).deactivateCurrentPlan(userId);
		verify(userPlanMappingRepository, times(1)).save(any(UserPlanMapping.class));
		verify(cachedLookupService, times(1)).evictActivePlanLimitPerHour(userId);
		verify(rateLimitingService, times(1)).reset(userId);
	}

	@Test
	void shouldRetrievePlansFromDatasource() {
		// prepare cached plans response
		final var planId = UUID.randomUUID();
		final var planName = "test-plan";
		final var limitPerHour = 20;
		final var expectedPlanResponse = PlanResponseDto.builder().id(planId).name(planName).limitPerHour(limitPerHour).build();
		when(cachedLookupService.retrievePlans()).thenReturn(List.of(expectedPlanResponse));

		// invoke method under test
		final var response = planService.retrieve();

		// assert response
		assertThat(response).isNotNull().hasSize(1).satisfies(plans -> {
			final var planResponse = plans.get(0);
			assertThat(planResponse.getId()).isEqualTo(planId);
			assertThat(planResponse.getName()).isEqualTo(planName);
			assertThat(planResponse.getLimitPerHour()).isEqualTo(limitPerHour);
		});

		// verify mock interaction
		verify(cachedLookupService, times(1)).retrievePlans();
	}

}
