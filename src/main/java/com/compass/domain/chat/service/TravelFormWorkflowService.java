package com.compass.domain.chat.service;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.orchestrator.ContextManager;
import com.compass.domain.chat.orchestrator.PhaseManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TravelFormWorkflowService {

    private final TravelInfoService travelInfoService;
    private final PhaseManager phaseManager;
    private final ContextManager contextManager;

    @Transactional
    public void persistFormData(TravelContext context,
                                String threadId,
                                TravelFormSubmitRequest formRequest,
                                boolean transitionToPlanGeneration) {
        travelInfoService.saveTravelInfo(threadId, formRequest);
        if (transitionToPlanGeneration) {
            phaseManager.savePhase(threadId, TravelPhase.PLAN_GENERATION);
        }
        contextManager.updateContext(context, context.getUserId());
    }
}
