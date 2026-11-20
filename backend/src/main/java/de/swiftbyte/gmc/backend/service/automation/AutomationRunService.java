package de.swiftbyte.gmc.backend.service.automation;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationRunStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.exception.AutomationException;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.infrastructure.packet.AutomationRunDeletedPacket;
import de.swiftbyte.gmc.backend.infrastructure.packet.AutomationRunUpdatedPacket;
import de.swiftbyte.gmc.backend.repository.AutomationRunRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import de.swiftbyte.gmc.backend.service.NotificationService;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import de.swiftbyte.gmc.common.model.NotificationSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;

@Service
public class AutomationRunService {

    @Autowired
    private AutomationRunRepository runRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private StompSender stomp;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private NotificationService notificationService;

    public AutomationRun getOrThrow(String runId) {
        return runRepository.findById(runId).orElseThrow();
    }

    public Page<AutomationRun> listByTeam(String teamId, String automationId, String statuses,
                                          String createdFrom, String createdTo,
                                          int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 200) size = 200;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Criteria c = Criteria.where("teamId").is(teamId);
        if (automationId != null && !automationId.isBlank()) {
            c = new Criteria().andOperator(c, Criteria.where("automationId").is(automationId));
        }

        // Status filter: CSV or single value
        java.util.List<de.swiftbyte.gmc.backend.domain.enums.automation.AutomationRunStatus> statusList = new java.util.ArrayList<>();
        if (statuses != null && !statuses.isBlank()) {
            for (String s : statuses.split(",")) {
                String t = s.trim();
                if (t.isEmpty()) continue;
                try {
                    statusList.add(de.swiftbyte.gmc.backend.domain.enums.automation.AutomationRunStatus.valueOf(t));
                } catch (Exception ignored) { }
            }
        }
        if (!statusList.isEmpty()) {
            c = new Criteria().andOperator(c, Criteria.where("status").in(statusList));
        }

        java.time.Instant from = null, to = null;
        try { if (createdFrom != null && !createdFrom.isBlank()) from = java.time.Instant.parse(createdFrom); } catch (Exception ignored) { }
        try { if (createdTo != null && !createdTo.isBlank()) to = java.time.Instant.parse(createdTo); } catch (Exception ignored) { }
        if (from != null && to != null) {
            c = new Criteria().andOperator(c, Criteria.where("createdAt").gte(from).lte(to));
        } else if (from != null) {
            c = new Criteria().andOperator(c, Criteria.where("createdAt").gte(from));
        } else if (to != null) {
            c = new Criteria().andOperator(c, Criteria.where("createdAt").lte(to));
        }

        Query q = new Query(c).with(pageable);
        java.util.List<AutomationRun> items = mongoTemplate.find(q, AutomationRun.class);

        // Count total without pagination
        Query countQ = new Query(c);
        long total = mongoTemplate.count(countQ, AutomationRun.class);
        return new org.springframework.data.domain.PageImpl<>(items, pageable, total);
    }

    public void deleteIfTerminal(String runId) {
        AutomationRun run = runRepository.findById(runId).orElseThrow();
        AutomationRunStatus st = run.getStatus();
        boolean terminal = (st == AutomationRunStatus.SUCCEEDED
                || st == AutomationRunStatus.FAILED
                || st == AutomationRunStatus.CANCELED
                || st == AutomationRunStatus.TIMEOUTED);
        if (!terminal) {
            throw new AutomationException("run_not_deletable");
        }
        runRepository.deleteById(runId);
        AutomationRunDeletedPacket pkt = new AutomationRunDeletedPacket();
        pkt.setRunId(runId);
        pkt.setTeamId(run.getTeamId());
        stomp.sendToTeam(teamRepository.findById(run.getTeamId()).orElseThrow(), "/automation-run/delete", pkt);
    }

    public AutomationRun cancel(String runId) {
        AutomationRun run = runRepository.findById(runId).orElseThrow();
        AutomationRunStatus st = run.getStatus();
        if (st == AutomationRunStatus.SUCCEEDED || st == AutomationRunStatus.FAILED
                || st == AutomationRunStatus.CANCELED || st == AutomationRunStatus.TIMEOUTED) {
            return run; // idempotent no-op for terminal runs
        }

        // If currently waiting (timed wait or state wait), cancel instantly
        AutomationRun.StepState step = run.getCurrentStep();
        if (step != null && (step.getType() == AutomationStepType.TIMED_WAIT
                || step.getType() == AutomationStepType.STATE_WAIT
                || step.getWaitingFor() != null)) {
            if (run.getTargetIds() != null) {
                for (String targetId : run.getTargetIds().keySet()) {
                    AutomationRun.StepState.TargetStatus ts = new AutomationRun.StepState.TargetStatus();
                    ts.setState(AutomationStepStatus.SKIPPED);
                    ts.setCompletedAt(Instant.now());
                    step.getPerTargetStatus().put(targetId, ts);
                }
            }
            step.setWakeAt(null);
            step.setLeaseUntil(null);
            step.setWaitingFor(null);
            step.setFinishedAt(Instant.now());
            run.getStepHistory().add(step);
            run.setCurrentStep(null);
            run.setStatus(AutomationRunStatus.CANCELED);
            run.setFinishedAt(Instant.now());
        } else {
            // Otherwise, request cancellation; engine will stop at next step boundary
            run.setCancelRequested(true);
        }

        AutomationRun saved = runRepository.save(run);
        try {
            AutomationRunUpdatedPacket pkt = new AutomationRunUpdatedPacket();
            pkt.setRun(saved);
            stomp.sendToTeam(teamRepository.findById(saved.getTeamId()).orElseThrow(), "/automation-run/update", pkt);
        } catch (Exception ignored) { }
        // Notify if we canceled immediately in this call
        if (run.getStatus() == AutomationRunStatus.CANCELED && st != AutomationRunStatus.CANCELED) {
            try {
                String title = "Automation run canceled";
                String msg = String.format("Automation (id=%s) run %s was canceled.",
                        saved.getAutomationId(), saved.getId());
                notificationService.create(saved.getTeamId(),
                        "AUTOMATION_RUN_CANCELED",
                        NotificationSeverity.INFO,
                        title,
                        msg,
                        NotificationSource.BACKEND);
            } catch (Exception ignored) { }
        }
        return saved;
    }
}
