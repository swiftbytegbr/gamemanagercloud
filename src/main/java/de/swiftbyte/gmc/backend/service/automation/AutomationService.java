package de.swiftbyte.gmc.backend.service.automation;

import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.ExplicitTargetSelector;
import de.swiftbyte.gmc.backend.domain.value.automation.TargetSelector;
import de.swiftbyte.gmc.backend.domain.value.automation.TriggerSourceTargetSelector;
import de.swiftbyte.gmc.backend.domain.value.automation.trigger.ScheduledTrigger;
import de.swiftbyte.gmc.backend.domain.value.automation.trigger.StateChangeTrigger;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.AutomationStep;
import de.swiftbyte.gmc.backend.dto.request.AutomationCreateRequest;
import de.swiftbyte.gmc.backend.dto.request.AutomationUpdateRequest;
import de.swiftbyte.gmc.backend.exception.AutomationException;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.infrastructure.packet.AutomationCreatedPacket;
import de.swiftbyte.gmc.backend.infrastructure.packet.AutomationDeletedPacket;
import de.swiftbyte.gmc.backend.infrastructure.packet.AutomationUpdatedPacket;
import de.swiftbyte.gmc.backend.repository.AutomationRepository;
import de.swiftbyte.gmc.backend.repository.ServerRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.util.AutomationConstraints;
import de.swiftbyte.gmc.backend.util.AutomationUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AutomationService {

    @Autowired
    private AutomationRepository automationRepository;

    @Autowired
    private AutomationEngine automationEngine;

    @Autowired
    private StompSender stomp;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ServerRepository serverRepository;

    public List<Automation> findByTeam(String teamId) {
        // Prefer updatedAt desc for stable ordering
        return automationRepository.findByTeamIdOrderByUpdatedAtDesc(teamId);
    }


    public Automation create(AutomationCreateRequest request) {
        Automation automation = new Automation();
        automation.setTeamId(request.getTeamId());
        automation.setName(request.getName());
        automation.setDescription(request.getDescription());
        automation.setEnabled(request.isEnabled());
        automation.setTargets(request.getTargets());
        automation.setWorkflow(request.getWorkflow());
        automation.setTrigger(request.getTrigger());
        automation.setRunTimeoutSec(request.getRunTimeoutSec());

        applyDefaults(automation);
        // Validate explicit references belong to the team
        validateAutomationTeamReferences(automation);
        Automation saved = automationEngine.createAutomation(automation);
        AutomationCreatedPacket pkt = new AutomationCreatedPacket();
        pkt.setAutomation(saved);
        stomp.sendToTeam(teamRepository.findById(saved.getTeamId()).orElseThrow(), "/automation/create", pkt);
        return saved;
    }

    public Automation update(String automationId, AutomationUpdateRequest request) {
        Automation existing = automationRepository.findById(automationId).orElseThrow();

        // Apply mutable fields
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setEnabled(request.isEnabled());
        existing.setTargets(request.getTargets());
        existing.setTrigger(request.getTrigger());
        existing.setWorkflow(request.getWorkflow());
        existing.setRunTimeoutSec(request.getRunTimeoutSec());
        existing.setUpdatedAt(Instant.now());

        applyDefaults(existing);
        validateAutomationTeamReferences(existing);

        // If scheduled, recompute next fire timestamp from now
        if (existing.getTrigger() instanceof ScheduledTrigger st) {
            st.setNextFireAt(AutomationUtility.nextCronFireUtc(st.getCron(), Instant.now()));
        }
        Automation saved = automationRepository.save(existing);
        AutomationUpdatedPacket pkt = new AutomationUpdatedPacket();
        pkt.setAutomation(saved);
        stomp.sendToTeam(teamRepository.findById(saved.getTeamId()).orElseThrow(), "/automation/update", pkt);
        return saved;
    }

    public void delete(String automationId) {
        // Load to get team id for broadcast
        Automation existing = automationRepository.findById(automationId).orElseThrow();
        automationRepository.deleteById(automationId);
        AutomationDeletedPacket pkt = new AutomationDeletedPacket();
        pkt.setAutomationId(automationId);
        pkt.setTeamId(existing.getTeamId());
        stomp.sendToTeam(teamRepository.findById(existing.getTeamId()).orElseThrow(), "/automation/delete", pkt);
    }

    public Automation getOrThrow(String automationId) {
        return automationRepository.findById(automationId).orElseThrow();
    }


    public AutomationRun triggerManual(String automationId, List<String> targetIds, Instant nextFireAt) {
        // We need the full automation including workflow for run creation
        Automation automation = automationRepository.findById(automationId).orElseThrow();

        // Disallow manual triggering if targets are derived from trigger-source
        if (automation.getTargets() instanceof TriggerSourceTargetSelector) {
            throw new AutomationException("targets_trigger_source_manual_forbidden");
        }

        // Optional target override validation against team
        if (targetIds != null && !targetIds.isEmpty()) {
            long ok = serverRepository.countByIdInAndTeamId(targetIds, automation.getTeamId());
            if (ok != targetIds.size()) {
                throw new AutomationException("targets_invalid_team");
            }
        }

        // If nextFireAt provided, queue run for later; otherwise start immediately
        if (nextFireAt != null) {
            return automationEngine.createRunFromAutomation(automation, nextFireAt, targetIds, true);
        }
        return automationEngine.createRunFromAutomation(automation, null, targetIds, false);
    }

    private void validateAutomationTeamReferences(Automation automation) {
        String teamId = automation.getTeamId();
        if (teamId == null || teamId.isBlank()) return;

        // Validate explicit target selector server IDs belong to the team
        TargetSelector sel = automation.getTargets();
        if (sel instanceof ExplicitTargetSelector ex && ex.getIds() != null && !ex.getIds().isEmpty()) {
            long ok = serverRepository.countByIdInAndTeamId(ex.getIds(), teamId);
            if (ok != ex.getIds().size()) {
                throw new AutomationException("targets_invalid_team");
            }
        }

        // Validate trigger-source selector is only used with STATE_CHANGE trigger
        if (sel instanceof TriggerSourceTargetSelector) {
            if (!(automation.getTrigger() instanceof StateChangeTrigger)) {
                throw new AutomationException("targets_trigger_source_only_for_state_change");
            }
        }

        // Validate state-change trigger serverIds belong to the team
        if (automation.getTrigger() instanceof StateChangeTrigger st && st.getServerIds() != null && !st.getServerIds().isEmpty()) {
            long ok = serverRepository.countByIdInAndTeamId(st.getServerIds(), teamId);
            if (ok != st.getServerIds().size()) {
                throw new AutomationException("trigger_server_ids_invalid_team");
            }
        }
    }

    private void applyDefaults(Automation automation) {
        // Default run timeout to 1 day if null
        if (automation.getRunTimeoutSec() == null) {
            automation.setRunTimeoutSec(AutomationConstraints.DEFAULT_TIMEOUT_SECONDS);
        }
        // Default step timeouts to 1 day if null
        if (automation.getWorkflow() != null) {
            for (AutomationStep step : automation.getWorkflow()) {
                if (step.getTimeoutSec() == null) {
                    step.setTimeoutSec(AutomationConstraints.DEFAULT_TIMEOUT_SECONDS);
                }
            }
        }
    }
}
