package com.nickfallico.financialriskmanagement.repository;

import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImmutableUserRiskProfileRepository 
    extends R2dbcRepository<ImmutableUserRiskProfile, String> {
}