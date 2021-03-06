/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.impl.bpmn.behavior;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntity;

public class IntermediateCatchMessageEventActivityBehavior extends IntermediateCatchEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected MessageEventDefinition messageEventDefinition;

    public IntermediateCatchMessageEventActivityBehavior(MessageEventDefinition messageEventDefinition) {
        this.messageEventDefinition = messageEventDefinition;
    }

    public void execute(DelegateExecution execution) {
        CommandContext commandContext = Context.getCommandContext();
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        String messageName = null;
        if (StringUtils.isNotEmpty(messageEventDefinition.getMessageRef())) {
            messageName = messageEventDefinition.getMessageRef();
        } else {
            Expression messageExpression = commandContext.getProcessEngineConfiguration().getExpressionManager()
                    .createExpression(messageEventDefinition.getMessageExpression());
            messageName = messageExpression.getValue(execution).toString();
        }

        commandContext.getEventSubscriptionEntityManager().insertMessageEvent(messageName, executionEntity);

        if (commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            commandContext.getProcessEngineConfiguration().getEventDispatcher()
                    .dispatchEvent(FlowableEventBuilder.createMessageEvent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, executionEntity.getActivityId(), messageName,
                            null, executionEntity.getId(), executionEntity.getProcessInstanceId(), executionEntity.getProcessDefinitionId()));
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        ExecutionEntity executionEntity = deleteMessageEventSubScription(execution);
        leaveIntermediateCatchEvent(executionEntity);
    }

    @Override
    public void eventCancelledByEventGateway(DelegateExecution execution) {
        deleteMessageEventSubScription(execution);
        Context.getCommandContext().getExecutionEntityManager().deleteExecutionAndRelatedData((ExecutionEntity) execution,
                DeleteReason.EVENT_BASED_GATEWAY_CANCEL, false);
    }

    protected ExecutionEntity deleteMessageEventSubScription(DelegateExecution execution) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        EventSubscriptionEntityManager eventSubscriptionEntityManager = Context.getCommandContext().getEventSubscriptionEntityManager();
        List<EventSubscriptionEntity> eventSubscriptions = executionEntity.getEventSubscriptions();
        for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
            if (eventSubscription instanceof MessageEventSubscriptionEntity && eventSubscription.getEventName().equals(messageEventDefinition.getMessageRef())) {

                eventSubscriptionEntityManager.delete(eventSubscription);
            }
        }
        return executionEntity;
    }
}
