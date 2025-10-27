package com.nickfallico.financialriskmanagement.repository;

import com.nickfallico.financialriskmanagement.model.UserRiskProfile;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRiskProfileRepository extends R2dbcRepository<UserRiskProfile, String> {
}