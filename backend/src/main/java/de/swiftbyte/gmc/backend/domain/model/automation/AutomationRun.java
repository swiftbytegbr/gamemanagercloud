package de.swiftbyte.gmc.backend.domain.model.automation;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationRunStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.common.entity.GameServerState;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Data
@Document("automation_runs")
@CompoundIndexes({
        // Listing: by team ordered by createdAt desc
        @CompoundIndex(name = "ar_team_created_desc", def = "{ 'teamId':1, 'createdAt':-1 }"),
        // Listing with status filter
        @CompoundIndex(name = "ar_team_status_created_desc", def = "{ 'teamId':1, 'status':1, 'createdAt':-1 }"),
        // Listing runs for a specific automation within a team
        @CompoundIndex(name = "ar_team_auto_created_desc", def = "{ 'teamId':1, 'automationId':1, 'createdAt':-1 }"),
        // Scanner: pick up queued manual runs due to fire (scheduledFireAt <= now)
        @CompoundIndex(name = "ar_queued_fire", def = "{ 'status':1, 'scheduledFireAt':1 }"),
        // Scanner: wake timed waits (optional lease hints)
        @CompoundIndex(name = "ar_wake_scan", def = "{ 'status':1, 'currentStep.type':1, 'currentStep.wakeAt':1, 'currentStep.leaseUntil':1 }"),
        // Event-driven wake for state waits
        @CompoundIndex(name = "ar_state_event", def = "{ 'teamId':1, 'status':1, 'currentStep.type':1, 'currentStep.waitingFor':1, 'targetIds':1 }"),
        // Timeout scan: step started and run is active
        @CompoundIndex(name = "ar_timeout_scan", def = "{ 'status':1, 'currentStep.startedAt':1 }")
})
@NoArgsConstructor
public class AutomationRun {

    @Id
    private String id;

    @Indexed
    private String automationId;

    @Indexed
    private String teamId;

    private AutomationRunStatus status = AutomationRunStatus.QUEUED;

    @Indexed
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    private Instant scheduledFireAt;

    @Indexed
    private Map<String, Boolean> targetIds = new HashMap<>();

    // State for the current step only (no historical list kept here)
    private StepState currentStep;

    // History of completed steps in order
    private List<StepState> stepHistory = new ArrayList<>();

    // If true, cancel execution at next safe point (start of next step
    // or immediately if waiting). Set by cancel endpoint/service.
    private Boolean cancelRequested = false;

    @Data
    public static class StepState {
        private AutomationStepType type;

        // Zero-based index of this step within the workflow definition
        private Integer workflowIndex;

        private Instant startedAt;
        private Instant finishedAt;

        private Instant leaseUntil;

        private Instant wakeAt;
        // For waits: if non-null, indicates the desired game server state to wait for
        private GameServerState waitingFor;

        private Map<String, Object> perTargetStatus;

        private Integer remainingCount;

        public AutomationStepStatus getStateOf(String targetId) {
            Object v = perTargetStatus != null ? perTargetStatus.get(targetId) : null;
            if (v == null) return null;
            if (v instanceof TargetStatus ts) return ts.getState();
            if (v instanceof java.util.Map<?, ?> map) {
                Object s = map.get("state");
                if (s instanceof AutomationStepStatus st) return st;
                if (s instanceof String str) {
                    try { return AutomationStepStatus.valueOf(str); } catch (Exception ignored) { return null; }
                }
                return null;
            }
            if (v instanceof String s) {
                try { return AutomationStepStatus.valueOf(s); } catch (Exception ignored) { return null; }
            }
            return null;
        }

        public void putStatus(String targetId, AutomationStepStatus state) {
            putStatus(targetId, state, null, null);
        }

        public void putStatus(String targetId, AutomationStepStatus state, String message, Instant completedAt) {
            if (perTargetStatus == null) perTargetStatus = new HashMap<>();
            TargetStatus ts = new TargetStatus();
            ts.setState(state);
            ts.setMessage(message);
            ts.setCompletedAt(completedAt);
            perTargetStatus.put(targetId, ts);
        }

        @Data
        public static class TargetStatus {
            private AutomationStepStatus state;
            private String message; // optional error/exception message on failure
            private Instant completedAt; // per-target completion timestamp
        }
    }

    // Size of the workflow (number of steps) captured at run creation
    private Integer workflowSize;

    // Ephemeral variables captured during the run (e.g., RCON responses)
    // Structure: vars[<name>][<serverId>] = response
    private Map<String, Map<String, String>> vars = new HashMap<>();
}
