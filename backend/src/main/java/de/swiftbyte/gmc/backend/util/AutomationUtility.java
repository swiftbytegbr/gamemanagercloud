package de.swiftbyte.gmc.backend.util;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationRunStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationTriggerType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.AutomationStep;
import de.swiftbyte.gmc.common.entity.GameServerState;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.HashMap;

/**
 * Utility helpers for automation scheduling and time calculations.
 */
public final class AutomationUtility {

    private static final ZoneId UTC = ZoneOffset.UTC;

    private AutomationUtility() {}

    /**
     * Calculates the next fire timestamp for a UTC cron expression.
     *
     * @param cronExpression Spring-style cron with seconds field (e.g. "0 0 2 * * *").
     * @param fromInstant    base instant to compute the next occurrence from (inclusive).
     * @return next occurrence as Instant, or null if none.
     * @throws IllegalArgumentException if the cron expression is invalid.
     */
    public static Instant nextCronFireUtc(String cronExpression, Instant fromInstant) {
        Objects.requireNonNull(cronExpression, "cronExpression");
        Objects.requireNonNull(fromInstant, "fromInstant");

        CronExpression cron = CronExpression.parse(cronExpression);
        ZonedDateTime base = fromInstant.atZone(UTC);
        ZonedDateTime next = cron.next(base);
        return next != null ? next.toInstant() : null;
    }

    /**
     * Calculates the next fire timestamp for a cron expression in the provided time zone.
     *
     * @param cronExpression Spring-style cron with seconds field.
     * @param fromInstant    base instant to compute the next occurrence from (inclusive).
     * @param zoneId         time zone to apply for cron evaluation.
     * @return next occurrence as Instant, or null if none.
     */
    public static Instant nextCronFire(String cronExpression, Instant fromInstant, ZoneId zoneId) {
        Objects.requireNonNull(cronExpression, "cronExpression");
        Objects.requireNonNull(fromInstant, "fromInstant");
        Objects.requireNonNull(zoneId, "zoneId");

        CronExpression cron = CronExpression.parse(cronExpression);
        ZonedDateTime base = fromInstant.atZone(zoneId);
        ZonedDateTime next = cron.next(base);
        return next != null ? next.toInstant() : null;
    }

    // Build a StepState for the given step index (type + zero-based workflowIndex; per-target statuses may be set later)
    public static AutomationRun.StepState buildStepStateFor(Automation automation, int stepIndex) {
        AutomationRun.StepState st = new AutomationRun.StepState();
        if (automation != null && automation.getWorkflow() != null
                && stepIndex >= 0 && stepIndex < automation.getWorkflow().size()) {
            AutomationStep s = automation.getWorkflow().get(stepIndex);
            if (s != null && s.getType() != null) {
                st.setType(s.getType());
            }
            // Store zero-based workflow index
            st.setWorkflowIndex(stepIndex);
        }
        return st;
    }

    // ---- Query builders for AutomationRun scanning/wakes ----

    /**
     * Builds a query to claim one due timed wait run: status=PENDING, type=TIMED_WAIT, wakeAt<=now, and lease missing/expired.
     */
    public static Query dueTimedWaitClaimQuery(Instant now) {
        Criteria base = new Criteria().andOperator(
                Criteria.where("status").is(AutomationRunStatus.PENDING),
                Criteria.where("currentStep.type").is(AutomationStepType.TIMED_WAIT),
                Criteria.where("currentStep.wakeAt").lte(now),
                new Criteria().orOperator(
                        Criteria.where("currentStep.leaseUntil").exists(false),
                        Criteria.where("currentStep.leaseUntil").lte(now)
                )
        );
        return Query.query(base);
    }

    /**
     * Builds a query to atomically update per-target status for state-waiting steps on server state changes.
     * Matches runs with status=PENDING, waitingFor=newState, current step type in one of the wait-capable
     * types (STATE_WAIT, START, STOP, RESTART, UPDATE), and where the target serverId is part of the run and
     * not globally skipped. Also avoids redundant writes for already SUCCEEDED targets.
     */
    public static Query stateWaitEventQuery(GameServerState newState, String serverId, String teamId) {
        Criteria base = new Criteria().andOperator(
                Criteria.where("teamId").is(teamId),
                Criteria.where("status").is(AutomationRunStatus.PENDING),
                Criteria.where("currentStep.type").in(
                        AutomationStepType.STATE_WAIT,
                        AutomationStepType.START,
                        AutomationStepType.STOP,
                        AutomationStepType.RESTART,
                        AutomationStepType.UPDATE
                ),
                Criteria.where("currentStep.waitingFor").is(newState),
                // target key exists and is not marked skipped (value != true)
                Criteria.where("targetIds." + serverId).ne(true),
                new Criteria().orOperator(
                        Criteria.where("currentStep.perTargetStatus." + serverId).exists(false),
                        Criteria.where("currentStep.perTargetStatus." + serverId + ".state").ne(AutomationStepStatus.SUCCEEDED)
                )
        );
        return Query.query(base);
    }

    /**
     * Builds a query to find Automations that should trigger on a server state change event.
     * Matches enabled automations in the team whose trigger is STATE_CHANGE, with matching newState
     * and containing the event serverId in trigger.serverIds.
     */
    public static Query stateChangeTriggerAutomationQuery(GameServerState newState, String serverId, String teamId) {
        Criteria base = new Criteria().andOperator(
                Criteria.where("enabled").is(true),
                Criteria.where("teamId").is(teamId),
                Criteria.where("trigger.type").is(AutomationTriggerType.STATE_CHANGE),
                Criteria.where("trigger.newState").is(newState),
                Criteria.where("trigger.serverIds").in(serverId)
        );
        return Query.query(base);
    }
}
