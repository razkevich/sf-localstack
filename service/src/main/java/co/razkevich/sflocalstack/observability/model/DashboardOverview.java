package co.razkevich.sflocalstack.observability.model;

import java.util.List;

public record DashboardOverview(
        String service,
        String status,
        String apiVersion,
        int totalRecords,
        int recentRequestCount,
        List<ObjectCount> objectCounts
) {
    public record ObjectCount(String objectType, int count) {}
}
