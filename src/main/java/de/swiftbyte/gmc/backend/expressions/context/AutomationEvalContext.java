package de.swiftbyte.gmc.backend.expressions.context;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Root object for SpEL evaluation. Exposes strongly-typed views of automation, run, step, targets and outputs.
 */
public class AutomationEvalContext {
    private final Automation automation;
    private final AutomationRun run;
    private final MongoTemplate mongo;

    public AutomationEvalContext(Automation automation, AutomationRun run, MongoTemplate mongoTemplate) {
        this.automation = automation;
        this.run = run;
        this.mongo = mongoTemplate;
    }

    public Automation getAutomation() { return automation; }
    public AutomationRun getRun() { return run; }
    public StepView getStep() { return new StepView(run); }
    public TargetsView getTargets() { return new TargetsView(run, mongo); }
    public OutputsView getOutputs() { return new OutputsView(run); }

    // --- Nested typed views ---

    public static class StepView {
        private final AutomationRun run;
        public StepView(AutomationRun run) { this.run = run; }
        public Integer getIndex() { return run.getCurrentStep() == null ? null : run.getCurrentStep().getWorkflowIndex(); }
        public String getIndexHuman() { Integer i = getIndex(); return i == null ? null : String.valueOf(i + 1); }
        public AutomationStepType getType() { return run.getCurrentStep() == null ? null : run.getCurrentStep().getType(); }
        public Instant getStartedAt() { return run.getCurrentStep() == null ? null : run.getCurrentStep().getStartedAt(); }
        public Instant getFinishedAt() { return run.getCurrentStep() == null ? null : run.getCurrentStep().getFinishedAt(); }
    }

    public static class TargetsView {
        private final AutomationRun run;
        private final MongoTemplate mongo;
        private List<String> idsCache;
        private List<String> namesCache;
        private List<String> activeIdsCache;
        private List<String> activeNamesCache;
        private List<String> succeededIdsCache;
        private List<String> succeededNamesCache;
        private List<String> failedIdsCache;
        private List<String> failedNamesCache;

        public TargetsView(AutomationRun run, MongoTemplate mongo) { this.run = run; this.mongo = mongo; }

        public int getCount() { return getIds().size(); }
        public List<String> getIds() {
            if (idsCache != null) return idsCache;
            idsCache = sorted(new ArrayList<>(run.getTargetIds() == null ? List.of() : run.getTargetIds().keySet()));
            return idsCache;
        }
        private List<String> activeIdsInternal() {
            if (activeIdsCache != null) return activeIdsCache;
            activeIdsCache = getIds().stream().filter(id -> !Boolean.TRUE.equals(run.getTargetIds().get(id))).collect(Collectors.toList());
            return activeIdsCache;
        }
        private List<String> succeededIdsInternal() {
            if (succeededIdsCache != null) return succeededIdsCache;
            Set<String> res = new HashSet<>();
            for (String id : getIds()) {
                AutomationRun.StepState st = run.getCurrentStep();
                if (st != null && st.getStateOf(id) == AutomationStepStatus.SUCCEEDED) res.add(id);
            }
            succeededIdsCache = sorted(new ArrayList<>(res));
            return succeededIdsCache;
        }
        private List<String> failedIdsInternal() {
            if (failedIdsCache != null) return failedIdsCache;
            Set<String> res = new HashSet<>();
            for (String id : getIds()) {
                AutomationRun.StepState st = run.getCurrentStep();
                if (st == null) continue;
                AutomationStepStatus s = st.getStateOf(id);
                if (s == AutomationStepStatus.FAILED || Boolean.TRUE.equals(run.getTargetIds().get(id))) res.add(id);
            }
            failedIdsCache = sorted(new ArrayList<>(res));
            return failedIdsCache;
        }
        public List<String> getNames() {
            if (namesCache != null) return namesCache;
            if (mongo == null) {
                namesCache = List.of();
                return namesCache;
            }
            Map<String, String> idToName = new HashMap<>();
            Query q = Query.query(Criteria.where("_id").in(getIds()));
            q.fields().include("_id").include("displayName");
            for (GameServer gs : mongo.find(q, GameServer.class)) {
                if (gs.getId() != null) idToName.put(gs.getId(), gs.getDisplayName());
            }
            List<String> out = new ArrayList<>();
            for (String id : getIds()) out.add(idToName.getOrDefault(id, id));
            namesCache = out;
            return namesCache;
        }

