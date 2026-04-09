package co.razkevich.sflocalstack.data.service;

import co.razkevich.sflocalstack.data.model.SObjectRecord;

public record UpsertResult(SObjectRecord record, boolean created) {
}
