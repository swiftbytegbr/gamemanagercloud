package de.swiftbyte.gmc.backend.util;

import de.swiftbyte.gmc.backend.domain.enums.automation.TargetSelectorOp;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.value.automation.DynamicTargetSelector;
import de.swiftbyte.gmc.common.entity.GameServerState;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class AutomationRunUtility {

    private AutomationRunUtility() {}

    /**
     * Build a Criteria (AND) for the dynamic selector entries, excluding team scope.
     * The caller should AND this with the teamId criteria (added first).
     */
    public static Criteria buildDynamicSelectorCriteria(DynamicTargetSelector dynamic) {
        List<Criteria> andCriteria = new ArrayList<>();

        if (dynamic.getSelectors() != null) {
            for (DynamicTargetSelector.Entry e : dynamic.getSelectors()) {
                DynamicTargetSelector.Field field = e.getField();
                if (field == null) continue;
                String path = mapFieldToPath(field);
                TargetSelectorOp op = e.getOp() == null ? TargetSelectorOp.EQ : e.getOp();
                String value = e.getValue();
                if (value == null) continue;

                switch (op) {
                    case EQ -> andCriteria.add(Criteria.where(path).is(value));
                    case CONTAINS -> andCriteria.add(Criteria.where(path).regex(Pattern.quote(value)));
                    case REGEX -> andCriteria.add(Criteria.where(path).regex(value));
                }
            }
        }

        if (andCriteria.isEmpty()) {
            // Match-all criteria (no dynamic filters); caller still ANDs teamId
            return new Criteria();
        }
        return new Criteria().andOperator(andCriteria.toArray(new Criteria[0]));
    }

    private static String mapFieldToPath(DynamicTargetSelector.Field field) {
        return switch (field) {
            case NODE_ID -> "nodeId";
            case GAME_TYPE -> "gameType";
            case STATE -> "state";
            case DISPLAY_NAME -> "displayName";
        };
    }

    /**
     * Fetch current server states for a set of server IDs in one query. Returns id -> state map.
     */
    public static Map<String, GameServerState> fetchServerStates(MongoTemplate mongoTemplate, List<String> serverIds) {
        Map<String, GameServerState> result = new HashMap<>();
        if (serverIds == null || serverIds.isEmpty()) return result;
        Query q = Query.query(Criteria.where("_id").in(serverIds));
        q.fields().include("_id").include("state");
        for (GameServer gs : mongoTemplate.find(q, GameServer.class)) {
            if (gs.getId() != null) {
                result.put(gs.getId(), gs.getState());
            }
        }
        return result;
    }
}
