package com.joe_bor.svt_api.repositories.session;

import com.joe_bor.svt_api.models.session.GameSessionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionRepository extends JpaRepository<GameSessionEntity, UUID> {
}
