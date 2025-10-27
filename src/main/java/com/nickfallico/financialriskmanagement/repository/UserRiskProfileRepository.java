package com.nickfallico.financialriskmanagement.repository;

import com.nickfallico.financialriskmanagement.model.UserRiskProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRiskProfileRepository extends JpaRepository<UserRiskProfile, String> {
}