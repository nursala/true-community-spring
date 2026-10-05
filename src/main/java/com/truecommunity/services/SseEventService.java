package com.truecommunity.services;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shared Server-Sent Events registry. Subscribers are grouped by channel name,
 * and the copy-on-write emitter lists allow concurrent publish and cleanup
 * callbacks without external synchronization.
 */
@Service
public class SseEventService {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emittersByChannel = new ConcurrentHashMap<>();

    /**
     * Opens an emitter for the requested channel and registers cleanup handlers
     * for normal completion, timeout, and errors.
     *
     * @param channel the logical channel name
     * @return the registered emitter
     */
    public SseEmitter subscribe(String channel) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emittersByChannel.computeIfAbsent(channel, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(channel, emitter));
        emitter.onTimeout(() -> remove(channel, emitter));
        emitter.onError(error -> remove(channel, emitter));
        return emitter;
    }

    /**
     * Sends an event to every active emitter on the channel. Failed emitters are
     * removed immediately so stale connections do not leak.
     *
     * @param channel the logical channel name
     * @param eventName the SSE event name
     * @param payload the event payload
     */
    public void publish(String channel, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByChannel.get(channel);
        if (emitters == null) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException ex) {
                remove(channel, emitter);
            }
        }
    }

    private void remove(String channel, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByChannel.get(channel);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByChannel.remove(channel, emitters);
        }
    }
}
