package com.behl.overseer.service;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.behl.overseer.configuration.CacheNames;
import com.behl.overseer.dto.PlanResponseDto;
import com.behl.overseer.exception.InvalidLoginCredentialsException;
import com.behl.overseer.repository.PlanRepository;
import com.behl.overseer.repository.UserPlanMappingRepository;
import com.behl.overseer.repository.UserRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CachedLookupService {

	private final UserRepository userRepository;
	private final PlanRepository planRepository;
	private final UserPlanMappingRepository userPlanMappingRepository;

	@Cacheable(cacheNames = CacheNames.USER_AUTH_BY_EMAIL, key = "#emailId")
	public CachedUserAuthentication getUserAuthenticationByEmailId(@NonNull final String emailId) {
		final var user = userRepository.findByEmailId(emailId)
				.orElseThrow(InvalidLoginCredentialsException::new);
		return new CachedUserAuthentication(user.getId(), user.getPassword());
	}

	@Cacheable(cacheNames = CacheNames.PLANS, key = "'all'")
	public List<PlanResponseDto> retrievePlans() {
		return planRepository.findAll()
				.stream()
				.map(plan -> PlanResponseDto.builder()
						.id(plan.getId())
						.name(plan.getName())
						.limitPerHour(plan.getLimitPerHour())
						.build())
				.toList();
	}

	@Cacheable(cacheNames = CacheNames.ACTIVE_PLAN_LIMIT_PER_HOUR, key = "#userId")
	public Integer getActivePlanLimitPerHour(@NonNull final UUID userId) {
		final var userPlanMapping = userPlanMappingRepository.getActivePlan(userId);
		return userPlanMapping.getPlan().getLimitPerHour();
	}

	@CacheEvict(cacheNames = CacheNames.USER_AUTH_BY_EMAIL, key = "#emailId")
	public void evictUserAuthenticationByEmailId(@NonNull final String emailId) {
	}

	@CacheEvict(cacheNames = CacheNames.ACTIVE_PLAN_LIMIT_PER_HOUR, key = "#userId")
	public void evictActivePlanLimitPerHour(@NonNull final UUID userId) {
	}

	@CacheEvict(cacheNames = CacheNames.PLANS, allEntries = true)
	public void evictAllPlans() {
	}

	public record CachedUserAuthentication(UUID userId, String encodedPassword) implements Serializable {
	}

}

