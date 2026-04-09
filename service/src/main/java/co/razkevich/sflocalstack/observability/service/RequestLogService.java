package co.razkevich.sflocalstack.observability.service;

import co.razkevich.sflocalstack.observability.model.RequestLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RequestLogService {

    private static final Logger log = LoggerFactory.getLogger(RequestLogService.class);
    private static final int MAX_ENTRIES = 1000;

    private final Deque<RequestLogEntry> buffer = new ArrayDeque<>(MAX_ENTRIES);
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public RequestLogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized void log(RequestLogEntry entry) {
        if (buffer.size() >= MAX_ENTRIES) {
            buffer.pollFirst();
        }
        buffer.addLast(entry);
        broadcast(entry);
    }

    public synchronized List<RequestLogEntry> getRecent(int count) {
        List<RequestLogEntry> all = new ArrayList<>(buffer);
        int from = Math.max(0, all.size() - count);
        return all.subList(from, all.size());
    }

    public synchronized int size() {
        return buffer.size();
    }

    public synchronized void reset() {
        buffer.clear();
    }

    public SseEmitter newEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        log.debug("New SSE subscriber. Total: {}", emitters.size());
        return emitter;
    }

    private void broadcast(RequestLogEntry entry) {
        if (emitters.isEmpty()) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(entry);
        } catch (Exception e) {
            log.error("Failed to serialize RequestLogEntry", e);
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(json));
            } catch (Exception e) {
                log.debug("Removing dead SSE emitter: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
