package com.memesphere.chat.repository;

import com.memesphere.chat.domain.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long>, ChatCustomRepository {

    //Chat findFirstByCoinIdOrderByCreatedAtDesc(Long coin_id);
    Chat findFirstByMemeCoin_IdOrderByCreatedAtDesc(Long coin_id);
}
