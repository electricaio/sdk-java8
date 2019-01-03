package io.electrica.sdk.java.core.message;

import io.electrica.sdk.java.core.dto.MessageDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookInboundMessage extends InboundMessage {

    private MessageDto data;

}
