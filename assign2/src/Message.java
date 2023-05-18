import java.io.Serializable;

public class Message implements Serializable {
    public enum MessageType {
        AUTHENTICATION, AUTHENTICATION_ACK, AUTHENTICATION_ERROR,
        QUEUE_REQUEST, QUEUE_ACK, QUEUE_ERROR,
        GAME_START, GAME_ACTION, GAME_UPDATE, GAME_END,
        ERROR, REGISTRATION_ACK, REGISTRATION_ERROR, REGISTRATION,RECONNECT_ACK,
        SELECT_GAME_MODE, GAME_MODE_ACK, GAME_MODE_ERROR,
        UPDATE_USER_LEVEL,

    }

    private MessageType messageType;
    private Object payload;

    public Message(MessageType messageType, Object payload) {
        this.messageType = messageType;
        this.payload = payload;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Object getPayload() {
        return payload;
    }
}
