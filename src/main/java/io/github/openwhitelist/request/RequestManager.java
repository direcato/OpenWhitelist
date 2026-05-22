package io.github.openwhitelist.request;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RequestManager {

    private final List<PendingRequest> requests;

    public RequestManager() {
        this.requests = new ArrayList<>();
    }

    public void add(String name, UUID uuid) {
        removeExpired();
        requests.removeIf(r -> r.getName().equalsIgnoreCase(name));
        requests.add(new PendingRequest(name, uuid));
    }

    public Optional<PendingRequest> get(String name) {
        removeExpired();
        return requests.stream()
            .filter(r -> r.getName().equalsIgnoreCase(name) && !r.isExpired())
            .findFirst();
    }

    public boolean remove(String name) {
        return requests.removeIf(r -> r.getName().equalsIgnoreCase(name));
    }

    public List<PendingRequest> getAll() {
        removeExpired();
        return List.copyOf(requests);
    }

    public boolean hasRequest(String name) {
        return requests.stream()
            .anyMatch(r -> r.getName().equalsIgnoreCase(name) && !r.isExpired());
    }

    public void removeExpired() {
        requests.removeIf(PendingRequest::isExpired);
    }

    public int getCount() {
        removeExpired();
        return requests.size();
    }
}
