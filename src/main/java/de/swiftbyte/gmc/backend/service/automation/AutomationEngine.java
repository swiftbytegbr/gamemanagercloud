package de.swiftbyte.gmc.backend.service.automation;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationTriggerType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.TriggerSourceTargetSelector;
import de.swiftbyte.gmc.backend.domain.value.automation.trigger.ScheduledTrigger;
import de.swiftbyte.gmc.backend.events.server.ServerStateChangedEvent;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.infrastructure.packet.AutomationRunCreatedPacket;
import de.swiftbyte.gmc.backend.repository.AutomationRepository;
import de.swiftbyte.gmc.backend.repository.AutomationRunRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.util.AutomationUtility;
import de.swiftbyte.gmc.common.entity.GameServerState;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AutomationEngine {

    private final AutomationRunRepository runRepository;
    private final AutomationRepository automationRepository;
    private final MongoTemplate mongoTemplate;
    private final AutomationRunEngine automationRunEngine;

    @Autowired
    @Qualifier("automation_run_executor")
    private ThreadPoolTaskExecutor runExecutor;

    @Autowired
    @Qualifier("automation_executor")
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private StompSender stomp;

    @Autowired
    private TeamRepository teamRepository;

    /**
     * Claim one due scheduled trigger by setting a short lease and advancing nextFireAt.
     * Simpler service-layer implementation (no repository customization).
     */
    public Automation claimNextScheduledAutomation() {
        Instant now = Instant.now();

        // First, find an enabled automation with a due scheduled trigger and free lease
        Query q = Query.query(Criteria.where("enabled").is(true)
                .and("trigger.type").is(AutomationTriggerType.SCHEDULED)
                .and("trigger.nextFireAt").lte(now)
                .orOperator(Criteria.where("trigger.leaseUntil").exists(false), Criteria.where("trigger.leaseUntil").lte(now))
        ).limit(1);

        Update update = new Update()
                .set("trigger.leaseUntil", now.plusSeconds(5));

        return  mongoTemplate.findAndModify(q, update, FindAndModifyOptions.options().returnNew(true),Automation.class);
    }

    public Automation claimNextTriggeredAutomation(String serverId, GameServerState gameServerState) {
        Query q = Query.query(new Criteria().andOperator(
                Criteria.where("enabled").is(true),
                Criteria.where("trigger.type").is(AutomationTriggerType.STATE_CHANGE),
                Criteria.where("trigger.newState").is(gameServerState),
                Criteria.where("trigger.serverIds").in(serverId)
        )).limit(1);
        return mongoTemplate.findOne(q, Automation.class);
    }

    // --- Scheduled scanner: pick up due scheduled automations every 10s ---
    @Scheduled(fixedRate = 10_000)
    public void scanDueScheduledAutomations() {
        while (true) {
            Automation claimed = claimNextScheduledAutomation();
            if (claimed == null) break;

            executor.execute(() -> {
                // Capture the due time before advancing nextFireAt
                Instant due = null;
                if (claimed.getTrigger() instanceof ScheduledTrigger st) {
                    due = st.getNextFireAt();
                    Instant next = AutomationUtility.nextCronFireUtc(st.getCron(), Instant.now());
                    Update up = new Update()
                            .set("trigger.nextFireAt", next)
                            .unset("trigger.leaseUntil");
                    mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(claimed.getId())), up, Automation.class);
                }

                // Create and start a run (due may be null if data malformed)
                Instant scheduledFireAt = due != null ? due : Instant.now();
                createRunFromAutomation(claimed, scheduledFireAt);
            });
        }
    }


    public AutomationRun createRunFromAutomation(Automation automation, Instant scheduledFireAt) {
        return createRunFromAutomation(automation, scheduledFireAt, null, false);
    }

    public AutomationRun createRunFromAutomation(Automation automation, Instant scheduledFireAt, List<String> overrideTargetIds, boolean deferStart) {
        AutomationRun run = new AutomationRun();
        run.setAutomationId(automation.getId());
        run.setTeamId(automation.getTeamId());
        run.setCreatedAt(Instant.now());
        run.setScheduledFireAt(scheduledFireAt);
        int wfSize = (automation.getWorkflow() != null) ? automation.getWorkflow().size() : 0;
        run.setWorkflowSize(wfSize);
        // Initialize current step state for step index 0
        run.setCurrentStep(AutomationUtility.buildStepStateFor(automation, 0));
        // If override targets provided, set them now as map (skipped=false); otherwise startRun will resolve
        if (overrideTargetIds != null && !overrideTargetIds.isEmpty()) {
            HashMap<String, Boolean> map = new HashMap<>();
            for (String id : overrideTargetIds) map.put(id, Boolean.FALSE);
            run.setTargetIds(map);
        }
        AutomationRun queuedRun = automationRunEngine.updateRun(run);
        // Broadcast run creation to team subscribers
        try {
            AutomationRunCreatedPacket pkt = new AutomationRunCreatedPacket();
            pkt.setRun(queuedRun);
            stomp.sendToTeam(teamRepository.findById(queuedRun.getTeamId()).orElseThrow(), "/automation-run/create", pkt);
        } catch (Exception ignored) { }

        if (!deferStart) {
            runExecutor.execute(() -> automationRunEngine.startRun(queuedRun));
        }
        return queuedRun;
    }

    // --- Event listener: create runs for matching state-change triggers ---
    @Async
    @EventListener(ServerStateChangedEvent.class)
    public void onServerStateChangedCreateRuns(ServerStateChangedEvent event) {
        String teamId = event.getServer().getTeamId();
        GameServerState newState = event.getNewState();

        Query q = AutomationUtility.stateChangeTriggerAutomationQuery(newState, event.getServer().getId(), teamId);

        List<Automation> autos = mongoTemplate.find(q, Automation.class);
        for (Automation a : autos) {
            try {
                if (a.getTargets() instanceof TriggerSourceTargetSelector) {
                    // Limit targets to the triggering server
                    createRunFromAutomation(a, null, java.util.List.of(event.getServer().getId()), false);
                } else {
                    createRunFromAutomation(a, null);
                }
            } catch (Exception ignored) { }
        }
    }

    // --- Helpers to create/trigger automations ---
    public Automation createAutomation(Automation automation) {
        Instant now = Instant.now();
        automation.setCreatedAt(now);
        automation.setUpdatedAt(now);
        // If scheduled trigger, compute initial nextFireAt
        if (automation.getTrigger() instanceof ScheduledTrigger st) {
            Instant next = AutomationUtility.nextCronFireUtc(st.getCron(), now);
            st.setNextFireAt(next);
        }
        return automationRepository.save(automation);
    }

    public AutomationRun triggerManualRun(String automationId) {
        Automation automation = automationRepository.findById(automationId).orElseThrow();
        // For manual triggers, we simply create a run now
        return createRunFromAutomation(automation, null);
    }


}
