package de.swiftbyte.gmc.backend.service.game;

import de.swiftbyte.gmc.common.entity.GameType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class GameHandlersRegistry {

    private final Map<GameType, GameHandler> handlers = new EnumMap<>(GameType.class);

    public GameHandlersRegistry(List<GameHandler> handlers) {
        for (GameHandler handler : handlers) {
            var previous = this.handlers.put(handler.getType(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate GameHandler for type: " + handler.getType());
            }
        }
    }

    public GameHandler get(GameType type) {
        return handlers.get(type);
    }
}