        public String nameById(String serverId) {
            if (serverId == null || mongo == null) return null;
            GameServer gs = mongo.findById(serverId, GameServer.class);
            return gs == null ? null : gs.getDisplayName();
        }

        private List<String> activeNamesInternal() {
            if (activeNamesCache != null) return activeNamesCache;
            activeNamesCache = mapIdsToNames(activeIdsInternal());
            return activeNamesCache;
        }
        private List<String> succeededNamesInternal() {
            if (succeededNamesCache != null) return succeededNamesCache;
            succeededNamesCache = mapIdsToNames(succeededIdsInternal());
            return succeededNamesCache;
        }
        private List<String> failedNamesInternal() {
            if (failedNamesCache != null) return failedNamesCache;
            failedNamesCache = mapIdsToNames(failedIdsInternal());
            return failedNamesCache;
        }

        private List<String> mapIdsToNames(List<String> ids) {
            if (mongo == null || ids == null || ids.isEmpty()) return List.of();
            Map<String, String> idToName = new HashMap<>();
            Query q = Query.query(Criteria.where("_id").in(ids));
            q.fields().include("_id").include("displayName");
            for (GameServer gs : mongo.find(q, GameServer.class)) {
                if (gs.getId() != null) idToName.put(gs.getId(), gs.getDisplayName());
            }
            List<String> out = new ArrayList<>();
            for (String id : ids) out.add(idToName.getOrDefault(id, id));
            return out;
        }
        private static List<String> sorted(List<String> list) { Collections.sort(list); return list; }

        // --- Nested bucket views for friendlier access ---
        public Bucket getAll() { return new Bucket(getIds(), getNames()); }
        public Bucket getActive() { return new Bucket(activeIdsInternal(), activeNamesInternal()); }
        public Bucket getSucceeded() { return new Bucket(succeededIdsInternal(), succeededNamesInternal()); }
        public Bucket getFailed() { return new Bucket(failedIdsInternal(), failedNamesInternal()); }

        public static class Bucket {
            private final List<String> ids;
            private final List<String> names;
            public Bucket(List<String> ids, List<String> names) { this.ids = ids == null ? List.of() : ids; this.names = names == null ? List.of() : names; }
            public List<String> getIds() { return ids; }
            public List<String> getNames() { return names; }
            public int getSize() { return ids.size(); }
            public int getCount() { return getSize(); }
        }
    }

    public static class OutputsView {
        private final AutomationRun run;
        public OutputsView(AutomationRun run) { this.run = run; }
        public RconSet rcon(String name) { return new RconSet(run, name); }
    }

    public static class RconSet {
        private final Map<String, String> byId;
        public RconSet(AutomationRun run, String name) {
            Map<String, Map<String, String>> all = run.getVars();
            this.byId = all == null ? Map.of() : all.getOrDefault(name, Map.of());
        }
        public RconEntry byId(String serverId) { return new RconEntry(byId.get(serverId)); }
        public List<String> responses() { return byId.values().stream().filter(Objects::nonNull).collect(Collectors.toList()); }
        public List<String> getResponses() { return responses(); }
    }

    public static class RconEntry {
        private final String response;
        public RconEntry(String response) { this.response = response; }
        public String getResponse() { return response; }
        public boolean isPresent() { return response != null; }
    }

    // Convenience accessors for time and team
    public String getNowIso() { return Instant.now().toString(); }
    public long getEpoch() { return Instant.now().getEpochSecond(); }
    public String getDate() { return Instant.now().atZone(ZoneOffset.UTC).toLocalDate().toString(); }
    public String getTimeHms() { return DateTimeFormatter.ofPattern("HH:mm:ss").format(Instant.now().atZone(ZoneOffset.UTC)); }

    public String getTeamName() {
        if (automation == null || automation.getTeamId() == null || mongo == null) return null;
        Team t = mongo.findById(automation.getTeamId(), Team.class);
        return t == null ? null : t.getName();
    }
}
